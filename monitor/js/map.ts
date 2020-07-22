import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';

import { Agent } from './interfaces';
import { Ctrl } from './ctrl';
import * as styles from './styles';

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

function mod(a: number, b: number): number {
  return ((a % b) + b) % b;
}

function render(canvas: HTMLCanvasElement, ctrl: MapCtrl, raf = false) {
  const vm = ctrl.vm;
  const width = canvas.width, height = canvas.height;
  console.log(width, height);

  const ctx = canvas.getContext('2d')!;
  ctx.save();

  // font
  ctx.textBaseline = 'middle';
  ctx.textAlign = 'center';
  ctx.font = '0.3px Arial';

  // fill background
  ctx.beginPath();
  ctx.fillStyle = '#eee';
  ctx.rect(0, 0, width, height);
  ctx.fill();

  // draw grid
  const transform = ctrl.vm.transform;
  ctx.translate(transform.x, transform.y);
  ctx.scale(transform.scale, transform.scale);

  const ymin = Math.floor(-transform.y / transform.scale);
  const xmin = Math.floor(-transform.x / transform.scale);

  const ymax = ymin + Math.ceil(canvas.height / transform.scale);
  const xmax = xmin + Math.ceil(canvas.width / transform.scale);

  ctx.beginPath();
  ctx.fillStyle = '#ddd';
  for (let y = ymin; y <= ymax; y++) {
    for (let x = xmin + (((xmin + y) % 2) + 2) % 2; x <= xmax; x += 2) {
      ctx.rect(x, y, 1, 1);
    }
  }
  ctx.fill();

  if (ctrl.root.vm.static && ctrl.root.vm.dynamic) {
    const grid = ctrl.root.vm.static.grid;

    const teams = Object.keys(ctrl.root.vm.static.teams);
    teams.sort();

    // terrain
    for (let y = ymin; y <= ymax; y++) {
      for (let x = xmin; x <= xmax; x++) {
        switch (ctrl.root.vm.dynamic.cells[mod(y, grid.height)][mod(x, grid.width)]) {
          case 1: // GOAL
            ctx.fillStyle = styles.goalFill;
            break;
          case 2: // OBSTABLE
            ctx.fillStyle = styles.obstacle;
            break;
          default: // EMPTY
            continue;
        }
        ctx.beginPath();
        ctx.rect(x, y, 1, 1);
        ctx.fill();
      }
    }

    for (let dy = Math.floor(ymin / grid.height) * grid.height; dy <= ymax + grid.height; dy += grid.height) {
      for (let dx = Math.floor(xmin / grid.width) * grid.width; dx <= xmax + grid.width; dx += grid.width) {
        // draw axis
        ctx.beginPath();
        ctx.lineWidth = 0.1;
        ctx.moveTo(dx - 1, dy);
        ctx.lineTo(dx + 1, dy);
        ctx.moveTo(dx, dy - 1);
        ctx.lineTo(dx, dy + 1);
        ctx.stroke();

        // dispensers
        for (const dispenser of ctrl.root.vm.dynamic.dispensers) {
          ctx.lineWidth = 2 * 0.025;
          const r1 = rect(1, dx + dispenser.x, dy + dispenser.y, 0.025);
          const color = styles.blocks[ctrl.root.vm.static.blockTypes.indexOf(dispenser.type) % styles.blocks.length];
          drawBlock(ctx, r1, color, 'white', 'black');
          const r2 = rect(1, dx + dispenser.x, dy + dispenser.y, 4 * 0.025);
          drawBlock(ctx, r2, color, 'white', 'black');
          const r3 = rect(1, dx + dispenser.x, dy + dispenser.y, 8 * 0.025);
          drawBlock(ctx, r3, color, 'white', 'black');
          ctx.fillStyle = 'white';
          ctx.fillText(`[${dispenser.type}]`, dx + dispenser.x + 0.5, dy + dispenser.y + 0.5);
        }

        // task boards
        for (const board of ctrl.root.vm.dynamic.taskboards) {
          ctx.lineWidth = 0.05;
          drawBlock(ctx, rect(1, dx + board.x, dy + board.y, 0.05), styles.board, 'white', 'black');
        }

        // blocks
        for (const block of ctrl.root.vm.dynamic.blocks) {
          ctx.lineWidth = 0.05;
          const color = styles.blocks[ctrl.root.vm.static.blockTypes.indexOf(block.type) % styles.blocks.length];
          drawBlock(ctx, rect(1, dx + block.x, dy + block.y, 0.025), color, 'white', 'black');
          ctx.fillStyle = 'white';
          ctx.fillText(block.type, dx + block.x + 0.5, dy + block.y + 0.5);
        }

        // agents
        for (const agent of ctrl.root.vm.dynamic.entities) {
          ctx.lineWidth = 0.125;
          ctx.strokeStyle = 'black';

          ctx.beginPath();
          ctx.moveTo(dx + agent.x + 0.5, dy + agent.y);
          ctx.lineTo(dx + agent.x + 0.5, dy + agent.y + 1);
          ctx.stroke();

          ctx.beginPath();
          ctx.moveTo(dx + agent.x, dy + agent.y + 0.5);
          ctx.lineTo(dx + agent.x + 1, dy + agent.y + 0.5);
          ctx.stroke();

          const color = styles.teams[teams.indexOf(agent.team)];
          if (teams.indexOf(agent.team) == 0) {
            ctx.lineWidth = 0.05;
            const margin = (1 - 15 / 16 / Math.sqrt(2)) / 2;
            const r = rect(1, dx + agent.x, dy + agent.y, margin);
            drawBlock(ctx, r, color, 'white', 'black');
          } else {
            ctx.lineWidth = 0.04;
            const r = rect(1, dx + agent.x, dy + agent.y, 0.0625);
            drawRotatedBlock(ctx, r, color, 'white', 'black');
          }

          ctx.fillStyle = 'white';
          ctx.fillText(shortName(agent), dx + agent.x + 0.5, dy + agent.y + 0.5);
        }

        // clear events
        for (const clear of ctrl.root.vm.dynamic.clear) {
          ctx.lineWidth = 0.1;
          ctx.strokeStyle = 'red';
          drawArea(ctx, dx + clear.x, dy + clear.y, clear.radius);
        }
      }
    }
  }

  ctx.restore();

  if (vm.dragging && raf) requestAnimationFrame(() => render(canvas, ctrl, true));
}

interface Rect {
  x1: number;
  y1: number;
  x2: number;
  y2: number;
  width: number;
  height: number;
}

function rect(blockSize: number, x: number, y: number, margin: number): Rect {
  return {
    x1: x * blockSize + margin,
    y1: y * blockSize + margin,
    x2: x * blockSize + blockSize - margin,
    y2: y * blockSize + blockSize - margin,
    width: blockSize - 2 * margin,
    height: blockSize - 2 * margin,
  };
}

function drawBlock(ctx: CanvasRenderingContext2D, r: Rect, color: string, light: string, dark: string) {
  ctx.beginPath();
  ctx.fillStyle = color;
  ctx.rect(r.x1, r.y1, r.width, r.height);
  ctx.fill();

  ctx.beginPath();
  ctx.moveTo(r.x1, r.y2);
  ctx.lineTo(r.x1, r.y1);
  ctx.lineTo(r.x2, r.y1);
  ctx.strokeStyle = light;
  ctx.stroke();

  ctx.beginPath();
  ctx.moveTo(r.x2, r.y1);
  ctx.lineTo(r.x2, r.y2);
  ctx.lineTo(r.x1, r.y2);
  ctx.strokeStyle = dark;
  ctx.stroke();
}

function drawArea(ctx: CanvasRenderingContext2D, x: number, y: number, radius: number) {
  ctx.beginPath();
  ctx.moveTo(x - radius, y + 0.5);
  ctx.lineTo(x + 0.5, y - radius);
  ctx.lineTo(x + 1 + radius, y + 0.5);
  ctx.lineTo(x + 0.5, y + radius + 1);
  ctx.lineTo(x - radius, y + 0.5);
  ctx.stroke();
}

function drawRotatedBlock(ctx: CanvasRenderingContext2D, r: Rect, color: string, light: string, dark: string) {
  ctx.beginPath();
  ctx.fillStyle = color;
  ctx.moveTo(r.x1, (r.y1 + r.y2) / 2);
  ctx.lineTo((r.x1 + r.x2) / 2, r.y1);
  ctx.lineTo(r.x2, (r.y1 + r.y2) / 2);
  ctx.lineTo((r.x1 + r.x2) / 2, r.y2);
  ctx.closePath();
  ctx.fill();

  ctx.beginPath();
  ctx.moveTo(r.x1, (r.y1 + r.y2) / 2);
  ctx.lineTo((r.x1 + r.x2) / 2, r.y1);
  ctx.lineTo(r.x2, (r.y1 + r.y2) / 2);
  ctx.strokeStyle = light;
  ctx.stroke();

  ctx.beginPath();
  ctx.moveTo(r.x2, (r.y1 + r.y2) / 2);
  ctx.lineTo((r.x1 + r.x2) / 2, r.y2);
  ctx.lineTo(r.x1, (r.y1 + r.y2) / 2);
  ctx.strokeStyle = dark;
  ctx.stroke();
}

function shortName(agent: Agent): string {
  const match = agent.name.match(/^agent-?([A-Za-z])[A-Za-z-_]*([0-9]+)$/);
  return match ? match[1] + match[2] : agent.name;
}
