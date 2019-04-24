import { Ctrl } from './interfaces';

const GRID = 2;

export default function(canvas: HTMLCanvasElement, ctrl: Ctrl) {
  if (!ctrl.vm.dynamic) return;

  const ctx = canvas.getContext('2d')!;

  ctrl.vm.dynamic.entities.map(agent => {
    ctx.fillStyle = 'green';
    ctx.fillRect(agent.x * GRID, agent.y * GRID, GRID, GRID);
  });
}
