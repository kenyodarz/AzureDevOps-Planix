import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_ENDPOINTS, dashboardStreamUrl, NotificationService } from '../../../../core';
import { DashboardData, DashboardStreamEvent } from '../../models/devops-agent.model';

/** Mensaje que se muestra si el evento `ERROR` del stream llega sin detalle. */
const DEFAULT_STREAM_ERROR_MESSAGE = 'Fallo en la comunicación con el agente o MCP.';

@Injectable({
  providedIn: 'root',
})
export class DashboardApiService {
  private readonly http = inject(HttpClient);
  private readonly notifications = inject(NotificationService);

  getDashboardData(cell: string, sprint: string): Observable<DashboardData> {
    return this.http.get<DashboardData>(API_ENDPOINTS.DEVOPS_DASHBOARD, {
      params: { cell, sprint },
    });
  }

  /** Abre el stream SSE del tablero atendiendo INITIAL, BATCH_UPDATE y ERROR (D-40). */
  getDashboardDataStream(cell: string, sprint: string): Observable<DashboardStreamEvent> {
    return new Observable<DashboardStreamEvent>((observer) => {
      const eventSource = new EventSource(dashboardStreamUrl(cell, sprint));

      const handler = (event: MessageEvent<string>) => {
        try {
          observer.next(JSON.parse(event.data) as DashboardStreamEvent);
        } catch {
          this.notifications.error('Error al procesar evento SSE');
        }
      };

      const errorEventHandler = (event: MessageEvent<string>) => {
        let message = DEFAULT_STREAM_ERROR_MESSAGE;
        try {
          message =
            (JSON.parse(event.data) as DashboardStreamEvent)?.message ??
            DEFAULT_STREAM_ERROR_MESSAGE;
        } catch {
          this.notifications.error('Error al procesar evento SSE de error');
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

      return () => eventSource.close();
    });
  }
}
