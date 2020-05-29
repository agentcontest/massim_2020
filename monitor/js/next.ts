import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';

import { Redraw } from './interfaces';

export interface MonitorNextCtrl {
  vm: CanvasVm;
}

export interface CanvasVm {
  mousedown?: [number, number];

  pan: Transform,
  transform: Transform,
}

export function makeMonitorNextCtrl(redraw: Redraw): MonitorNextCtrl {
  return {
    vm: {
      pan: new Transform(0, 0, 1),
      transform: new Transform(0, 0, 20),
    }
  };
}

class Transform {
  constructor(readonly x: number, readonly y: number, readonly scale: number) { }

  static identity() {
    return new Transform(0, 0, 1);
  }

  apply(transform: Transform): Transform {
    return new Transform(
      this.x + this.scale * transform.x,
      this.y + this.scale * transform.y,
      this.scale * transform.scale);
  }

  inv(): Transform {
    return new Transform(-this.x * this.scale, -this.y * this.scale, 1 / this.scale);
  }
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
                ctrl.vm.pan = new Transform(ev.clientX - bounds.left - ctrl.vm.mousedown[0], ev.clientY - bounds.top - ctrl.vm.mousedown[1], 1);
                ctrl.vm.transform = ctrl.vm.pan.apply(ctrl.vm.transform);
                ctrl.vm.pan = Transform.identity();
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
                ctrl.vm.pan = new Transform(ev.clientX - bounds.left - ctrl.vm.mousedown[0], ev.clientY - bounds.top - ctrl.vm.mousedown[1], 1);
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
        wheel(ev) {
          ev.preventDefault();
          const zoom = (ev.deltaY > 0 ? 1.5 : 1 / 1.5) * ctrl.vm.transform.scale;
          ctrl.vm.transform = new Transform(
            ev.offsetX + (ctrl.vm.transform.x - ev.offsetX) * zoom / ctrl.vm.transform.scale,
            ev.offsetY + (ctrl.vm.transform.y - ev.offsetY) * zoom / ctrl.vm.transform.scale,
            zoom
          );
          render(ev.target as HTMLCanvasElement, ctrl.vm);
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

  const transform = vm.pan.apply(vm.transform);
  ctx.translate(transform.x, transform.y);
  ctx.scale(transform.scale, transform.scale);

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
