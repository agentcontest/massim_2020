import { Redraw, ConnectionState } from './interfaces';

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
  entities: EntityStatus[]
}

export interface EntityStatus {
  name: string
  team: string
  action: string
  actionResult: string
}
