export type Redraw = () => void

type ConnectionState = 'offline' | 'online' | 'connecting' | 'error'

export interface ViewModel {
  state: ConnectionState
  static?: StaticWorld
  dynamic?: DynamicWorld
  taskName?: string
  hover?: Pos
}

export interface Ctrl {
  vm: ViewModel
  redraw: Redraw
  setHover(pos?: Pos): void
}

export type BlockType = string

export interface StaticWorld {
  grid: Grid
  teams: { [key: string]: Team }
  blockTypes: BlockType[]
  steps: number
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
  step: number
  entities: Agent[]
  blocks: Block[]
  dispensers: Dispenser[]
  tasks: Task[]
  clear: ClearEvent[]
  cells: Terrain[][]
  scores: { [team: string]: number }
}

export interface Agent {
  id: number
  x: number
  y: number
  name: string
  team: string
  energy: number
  vision: number
  attached?: Pos[]
}

export interface Block {
  x: number
  y: number
  type: BlockType
  attached?: Pos[]
}

export interface Dispenser {
  id: number
  x: number
  y: number
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

export interface Pos {
  x: number
  y: number
}
