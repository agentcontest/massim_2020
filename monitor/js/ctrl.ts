import { Redraw, Ctrl, ViewModel } from './interfaces';

export default function(redraw: Redraw): Ctrl {
  const vm: ViewModel = {
    state: 'connecting'
  };

  function connect() {
    const protocol = document.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const ws = new WebSocket(protocol + '//' + document.location.host + '/socket');

    ws.onmessage = function(msg) {
      const data = JSON.parse(msg.data);
      console.log(data);
      if (data.grid) {
        data.blockTypes.sort();
        vm.static = data;
      }
      else vm.dynamic = data;
      redraw();
    };

    ws.onopen = function() {
      console.log('Connected');
      vm.state = 'online';
      redraw();
    };

    ws.onclose = function() {
      console.log('Disconnected');
      setTimeout(connect, 5000);
      vm.state = 'error';
      redraw();
    };
  }

  connect();

  return {
    vm
  }
}
