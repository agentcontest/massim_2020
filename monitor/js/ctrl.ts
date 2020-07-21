import { Redraw, ViewModel, Pos, MapCtrl } from './interfaces';
import { makeMapCtrl } from './map';

export class Ctrl {
  readonly vm: ViewModel;
  readonly replay: ReplayCtrl | undefined;
  readonly map: MapCtrl;

  constructor(readonly redraw: Redraw, replayPath?: string) {
    this.vm = {
      'state': 'connecting',
    };

    if (replayPath) this.replay = new ReplayCtrl(this, replayPath);
    else this.connect();

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

  setHover(pos?: Pos) {
    const changed = (!pos && this.vm.hover) || (pos && !this.vm.hover) || (pos && this.vm.hover && (pos.x != this.vm.hover.x || pos.y != this.vm.hover.y));
    this.vm.hover = pos;
    if (changed) this.redraw();
  }
}

export class ReplayCtrl {
  public step = -1;

  private suffix: string;
  private timer: NodeJS.Timeout | undefined;

  private cache = new Map<number, any>();

  constructor(readonly root: Ctrl, readonly path: string) {
    if (path[path.length - 1] == '/') path = path.substr(0, path.length - 1);
    this.suffix = location.pathname == '/' ? `?sri=${Math.random().toString(36).slice(-8)}` : '';

    this.loadStatic();
  }

  stop() {
    if (this.timer) clearInterval(this.timer);
    this.timer = undefined;
    this.root.redraw();
  }

  start() {
    if (!this.timer) this.timer = setInterval(() => {
      if (this.root.vm.state !== 'connecting') this.setStep(this.step + 1);
    }, 1000);
    this.root.redraw();
  }

  loadStatic() {
    const xhr = new XMLHttpRequest();
    xhr.open('GET', `${this.path}/static.json${this.suffix}`);
    xhr.onload = () => {
      if (xhr.status === 200) {
        this.root.vm.static = JSON.parse(xhr.responseText);
        this.setStep(this.step);
      } else {
        this.root.vm.state = 'error';
      }
      this.root.redraw();
    };
    xhr.onerror = () => {
      this.root.vm.state = 'error';
      this.root.redraw();
    };
    xhr.send();
  }

  loadDynamic(step: number) {
    // got from cache
    const entry = this.cache.get(step);
    if (entry) {
      this.root.vm.dynamic = entry;
      this.root.vm.state = (this.root.vm.dynamic && this.root.vm.dynamic.step == step) ? 'online' : 'connecting';
      this.root.redraw();
      return;
    }

    const group = step > 0 ? Math.floor(step / 5) * 5 : 0;
    const xhr = new XMLHttpRequest();
    xhr.open('GET', `${this.path}/${group}.json${this.suffix}`);
    xhr.onload = () => {
      if (xhr.status === 200) {
        var response = JSON.parse(xhr.responseText);
        this.root.vm.dynamic = response[step];
        this.root.vm.state = (this.root.vm.dynamic && this.root.vm.dynamic.step == step) ? 'online' : 'connecting';

        // write to cache
        if (this.cache.size > 100) this.cache.clear();
        for (const s in response) this.cache.set(parseInt(s), response[s]);
      } else {
        this.root.vm.state = 'error';
        this.stop();
      }
      this.root.redraw();
    };
    xhr.onerror = () => {
      this.root.vm.state = 'error';
      this.stop();
      this.root.redraw();
    };
    xhr.send();
  }

  setStep(s: number) {
    // keep step in bounds
    this.step = Math.max(-1, s);
    if (this.root.vm.static && this.step >= this.root.vm.static.steps) {
      this.stop();
      this.step = this.root.vm.static.steps - 1;
    }

    // show connecting after a while
    this.root.vm.state = 'connecting';
    setTimeout(() => this.root.redraw(), 500);

    // update url
    if (history.replaceState) history.replaceState({}, document.title, '#' + this.step);

    this.loadDynamic(this.step);
  }

  name() {
    const parts = this.path.split('/');
    return parts[parts.length - 1];
  }

  toggle() {
    if (this.timer) this.stop();
    else this.start();
  }

  playing() {
    return !!this.timer;
  }
}
