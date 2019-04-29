import { Ctrl, StaticWorld } from './interfaces';

const GRID = 40;

export default function(canvas: HTMLCanvasElement, ctrl: Ctrl) {
  const ctx = canvas.getContext('2d')!;

  if (ctrl.vm.static) renderStatic(canvas, ctx, ctrl.vm.static);
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

  // goal
  for (let y = 0; y < world.grid.height; y++) {
    for (let x = 0; x < world.grid.width; x++) {
      switch (world.grid.cells[y][x]) {
        case 0: // EMPTY
          continue;
        case 1: // GOAL
          ctx.fillStyle = 'red';
      }
      ctx.beginPath();
      ctx.rect(x * GRID, y * GRID, GRID, GRID);
      ctx.fill();
    }
  }
}
