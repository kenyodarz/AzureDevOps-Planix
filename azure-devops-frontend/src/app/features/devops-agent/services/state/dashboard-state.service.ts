import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { DevopsAgentApiService } from '../devops-agent-api.service';
import { DashboardData } from '../../models/devops-agent.model';

@Injectable({
  providedIn: 'root',
})
export class DashboardStateService {
  private readonly api = inject(DevopsAgentApiService);

  private readonly dashboardData$ = new BehaviorSubject<DashboardData | null>(null);
  public readonly dashboardData: Observable<DashboardData | null> =
    this.dashboardData$.asObservable();

  private readonly dashboardError$ = new BehaviorSubject<string | null>(null);
  public readonly dashboardError: Observable<string | null> = this.dashboardError$.asObservable();

  private readonly loading$ = new BehaviorSubject<boolean>(false);
  public readonly loading: Observable<boolean> = this.loading$.asObservable();

  public loadDashboardData(cell: string, sprint: string): void {
    if (!cell || !sprint) {
      console.warn('Célula y Sprint son requeridos para cargar el dashboard.');
      return;
    }
    this.loading$.next(true);
    this.dashboardError$.next(null);

    this.api.getDashboardDataStream(cell, sprint).subscribe({
      next: (event) => {
        if (event.event === 'INITIAL') {
          this.loading$.next(false);
        }
        if (event.data) {
          this.dashboardData$.next(event.data);
        }
      },
      error: (error) => {
        console.error('Error al cargar datos del dashboard por SSE', error);
        this.loading$.next(false);
        this.dashboardData$.next(null);
        const errText =
          error.error || error.message || 'Fallo en la comunicación con el agente o MCP.';
        this.dashboardError$.next(errText);
      },
      complete: () => {
        this.loading$.next(false);
      },
    });
  }
}
