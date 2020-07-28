import { Redraw } from './interfaces';
import { StatusCtrl, StatusViewModel } from './statusInterfaces';

export function makeStatusCtrl(redraw: Redraw): StatusCtrl {
  const vm: StatusViewModel = {
    state: 'connecting'
  };

  function connect() {
    const protocol = document.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const path = document.location.pathname.substr(0, document.location.pathname.lastIndexOf('/'));
    const ws = new WebSocket(protocol + '//' + document.location.host + path + '/live/status');

    ws.onmessage = (msg) => {
      const data = JSON.parse(msg.data);
      console.log(data);
      vm.data = data;
      redraw();
    };

    ws.onopen = () => {
      console.log('Connected');
      vm.state = 'online';
      redraw();
    };

    ws.onclose = () => {
      console.log('Disconnected');
      setTimeout(() => connect(), 5000);
      vm.data = undefined;
      vm.state = 'offline';
      redraw();
    };
  }

  connect();

  return {
    vm,
    redraw
  };
}
