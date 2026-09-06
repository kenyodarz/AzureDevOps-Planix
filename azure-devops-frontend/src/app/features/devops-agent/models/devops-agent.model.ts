export interface AgentCard {
  name: string;
  version: string;
  description: string;
}

export interface MessagePart {
  text: string;
}

export interface Message {
  role: 'user' | 'agent';
  messageId?: string;
  contextId?: string;
  parts: MessagePart[];
}

export interface SendMessageRequest {
  message: {
    role: 'user';
    messageId: string;
    contextId: string;
    parts: MessagePart[];
  };
}

export interface SendMessageResponse {
  message?: {
    role?: 'agent';
    messageId?: string;
    contextId?: string;
    parts?: MessagePart[];
  };
}

export interface IngestPayload {
  initiativeId: string;
  title: string;
  markdownContent: string;
}

export interface Initiative {
  initiative_id: string;
  initiative_title: string;
  cell?: string;
}

export interface UpdateCellPayload {
  cell: string;
}

export interface DashboardMetrics {
  totalPoints: number;
  completedPoints: number;
  completedPercentage: number;
  avgQualityScore: number;
  undocumentedCount: number;
}

export interface DashboardStoryItem {
  id: string;
  title: string;
  points: number;
  state: string;
  hasAcceptanceCriteria: boolean;
  hasDoR?: boolean;
  hasDoD: boolean;
  qualityScore: number;
  linkedTasksCount: number;
  assignedMember?: string;
  feedback?: string;
}

export interface DashboardData {
  metrics: DashboardMetrics;
  items: DashboardStoryItem[];
}

/**
 * Tipo de evento que emite el stream SSE del tablero.
 *
 * `ERROR` lo añadió la Fase 06 del backend: desde entonces el stream nunca termina en error, sino
 * que el fallo viaja como un evento más para que el navegador llegue a recibir la cabecera
 * `text/event-stream` y el usuario pueda leer el motivo (D-31).
 */
export type DashboardStreamEventType = 'INITIAL' | 'BATCH_UPDATE' | 'ERROR';

/**
 * Evento del stream `GET /api/devops/dashboard/stream`.
 *
 * En los eventos `INITIAL` y `BATCH_UPDATE` viaja `data`; en los `ERROR`, `message`.
 */
export interface DashboardStreamEvent {
  event: DashboardStreamEventType;
  data: DashboardData | null;
  message?: string | null;
}

export interface AgentTaskStatus {
  state: 'submitted' | 'working' | 'completed' | 'failed' | 'canceled' | 'rejected' | 'input-required';
  message?: Message;
  timestamp?: string;
}

export interface AgentTask {
  id: string;
  contextId?: string;
  status?: AgentTaskStatus;
}

// -------------------------------------------------------------------------------------------------
// Contratos derivados de `azure-devops-backend` / `azure-devops-agent` (FASE 02, DP-09).
//
// DP-09 se resolvió el 2026-08-31: el usuario autorizó derivar estos tres tipos de la fuente de
// verdad del contrato en lugar de inventarlos. No se ha supuesto ningún campo.
// -------------------------------------------------------------------------------------------------

/**
 * Respuesta de `POST /api/planning/ingest`.
 *
 * Fuente: `azure-devops-backend/.../api/Handler.java:60-69` → `{ "message": "..." }`.
 */
export interface IngestResponse {
  message: string;
}

/**
 * Fragmento vectorizado de una planeación, devuelto por
 * `GET /api/planning/initiatives/{id}/chunks`.
 *
 * Fuente: `azure-devops-backend/.../api/dto/planning/PlanningChunkResponse.java:23-29`.
 * Serializa en camelCase (a diferencia de `Initiative`, que llega en snake_case desde el adaptador
 * pgvector).
 */
export interface PlanningChunk {
  id: string;
  initiativeId: string;
  sectionName: string | null;
  content: string;
  metadata: Record<string, unknown>;
}

/**
 * Error de una respuesta JSON-RPC 2.0.
 *
 * Fuente: `azure-devops-agent/.../api/JsonRpcResponse.java:13-20`.
 */
export interface JsonRpcError {
  code: number;
  message: string;
  data?: unknown;
}

/**
 * Envoltorio de respuesta JSON-RPC 2.0 del agente.
 *
 * Fuente: `azure-devops-agent/.../api/JsonRpcResponse.java:13-20` y
 * `JsonRpcResponseFactory.java:74-82`.
 */
export interface JsonRpcResponse<TResult> {
  jsonrpc: string;
  id: string;
  result?: TResult | null;
  error?: JsonRpcError | null;
}

/**
 * Respuesta del método JSON-RPC `tasks/cancel`.
 *
 * DP-03 (resuelta): viaja como `POST '/'`, no como ruta REST. El `result` envuelve la tarea ya
 * cancelada.
 */
export type CancelTaskResponse = JsonRpcResponse<{ task: AgentTask }>;

// -------------------------------------------------------------------------------------------------
// Contratos para Program Planning y Especificaciones Documentales (FASE 01).
// -------------------------------------------------------------------------------------------------

/** Parámetros para iniciar la planeación de un programa trimestral. */
export interface ProgramPlanRequestDTO {
  readonly quarter: string;
  readonly sprintCount: number;
  readonly maxCapacityPerSprint: number;
  readonly targetFronts: readonly string[];
  readonly objectives: string;
  readonly contextId?: string;
}

/** Respuesta inmediata (202 Accepted) tras encolar la planeación macro. */
export interface ProgramPlanResponseDTO {
  readonly summary: string;
  readonly quarter: string;
  readonly sprintCount: number;
  readonly maxCapacityPerSprint: number;
  readonly targetFronts: readonly string[];
  readonly contextId: string;
  readonly task: AgentTask;
}

/** Listado de especificaciones documentales disponibles en el repositorio. */
export interface SpecListDTO {
  readonly specs: readonly string[];
  readonly total: number;
}

/** Contenido íntegro de una especificación Markdown. */
export interface SpecDocumentDTO {
  readonly name: string;
  readonly content: string;
  readonly path: string;
}

/** Vista activa en el centro unificado de planeación. */
export type PlanningActiveView = 'explorer' | 'new-plan' | 'legacy-vector';



