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
  return {
    vm: {}
  };
}

export function monitorNextView(ctrl: MonitorNextCtrl): VNode {
  return h('div#monitor', [
    h('br'),
    h('canvas', {
      attrs: {
        width: 800,
        height: 300,
      },
      hook: {
        insert(vnode) {
          render(vnode.elm as HTMLCanvasElement, ctrl.vm);
          if (!vnode.data) vnode.data = {};
          let redrawing = false;

          vnode.data.massim = {
            mouseup(ev: MouseEvent) {
              ctrl.vm.mousemove = [ev.offsetX, ev.offsetY];
              ctrl.vm.mousedown = undefined;
              requestAnimationFrame(() => {
                render(vnode.elm as HTMLCanvasElement, ctrl.vm);
                redrawing = false;
              });
            },
            mousemove(ev: MouseEvent) {
              if (ctrl.vm.mousedown) {
                ctrl.vm.mousemove = [ev.offsetX, ev.offsetY];
                if (redrawing) return;
                redrawing = true;
                requestAnimationFrame(() => {
                  render(vnode.elm as HTMLCanvasElement, ctrl.vm);
                  redrawing = false;
                });
              }
            }
          };
          document.addEventListener('mouseup', vnode.data.massim.mouseup);
          document.addEventListener('mousemove', vnode.data.massim.mousemove);
        },
        update(_, vnode) {
          console.log('update');
          render(vnode.elm as HTMLCanvasElement, ctrl.vm);
        },
        destroy(vnode) {
          if (vnode.data) {
            document.removeEventListener('mouseup', vnode.data.massim.mouseup);
            document.removeEventListener('mousemove', vnode.data.massim.mousemove);
          }
        },
      },
      on: {
        mousedown(ev) {
          ctrl.vm.mousedown = [ev.offsetX, ev.offsetY];
        },
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
