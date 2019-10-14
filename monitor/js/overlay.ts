import { Ctrl, ReplayCtrl, StaticWorld, DynamicWorld, Task, Block, Pos } from './interfaces';
import { renderBlocks } from './canvas';
import  * as styles from './styles';

import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';

function replay(ctrl: ReplayCtrl) {
  return h('div.box.replay', [
    h('div', [h('strong', 'Replay:'), ' ', ctrl.name()]),
    h('div', [
      h('button', { on: { click: () => ctrl.setStep(-1) } }, '|<<'),
      h('button', { on: { click: () => ctrl.setStep(ctrl.step() - 10) } }, '<<'),
      h('button', {
        on: { click: () => ctrl.toggle() }
      }, ctrl.playing() ? '||' : '>'),
      h('button', { on: { click: () => ctrl.setStep(ctrl.step() + 10) } }, '>>'),
      h('button', { on: { click: () => ctrl.setStep(99999999) } }, '>>|')
    ])
  ]);
}

function simplePlural(n: number, singular: string): string {
  if (n === 1) return '1 ' + singular;
  else return n + ' ' + singular + 's';
}

function teams(st: StaticWorld, world: DynamicWorld): VNode[] {
  const teamNames = Object.keys(st.teams);
  teamNames.sort();
  return teamNames.map((name, i) => h('div.team', {
    style: { background: styles.teams[i] }
  }, `${name}: $${world.scores[name]}`));
}

function tasks(ctrl: Ctrl, st: StaticWorld, world: DynamicWorld): VNode[] {
  const selectedTask = world.tasks.filter(t => t.name === ctrl.vm.taskName)[0];
  return [
    h('select', {
      on: {
        change: function(e) {
          ctrl.vm.taskName = (e.target as HTMLOptionElement).value;
          ctrl.redraw();
        }
      }
    }, [
      h('option', {
        props: {
          value: ''
        },
      }, simplePlural(world.tasks.length, 'task')),
      ...world.tasks.map(t => h('option', {
        props: {
          value: t.name
        },
      }, `${t.reward}$ for ${t.name} until step ${t.deadline}`))
    ]),
    ...(selectedTask ? taskDetails(st, selectedTask) : [])
  ]
}

function hover(world: DynamicWorld, pos: Pos): VNode | undefined {
  if (!world.cells[pos.y]) return;
  const terrain = world.cells[pos.y][pos.x];
  if (typeof terrain == 'undefined') return;

  // pos
  const r = [h('li', `x = ${pos.x}, y = ${pos.y}`)];

  // terrain
  if (terrain === 0) r.push(h('li', 'terrain: empty'));
  else if (terrain === 1) r.push(h('li', 'terrain: goal'));
  else if (terrain === 2) r.push(h('li', 'terrain: obstacle'));

  // dispensers
  for (let dispenser of world.dispensers) {
    if (dispenser.x == pos.x && dispenser.y == pos.y) {
      r.push(h('li', `dispenser: type = ${dispenser.type}`));
    }
  }

  // blocks
  for (let block of world.blocks) {
    if (block.x == pos.x && block.y == pos.y) {
      r.push(h('li', `block: type = ${block.type}`));
    }
  }

  // agents
  for (let agent of world.entities) {
    if (agent.x == pos.x && agent.y == pos.y) {
      let description = `agent: name = ${agent.name}, team = ${agent.team}, energy = ${agent.energy}, ${agent.action}(…) = ${agent.actionResult}`;
      if (agent.disabled) description += ', disabled';
      r.push(h('li', description));
    }
  }

  return h('ul', r);
}

function taskDetails(st: StaticWorld, task: Task): VNode[] {
  const xs = task.requirements.map(b => Math.abs(b.x));
  const ys = task.requirements.map(b => Math.abs(b.y));
  const width = 2 * Math.max(...xs) + 1;
  const height = 2 * Math.max(...ys) + 1;
  const elementWidth = 318;
  const gridSize = Math.min(Math.floor(elementWidth / width), 50);
  const elementHeight = gridSize * height;
  const render = function (vnode: VNode) {
    const canvas = vnode.elm as HTMLCanvasElement;
    const ctx = canvas.getContext('2d')!;
    ctx.save();
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.translate((elementWidth - gridSize) / 2, (elementHeight - gridSize) / 2);
    ctx.beginPath();
    ctx.rect(gridSize * 0.4, gridSize * 0.4, gridSize * 0.2, gridSize * 0.2);
    ctx.fillStyle = 'red';
    ctx.fill();
    renderBlocks(ctx, st, task.requirements, gridSize);
    ctx.restore();
  };
  return [h('canvas', {
    props: {
      width: elementWidth,
      height: elementHeight
    },
    hook: {
      insert: render,
      update: (_, vnode) => render(vnode)
    }
  }), h('p', simplePlural(task.requirements.length, 'block'))];
}

function disconnected(): VNode {
  return h('div.box', [
    h('p', 'Live server not connected.'),
    h('a', {
      props: { href: document.location.pathname + document.location.search }
    }, 'Retry now.')
  ]);
}

function box(child: VNode | undefined): VNode | undefined {
  return child ? h('div.box', child) : undefined;
}

export default function(ctrl: Ctrl): VNode {
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
    ctrl.vm.state === 'connecting' ? h('div.box', [h('div.loader', 'Loading ...')]) : undefined,
    ...((ctrl.vm.state === 'online' && ctrl.vm.static && ctrl.vm.dynamic) ? [
      h('div.box', teams(ctrl.vm.static, ctrl.vm.dynamic)),
      h('div.box', tasks(ctrl, ctrl.vm.static, ctrl.vm.dynamic)),
      ctrl.vm.hover ? box(hover(ctrl.vm.dynamic, ctrl.vm.hover)) : undefined
    ] : [])
  ]);
}
