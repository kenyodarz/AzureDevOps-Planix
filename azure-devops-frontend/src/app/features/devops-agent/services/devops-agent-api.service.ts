import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {
  AgentCard,
  AgentTask,
  DashboardData,
  IngestPayload,
  Initiative,
  SendMessageRequest,
  SendMessageResponse
} from '../models/devops-agent.model';

@Injectable({
  providedIn: 'root'
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
    const params = {cell, sprint};
    return this.http.get<DashboardData>('/api/devops/dashboard', {params});
  }

  getDashboardDataStream(cell: string, sprint: string): Observable<any> {
    return new Observable<any>(observer => {
      const url = `/api/devops/dashboard/stream?cell=${encodeURIComponent(cell)}&sprint=${encodeURIComponent(sprint)}`;
      const eventSource = new EventSource(url);

      const handler = (event: MessageEvent) => {
        try {
          const parsed = JSON.parse(event.data);
          observer.next(parsed);
        } catch (e) {
          console.error('Error parsing SSE event data', e);
        }
      };

      eventSource.addEventListener('INITIAL', handler);
      eventSource.addEventListener('BATCH_UPDATE', handler);

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
        taskId: id
      },
      id: `cancel-${id}`
    };
    return this.http.post<any>('/', payload);
  }
}
