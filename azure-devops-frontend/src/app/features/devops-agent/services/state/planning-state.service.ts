import { inject, Injectable, Signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { catchError, finalize, tap } from 'rxjs/operators';
import { extractErrorMessage, NotificationService } from '../../../../core';
import { DevopsAgentApiService } from '../devops-agent-api.service';
import { TasksStateService } from './tasks-state.service';
import {
  Initiative,
  PlanningChunk,
  ProgramPlanRequestDTO,
  ProgramPlanResponseDTO,
  SpecDocumentDTO,
} from '../../models/devops-agent.model';

@Injectable({
  providedIn: 'root',
})
export class PlanningStateService {
  private readonly api = inject(DevopsAgentApiService);
  private readonly notifications = inject(NotificationService);

  private readonly initiatives$ = new BehaviorSubject<Initiative[]>([]);
  public readonly initiatives: Observable<Initiative[]> = this.initiatives$.asObservable();

  private readonly loading$ = new BehaviorSubject<boolean>(false);
  public readonly loading: Observable<boolean> = this.loading$.asObservable();

  private readonly uploading$ = new BehaviorSubject<boolean>(false);
  public readonly uploading: Observable<boolean> = this.uploading$.asObservable();

  private readonly uploadStatus$ = new BehaviorSubject<string | null>(null);
  public readonly uploadStatus: Observable<string | null> = this.uploadStatus$.asObservable();

  // Estado reactivo para Especificaciones Documentales y Program Planning
  private readonly availableSpecs$ = new BehaviorSubject<string[]>([]);
  public readonly availableSpecs: Observable<string[]> = this.availableSpecs$.asObservable();

  private readonly selectedSpec$ = new BehaviorSubject<SpecDocumentDTO | null>(null);
  public readonly selectedSpec: Observable<SpecDocumentDTO | null> =
    this.selectedSpec$.asObservable();

  private readonly loadingSpecs$ = new BehaviorSubject<boolean>(false);
  public readonly loadingSpecs: Observable<boolean> = this.loadingSpecs$.asObservable();

  private readonly planningRunning$ = new BehaviorSubject<boolean>(false);
  public readonly planningRunning: Observable<boolean> = this.planningRunning$.asObservable();
  public readonly availableSpecsSignal: Signal<string[]> = toSignal(this.availableSpecs, {
    initialValue: [] as string[],
  });
  public readonly selectedSpecSignal: Signal<SpecDocumentDTO | null> = toSignal(this.selectedSpec, {
    initialValue: null,
  });
  public readonly loadingSpecsSignal: Signal<boolean> = toSignal(this.loadingSpecs, {
    initialValue: false,
  });
  public readonly planningRunningSignal: Signal<boolean> = toSignal(this.planningRunning, {
    initialValue: false,
  });
  private readonly tasksState = inject(TasksStateService);

  public loadInitiatives(): void {
    this.loading$.next(true);
    this.api
      .getInitiatives()
      .pipe(finalize(() => this.loading$.next(false)))
      .subscribe({
        next: (initiatives) => {
          this.initiatives$.next(initiatives);
        },
        error: () => {
          this.notifications.error('Error al cargar iniciativas');
        },
      });
  }

  public uploadPlanning(initiativeId: string, title: string, content: string): void {
    this.uploading$.next(true);
    this.uploadStatus$.next('Vectorizando planeación...');

    this.api
      .uploadPlanning({ initiativeId, title, markdownContent: content })
      .pipe(finalize(() => this.uploading$.next(false)))
      .subscribe({
        next: () => {
          this.uploadStatus$.next('¡Planeación indexada con éxito!');
          this.loadInitiatives();
        },
        error: (error: unknown) => {
          this.notifications.error('Error al cargar planeación');
          const errText = extractErrorMessage(error, 'Error desconocido');
          this.uploadStatus$.next(`Error: ${errText}`);
        },
      });
  }

  public clearUploadStatus(): void {
    this.uploadStatus$.next(null);
  }

  public updateInitiativeCell(id: string, cell: string): void {
    this.loading$.next(true);
    this.api
      .updateInitiativeCell(id, cell)
      .pipe(finalize(() => this.loading$.next(false)))
      .subscribe({
        next: () => {
          const updated = this.initiatives$.value.map((init) =>
            init.initiative_id === id ? { ...init, cell } : init,
          );
          this.initiatives$.next(updated);
        },
        error: () => {
          this.notifications.error(`Error al actualizar la célula de la iniciativa ${id}`);
        },
      });
  }

  public deleteInitiative(id: string): void {
    this.loading$.next(true);
    this.api
      .deleteInitiative(id)
      .pipe(finalize(() => this.loading$.next(false)))
      .subscribe({
        next: () => {
          const filtered = this.initiatives$.value.filter((init) => init.initiative_id !== id);
          this.initiatives$.next(filtered);
        },
        error: () => {
          this.notifications.error(`Error al eliminar la iniciativa ${id}`);
        },
      });
  }

  public getInitiativeChunks(id: string): Observable<PlanningChunk[]> {
    return this.api.getInitiativeChunks(id);
  }

  public loadAvailableSpecs(): void {
    this.loadingSpecs$.next(true);
    this.api
      .getAvailableSpecs()
      .pipe(finalize(() => this.loadingSpecs$.next(false)))
      .subscribe({
        next: (res) => {
          this.availableSpecs$.next([...res.specs]);
        },
        error: () => {
          this.notifications.error('Error al cargar especificaciones documentales');
        },
      });
  }

  public selectSpec(name: string): void {
    const trimmed = name?.trim();
    if (!trimmed) {
      return;
    }

    this.loadingSpecs$.next(true);
    this.api
      .getSpecDocument(trimmed)
      .pipe(finalize(() => this.loadingSpecs$.next(false)))
      .subscribe({
        next: (doc) => {
          this.selectedSpec$.next(doc);
        },
        error: () => {
          this.notifications.error(`Error al cargar el documento ${trimmed}`);
        },
      });
  }

  public clearSelectedSpec(): void {
    this.selectedSpec$.next(null);
  }

  public triggerProgramPlanning(
    request: ProgramPlanRequestDTO,
  ): Observable<ProgramPlanResponseDTO> {
    this.planningRunning$.next(true);
    return this.api.triggerProgramPlanning(request).pipe(
      tap((response) => {
        this.notifications.success(`Planeación para ${response.quarter} encolada exitosamente`);
        this.tasksState.triggerImmediatePoll();
      }),
      catchError((error: unknown) => {
        this.notifications.error('Error al solicitar planeación de programa');
        return throwError(() => error);
      }),
      finalize(() => this.planningRunning$.next(false)),
    );
  }
}
