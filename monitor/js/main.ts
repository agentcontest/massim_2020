import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import { classModule } from 'snabbdom/modules/class';
import { attributesModule } from 'snabbdom/modules/attributes';
import { eventListenersModule } from 'snabbdom/modules/eventlisteners';
import { styleModule } from 'snabbdom/modules/style';

import { Ctrl } from './ctrl';
import { view } from './view';

import { StatusCtrl } from './statusInterfaces';
import { makeStatusCtrl } from './statusCtrl';
import { statusView } from './statusView';

const patch = init([
  classModule,
  attributesModule,
  styleModule,
  eventListenersModule
]);

export function Monitor(element: Element) {
  let vnode: VNode | Element = element;
  let ctrl: Ctrl;

  let redrawRequested = false;

  const redraw = function() {
    if (redrawRequested) return;
    redrawRequested = true;
    requestAnimationFrame(() => {
      redrawRequested = false;
      vnode = patch(vnode, view(ctrl));
    });
  };

  const hashChange = function() {
    if (ctrl.replay) {
      const step = parseInt(document.location.hash.substr(1), 10);
      if (step > 0) ctrl.replay.setStep(step);
      else if (!document.location.hash) ctrl.replay.start();
    }
  };

  const replayPath = window.location.search.length > 1 ? window.location.search.substr(1) : undefined;
  ctrl = new Ctrl(redraw, replayPath);

  hashChange();
  window.onhashchange = hashChange;

  redraw();

  /* canvas.addEventListener('mousemove', e => {
    if (!ctrl.vm.static) return;
    ctrl.setHover(invClientPos(canvas, ctrl.vm.static, e.clientX, e.clientY));
  });
  canvas.addEventListener('mouseleave', e => {
    ctrl.setHover(undefined);
  }); */
}

export function Status(target: Element) {
  let vnode: VNode | Element = target;
  let ctrl: StatusCtrl;

  let redrawRequested = false;

  const redraw = function() {
    if (redrawRequested) return;
    redrawRequested = true;
    requestAnimationFrame(() => {
      redrawRequested = false;
      vnode = patch(vnode, statusView(ctrl));
    });
  };

  ctrl = makeStatusCtrl(redraw);

  redraw();
}
