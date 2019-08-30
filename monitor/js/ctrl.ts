import { Redraw, Ctrl, ViewModel, Pos } from './interfaces';

export default function(redraw: Redraw): Ctrl {
  const vm: ViewModel = {
    state: 'connecting'
  };

  function connect() {
    const source = new EventSource('/live/monitor');

    source.addEventListener('message', function(msg) {
      const data = JSON.parse(msg.data);
      console.log(data);
      if (data.grid) {
        data.blockTypes.sort();
        vm.static = data;
      }
      else vm.dynamic = data;
      redraw();
    });

    source.addEventListener('open', function() {
      console.log('Connected');
      vm.state = 'online';
      redraw();
    });

    source.addEventListener('error', function() {
      console.log('Disconnected');
      setTimeout(connect, 5000);
      vm.state = 'error';
      redraw();
    });
  }

  connect();

  return {
    vm,
    redraw,
    setHover(pos?: Pos) {
      const changed = (!pos && vm.hover) || (pos && !vm.hover) || (pos && vm.hover && (pos.x != vm.hover.x || pos.y != vm.hover.y));
      vm.hover = pos;
      if (changed) redraw();
    }
  };
}
