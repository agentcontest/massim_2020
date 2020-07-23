import { AgentStatus } from './interfaces';

export function compareAgent(a: AgentStatus, b: AgentStatus): number {
  if (a.team < b.team) return -1;
  else if (a.team > b.team) return 1;

  const suffixA = parseInt(a.name.replace(/^[^\d]*/, ''), 10);
  const suffixB = parseInt(b.name.replace(/^[^\d]*/, ''), 10);
  if (suffixA < suffixB) return -1;
  else if (suffixA > suffixB) return 1;

  if (a.name < b.name) return -1;
  else if (a.name > b.name) return 1;
  else return 0;
}
