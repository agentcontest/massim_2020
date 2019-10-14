import { Redraw, Ctrl, ReplayCtrl, ViewModel, Pos } from './interfaces';

export default function(redraw: Redraw, replayPath?: string): Ctrl {
  const vm: ViewModel = {
    state: 'connecting'
  };


  function connect() {
    const protocol = document.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const path = document.location.pathname.substr(0, document.location.pathname.lastIndexOf('/'));
    const ws = new WebSocket(protocol + '//' + document.location.host + path + '/live/monitor');

    ws.onmessage = (msg) => {
      const data = JSON.parse(msg.data);
      console.log(data);
      if (data.grid) {
        data.blockTypes.sort();
        vm.static = data;
      }
      else vm.dynamic = data;
      redraw();
    };

    ws.onopen = () => {
      console.log('Connected');
      vm.state = 'online';
      redraw();
    };

    ws.onclose = () => {
      console.log('Disconnected');
      setTimeout(connect, 5000);
      vm.state = 'offline';
      redraw();
    };
  }

  const makeReplayCtrl = function(path: string): ReplayCtrl {
    if (path[path.length - 1] == '/') path = path.substr(0, path.length - 1);
    const suffix = location.pathname == '/' ? `?sri=${Math.random().toString(36).slice(-8)}` : '';

    var step = -1;
    var timer: number | undefined = undefined;

    var cache: any = {};
    var cacheSize = 0;


    function stop() {
      if (timer) clearInterval(timer);
      timer = undefined;
      redraw();
    }

    function start() {
      if (!timer) timer = setInterval(function() {
        if (vm.state !== 'connecting') setStep(step + 1);
      }, 1000);
      redraw();
    }

    function loadStatic() {
      const xhr = new XMLHttpRequest();
      xhr.open('GET', `${path}/static.json${suffix}`);
      xhr.onload = function() {
        if (xhr.status === 200) {
          vm.static = JSON.parse(xhr.responseText);
          setStep(step);
        } else {
          vm.state = 'error';
        }
        redraw();
      };
      xhr.onerror = function() {
        vm.state = 'error';
        redraw();
      };
      xhr.send();
    }

    function loadDynamic(step: number) {
      // got from cache
      if (cache[step]) {
        vm.dynamic = cache[step];
        vm.state = (vm.dynamic && vm.dynamic.step == step) ? 'online' : 'connecting';
        redraw();
        return;
      }

      const group = step > 0 ? Math.floor(step / 5) * 5 : 0;
      const xhr = new XMLHttpRequest();
      xhr.open('GET', `${path}/${group}.json${suffix}`);
      xhr.onload = function() {
        if (xhr.status === 200) {
          var response = JSON.parse(xhr.responseText);
          vm.dynamic = response[step];
          vm.state = (vm.dynamic && vm.dynamic.step == step) ? 'online' : 'connecting';

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
          vm.state = 'error';
          stop();
        }
        redraw();
      };
      xhr.onerror = function() {
        vm.state = 'error';
        stop();
        redraw();
      };
      xhr.send();
    }

    function setStep(s: number) {
      // keep step in bounds
      step = Math.max(-1, s);
      if (vm.static && step >= vm.static.steps) {
        stop();
        step = vm.static.steps - 1;
      }

      // show connecting after a while
      vm.state = 'connecting';
      setTimeout(() => redraw(), 500);

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
  };

  const replay = replayPath ? makeReplayCtrl(replayPath) : undefined;
  if (!replay) connect();

  return {
    replay,
    vm,
    redraw,
    setHover(pos?: Pos) {
      const changed = (!pos && vm.hover) || (pos && !vm.hover) || (pos && vm.hover && (pos.x != vm.hover.x || pos.y != vm.hover.y));
      vm.hover = pos;
      if (changed) redraw();
    }
  };
}
