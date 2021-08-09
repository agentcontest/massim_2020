import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';

import { StatusCtrl, StatusData } from './statusInterfaces';
import { compareAgent } from './util';
import * as styles from './styles';

function view(data: StatusData): VNode[] {
  data.entities.sort(compareAgent);

  const teams: string[] = [];
  for (const entity of data.entities) {
    if (teams.indexOf(entity.team) == -1) teams.push(entity.team);
  }

  return [
    h('h2', `Step ${data.step}/${data.steps - 1}`),
    h('table', [
      h('thead', [
        h('tr', [
          h('th', 'Team'),
          h('th', 'Agent'),
          h('th', 'Last action'),
          h('th', 'Last action result')
        ])
      ]),
      h('tbody', data.entities.map((entity) => {
        const teamStyle = { style: styles.team(teams.indexOf(entity.team)) };
        return h('tr', [
          h('td', teamStyle, entity.team),
          h('td', teamStyle, entity.name),
          h('td', { attrs: { class: entity.action } }, entity.action),
          h('td', { attrs: { class: entity.actionResult } }, entity.actionResult)
        ]);
      }))
    ])
  ];
}

export function statusView(ctrl: StatusCtrl): VNode {
  return h('div#status', [
    h('h1', ['Status: ', ctrl.vm.data ? ctrl.vm.data.sim : ctrl.vm.state]),
    ...(ctrl.vm.data ? view(ctrl.vm.data) : [])
  ]);
}
