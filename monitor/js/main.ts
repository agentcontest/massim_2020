import { Ctrl } from './interfaces';
import makeCtrl from './ctrl';
import render from './view';

export default function Monitor(canvas: HTMLCanvasElement) {
  let ctrl: Ctrl;
  let redrawRequested = false;

  const redraw = function() {
    if (redrawRequested) return;
    redrawRequested = true;
    requestAnimationFrame(() => {
      redrawRequested = false;
      render(canvas, ctrl);
    });
  };

  ctrl = makeCtrl(redraw);

  redraw();
}
