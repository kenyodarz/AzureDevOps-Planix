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

