import { Ctrl, DynamicWorld, StaticWorld, Block, Rect, Agent, Pos } from './interfaces';
import * as styles from './styles';

let GRID = 20; // todo: make const

export function render(canvas: HTMLCanvasElement, ctrl: Ctrl) {
  const ctx = canvas.getContext('2d')!;
  ctx.save();
  if (ctrl.vm.static) renderStatic(canvas, ctx, ctrl.vm.static);
  if (ctrl.vm.static && ctrl.vm.dynamic) renderDynamic(ctx, ctrl.vm.static, ctrl.vm.dynamic);
  if (ctrl.vm.static && ctrl.vm.dynamic && ctrl.vm.hover) renderHover(ctx, ctrl.vm.static, ctrl.vm.dynamic, ctrl.vm.hover);
  ctx.restore();
}

export function invClientPos(canvas: HTMLCanvasElement, world: StaticWorld, clientX: number, clientY: number): Pos {
  const clientRect = canvas.getBoundingClientRect();
  return {
    x: Math.floor((clientX - clientRect.left - Math.floor((canvas.width - world.grid.width * GRID) / 2)) / GRID),
    y: Math.floor((clientY - clientRect.top - Math.floor((canvas.height - world.grid.height * GRID) / 2)) / GRID)
  };
}

function renderHover(ctx: CanvasRenderingContext2D, st: StaticWorld, world: DynamicWorld, hover: Pos) {
  if (hover.x < 0 || hover.x >= st.grid.width || hover.y < 0 || hover.y >= st.grid.height) return;
  ctx.beginPath();
  ctx.fillStyle = 'rgba(180, 180, 255, 0.4)';
  ctx.rect(hover.x * GRID, hover.y * GRID, GRID, GRID);
  ctx.fill();

  for (let attachable of (world.entities as Array<Agent | Block>).concat(world.blocks)) {
    if (attachable.x == hover.x && attachable.y == hover.y && attachable.attached) {
      for (let pos of attachable.attached) {
        ctx.beginPath();
        ctx.rect(pos.x * GRID, pos.y * GRID, GRID, GRID);
        ctx.fill();
      }
    }
  }

  const teamNames = Object.keys(st.teams);
  teamNames.sort();
  for (let agent of world.entities) {
    if (Math.abs(agent.x - hover.x) + Math.abs(agent.y - hover.y) <= agent.vision) {
      ctx.lineWidth = 2;
      ctx.strokeStyle = styles.teams[teamNames.indexOf(agent.team)];
      drawArea(ctx, agent.x, agent.y, 5);
    }
  }
}

function renderStatic(canvas: HTMLCanvasElement, ctx: CanvasRenderingContext2D, world: StaticWorld) {
  canvas.width = window.innerWidth - 350;
  canvas.height = window.innerHeight;

  GRID = Math.floor(Math.min(canvas.width / world.grid.width, canvas.height / world.grid.height));

  ctx.translate(
    Math.floor((canvas.width - world.grid.width * GRID) / 2),
    Math.floor((canvas.height - world.grid.height * GRID) / 2));

  // background
  ctx.beginPath();
  ctx.fillStyle = '#eee';
  ctx.rect(0, 0, world.grid.width * GRID, world.grid.height * GRID);
  ctx.fill();

  // background pattern
  ctx.fillStyle = '#ddd';
  for (let y = 0; y < world.grid.height; y++) {
    for (let x = y % 2; x < world.grid.width; x += 2) {
      ctx.beginPath();
      ctx.rect(x * GRID, y * GRID, GRID, GRID);
      ctx.fill();
    }
  }
}

function rect(ctx: CanvasRenderingContext2D, blockSize: number, x: number, y: number, margin: number): Rect {
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

export function renderBlocks(ctx: CanvasRenderingContext2D, st: StaticWorld, blocks: Block[], blockSize: number) {
  for (let block of blocks) {
    ctx.lineWidth = blockSize / 20;
    const r = rect(ctx, blockSize, block.x, block.y, ctx.lineWidth / 2);
    drawBlock(ctx, r, styles.blocks[st.blockTypes.indexOf(block.type) % styles.blocks.length], 'white', 'black');

    ctx.textBaseline = 'middle';
    ctx.textAlign = 'center';
    ctx.fillStyle = 'white';
    ctx.fillText(block.type, (block.x + 0.5) * blockSize, (block.y + 0.5) * blockSize);
  }
}

function renderDynamic(ctx: CanvasRenderingContext2D, st: StaticWorld, dynamic: DynamicWorld) {
  // terrain
  for (let y = 0; y < st.grid.height; y++) {
    for (let x = 0; x < st.grid.width; x++) {
      switch (dynamic.cells[y][x]) {
        case 0: // EMPTY
          continue;
        case 1: // GOAL
          ctx.fillStyle = styles.goalFill;
          ctx.strokeStyle = styles.goalStroke;
          ctx.beginPath();
          ctx.rect(x * GRID, y * GRID, GRID, GRID);
          ctx.fill();
          continue;
        case 2: // OBSTABLE
          ctx.fillStyle = styles.obstacle;
          break;
      }
      ctx.beginPath();
      ctx.rect(x * GRID, y * GRID, GRID, GRID);
      ctx.fill();
    }
  }

  // dispensers
  for (let dispenser of dynamic.dispensers) {
    ctx.lineWidth = GRID / 20;
    const r1 = rect(ctx, GRID, dispenser.x, dispenser.y, ctx.lineWidth / 2);
    const color = styles.blocks[st.blockTypes.indexOf(dispenser.type) % styles.blocks.length];
    drawBlock(ctx, r1, color, 'white', 'black');

    const r2 = rect(ctx, GRID, dispenser.x, dispenser.y, 4 * ctx.lineWidth / 2);
    drawBlock(ctx, r2, color, 'white', 'black');

    const r3 = rect(ctx, GRID, dispenser.x, dispenser.y, 8 * ctx.lineWidth / 2);
    drawBlock(ctx, r3, color, 'white', 'black');

    ctx.textBaseline = 'middle';
    ctx.textAlign = 'center';
    ctx.fillStyle = 'white';
    ctx.fillText(`[${dispenser.type}]`, (dispenser.x + 0.5) * GRID, (dispenser.y + 0.5) * GRID);
  }

  // blocks
  renderBlocks(ctx, st, dynamic.blocks, GRID);

  // agents
  const teams = Object.keys(st.teams);
  teams.sort();
  for (let agent of dynamic.entities) {
    ctx.beginPath();
    ctx.lineWidth = GRID / 8;
    ctx.moveTo((agent.x + 0.5) * GRID, agent.y * GRID);
    ctx.lineTo((agent.x + 0.5) * GRID, agent.y * GRID + GRID);
    ctx.strokeStyle = 'black';
    ctx.stroke();

    ctx.beginPath();
    ctx.lineWidth = GRID / 8;
    ctx.moveTo(agent.x * GRID, (agent.y + 0.5) * GRID);
    ctx.lineTo(agent.x * GRID + GRID, (agent.y + 0.5) * GRID);
    ctx.strokeStyle = 'black';
    ctx.stroke();

    const color = styles.teams[teams.indexOf(agent.team)];
    if (teams.indexOf(agent.team) == 0) {
      ctx.lineWidth = GRID / 20;
      const margin = GRID * (1 - 15 / 16 / Math.sqrt(2)) / 2;
      const r = rect(ctx, GRID, agent.x, agent.y, margin);
      drawBlock(ctx, r, color, 'white', 'black');
    } else {
      ctx.lineWidth = GRID / 25;
      const r = rect(ctx, GRID, agent.x, agent.y, GRID / 16);
      drawRotatedBlock(ctx, r, color, 'white', 'black');
    }

    ctx.textBaseline = 'middle';
    ctx.textAlign = 'center';
    ctx.fillStyle = 'white';
    ctx.fillText(shortName(agent), (agent.x + 0.5) * GRID, (agent.y + 0.5) * GRID);

    // clear action
    if (agent.action == 'clear' && agent.actionResult.indexOf("failed_") != 0) {
      const x = agent.x + parseInt(agent.actionParams[0], 10);
      const y = agent.y + parseInt(agent.actionParams[1], 10);
      ctx.lineWidth = 1;
      ctx.strokeStyle = 'red';
      drawArea(ctx, x, y, 1);
    }
  }

  // clear events
  for (let clear of dynamic.clear) {
    ctx.lineWidth = 2;
    ctx.strokeStyle = 'red';
    drawArea(ctx, clear.x, clear.y, clear.radius);
  }
}

function shortName(agent: Agent): string {
  const match = agent.name.match(/^agent-([A-Za-z])[A-Za-z-_]*([0-9]+)$/);
  return match ? match[1] + match[2] : agent.name;
}

function drawArea(ctx: CanvasRenderingContext2D, x: number, y: number, radius: number) {
  ctx.beginPath();
  ctx.moveTo((x - radius) * GRID, (y + 0.5) * GRID);
  ctx.lineTo((x + 0.5) * GRID, (y - radius) * GRID);
  ctx.lineTo((x + 1 + radius) * GRID, (y + 0.5) * GRID);
  ctx.lineTo((x + 0.5) * GRID, (y + radius + 1) * GRID);
  ctx.lineTo((x - radius) * GRID, (y + 0.5) * GRID);
  ctx.stroke();
}
