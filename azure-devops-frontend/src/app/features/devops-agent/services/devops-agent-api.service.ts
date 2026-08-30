import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AgentCard,
  AgentTask,
  DashboardData,
  DashboardStreamEvent,
  IngestPayload,
  Initiative,
  SendMessageRequest,
  SendMessageResponse
} from '../models/devops-agent.model';

/** Mensaje que se muestra si el evento `ERROR` del stream llega sin detalle. */
const DEFAULT_STREAM_ERROR_MESSAGE = 'Fallo en la comunicación con el agente o MCP.';

@Injectable({
  providedIn: 'root',
})
export class DevopsAgentApiService {
  private readonly http = inject(HttpClient);

  getAgentCard(): Observable<AgentCard> {
    return this.http.get<AgentCard>('/.well-known/agent-card.json');
  }

  sendMessage(payload: SendMessageRequest): Observable<SendMessageResponse> {
    return this.http.post<SendMessageResponse>('/message:send', payload);
  }

  uploadPlanning(payload: IngestPayload): Observable<any> {
    return this.http.post<any>('/api/planning/ingest', payload);
  }

  getInitiatives(): Observable<Initiative[]> {
    return this.http.get<Initiative[]>('/api/planning/initiatives');
  }

  deleteInitiative(id: string): Observable<void> {
    return this.http.delete<void>(`/api/planning/initiatives/${id}`);
  }

  updateInitiativeCell(id: string, cell: string): Observable<void> {
    return this.http.put<void>(`/api/planning/initiatives/${id}/cell`, { cell });
  }

  getDashboardData(cell: string, sprint: string): Observable<DashboardData> {
    const params = { cell, sprint };
    return this.http.get<DashboardData>('/api/devops/dashboard', { params });
  }

  /**
   * Abre el stream SSE del tablero.
   *
   * El backend nunca termina el stream en error: si algo falla, envía un evento `ERROR` con su
   * mensaje y completa (D-31). Escucharlo es lo que cierra D-40; hasta ahora solo se atendían
   * `INITIAL` y `BATCH_UPDATE`, así que un fallo del agente llegaba como un `onerror` mudo del
   * `EventSource` y el observable se completaba en silencio: el usuario no distinguía «no hay
   * datos» de «el agente se cayó».
   */
  getDashboardDataStream(cell: string, sprint: string): Observable<DashboardStreamEvent> {
    return new Observable<DashboardStreamEvent>((observer) => {
      const url = `/api/devops/dashboard/stream?cell=${encodeURIComponent(cell)}&sprint=${encodeURIComponent(sprint)}`;
      const eventSource = new EventSource(url);

      const handler = (event: MessageEvent<string>) => {
        try {
          const parsed = JSON.parse(event.data) as DashboardStreamEvent;
          observer.next(parsed);
        } catch (e) {
          console.error('Error parsing SSE event data', e);
        }
      };

      const errorEventHandler = (event: MessageEvent<string>) => {
        let message = DEFAULT_STREAM_ERROR_MESSAGE;
        try {
          message =
            (JSON.parse(event.data) as DashboardStreamEvent)?.message ??
            DEFAULT_STREAM_ERROR_MESSAGE;
        } catch (e) {
          console.error('Error parsing SSE ERROR event data', e);
        }
        eventSource.close();
        observer.error(new Error(message));
      };

      eventSource.addEventListener('INITIAL', handler);
      eventSource.addEventListener('BATCH_UPDATE', handler);
      eventSource.addEventListener('ERROR', errorEventHandler);

      eventSource.onerror = () => {
        eventSource.close();
        observer.complete();
      };

      return () => {
        eventSource.close();
      };
    });
  }

  getInitiativeChunks(id: string): Observable<any[]> {
    return this.http.get<any[]>(`/api/planning/initiatives/${id}/chunks`);
  }

  getTasks(): Observable<AgentTask[]> {
    return this.http.get<AgentTask[]>('/api/tasks');
  }

  cancelTask(id: string): Observable<any> {
    const payload = {
      jsonrpc: '2.0',
      method: 'tasks/cancel',
      params: {
        taskId: id,
      },
      id: `cancel-${id}`,
    };
    return this.http.post<any>('/', payload);
  }
}
