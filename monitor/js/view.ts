import { h } from 'snabbdom/h';
import { VNode } from 'snabbdom/vnode';

import { Ctrl } from './ctrl';
import { overlay } from './overlay';
import { mapView } from './map';

export function view(ctrl: Ctrl): VNode {
  return h('div#monitor', [
    overlay(ctrl),
    mapView(ctrl.map),
  ]);
}
