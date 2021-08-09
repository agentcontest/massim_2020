import { Agent, StaticWorld, DynamicWorld, Task, Pos } from './interfaces';
import { Ctrl, ReplayCtrl } from './ctrl';
import { drawBlocks, drawAgent } from './map';
import  * as styles from './styles';

import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';

function replay(ctrl: ReplayCtrl) {
  return h('div.box.replay', [
    h('div', [h('strong', 'Replay:'), ' ', ctrl.name()]),
    h('div', [
      h('button', { on: { click: () => ctrl.setStep(-1) } }, '|<<'),
      h('button', { on: { click: () => ctrl.setStep(ctrl.step - 10) } }, '<<'),
      h('button', {
        on: { click: () => ctrl.toggle() }
      }, ctrl.playing() ? '||' : '>'),
      h('button', { on: { click: () => ctrl.setStep(ctrl.step + 10) } }, '>>'),
      h('button', { on: { click: () => ctrl.setStep(99999999) } }, '>>|')
    ])
  ]);
}

function simplePlural(n: number, singular: string): string {
  if (n === 1) return '1 ' + singular;
  else return n + ' ' + singular + 's';
}

function teams(teamNames: string[], world: DynamicWorld): VNode[] {
  return teamNames.map((name, i) => h('div.team', {
    style: styles.team(i),
  }, `${name}: $${world.scores[name]}`));
}

function tasks(ctrl: Ctrl, st: StaticWorld, world: DynamicWorld): VNode[] {
  const selectedTask = world.tasks.find(t => t.name === ctrl.vm.taskName);
  return [
    h('select', {
      attrs: {
        name: 'tasks',
      },
      on: {
        change: function(e) {
          ctrl.vm.taskName = (e.target as HTMLOptionElement).value;
          ctrl.redraw();
        }
      }
    }, [
      h('option', {
        attrs: {
          value: ''
        },
      }, simplePlural(world.tasks.length, 'task')),
      ...world.tasks.map(t => {
        const acceptedBy = world.entities.filter(a => a.acceptedTask === t.name).length;
        return h('option', {
          attrs: {
            value: t.name,
            selected: t.name === ctrl.vm.taskName,
          },
        }, [
          `$${t.reward} for ${t.name} until step ${t.deadline}`,
          acceptedBy ? ` (${acceptedBy} accepted)` : undefined,
        ]);
      }),
    ]),
    ...(selectedTask ? taskDetails(ctrl, st, world, selectedTask) : [])
  ]
}

function hover(ctrl: Ctrl, st: StaticWorld, world: DynamicWorld, pos: Pos): VNode | undefined {
  if (!world.cells[pos.y]) return;
  const terrain = world.cells[pos.y][pos.x];
  if (typeof terrain == 'undefined') return;

  // pos
  const r = [h('li', `x = ${pos.x}, y = ${pos.y}`)];

  // terrain
  if (terrain === 1) r.push(h('li', ['terrain: ', h('span', {
    style: {
      background: styles.goalOnLight,
      color: 'black',
    }
  }, 'goal')]));
  else if (terrain === 2) r.push(h('li', 'terrain: obstacle'));

  // dispensers
  for (const dispenser of world.dispensers) {
    if (dispenser.x == pos.x && dispenser.y == pos.y) {
      r.push(h('li', ['dispenser: type = ', blockSpan(st, dispenser.type)]));
    }
  }

  // task boards
  if (world.taskboards) {
    for (const board of world.taskboards) {
      if (board.x == pos.x && board.y == pos.y) {
        r.push(h('li', h('span', {
          style: {
            background: styles.board,
            color: 'black',
          }
        }, 'task board')));
      }
    }
  }

  // blocks
  for (const block of world.blocks) {
    if (block.x == pos.x && block.y == pos.y) {
      r.push(h('li', ['block: type = ', blockSpan(st, block.type)]));
    }
  }

  // agents
  for (const agent of world.entities) {
    if (agent.x == pos.x && agent.y == pos.y) {
      r.push(h('li', ['agent: ', ...agentDescription(ctrl, agent)]));
    }
  }

  return h('ul', r);
}

function blockSpan(st: StaticWorld, type: string): VNode {
  return h('span', {
    style: {
      background: styles.blocks[st.blockTypes.indexOf(type)],
      color: 'white',
    }
  }, type)
}

function agentDescription(ctrl: Ctrl, agent: Agent): Array<VNode | string> {
  const r = [
    'name = ', h('span', {
      style: styles.team(ctrl.vm.teamNames.indexOf(agent.team)),
    }, agent.name),
    `, energy = ${agent.energy}`
  ];
  if (agent.action && agent.actionResult) r.push(', ', h('span', {
    class: {
      [agent.action]: true,
      [agent.actionResult]: true,
    }
  }, `${agent.action}(…) = ${agent.actionResult}`));
  if (agent.attached?.length) r.push(`, ${agent.attached.length}\xa0attached`);
  if (agent.acceptedTask) r.push(', ', h('a', {
    on: {
      click() {
        ctrl.vm.taskName = agent.acceptedTask;
        ctrl.redraw();
      }
    }
  }, agent.acceptedTask));
  if (agent.disabled) r.push(', disabled');
  return r;
}

function taskDetails(ctrl: Ctrl, st: StaticWorld, dynamic: DynamicWorld, task: Task): VNode[] {
  const xs = task.requirements.map(b => Math.abs(b.x));
  const ys = task.requirements.map(b => Math.abs(b.y));
  const width = 2 * Math.max(...xs) + 1;
  const height = 2 * Math.max(...ys) + 1;
  const elementWidth = 218;
  const gridSize = Math.min(Math.floor(elementWidth / width), 50);
  const elementHeight = gridSize * height;
  const render = function (vnode: VNode) {
    const canvas = vnode.elm as HTMLCanvasElement;
    const ctx = canvas.getContext('2d')!;
    ctx.save();
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.translate((elementWidth - gridSize) / 2, (elementHeight - gridSize) / 2);
    ctx.scale(gridSize, gridSize);
    drawAgent(ctx, 0, 0, { x: 0, y: 0 }, 0);
    drawBlocks(ctx, 0, 0, st, task.requirements);
    ctx.restore();
  };
  const acceptedBy = dynamic.entities.filter(a => a.acceptedTask === task.name);
  return [
    h('canvas', {
      attrs: {
        width: elementWidth,
        height: elementHeight
      },
      hook: {
        insert: render,
        update: (_, vnode) => render(vnode)
      }
    }),
    ...(acceptedBy.length ? [
      h('p', `Accepted by ${simplePlural(acceptedBy.length, 'agent')}:`),
      h('ul', acceptedBy.map(by => h('li', h('a', {
        style: styles.team(ctrl.vm.teamNames.indexOf(by.team)),
        on: {
          click() {
            ctrl.map.vm.selected = by.id;
            ctrl.redraw();
          }
        },
      }, by.name)))),
    ] : []),
    h('p', simplePlural(task.requirements.length, 'block')),
  ];
}

function disconnected(): VNode {
  return h('div.box', [
    h('p', 'Live server not connected.'),
    h('a', {
      attrs: { href: document.location.pathname + document.location.search }
    }, 'Retry now.')
  ]);
}

function box(child: VNode | undefined): VNode | undefined {
  return child ? h('div.box', child) : undefined;
}

export function overlay(ctrl: Ctrl): VNode {
  const selectedAgent = ctrl.map.selectedAgent();
  return h('div#overlay', [
    ctrl.vm.static && (ctrl.replay ? replay(ctrl.replay) : h('div.box', ctrl.vm.static.sim)),
    (ctrl.vm.state === 'error' || ctrl.vm.state === 'offline') ?
      ctrl.replay ?
        h('div.box', ctrl.vm.static ? 'Could not load step' : 'Could not load replay') :
        disconnected() : undefined,
    (ctrl.vm.static && ctrl.vm.dynamic) ?
      h('div.box', [
        `Step: ${ctrl.vm.dynamic.step} / ${ctrl.vm.static.steps - 1}`
      ]) : undefined,
    ctrl.vm.state === 'connecting' ? h('div.box', ['Connecting ...', h('div.loader')]) : undefined,
    (ctrl.vm.state === 'online' && (!ctrl.vm.static || !ctrl.vm.dynamic)) ? h('div.box', ['Waiting ...', h('div.loader')]) : undefined,
    ...((ctrl.vm.state === 'online' && ctrl.vm.static && ctrl.vm.dynamic) ? [
      h('div.box', teams(ctrl.vm.teamNames, ctrl.vm.dynamic)),
      h('div.box', [
        h('button', {
          on: {
            click: () => ctrl.toggleMaps(),
          }
        }, ctrl.maps.length ? 'Global view' : 'Agent view'),
        ctrl.maps.length ? undefined : h('button', {
          on: {
            click() {
              ctrl.resetTransform();
              ctrl.redraw();
            }
          }
        }, 'Reset zoom'),
      ]),
      h('div.box', tasks(ctrl, ctrl.vm.static, ctrl.vm.dynamic)),
      selectedAgent ? box(h('div', ['Selected agent: ', ...agentDescription(ctrl, selectedAgent)])) : undefined,
      ctrl.vm.hover ? box(hover(ctrl, ctrl.vm.static, ctrl.vm.dynamic, ctrl.vm.hover)) : undefined,
    ] : [])
  ]);
}
