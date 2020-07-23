import { Redraw, ConnectionState, AgentStatus } from './interfaces';

export interface StatusViewModel {
  state: ConnectionState
  data?: StatusData
}

export interface StatusCtrl {
  vm: StatusViewModel;
  redraw: Redraw
}

export interface StatusData {
  sim: string
  step: number
  steps: number
  entities: AgentStatus[]
}
