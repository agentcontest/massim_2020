import { Ctrl, DynamicWorld, StaticWorld } from './interfaces';
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

function renderDynamic(ctx: CanvasRenderingContext2D, st: StaticWorld, dynamic: DynamicWorld) {
  // blocks
  for (let block of dynamic.blocks) {
    ctx.beginPath();
    ctx.fillStyle = styles.blocks[st.blockTypes.indexOf(block.type) % styles.blocks.length];
    ctx.rect(block.x * GRID, block.y * GRID, GRID, GRID);
    ctx.fill();

    ctx.textBaseline = 'middle';
    ctx.textAlign = 'center';
    ctx.fillStyle = 'white';
    ctx.fillText(block.type, (block.x + 0.5) * GRID, (block.y + 0.5) * GRID);
  }

  // dispensers
  for (let dispenser of dynamic.dispensers) {
    ctx.beginPath();
    ctx.fillStyle = styles.blocks[st.blockTypes.indexOf(dispenser.type) % styles.blocks.length];
    ctx.rect(dispenser.x * GRID, dispenser.y * GRID, GRID, GRID);
    ctx.fill();

    ctx.textBaseline = 'middle';
    ctx.textAlign = 'center';
    ctx.fillStyle = 'white';
    ctx.fillText(dispenser.type, (dispenser.x + 0.5) * GRID, (dispenser.y + 0.5) * GRID);
  }

  // agents
  const teams = Object.keys(st.teams);
  teams.sort();
  for (let agent of dynamic.entities) {
    ctx.beginPath();
    ctx.fillStyle = styles.teams[teams.indexOf(agent.team)];
    ctx.rect(agent.x * GRID, agent.y * GRID, GRID, GRID);
    ctx.fill();

    ctx.textBaseline = 'middle';
    ctx.textAlign = 'center';
    ctx.fillStyle = 'white';
    ctx.fillText(agent.name.replace('agent', ''), (agent.x + 0.5) * GRID, (agent.y + 0.5) * GRID);
  }
}
