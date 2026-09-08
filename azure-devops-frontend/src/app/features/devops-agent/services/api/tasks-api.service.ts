import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_ENDPOINTS } from '../../../../core';
import { AgentTask, CancelTaskResponse, TaskStreamEvent } from '../../models/devops-agent.model';

@Injectable({
  providedIn: 'root',
})
export class TasksApiService {
  private readonly http = inject(HttpClient);

  getTasks(): Observable<AgentTask[]> {
    return this.http.get<AgentTask[]>(API_ENDPOINTS.TASKS);
  }

  getTasksStream(): Observable<TaskStreamEvent> {
    return new Observable<TaskStreamEvent>((observer) => {
      const eventSource = new EventSource(API_ENDPOINTS.TASKS_STREAM);

      const handler = (event: MessageEvent<string>) => {
        try {
          observer.next(JSON.parse(event.data) as TaskStreamEvent);
        } catch {
          // Ignorar eventos con formato incorrecto
        }
      };

      eventSource.addEventListener('INITIAL', handler);
      eventSource.addEventListener('TASKS_UPDATE', handler);
      eventSource.onerror = () => {
        // EventSource maneja reconexión automáticamente de fondo
      };

      return () => eventSource.close();
    });
  }

  cancelTask(id: string): Observable<CancelTaskResponse> {
    const payload = {
      jsonrpc: '2.0',
      method: 'tasks/cancel',
      params: {
        taskId: id,
      },
      id: `cancel-${id}`,
    };
    return this.http.post<CancelTaskResponse>(API_ENDPOINTS.TASKS_CANCEL_RPC, payload);
  }
}
