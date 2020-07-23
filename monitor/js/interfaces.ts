export type Redraw = () => void

export type ConnectionState = 'offline' | 'online' | 'connecting' | 'error'

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
  taskboards?: TaskBoard[]
  tasks: Task[]
  clear: ClearEvent[]
  cells: Terrain[][]
  scores: { [team: string]: number }
}

export interface Positionable {
  x: number
  y: number
}

export interface AgentStatus {
  name: string
  team: string
  action: string // can be empty string before first step
  actionResult: string
}

export interface Agent extends Positionable, AgentStatus {
  id: number
  energy: number
  vision: number
  attached?: Pos[]
  disabled?: boolean
  actionParams: string[]
  acceptedTask?: string // can be empty string before first accept
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
