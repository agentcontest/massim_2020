import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import klass from 'snabbdom/modules/class';
import props from 'snabbdom/modules/props';
import attributes from 'snabbdom/modules/attributes';
import listeners from 'snabbdom/modules/eventlisteners';
import style from 'snabbdom/modules/style';

import { Ctrl } from './interfaces';
import makeCtrl from './ctrl';
import { render, invClientPos } from './canvas';
import overlay from './overlay';

import { StatusCtrl } from './statusInterfaces';
import makeStatusCtrl from './statusCtrl';
import statusView from './statusView';

const patch = init([klass, props, attributes, listeners, style]);

export default function Monitor(overlayTarget: Element, canvas: HTMLCanvasElement) {
  let vnode: VNode | Element = overlayTarget;
  let ctrl: Ctrl;

  let redrawRequested = false;

  const redraw = function() {
    if (redrawRequested) return;
    redrawRequested = true;
    requestAnimationFrame(() => {
      redrawRequested = false;
      vnode = patch(vnode, overlay(ctrl));
      render(canvas, ctrl);
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
  ctrl = makeCtrl(redraw, replayPath);

  hashChange();
  window.onhashchange = hashChange;

  redraw();

  window.addEventListener('resize', redraw, { passive: true });

  canvas.addEventListener('mousemove', e => {
    if (!ctrl.vm.static) return;
    ctrl.setHover(invClientPos(canvas, ctrl.vm.static, e.clientX, e.clientY));
  });
  canvas.addEventListener('mouseleave', e => {
    ctrl.setHover(undefined);
  });
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
