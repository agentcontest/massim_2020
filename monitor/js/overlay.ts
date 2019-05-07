import { Ctrl, StaticWorld } from './interfaces';

import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';

function teams(world: StaticWorld): string {
  return `${world.teams['A'].name} : ${world.teams['B'].name}`;
}

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
  if (ctrl.vm.state === 'error') return disconnected(ctrl);
  if (ctrl.vm.state === 'connecting' || !ctrl.vm.static || !ctrl.vm.dynamic)
    return h('div.box', [
      h('div.loader', 'Loading ...')
    ]);
  return h('div#overlay', [
    h('div.box', [
      'Connected.'
    ]),
    h('div.box', [
      teams(ctrl.vm.static)
    ])
  ]);
}
