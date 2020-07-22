import { h } from 'snabbdom/h';
import { VNode } from 'snabbdom/vnode';

import { Ctrl } from './ctrl';
import { overlay } from './overlay';
import { MapCtrl, mapView } from './map';
import * as styles from './styles';

export function view(ctrl: Ctrl): VNode {
  return h('div#monitor', [
    overlay(ctrl),
    ctrl.maps.length > 0 ? allMaps(ctrl) : mapView(ctrl.map),
  ]);
}

function allMaps(ctrl: Ctrl): VNode | undefined {
  if (!ctrl.vm.static) return;
  const teamNames = Object.keys(ctrl.vm.static.teams);
  teamNames.sort();

  return h('div.maps', ctrl.maps.map(m => {
    if (!ctrl.vm.dynamic) return;
    const agent = ctrl.vm.dynamic.entities.find(a => a.id === m.vm.selected);
    if (!agent) return;
    return h('div.map', [
      h('div.label', {
        style: {
          background: styles.teams[teamNames.indexOf(agent.team)],
        }
      }, agent.name),
      mapView(m, {
        size: 250,
        viewOnly: true,
        denseFog: true
      }),
      h('div.meta', [
        h('div', `energy = ${agent.energy}`),
        h('div', `${agent.action}(…) = ${agent.actionResult}`),
        agent.disabled ? h('div', 'disabled') : undefined,
      ]),
    ]);
  }));
}
