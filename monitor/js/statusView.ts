import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';

import { StatusCtrl, StatusData, EntityStatus } from './statusInterfaces';
import * as styles from './styles';

function compare(a: EntityStatus, b: EntityStatus) {
  if (a.team < b.team) return -1;
  else if (a.team > b.team) return 1;

  const suffixA = parseInt(a.name.replace(/^[^\d]*/, ''), 10);
  const suffixB = parseInt(b.name.replace(/^[^\d]*/, ''), 10);
  if (suffixA < suffixB) return -1;
  else if (suffixA > suffixB) return 1;

  if (a.name < b.name) return -1;
  else if (a.name > b.name) return 1;
  else return 0;
}

function view(data: StatusData): VNode[] {
  data.entities.sort(compare);

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
        const teamColors = { style: { background: styles.teams[teams.indexOf(entity.team)] } };
        return h('tr', [
          h('td', teamColors, entity.team),
          h('td', teamColors, entity.name),
          h('td', { attrs: { class: entity.action } }, entity.action),
          h('td', { attrs: { class: entity.actionResult } }, entity.actionResult)
        ]);
      }))
    ])
  ];
}

export default function(ctrl: StatusCtrl): VNode {
  return h('div#status', [
    h('h1', ['Status: ', ctrl.vm.data ? ctrl.vm.data.sim : ctrl.vm.state]),
    ...(ctrl.vm.data ? view(ctrl.vm.data) : [])
  ]);
}
