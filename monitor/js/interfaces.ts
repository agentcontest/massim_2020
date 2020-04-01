export type Redraw = () => void

export type ConnectionState = 'offline' | 'online' | 'connecting' | 'error'

export interface ViewModel {
  state: ConnectionState
  static?: StaticWorld
  dynamic?: DynamicWorld
  taskName?: string
  hover?: Pos
}

export interface ReplayCtrl {
  name(): string
  step(): number
  setStep(s: number): void
  toggle(): void
  stop(): void
  start(): void
  playing(): boolean
}

export interface Ctrl {
  vm: ViewModel
  redraw: Redraw
  replay?: ReplayCtrl
  setHover(pos?: Pos): void
}

export type BlockType = string

export interface StaticWorld {
  sim: string
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
  taskboards: TaskBoard[]
  tasks: Task[]
  clear: ClearEvent[]
  cells: Terrain[][]
  scores: { [team: string]: number }
}

export interface Positionable {
  x: number
  y: number
}

export interface Agent extends Positionable {
  id: number
  name: string
  team: string
  energy: number
  vision: number
  attached?: Pos[]
  disabled?: boolean
  action: string
  actionParams: string[]
  actionResult: string
}

export interface Block extends Positionable {
  type: BlockType
  attached?: Pos[]
}

export interface Dispenser extends Positionable {
  id: number
  type: BlockType
}

export type TaskBoard = Positionable

export interface Task {
  reward: number
  name: string
  deadline: number
  requirements: Block[]
}

export interface ClearEvent extends Positionable {
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

export type Pos = Positionable
