import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';

import { Pos, Agent, Block, StaticWorld, DynamicWorld } from './interfaces';
import { Ctrl } from './ctrl';
import { compareAgent } from './util';
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
  selected?: number; // agent.id
}

export interface MapViewOpts {
  size?: number;
  viewOnly?: boolean;
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

  selectedAgent(): Agent | undefined {
    if (!this.root.vm.dynamic) return;
    return this.root.vm.dynamic.entities.find(a => a.id === this.vm.selected);
  }

  select(pos?: Pos) {
    if (pos && this.root.vm.dynamic) {
      const agents = this.root.vm.dynamic.entities.filter(a => a.x == pos.x && a.y == pos.y);
      agents.reverse(); // opposite of rendering order

      if (agents.every(a => a.id !== this.vm.selected)) this.vm.selected = undefined;
      const selected = this.selectedAgent();

      for (const agent of agents) {
        if (!selected || compareAgent(selected, agent) > 0) {
          this.vm.selected = agent.id;
          this.root.redraw();
          return;
        }
      }
    }

    this.vm.selected = undefined;
    this.root.redraw();
  }

  invPos(pos: [number, number], bounds: DOMRect): Pos | undefined {
    // relative to bounds
    const x = pos[0] - bounds.x;
    const y = pos[1] - bounds.y;
    if (x < 0 || x > bounds.width || y < 0 || y > bounds.height) return;

    // relative to transform
    const p = {
      x: Math.floor((x - this.vm.transform.x) / this.vm.transform.scale),
      y: Math.floor((y - this.vm.transform.y) / this.vm.transform.scale),
    };

    // relative to grid
    if (this.root.vm.static) {
      return {
        x: mod(p.x, this.root.vm.static.grid.width),
        y: mod(p.y, this.root.vm.static.grid.height),
      };
    } else return p;
  }
}

export function mapView(ctrl: MapCtrl, opts?: MapViewOpts): VNode {
  return h('canvas', {
    attrs: opts?.size ? {
      width: opts.size,
      height: opts.size,
    } : undefined,
    hook: {
      insert(vnode) {
        const elm = vnode.elm as HTMLCanvasElement;

        if (opts?.size) render(elm, ctrl, opts);
        else new (window as any).ResizeObserver((entries: any) => {
          for (const entry of entries) {
            elm.width = entry.contentRect.width;
            elm.height = entry.contentRect.height;
            requestAnimationFrame(() => render(elm, ctrl, opts));
          }
        }).observe(elm);

        const mouseup = (ev: Event) => {
          ev.preventDefault();
          console.log('mouseup', ev);
          if (ctrl.vm.dragging && !ctrl.vm.dragging.started) {
            const pos = eventPosition(ev) || ctrl.vm.dragging.first;
            ctrl.select(ctrl.invPos(pos, elm.getBoundingClientRect()));
          }
          ctrl.vm.dragging = undefined;
        };

        const mousemove = (ev: Partial<MouseEvent & TouchEvent> & Event) => {
          const pos = eventPosition(ev);
          if (ctrl.vm.dragging && pos) {
            if (ctrl.vm.dragging.started || distanceSq(ctrl.vm.dragging.first, pos) > 20 * 20) {
              ctrl.vm.dragging.started = true;
              ctrl.vm.transform.x += pos[0] - ctrl.vm.dragging.latest[0];
              ctrl.vm.transform.y += pos[1] - ctrl.vm.dragging.latest[1];
              ctrl.vm.dragging.latest = pos;
            }
            ev.preventDefault();
          } else if (pos) {
            const inv = ctrl.invPos(pos, elm.getBoundingClientRect());
            if (inv) ctrl.root.setHover(inv);
          }
        };

        (elm as any).massim = {
          unbinds: opts?.viewOnly ? [
            unbindable(document, 'mousemove', mousemove, { passive: false }),
          ] : [
            unbindable(document, 'mouseup', mouseup),
            unbindable(document, 'touchend', mouseup),
            unbindable(document, 'mousemove', mousemove, { passive: false }),
            unbindable(document, 'touchmove', mousemove, { passive: false }),
          ],
        };
      },
      update(_, vnode) {
        render(vnode.elm as HTMLCanvasElement, ctrl, opts);
      },
      destroy(vnode) {
        const unbinds = (vnode.elm as any).massim?.unbinds;
        if (unbinds) for (const unbind of unbinds) unbind();
      },
    },
    on: opts?.viewOnly ? undefined : {
      mousedown(ev) {
        console.log(ev);
        if (ev.button !== undefined && ev.button !== 0) return; // only left click
        ev.preventDefault();
        const pos = eventPosition(ev);
        if (pos) ctrl.vm.dragging = {
          first: pos,
          latest: pos,
          started: false,
        };
        requestAnimationFrame(() => render(ev.target as HTMLCanvasElement, ctrl, opts, true));
      },
      touchstart(ev) {
        ev.preventDefault();
        const pos = eventPosition(ev);
        if (pos) ctrl.vm.dragging = {
          first: pos,
          latest: pos,
          started: false,
        };
        requestAnimationFrame(() => render(ev.target as HTMLCanvasElement, ctrl, opts, true));
      },
      wheel(ev) {
        ev.preventDefault();
        let zoom = Math.pow(3 / 2, -ev.deltaY / (ev.deltaMode ? 6.25 : 100));
        if (ctrl.vm.transform.scale * zoom < 10) zoom = 10 / ctrl.vm.transform.scale;
        if (ctrl.vm.transform.scale * zoom > 100) zoom = 100 / ctrl.vm.transform.scale;
        ctrl.vm.transform = {
          x: ev.offsetX + (ctrl.vm.transform.x - ev.offsetX) * zoom,
          y: ev.offsetY + (ctrl.vm.transform.y - ev.offsetY) * zoom,
          scale: ctrl.vm.transform.scale * zoom,
        };
        requestAnimationFrame(() => render(ev.target as HTMLCanvasElement, ctrl, opts));
      },
    },
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

function render(canvas: HTMLCanvasElement, ctrl: MapCtrl, opts: MapViewOpts | undefined, raf = false) {
  const vm = ctrl.vm;
  const width = canvas.width, height = canvas.height;

  const ctx = canvas.getContext('2d')!;
  ctx.save();

  // font
  ctx.textBaseline = 'middle';
  ctx.textAlign = 'center';
  ctx.font = '0.4px Arial';

  // fill background
  ctx.fillStyle = '#eee';
  ctx.fillRect(0, 0, width, height);

  // draw grid
  const transform = ctrl.vm.transform;
  const selectedAgent = ctrl.selectedAgent();
  if (opts?.viewOnly && selectedAgent) {
    // auto center to selection
    transform.scale = Math.min(canvas.width, canvas.height) / (selectedAgent.vision * 2 + 3);
    transform.x = canvas.width / 2 - (selectedAgent.x + 0.5) * transform.scale;
    transform.y = canvas.height / 2 - (selectedAgent.y + 0.5) * transform.scale;
  }
  ctx.translate(transform.x, transform.y);
  ctx.scale(transform.scale, transform.scale);

  const ymin = Math.floor(-transform.y / transform.scale);
  const xmin = Math.floor(-transform.x / transform.scale);

  const ymax = ymin + Math.ceil(canvas.height / transform.scale);
  const xmax = xmin + Math.ceil(canvas.width / transform.scale);

  ctx.fillStyle = '#ddd';
  for (let y = ymin; y <= ymax; y++) {
    for (let x = xmin + (((xmin + y) % 2) + 2) % 2; x <= xmax; x += 2) {
      ctx.fillRect(x, y, 1, 1);
    }
  }

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
            ctx.fillRect(x, y, 1, 1);
            break;
          case 2: // OBSTABLE
            ctx.fillStyle = styles.obstacle;
            ctx.fillRect(x - 0.04, y - 0.04, 1.08, 1.08);
            break;
        }
      }
    }

    for (let dy = Math.floor(ymin / grid.height) * grid.height; dy <= ymax + grid.height; dy += grid.height) {
      for (let dx = Math.floor(xmin / grid.width) * grid.width; dx <= xmax + grid.width; dx += grid.width) {
        // draw axis
        ctx.strokeStyle = 'black';
        ctx.lineWidth = 0.3;
        ctx.beginPath();
        if (ctrl.root.vm.dynamic.taskboards) {
          ctx.moveTo(dx - 1.5, dy);
          ctx.lineTo(dx + 1.5, dy);
          ctx.moveTo(dx, dy - 1.5);
          ctx.lineTo(dx, dy + 1.5);
        } else {
          // 2019
          ctx.rect(dx, dy, grid.width, grid.height);
        }
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
        if (ctrl.root.vm.dynamic.taskboards) {
          for (const board of ctrl.root.vm.dynamic.taskboards) {
            ctx.lineWidth = 0.05;
            drawBlock(ctx, rect(1, dx + board.x, dy + board.y, 0.05), styles.board, 'white', 'black');
          }
        }

        // blocks
        drawBlocks(ctx, dx, dy, ctrl.root.vm.static, ctrl.root.vm.dynamic.blocks);

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

          // agent action
          if (agent.action == 'clear' && agent.actionResult.indexOf('failed_') != 0) {
            const x = dx + agent.x + parseInt(agent.actionParams[0], 10);
            const y = dy + agent.y + parseInt(agent.actionParams[1], 10);
            ctx.lineWidth = 0.05;
            ctx.strokeStyle = 'red';
            drawArea(ctx, x, y, 1);
          }
        }

        // attachables of selected agent
        if (selectedAgent?.attached) {
          ctx.fillStyle = styles.hover;
          for (const attached of selectedAgent.attached) {
            if (attached.x != selectedAgent.x || attached.y != selectedAgent.y) {
              ctx.fillRect(dx + attached.x, dy + attached.y, 1, 1);
            }
          }
        }

        // clear events
        for (const clear of ctrl.root.vm.dynamic.clear) {
          ctx.lineWidth = 0.1;
          ctx.strokeStyle = 'red';
          drawArea(ctx, dx + clear.x, dy + clear.y, clear.radius);
        }

        // hover
        if (ctrl.root.vm.hover) {
          drawHover(ctx, ctrl.root.vm.static, ctrl.root.vm.dynamic, dx, dy, ctrl.root.vm.hover);
        }
      }
    }

    // fog of war
    for (let dy = Math.floor(ymin / grid.height) * grid.height; dy <= ymax + grid.height; dy += grid.height) {
      for (let dx = Math.floor(xmin / grid.width) * grid.width; dx <= xmax + grid.width; dx += grid.width) {
        for (const agent of ctrl.root.vm.dynamic.entities) {
          if (agent.id === ctrl.vm.selected) {
            drawFogOfWar(ctx, ctrl.root.vm.static, dx, dy, agent);
          }
        }
      }
    }
  }

  ctx.restore();

  if (vm.dragging && raf) requestAnimationFrame(() => render(canvas, ctrl, opts, true));
}

function drawFogOfWar(ctx: CanvasRenderingContext2D, st: StaticWorld, dx: number, dy: number, agent: Agent) {
  ctx.fillStyle = 'rgba(0, 0, 0, 0.3)';
  const top = dy - st.grid.height + agent.y + agent.vision + 1;
  ctx.fillRect(dx, top, st.grid.width, st.grid.height - 2 * agent.vision - 1); // above
  ctx.fillRect(dx - st.grid.width + agent.x + agent.vision + 1, dy + agent.y - agent.vision, st.grid.width - 2 * agent.vision - 1, 2 * agent.vision + 1);
  for (let x = -agent.vision; x <= agent.vision; x++) {
    for (let y = -agent.vision; y <= agent.vision; y++) {
      if (Math.abs(x) + Math.abs(y) > agent.vision) {
        ctx.fillRect(dx + agent.x + x, dy + agent.y + y, 1, 1);
      }
    }
  }
}

function drawHover(ctx: CanvasRenderingContext2D, st: StaticWorld, world: DynamicWorld, dx: number, dy: number, hover: Pos) {
  if (hover.x < 0 || hover.x >= st.grid.width || hover.y < 0 || hover.y >= st.grid.height) return;
  ctx.beginPath();
  ctx.fillStyle = styles.hover;
  ctx.fillRect(dx + hover.x, dy + hover.y, 1, 1);

  for (const attachable of (world.entities as Array<Agent | Block>).concat(world.blocks)) {
    if (attachable.x == hover.x && attachable.y == hover.y && attachable.attached) {
      for (let pos of attachable.attached) {
        ctx.fillRect(dx + pos.x, dy + pos.y, 1, 1);
      }
    }
  }

  ctx.lineWidth = 0.1;
  if (world.taskboards) for (const taskboard of world.taskboards) {
    if (Math.abs(taskboard.x - hover.x) + Math.abs(taskboard.y - hover.y) <= 2) {
      ctx.strokeStyle = styles.board;
      drawArea(ctx, dx + taskboard.x, dy + taskboard.y, 2);
    }
  }
  const teamNames = Object.keys(st.teams);
  teamNames.sort();
  for (const agent of world.entities) {
    if (Math.abs(agent.x - hover.x) + Math.abs(agent.y - hover.y) <= agent.vision) {
      ctx.strokeStyle = styles.teams[teamNames.indexOf(agent.team)];
      drawArea(ctx, dx + agent.x, dy + agent.y, 5);
    }
  }
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

export function drawBlocks(ctx: CanvasRenderingContext2D, dx: number, dy: number, st: StaticWorld, blocks: Block[]) {
  for (const block of blocks) {
    ctx.lineWidth = 0.05;
    const r = rect(1, dx + block.x, dy + block.y, 0.025);
    drawBlock(ctx, r, styles.blocks[st.blockTypes.indexOf(block.type) % styles.blocks.length], 'white', 'black');

    ctx.textBaseline = 'middle';
    ctx.textAlign = 'center';
    ctx.fillStyle = 'white';
    ctx.font = '0.5px Arial';
    ctx.fillText(block.type, dx + block.x + 0.5, dy + block.y + 0.5);
  }
}

function drawBlock(ctx: CanvasRenderingContext2D, r: Rect, color: string, light: string, dark: string) {
  ctx.fillStyle = color;
  ctx.fillRect(r.x1, r.y1, r.width, r.height);

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
