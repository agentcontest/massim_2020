import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';

import { Redraw } from './interfaces';

export interface MonitorNextCtrl {
  vm: CanvasVm;
}

export interface CanvasVm {
  mousedown?: [number, number];
  mousemove?: [number, number];
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
          render(vnode.elm as HTMLCanvasElement, ctrl.vm);
        },
        update(_, vnode) {
          render(vnode.elm as HTMLCanvasElement, ctrl.vm);
        }
      },
      on: {
        mousedown(ev) {
          ctrl.vm.mousedown = [ev.offsetX, ev.offsetY];
        },
        mousemove(ev) {
          ctrl.vm.mousemove = [ev.offsetX, ev.offsetY];
          render(ev.target as HTMLCanvasElement, ctrl.vm);
        },
        mouseup(ev) {
          ctrl.vm.mousemove = [ev.offsetX, ev.offsetY];
          render(ev.target as HTMLCanvasElement, ctrl.vm);
          ctrl.vm.mousedown = undefined;
        }
      }
    })
  ]);
}

function render(canvas: HTMLCanvasElement, vm: CanvasVm) {
  const ctx = canvas.getContext('2d')!;
  ctx.save();

  // fill background
  ctx.beginPath();
  ctx.fillStyle = '#eee';
  ctx.rect(0, 0, canvas.width, canvas.height);
  ctx.fill();

  const width = canvas.width, height = canvas.height;
  console.log(width, height);

  if (vm.mousemove && vm.mousedown) {
    ctx.translate(-vm.mousedown[0] + vm.mousemove[0], -vm.mousedown[1] + vm.mousemove[1]);
  }

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
