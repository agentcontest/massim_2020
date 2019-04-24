export default function Monitor(canvas: HTMLCanvasElement) {
  const ctx = canvas.getContext('2d')!;
  ctx.fillStyle = 'green';
  ctx.fillRect(10, 10, 10, 10);
}
