import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';

import { StatusCtrl } from './statusInterfaces';

export default function(ctrl: StatusCtrl): VNode {
  return h('div', 'hello world!');
}
