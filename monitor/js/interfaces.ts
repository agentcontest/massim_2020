export type Redraw = () => void

type ConnectionState = 'offline' | 'online' | 'connecting' | 'error'

export interface ViewModel {
  state: ConnectionState
  static?: StaticWorld
  dynamic?: DynamicWorld
}

export interface Ctrl {
  vm: ViewModel
}

export interface StaticWorld {
  grid: Grid
  teams: { [key: string]: Team }
}

export interface Team {
  name: string
}

export interface Grid {
  width: number
  height: number
  cells: Terrain[][]
}

type Terrain = 0 | 1 | 2

export interface DynamicWorld {
  entities: Agent[]
}

export interface Agent {
  x: number
  y: number
  name: string
  id: string
  team: string
}
