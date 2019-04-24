export type Redraw = () => void

type ConnectionState = 'offline' | 'online' | 'connecting' | 'error'

export interface ViewModel {
  state: ConnectionState
  dynamic?: DynamicWorld
}

export interface Ctrl {
  vm: ViewModel
}

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
