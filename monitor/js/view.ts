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
    const acceptedTask = agent.acceptedTask;
    return h('div', {
      class: (agent.action && agent.actionResult) ? {
        'map': true,
        [agent.action]: true,
        [agent.actionResult]: true,
      } : {
        map: true,
      },
    }, [
      h('a.team', {
        style: m.vm.selected === ctrl.map.vm.selected ? {
          background: 'white',
          color: 'black',
        } : {
          background: styles.teams[teamNames.indexOf(agent.team)],
        },
        on: {
          click() {
            ctrl.map.vm.selected = agent.id;
            ctrl.toggleMaps();
          },
        },
      }, `${agent.name} (${agent.x}|${agent.y})`),
      mapView(m, {
        size: 250,
        viewOnly: true,
      }),
      h('div.meta', [
        h('div', `energy = ${agent.energy}`),
        h('div', `${agent.action}(…) = ${agent.actionResult}`),
        acceptedTask ? h('a', {
          on: {
            click() {
              ctrl.vm.taskName = acceptedTask;
              ctrl.redraw();
            }
          }
        }, agent.acceptedTask) : undefined,
        agent.disabled ? h('div', 'disabled') : undefined,
      ]),
    ]);
  }));
}
