import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';

import { Ctrl } from './ctrl';

export interface MapTransform {
  readonly x: number;
  readonly y: number;
  readonly scale: number;
}

export interface MapViewModel {
  mousedown?: [number, number];

  pan: MapTransform;
  transform: MapTransform;
}

function chain(a: MapTransform, b: MapTransform) {
  return {
    x: a.x + a.scale * b.x,
    y: a.y + a.scale * b.y,
    scale: a.scale * b.scale,
  };
}

export class MapCtrl {
  readonly vm: MapViewModel;

  constructor(readonly root: Ctrl) {
    this.vm = {
      pan: {x: 0, y: 0, scale: 1},
      transform: {x: 0, y: 0, scale: 20},
    };
  }
}

export function mapView(ctrl: MapCtrl): VNode {
  return h('canvas', {
    hook: {
      insert(vnode) {
        const elm = vnode.elm as HTMLCanvasElement;

        new (window as any)['ResizeObserver']((entries: any) => {
          for (const entry of entries) {
            elm.width = entry.contentRect.width;
            elm.height = entry.contentRect.height;
            requestAnimationFrame(() => render(elm, ctrl.vm));
          }
        }).observe(elm);

        if (!vnode.data) vnode.data = {};

        vnode.data.massim = {
          mouseup(ev: MouseEvent) {
            if (ctrl.vm.mousedown) {
              const bounds = elm.getBoundingClientRect();
              ctrl.vm.pan = {
                x: ev.clientX - bounds.left - ctrl.vm.mousedown[0],
                y: ev.clientY - bounds.top - ctrl.vm.mousedown[1],
                scale: 1,
              };
              ctrl.vm.transform = chain(ctrl.vm.pan, ctrl.vm.transform);
              ctrl.vm.pan = {x: 0, y: 0, scale: 1};
              ctrl.vm.mousedown = undefined;
            }
          },
          mousemove(ev: MouseEvent) {
            if (ctrl.vm.mousedown) {
              const bounds = elm.getBoundingClientRect();
              ctrl.vm.pan = {
                x: ev.clientX - bounds.left - ctrl.vm.mousedown[0],
                y: ev.clientY - bounds.top - ctrl.vm.mousedown[1],
                scale: 1,
              };
            }
          }
        };
        document.addEventListener('mouseup', vnode.data.massim.mouseup);
        document.addEventListener('mousemove', vnode.data.massim.mousemove);
      },
      update(_, vnode) {
        requestAnimationFrame(() => render(vnode.elm as HTMLCanvasElement, ctrl.vm));
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
        ev.preventDefault();
        ctrl.vm.mousedown = [ev.offsetX, ev.offsetY];
        requestAnimationFrame(() => render(ev.target as HTMLCanvasElement, ctrl.vm, true));
      },
      wheel(ev) {
        ev.preventDefault();
        const zoom = (ev.deltaY < 0 ? 1.5 : 1 / 1.5) * ctrl.vm.transform.scale;
        ctrl.vm.transform = {
          x: ev.offsetX + (ctrl.vm.transform.x - ev.offsetX) * zoom / ctrl.vm.transform.scale,
          y: ev.offsetY + (ctrl.vm.transform.y - ev.offsetY) * zoom / ctrl.vm.transform.scale,
          scale: zoom,
        };
        requestAnimationFrame(() => render(ev.target as HTMLCanvasElement, ctrl.vm));
      },
    }
  });
}

function render(canvas: HTMLCanvasElement, vm: MapViewModel, raf = false) {
  const ctx = canvas.getContext('2d')!;
  ctx.save();

  // fill background
  ctx.beginPath();
  ctx.fillStyle = '#eee';
  ctx.rect(0, 0, canvas.width, canvas.height);
  ctx.fill();

  const width = canvas.width, height = canvas.height;
  console.log(width, height);

  const transform = chain(vm.pan, vm.transform);
  ctx.translate(transform.x, transform.y);
  ctx.scale(transform.scale, transform.scale);

  const ymin = Math.floor(-transform.y / transform.scale);
  const xmin = Math.floor(-transform.x / transform.scale);

  const ymax = ymin + Math.ceil(canvas.height / transform.scale);
  const xmax = xmin + Math.ceil(canvas.width / transform.scale);

  const period = 5;

  // draw grid
  ctx.beginPath();
  ctx.fillStyle = '#ddd';
  for (let y = ymin; y <= ymax; y++) {
    for (let x = xmin + (((xmin + y) % 2) + 2) % 2; x <= xmax; x += 2) {
      ctx.rect(x, y, 1, 1);
    }
  }
  ctx.fill();

  // draw axis
  for (let y = Math.floor(ymin / period) * period; y <= ymax + period; y += period) {
    for (let x = Math.floor(xmin / period) * period; x <= xmax + period; x += period) {
      ctx.beginPath();
      ctx.lineWidth = 0.1;
      ctx.moveTo(x - 1, y);
      ctx.lineTo(x + 1, y);
      ctx.moveTo(x, y - 1);
      ctx.lineTo(x, y + 1);
      ctx.stroke();
    }
  }

  /* ctx.scale(100, 100);
  ctx.rect(0, 0, 1, 1);
  ctx.fillStyle = Math.random() > 0.5 ? 'red' : 'blue';
  ctx.fill(); */
  ctx.restore();
  console.log(canvas.width, canvas.height);

  if (vm.mousedown && raf) requestAnimationFrame(() => render(canvas, vm, true));
}
