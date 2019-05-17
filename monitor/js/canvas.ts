import { Ctrl, DynamicWorld, StaticWorld, Block, Rect } from './interfaces';
import * as styles from './styles';

let GRID = 20; // todo: make const

export default function(canvas: HTMLCanvasElement, ctrl: Ctrl) {
  const ctx = canvas.getContext('2d')!;

  if (ctrl.vm.static) renderStatic(canvas, ctx, ctrl.vm.static);
  if (ctrl.vm.static && ctrl.vm.dynamic) renderDynamic(ctx, ctrl.vm.static, ctrl.vm.dynamic);
  /*const ctx = canvas.getContext('2d')!;

  ctrl.vm.dynamic.entities.map(agent => {
    ctx.fillStyle = 'green';
    ctx.fillRect(agent.x * GRID, agent.y * GRID, GRID, GRID);
  });a */
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

  // terrain
  for (let y = 0; y < world.grid.height; y++) {
    for (let x = 0; x < world.grid.width; x++) {
      switch (world.grid.cells[y][x]) {
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
}

function rect(ctx: CanvasRenderingContext2D, x: number, y: number, margin: number): Rect {
  return {
    x1: x * GRID + margin,
    y1: y * GRID + margin,
    x2: x * GRID + GRID - margin,
    y2: y * GRID + GRID - margin,
    width: GRID - 2 * margin,
    height: GRID - 2 * margin,
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

function renderBlocks(ctx: CanvasRenderingContext2D, st: StaticWorld, blocks: Block[]) {
  for (let block of blocks) {
    ctx.lineWidth = GRID / 20;
    const r = rect(ctx, block.x, block.y, ctx.lineWidth / 2);
    drawBlock(ctx, r, styles.blocks[st.blockTypes.indexOf(block.type) % styles.blocks.length], 'white', 'black');

    ctx.textBaseline = 'middle';
    ctx.textAlign = 'center';
    ctx.fillStyle = 'white';
    ctx.fillText(block.type, (block.x + 0.5) * GRID, (block.y + 0.5) * GRID);
  }
}

function renderDynamic(ctx: CanvasRenderingContext2D, st: StaticWorld, dynamic: DynamicWorld) {
  // blocks
  renderBlocks(ctx, st, dynamic.blocks);

  // dispensers
  for (let dispenser of dynamic.dispensers) {
    ctx.lineWidth = GRID / 20;
    const r1 = rect(ctx, dispenser.x, dispenser.y, ctx.lineWidth / 2);
    const color = styles.blocks[st.blockTypes.indexOf(dispenser.type) % styles.blocks.length];
    drawBlock(ctx, r1, color, 'white', 'black');

    const r2 = rect(ctx, dispenser.x, dispenser.y, 4 * ctx.lineWidth / 2);
    drawBlock(ctx, r2, color, 'white', 'black');

    const r3 = rect(ctx, dispenser.x, dispenser.y, 8 * ctx.lineWidth / 2);
    drawBlock(ctx, r3, color, 'white', 'black');

    ctx.textBaseline = 'middle';
    ctx.textAlign = 'center';
    ctx.fillStyle = 'white';
    ctx.fillText(`[${dispenser.type}]`, (dispenser.x + 0.5) * GRID, (dispenser.y + 0.5) * GRID);
  }

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

    ctx.lineWidth = GRID / 20;
    const color = styles.teams[teams.indexOf(agent.team)];
    const r = rect(ctx, agent.x, agent.y, GRID / 8);
    drawBlock(ctx, r, color, 'white', 'black');

    ctx.textBaseline = 'middle';
    ctx.textAlign = 'center';
    ctx.fillStyle = 'white';
    ctx.fillText(agent.name.replace('agent', ''), (agent.x + 0.5) * GRID, (agent.y + 0.5) * GRID);
  }
}
