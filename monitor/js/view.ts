import { h } from 'snabbdom/h';
import { VNode } from 'snabbdom/vnode';

import { Ctrl } from './ctrl';
import { overlay } from './overlay';
import { MapCtrl, mapView } from './map';

export function view(ctrl: Ctrl): VNode {
  return h('div#monitor', [
    overlay(ctrl),
    ctrl.maps.length > 0 ? allMaps(ctrl.maps) : mapView(ctrl.map),
  ]);
}

function allMaps(maps: MapCtrl[]): VNode {
  return h('div.maps', maps.map(m => mapView(m, { size: 200, viewOnly: true, denseFog: true })));
}
