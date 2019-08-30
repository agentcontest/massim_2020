import { Redraw } from './interfaces';
import { StatusCtrl, StatusViewModel } from './statusInterfaces';

export default function(redraw: Redraw): StatusCtrl {
  const vm: StatusViewModel = {
    state: 'connecting'
  };

  function connect() {
    const source = new EventSource('/status');

    source.addEventListener('message', (msg) => {
      const data = JSON.parse(msg.data);
      console.log(data);
      vm.data = data;
      redraw();
    });

    source.addEventListener('open', () => {
      console.log('Connected');
      vm.state = 'online';
      redraw();
    });

    source.addEventListener('error', () => {
      console.log('Disconnected');
      setTimeout(connect, 5000);
      vm.state = 'error';
      redraw();
    });
  }

  connect();

  return {
    vm,
    redraw
  };
}
