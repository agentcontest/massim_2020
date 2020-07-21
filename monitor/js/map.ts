import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';

import { Ctrl } from './ctrl';

export interface MapTransform {
  x: number;
  y: number;
  scale: number;
}

export interface MapViewModel {
  drag?: [number, number];
  transform: MapTransform;
}

export class MapCtrl {
  readonly vm: MapViewModel;

  constructor(readonly root: Ctrl) {
    this.vm = {
      transform: {
        x: 0,
        y: 0,
        scale: 20,
      },
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
            requestAnimationFrame(() => render(elm, ctrl));
          }
        }).observe(elm);

        if (!vnode.data) vnode.data = {};

        vnode.data.massim = {
          mouseup(ev: Event) {
            ev.preventDefault();
            ctrl.vm.drag = undefined;
          },
          mousemove(ev: Partial<MouseEvent & TouchEvent> & Event) {
            ev.preventDefault();
            const pos = eventPosition(ev);
            if (ctrl.vm.drag && pos) {
              ctrl.vm.transform.x += pos[0] - ctrl.vm.drag[0];
              ctrl.vm.transform.y += pos[1] - ctrl.vm.drag[1];
              ctrl.vm.drag = pos;
            }
          }
        };
        document.addEventListener('mouseup', vnode.data.massim.mouseup);
        document.addEventListener('touchend', vnode.data.massim.touchend);
        document.addEventListener('mousemove', vnode.data.massim.mousemove, { passive: false });
        document.addEventListener('touchmove', vnode.data.massim.mousemove, { passive: false });
      },
      update(_, vnode) {
        render(vnode.elm as HTMLCanvasElement, ctrl);
      },
      destroy(vnode) {
        if (vnode.data) {
          document.removeEventListener('mouseup', vnode.data.massim.mouseup);
          document.removeEventListener('touchend', vnode.data.massim.mouseup);
          document.removeEventListener('mousemove', vnode.data.massim.mousemove);
          document.removeEventListener('touchmove', vnode.data.massim.mousemove);
        }
      },
    },
    on: {
      mousedown(ev) {
        ev.preventDefault();
        ctrl.vm.drag = eventPosition(ev);
        requestAnimationFrame(() => render(ev.target as HTMLCanvasElement, ctrl, true));
      },
      touchstart(ev) {
        ev.preventDefault();
        ctrl.vm.drag = eventPosition(ev);
        requestAnimationFrame(() => render(ev.target as HTMLCanvasElement, ctrl, true));
      },
      wheel(ev) {
        ev.preventDefault();
        let zoom = Math.pow(3 / 2, -ev.deltaY / 100);
        if (ctrl.vm.transform.scale * zoom < 5) zoom = 5 / ctrl.vm.transform.scale;
        if (ctrl.vm.transform.scale * zoom > 100) zoom = 100 / ctrl.vm.transform.scale;
        ctrl.vm.transform = {
          x: ev.offsetX + (ctrl.vm.transform.x - ev.offsetX) * zoom,
          y: ev.offsetY + (ctrl.vm.transform.y - ev.offsetY) * zoom,
          scale: ctrl.vm.transform.scale * zoom,
        };
        requestAnimationFrame(() => render(ev.target as HTMLCanvasElement, ctrl));
      },
    }
  });
}

function eventPosition(e: Partial<MouseEvent & TouchEvent>): [number, number] | undefined {
  if (e.offsetX || e.offsetX === 0) return [e.offsetX, e.offsetY!];
  if (e.targetTouches?.[0]) return [e.targetTouches[0].clientX, e.targetTouches[0].clientY];
  return;
}

function render(canvas: HTMLCanvasElement, ctrl: MapCtrl, raf = false) {
  const vm = ctrl.vm;
  const ctx = canvas.getContext('2d')!;
  ctx.save();

  // fill background
  ctx.beginPath();
  ctx.fillStyle = '#eee';
  ctx.rect(0, 0, canvas.width, canvas.height);
  ctx.fill();

  const width = canvas.width, height = canvas.height;
  console.log(width, height);

  const transform = ctrl.vm.transform;
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

  if (vm.drag && raf) requestAnimationFrame(() => render(canvas, ctrl, true));
}
