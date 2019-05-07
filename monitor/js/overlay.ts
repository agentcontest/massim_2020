import { Ctrl } from './interfaces';

import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';

function disconnected(_ctrl: Ctrl): VNode {
  // TODO: Replay available?
  return h('div.box', [
    h('p', 'Live server not connected.'),
    h('a', {
      props: { href: document.location.pathname + document.location.search }
    }, 'Retry now.')
  ]);
}

export default function(ctrl: Ctrl): VNode {
  console.log('vm', ctrl.vm);
  if (ctrl.vm.state === 'error') return disconnected(ctrl);
  if (ctrl.vm.state === 'connecting' || !ctrl.vm.dynamic)
    return h('div.box', [
      h('div.loader', 'Loading ...')
    ]);
  return h('div.box', [
    'Connected.'
  ]);
}
