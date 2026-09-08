import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of, Subject, timer } from 'rxjs';
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';
import {
  NotificationService,
  POLLING_INTERVAL_ACTIVE_MS,
  POLLING_INTERVAL_IDLE_MS,
} from '../../../../core';
import { DevopsAgentApiService } from '../devops-agent-api.service';
import { AgentTask } from '../../models/devops-agent.model';

@Injectable({
  providedIn: 'root',
})
export class TasksStateService {
  private readonly api = inject(DevopsAgentApiService);
  private readonly notifications = inject(NotificationService);

  private pollingInterval = POLLING_INTERVAL_IDLE_MS;
  private readonly pollTrigger$ = new Subject<{ delay: number }>();
  private readonly destroy$ = new Subject<void>();

  private readonly tasks$ = new BehaviorSubject<AgentTask[]>([]);
  public readonly tasks: Observable<AgentTask[]> = this.tasks$.asObservable();

  private isStreamActive = false;

  constructor() {
    this.setupStreamPipeline();
    if (!this.isStreamActive) {
      this.setupPollingPipeline();
      this.startDynamicPolling();
    }
  }

  public triggerImmediatePoll(): void {
    if (this.isStreamActive) {
      this.loadTasks();
    } else {
      this.pollingInterval = POLLING_INTERVAL_ACTIVE_MS;
      this.startDynamicPolling();
    }
  }

  public stopDynamicPolling(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  public startDynamicPolling(): void {
    this.loadTasks();
    this.scheduleNextPoll();
  }

  private setupStreamPipeline(): void {
    if (typeof this.api.getTasksStream === 'function') {
      try {
        const stream$ = this.api.getTasksStream();
        if (stream$) {
          this.isStreamActive = true;
          stream$.pipe(takeUntil(this.destroy$)).subscribe({
            next: (event) => {
              if (event && event.data) {
                this.tasks$.next(event.data);
              }
            },
            error: () => {
              if (this.isStreamActive) {
                this.isStreamActive = false;
                this.setupPollingPipeline();
                this.startDynamicPolling();
              }
            },
          });
        }
      } catch {
        this.isStreamActive = false;
      }
    }
  }

  public loadTasks(): void {
    this.api
      .getTasks()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (tasks) => {
          this.tasks$.next(tasks || []);
        },
        error: () => {
          this.notifications.error('Error al cargar tareas');
        },
      });
  }

  public cancelTask(id: string): void {
    this.api
      .cancelTask(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.loadTasks();
        },
        error: () => {
          this.notifications.error(`Error al cancelar tarea ${id}`);
        },
      });
  }

  private setupPollingPipeline(): void {
    this.pollTrigger$
      .pipe(
        switchMap(({ delay }) =>
          timer(delay).pipe(
            switchMap(() =>
              this.api.getTasks().pipe(
                tap((tasks) => {
                  this.tasks$.next(tasks || []);
                  this.adjustPollingInterval(tasks || []);
                }),
                catchError(() => {
                  this.notifications.error('Error al cargar tareas');
                  this.pollingInterval = POLLING_INTERVAL_IDLE_MS;
                  return of(null);
                }),
              ),
            ),
          ),
        ),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: () => {
          this.scheduleNextPoll();
        },
      });
  }

  private scheduleNextPoll(): void {
    this.pollTrigger$.next({ delay: this.pollingInterval });
  }

  private adjustPollingInterval(tasks: AgentTask[]): void {
    const hasActiveTasks = tasks.some(
      (task) => task.status?.state === 'submitted' || task.status?.state === 'working',
    );
    this.pollingInterval = hasActiveTasks ? POLLING_INTERVAL_ACTIVE_MS : POLLING_INTERVAL_IDLE_MS;
  }
}
