import { Ctrl, StaticWorld, DynamicWorld, Task } from './interfaces';
import { renderBlocks } from './canvas';
import  * as styles from './styles';

import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';

function teams(world: StaticWorld): VNode[] {
  const teamNames = Object.keys(world.teams);
  teamNames.sort();
  return teamNames.map((name, i) => h('div.team', {
    style: { background: styles.teams[i] }
  }, name));
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
      }, `${world.tasks.length} tasks`),
      ...world.tasks.map(t => h('option', {
        props: {
          value: t.name
        },
      }, `${t.reward}$ for ${t.name} until ${t.deadline}`))
    ]),
    ...(selectedTask ? [taskDetails(st, selectedTask)] : [])
  ]
}

function taskDetails(st: StaticWorld, task: Task): VNode {
  return h('canvas', {
    props: {
      width: 300,
      height: 300
    },
    hook: {
      insert: function (canvas) {
        const ctx = (canvas.elm as HTMLCanvasElement).getContext('2d')!;
        renderBlocks(ctx, st, task.requirements, 20);
      }
    }
  });
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
    h('div.box', teams(ctrl.vm.static)),
    h('div.box', tasks(ctrl, ctrl.vm.static, ctrl.vm.dynamic))
  ]);
}
