export type Style = {
  background: string,
  color: string,
};

const teams: Style[] = [
  { background: '#0000ff', color: 'white' },
  { background: '#00ff00', color: 'black' },
  { background: '#ff1493', color: 'white' },
  { background: '#8b0000', color: 'white' },
  { background: '#ed553b', color: 'white' },
  { background: '#a63d40', color: 'white' },
  { background: '#e9b872', color: 'black' },
  { background: '#90a959', color: 'white' },
  { background: '#6494aa', color: 'white' },
  { background: '#192457', color: 'white' },
  { background: '#2b5397', color: 'white' },
  { background: '#a2dcdc', color: 'black' },
  { background: '#27ec5f', color: 'black' },
  { background: '#3ab1ad', color: 'white' },
];

export function team(index: number): Style {
  return teams[index % teams.length];
}

export const goal = 'rgba(255, 0, 0, 0.4)';
export const goalOnLight = '#f58f8f';

export const obstacle = '#333';

export const board = '#00ffff';

export const blocks = ['#41470b', '#78730d', '#bab217', '#e3d682', '#b3a06f', '#9c7640', '#5a4c35'];

export const hover = 'rgba(180, 180, 255, 0.4)';
