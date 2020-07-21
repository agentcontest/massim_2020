import { Redraw, ReplayCtrl, ViewModel, Pos, MapCtrl } from './interfaces';
import { makeMapCtrl } from './map';

export class Ctrl {
  readonly vm: ViewModel;
  readonly replay: ReplayCtrl | undefined;
  readonly map: MapCtrl;

  constructor(readonly redraw: Redraw, replayPath?: string) {
    this.vm = {
      'state': 'connecting',
    };

    this.replay = replayPath ? this.makeReplayCtrl(replayPath) : undefined;
    if (!this.replay) this.connect();

    this.map = makeMapCtrl(this, redraw);
  }

  private connect(): void {
    const protocol = document.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const path = document.location.pathname.substr(0, document.location.pathname.lastIndexOf('/'));
    const ws = new WebSocket(protocol + '//' + document.location.host + path + '/live/monitor');

    ws.onmessage = (msg) => {
      const data = JSON.parse(msg.data);
      console.log(data);
      if (data.grid) {
        data.blockTypes.sort();
        this.vm.static = data;
      }
      else this.vm.dynamic = data;
      this.redraw();
    };

    ws.onopen = () => {
      console.log('Connected');
      this.vm.state = 'online';
      this.redraw();
    };

    ws.onclose = () => {
      console.log('Disconnected');
      setTimeout(this.connect, 5000);
      this.vm.state = 'offline';
      this.redraw();
    };
  }

  private makeReplayCtrl(path: string): ReplayCtrl {
    if (path[path.length - 1] == '/') path = path.substr(0, path.length - 1);
    const suffix = location.pathname == '/' ? `?sri=${Math.random().toString(36).slice(-8)}` : '';

    var step = -1;
    var timer: NodeJS.Timeout | undefined = undefined;

    var cache: any = {};
    var cacheSize = 0;

    const stop = () => {
      if (timer) clearInterval(timer);
      timer = undefined;
      this.redraw();
    }

    const start = () => {
      if (!timer) timer = setInterval(() => {
        if (this.vm.state !== 'connecting') setStep(step + 1);
      }, 1000);
      this.redraw();
    }

    const loadStatic = () => {
      const xhr = new XMLHttpRequest();
      xhr.open('GET', `${path}/static.json${suffix}`);
      xhr.onload = () => {
        if (xhr.status === 200) {
          this.vm.static = JSON.parse(xhr.responseText);
          setStep(step);
        } else {
          this.vm.state = 'error';
        }
        this.redraw();
      };
      xhr.onerror = () => {
        this.vm.state = 'error';
        this.redraw();
      };
      xhr.send();
    }

    const loadDynamic = (step: number) => {
      // got from cache
      if (cache[step]) {
        this.vm.dynamic = cache[step];
        this.vm.state = (this.vm.dynamic && this.vm.dynamic.step == step) ? 'online' : 'connecting';
        this.redraw();
        return;
      }

      const group = step > 0 ? Math.floor(step / 5) * 5 : 0;
      const xhr = new XMLHttpRequest();
      xhr.open('GET', `${path}/${group}.json${suffix}`);
      xhr.onload = () => {
        if (xhr.status === 200) {
          var response = JSON.parse(xhr.responseText);
          this.vm.dynamic = response[step];
          this.vm.state = (this.vm.dynamic && this.vm.dynamic.step == step) ? 'online' : 'connecting';

          // write to cache
          if (cacheSize > 100) {
            cache = {};
            cacheSize = 0;
          }
          for (var s in response) {
            cache[s] = response[s];
            cacheSize++;
          }
        } else {
          this.vm.state = 'error';
          stop();
        }
        this.redraw();
      };
      xhr.onerror = () => {
        this.vm.state = 'error';
        stop();
        this.redraw();
      };
      xhr.send();
    }

    const setStep = (s: number) => {
      // keep step in bounds
      step = Math.max(-1, s);
      if (this.vm.static && step >= this.vm.static.steps) {
        stop();
        step = this.vm.static.steps - 1;
      }

      // show connecting after a while
      this.vm.state = 'connecting';
      setTimeout(() => this.redraw(), 500);

      // update url
      if (history.replaceState) history.replaceState({}, document.title, '#' + step);

      loadDynamic(step);
    }

    loadStatic();

    return {
      name: function() {
        const parts = path.split('/');
        return parts[parts.length - 1];
      },
      step: function() {
        return step;
      },
      setStep,
      toggle: function() {
        if (timer) stop();
        else start();
      },
      stop,
      start,
      playing: function() {
        return !!timer;
      }
    };
  }

  setHover(pos?: Pos) {
    const changed = (!pos && this.vm.hover) || (pos && !this.vm.hover) || (pos && this.vm.hover && (pos.x != this.vm.hover.x || pos.y != this.vm.hover.y));
    this.vm.hover = pos;
    if (changed) this.redraw();
  }
}
