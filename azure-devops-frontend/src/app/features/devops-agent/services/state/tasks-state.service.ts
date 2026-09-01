import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { DevopsAgentApiService } from '../devops-agent-api.service';
import { AgentTask } from '../../models/devops-agent.model';
import { POLLING_INTERVAL_ACTIVE_MS, POLLING_INTERVAL_IDLE_MS } from '../../../../core';

@Injectable({
  providedIn: 'root',
})
export class TasksStateService {
  private readonly api = inject(DevopsAgentApiService);

  private pollingTimer: ReturnType<typeof setTimeout> | null = null;
  private pollingInterval = POLLING_INTERVAL_IDLE_MS;

  private readonly tasks$ = new BehaviorSubject<AgentTask[]>([]);
  public readonly tasks: Observable<AgentTask[]> = this.tasks$.asObservable();

  constructor() {
    this.startDynamicPolling();
  }

  public startDynamicPolling(): void {
    this.loadTasks();
    this.scheduleNextPoll();
  }

  public triggerImmediatePoll(): void {
    this.pollingInterval = POLLING_INTERVAL_ACTIVE_MS;
    this.startDynamicPolling();
  }

  public loadTasks(): void {
    this.api.getTasks().subscribe({
      next: (tasks) => {
        this.tasks$.next(tasks || []);
      },
      error: (error) => {
        console.error('Error al cargar tareas', error);
      },
    });
  }

  public cancelTask(id: string): void {
    this.api.cancelTask(id).subscribe({
      next: () => {
        this.loadTasks();
      },
      error: (error) => {
        console.error(`Error al cancelar tarea ${id}`, error);
      },
    });
  }

  private scheduleNextPoll(): void {
    if (this.pollingTimer) {
      clearTimeout(this.pollingTimer);
    }

    this.pollingTimer = setTimeout(() => {
      this.api.getTasks().subscribe({
        next: (tasks) => {
          this.tasks$.next(tasks || []);
          this.adjustPollingInterval(tasks || []);
          this.scheduleNextPoll();
        },
        error: (error) => {
          console.error('Error al cargar tareas', error);
          this.pollingInterval = POLLING_INTERVAL_IDLE_MS;
          this.scheduleNextPoll();
        },
      });
    }, this.pollingInterval);
  }

  private adjustPollingInterval(tasks: AgentTask[]): void {
    const hasActiveTasks = tasks.some(
      (task) => task.status?.state === 'submitted' || task.status?.state === 'working',
    );
    this.pollingInterval = hasActiveTasks ? POLLING_INTERVAL_ACTIVE_MS : POLLING_INTERVAL_IDLE_MS;
  }
}
