import { Ctrl, DynamicWorld, StaticWorld } from './interfaces';

const GRID = 20;

export default function(canvas: HTMLCanvasElement, ctrl: Ctrl) {
  const ctx = canvas.getContext('2d')!;

  if (ctrl.vm.static) renderStatic(canvas, ctx, ctrl.vm.static);
  if (ctrl.vm.dynamic) renderDynamic(ctx, ctrl.vm.dynamic);
  /*const ctx = canvas.getContext('2d')!;

  ctrl.vm.dynamic.entities.map(agent => {
    ctx.fillStyle = 'green';
    ctx.fillRect(agent.x * GRID, agent.y * GRID, GRID, GRID);
  });a */
}

function renderStatic(canvas: HTMLCanvasElement, ctx: CanvasRenderingContext2D, world: StaticWorld) {
  // canvas size
  canvas.width = world.grid.width * GRID;
  canvas.height = world.grid.height * GRID;

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
          ctx.fillStyle = 'red';
          break;
        case 2: // OBSTABLE
          ctx.fillStyle = '#333';
          break;
      }
      ctx.beginPath();
      ctx.rect(x * GRID, y * GRID, GRID, GRID);
      ctx.fill();
    }
  }
}

function renderDynamic(ctx: CanvasRenderingContext2D, world: DynamicWorld) {
  // blocks
  for (let block of world.blocks) {
    ctx.beginPath();
    ctx.fillStyle = 'green';
    ctx.rect(block.x * GRID, block.y * GRID, GRID, GRID);
    ctx.fill();

    ctx.textBaseline = 'middle';
    ctx.textAlign = 'center';
    ctx.fillStyle = '#fff';
    ctx.fillText(block.type, (block.x + 0.5) * GRID, (block.y + 0.5) * GRID);
  }

  // dispensers
  for (let dispenser of world.dispensers) {
    ctx.beginPath();
    ctx.fillStyle = 'blue';
    ctx.rect(dispenser.x * GRID, dispenser.y * GRID, GRID, GRID);
    ctx.fill();

    ctx.textBaseline = 'middle';
    ctx.textAlign = 'center';
    ctx.fillStyle = '#fff';
    ctx.fillText(dispenser.type, (dispenser.x + 0.5) * GRID, (dispenser.y + 0.5) * GRID);
  }

  // agents
  for (let agent of world.entities) {
    ctx.beginPath();
    ctx.fillStyle = 'orange';
    ctx.rect(agent.x * GRID, agent.y * GRID, GRID, GRID);
    ctx.fill();

    ctx.textBaseline = 'middle';
    ctx.textAlign = 'center';
    ctx.fillStyle = 'black';
    ctx.fillText(agent.name.replace('agent', ''), (agent.x + 0.5) * GRID, (agent.y + 0.5) * GRID);
  }
}
