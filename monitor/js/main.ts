import { Ctrl } from './interfaces';

import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import klass from 'snabbdom/modules/class';
import props from 'snabbdom/modules/props';
import attributes from 'snabbdom/modules/attributes';
import listeners from 'snabbdom/modules/eventlisteners';

import makeCtrl from './ctrl';
import render from './canvas';
import overlay from './overlay';

const patch = init([klass, props, attributes, listeners]);

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

  ctrl = makeCtrl(redraw);

  redraw();
}
