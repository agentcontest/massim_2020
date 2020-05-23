import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';

import { Redraw } from './interfaces';

export interface MonitorNextCtrl {
  vm: CanvasVm;
}

export interface CanvasVm {
  mousedown?: [number, number];

  translate: [number, number];
  offset: [number, number];
  scale: number;
}

export function makeMonitorNextCtrl(redraw: Redraw): MonitorNextCtrl {
  return {
    vm: {
      translate: [0, 0],
      offset: [0, 0],
      scale: 20,
    }
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
          const elm = vnode.elm as HTMLCanvasElement;
          render(elm, ctrl.vm);
          if (!vnode.data) vnode.data = {};
          let redrawing = false;

          vnode.data.massim = {
            mouseup(ev: MouseEvent) {
              if (ctrl.vm.mousedown) {
                const bounds = elm.getBoundingClientRect();
                ctrl.vm.translate[0] += ev.clientX - bounds.left - ctrl.vm.mousedown[0];
                ctrl.vm.translate[1] += ev.clientY - bounds.top - ctrl.vm.mousedown[1];
                ctrl.vm.offset = [0, 0];
                ctrl.vm.mousedown = undefined;
                requestAnimationFrame(() => {
                  render(elm, ctrl.vm);
                  redrawing = false;
                });
              }
            },
            mousemove(ev: MouseEvent) {
              if (ctrl.vm.mousedown) {
                const bounds = elm.getBoundingClientRect();
                ctrl.vm.offset = [ev.clientX - bounds.left - ctrl.vm.mousedown[0], ev.clientY - bounds.top - ctrl.vm.mousedown[1]];
                if (redrawing) return;
                redrawing = true;
                requestAnimationFrame(() => {
                  render(elm, ctrl.vm);
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

  ctx.translate(vm.offset[0] + vm.translate[0], vm.offset[1] + vm.translate[1]);
  ctx.scale(vm.scale, vm.scale);

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
