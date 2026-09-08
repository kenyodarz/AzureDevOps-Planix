import { environment } from '../../../environments/environment';

/**
 * Rutas del BFF consumidas por la feature `devops-agent`.
 *
 * Se trasladan LITERALMENTE desde `devops-agent-api.service.ts` (Fase 02, D-08). Ni un carácter
 * cambia: modificar una ruta rompería la aplicación en producción.
 *
 * `environment.apiBaseUrl` está vacío hoy, así que la concatenación es la identidad y el
 * comportamiento observable es idéntico al anterior. Existe para que un despliegue con el BFF en
 * otro origen no requiera tocar la capa de API.
 */
const BASE = environment.apiBaseUrl;

export const API_ENDPOINTS = {
  AGENT_CARD: `${BASE}/.well-known/agent-card.json`,
  MESSAGE_SEND: `${BASE}/message:send`,
  PLANNING_INGEST: `${BASE}/api/planning/ingest`,
  PLANNING_INITIATIVES: `${BASE}/api/planning/initiatives`,
  DEVOPS_DASHBOARD: `${BASE}/api/devops/dashboard`,
  DEVOPS_DASHBOARD_STREAM: `${BASE}/api/devops/dashboard/stream`,
  TASKS: `${BASE}/api/tasks`,
  TASKS_STREAM: `${BASE}/api/tasks/stream`,
  PLANNING_PROGRAM: `${BASE}/api/planning/program`,
  PLANNING_SPECS: `${BASE}/api/planning/specs`,
  /**
   * DP-03 (resuelta 2026-08-31: «conservar la acción por defecto»): la cancelación de tareas viaja
   * como envoltorio JSON-RPC 2.0 contra la RAÍZ del BFF, no contra una ruta REST explícita.
   * NO cambiar sin reabrir DP-03.
   */
  TASKS_CANCEL_RPC: '/',
} as const;

/** URL de una iniciativa concreta: `/api/planning/initiatives/{id}`. */
export const initiativeUrl = (id: string): string => `${API_ENDPOINTS.PLANNING_INITIATIVES}/${id}`;

/** URL de la célula de una iniciativa: `/api/planning/initiatives/{id}/cell`. */
export const initiativeCellUrl = (id: string): string => `${initiativeUrl(id)}/cell`;

/** URL de los fragmentos vectorizados: `/api/planning/initiatives/{id}/chunks`. */
export const initiativeChunksUrl = (id: string): string => `${initiativeUrl(id)}/chunks`;

/** URL de una especificación Markdown documental: `/api/planning/specs/{name}`. */
export const specUrl = (name: string): string =>
  `${API_ENDPOINTS.PLANNING_SPECS}/${encodeURIComponent(name)}`;

/** URL del stream SSE del tablero, con `cell` y `sprint` ya codificados. */
export const dashboardStreamUrl = (cell: string, sprint: string): string =>
  `${API_ENDPOINTS.DEVOPS_DASHBOARD_STREAM}?cell=${encodeURIComponent(cell)}&sprint=${encodeURIComponent(sprint)}`;
