import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { DevopsAgentApiService } from '../devops-agent-api.service';
import { Initiative, PlanningChunk } from '../../models/devops-agent.model';

@Injectable({
  providedIn: 'root',
})
export class PlanningStateService {
  private readonly api = inject(DevopsAgentApiService);

  private readonly initiatives$ = new BehaviorSubject<Initiative[]>([]);
  public readonly initiatives: Observable<Initiative[]> = this.initiatives$.asObservable();

  private readonly loading$ = new BehaviorSubject<boolean>(false);
  public readonly loading: Observable<boolean> = this.loading$.asObservable();

  private readonly uploading$ = new BehaviorSubject<boolean>(false);
  public readonly uploading: Observable<boolean> = this.uploading$.asObservable();

  private readonly uploadStatus$ = new BehaviorSubject<string | null>(null);
  public readonly uploadStatus: Observable<string | null> = this.uploadStatus$.asObservable();

  public loadInitiatives(): void {
    this.loading$.next(true);
    this.api
      .getInitiatives()
      .pipe(finalize(() => this.loading$.next(false)))
      .subscribe({
        next: (initiatives) => {
          this.initiatives$.next(initiatives);
        },
        error: (error) => {
          console.error('Error al cargar iniciativas', error);
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
        error: (error) => {
          console.error(error);
          const errText = error.error || error.message || 'Error desconocido';
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
        error: (error) => {
          console.error(`Error al actualizar la célula de la iniciativa ${id}`, error);
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
        error: (error) => {
          console.error(`Error al eliminar la iniciativa ${id}`, error);
        },
      });
  }

  public getInitiativeChunks(id: string): Observable<PlanningChunk[]> {
    return this.api.getInitiativeChunks(id);
  }
}
