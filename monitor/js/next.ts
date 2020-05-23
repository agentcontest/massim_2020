import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';

import { Redraw } from './interfaces';

export interface MonitorNextCtrl {
  vm: CanvasVm;
}

export interface CanvasVm {
  mousedown?: [number, number];
}

export function makeMonitorNextCtrl(redraw: Redraw): MonitorNextCtrl {
  setInterval(redraw, 1000);
  return {
    vm: {}
  };
}

export function monitorNextView(ctrl: MonitorNextCtrl): VNode {
  return h('div#monitor', [
    h('canvas', {
      hook: {
        insert(vnode) {
          render(vnode);
        },
        update(_, vnode) {
          render(vnode);
        }
      },
      on: {
        mousedown(ev) {
          console.log(ev);
        }
      }
    })
  ]);
}

function render(vnode: VNode) {
  const canvas = vnode.elm as HTMLCanvasElement;
  const ctx = canvas.getContext('2d')!;
  ctx.save();


  const width = canvas.width, height = canvas.height;
  console.log(width, height);

  // fill background
  ctx.beginPath();
  ctx.fillStyle = '#eee';
  ctx.rect(0, 0, canvas.width, canvas.height);
  ctx.fill();

  ctx.scale(20, 20);
  ctx.translate(0.5, 0.5);

  // draw grid
  ctx.beginPath();
  ctx.fillStyle = '#ddd';
  for (let y = 0; y < height; y++) {
    for (let x = y % 2; x < width; x += 2) {
      ctx.rect(x, y, 1, 1);
    }
  }
  ctx.fill();

  // draw axis
  ctx.beginPath();
  ctx.lineWidth = 0.1;
  ctx.moveTo(0, 0);
  ctx.lineTo(1, 0);
  ctx.moveTo(0, 0);
  ctx.lineTo(0, 1);
  ctx.stroke();

  /* ctx.scale(100, 100);
  ctx.rect(0, 0, 1, 1);
  ctx.fillStyle = Math.random() > 0.5 ? 'red' : 'blue';
  ctx.fill(); */
  ctx.restore();
  console.log(canvas.width, canvas.height);
}
