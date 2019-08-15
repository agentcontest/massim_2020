export type Redraw = () => void

type ConnectionState = 'offline' | 'online' | 'connecting' | 'error'

export interface ViewModel {
  state: ConnectionState
  static?: StaticWorld
  dynamic?: DynamicWorld
  taskName?: string
}

export interface Ctrl {
  vm: ViewModel
  redraw: Redraw
}

export type BlockType = string

export interface StaticWorld {
  grid: Grid
  teams: { [key: string]: Team }
  blockTypes: BlockType[]
}

export interface Team {
  name: string
}

export interface Grid {
  width: number
  height: number
}

type Terrain = 0 | 1 | 2

export interface DynamicWorld {
  entities: Agent[]
  blocks: Block[]
  dispensers: Dispenser[]
  tasks: Task[]
  clear: ClearEvent[]
  cells: Terrain[][]
}

export interface Agent {
  x: number
  y: number
  name: string
  id: string
  team: string
}

export interface Block {
  x: number
  y: number
  type: BlockType
}

export interface Dispenser {
  x: number
  y: number
  id: string
  type: BlockType
}

export interface Task {
  reward: number
  name: string
  deadline: number
  requirements: Block[]
}

export interface ClearEvent {
  x: number
  y: number
  radius: number
}

export interface Rect {
  x1: number
  x2: number
  y1: number
  y2: number
  width: number
  height: number
}
