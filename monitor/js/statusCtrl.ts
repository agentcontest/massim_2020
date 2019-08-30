import { Redraw } from './interfaces';
import { StatusCtrl, StatusViewModel } from './statusInterfaces';

export default function(redraw: Redraw): StatusCtrl {
  const vm: StatusViewModel = {
    state: 'connecting'
  };

  return {
    vm,
    redraw
  };
}
