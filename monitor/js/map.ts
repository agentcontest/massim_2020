import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';

import { Ctrl } from './ctrl';

interface Transform {
  x: number;
  y: number;
  scale: number;
}

interface Dragging {
  first: [number, number];
  latest: [number, number];
  started: boolean;
}

export interface MapViewModel {
  dragging?: Dragging;
  transform: Transform;
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

        const mouseup = (ev: Event) => {
          ev.preventDefault();
          ctrl.vm.dragging = undefined;
        };

        const mousemove = (ev: Partial<MouseEvent & TouchEvent> & Event) => {
          ev.preventDefault();
          const pos = eventPosition(ev);
          if (ctrl.vm.dragging && pos) {
            if (ctrl.vm.dragging.started || distanceSq(ctrl.vm.dragging.first, pos) > 20 * 20) {
              ctrl.vm.dragging.started = true;
              ctrl.vm.transform.x += pos[0] - ctrl.vm.dragging.latest[0];
              ctrl.vm.transform.y += pos[1] - ctrl.vm.dragging.latest[1];
              ctrl.vm.dragging.latest = pos;
            }
          }
        };

        if (!vnode.data) vnode.data = {};
        vnode.data.massim = {
          unbinds: [
            unbindable(document, 'mouseup', mouseup),
            unbindable(document, 'touchend', mouseup),
            unbindable(document, 'mousemove', mousemove, { passive: false }),
            unbindable(document, 'touchmove', mousemove, { passive: false }),
          ],
        };
      },
      update(_, vnode) {
        render(vnode.elm as HTMLCanvasElement, ctrl);
      },
      destroy(vnode) {
        const unbinds  = vnode.data?.massim?.unbinds;
        if (unbinds) for (const unbind of unbinds) unbind();
      },
    },
    on: {
      mousedown(ev) {
        if (ev.button !== undefined && ev.button !== 0) return; // only left click
        ev.preventDefault();
        const pos = eventPosition(ev);
        if (pos) ctrl.vm.dragging = {
          first: pos,
          latest: pos,
          started: false,
        };
        requestAnimationFrame(() => render(ev.target as HTMLCanvasElement, ctrl, true));
      },
      touchstart(ev) {
        ev.preventDefault();
        const pos = eventPosition(ev);
        if (pos) ctrl.vm.dragging = {
          first: pos,
          latest: pos,
          started: false,
        };
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

function unbindable(el: EventTarget, eventName: string, callback: EventListener, options?: AddEventListenerOptions) {
  el.addEventListener(eventName, callback, options);
  return () => el.removeEventListener(eventName, callback, options);
}

function eventPosition(e: Partial<MouseEvent & TouchEvent>): [number, number] | undefined {
  if (e.clientX || e.clientX === 0) return [e.clientX, e.clientY!];
  if (e.targetTouches?.[0]) return [e.targetTouches[0].clientX, e.targetTouches[0].clientY];
  return;
}

function distanceSq(a: [number, number], b: [number, number]): number {
  const dx = a[0] - b[0];
  const dy = a[1] - b[1];
  return dx * dx + dy * dy;
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

  if (vm.dragging && raf) requestAnimationFrame(() => render(canvas, ctrl, true));
}
