import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of, Subject, timer } from 'rxjs';
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';
import { DevopsAgentApiService } from '../devops-agent-api.service';
import { AgentTask } from '../../models/devops-agent.model';
import { POLLING_INTERVAL_ACTIVE_MS, POLLING_INTERVAL_IDLE_MS } from '../../../../core';

@Injectable({
  providedIn: 'root',
})
export class TasksStateService {
  private readonly api = inject(DevopsAgentApiService);

  private pollingInterval = POLLING_INTERVAL_IDLE_MS;
  private readonly pollTrigger$ = new Subject<{ delay: number }>();
  private readonly destroy$ = new Subject<void>();

  private readonly tasks$ = new BehaviorSubject<AgentTask[]>([]);
  public readonly tasks: Observable<AgentTask[]> = this.tasks$.asObservable();

  constructor() {
    this.setupPollingPipeline();
    this.startDynamicPolling();
  }

  public stopDynamicPolling(): void {
    this.destroy$.next();
    this.destroy$.complete();
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
    this.api
      .getTasks()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (tasks) => {
          this.tasks$.next(tasks || []);
        },
        error: (error) => {
          console.error('Error al cargar tareas', error);
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
        error: (error) => {
          console.error(`Error al cancelar tarea ${id}`, error);
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
                catchError((error) => {
                  console.error('Error al cargar tareas', error);
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
