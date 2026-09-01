import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';
import { PlanningManagementComponent } from './planning-management.component';
import { DevopsAgentStateService } from '../../services/devops-agent-state.service';
import { DevopsAgentApiService } from '../../services/devops-agent-api.service';
import { Initiative } from '../../models/devops-agent.model';

/**
 * Caracterización FASE 01 — Gestión de Planeaciones.
 *
 * D-26 (nueva) — Este componente vive en `components/` (capa de presentación) pero inyecta
 * DIRECTAMENTE `DevopsAgentApiService` además del servicio de estado. Es el único que se salta la
 * capa de estado para hablar con la red. La FASE 05 lo corregirá; aquí solo se congela.
 */

interface ManagementInternals {
  editableInitiatives: {
    initiative_id: string;
    initiative_title: string;
    cell: string;
    tempCell: string;
  }[];
  saveCell(init: { initiative_id: string; tempCell: string }): void;
  deleteInitiative(init: { initiative_id: string; initiative_title: string }): void;
  previewInitiative(init: { initiative_id: string; initiative_title: string }): void;
  closeModal(): void;
  isModalOpen(): boolean;
  loadingChunks(): boolean;
  selectedChunks(): unknown[];
  selectedInitiativeTitle(): string;
}

class MockStateService {
  readonly initiatives$ = new BehaviorSubject<Initiative[]>([]);
  readonly loading$ = new BehaviorSubject<boolean>(false);

  readonly initiatives: Observable<Initiative[]> = this.initiatives$.asObservable();
  readonly loading: Observable<boolean> = this.loading$.asObservable();

  readonly loadInitiatives = vi.fn((): void => undefined);
  readonly updateInitiativeCell = vi.fn((_id: string, _cell: string): void => undefined);
  readonly deleteInitiative = vi.fn((_id: string): void => undefined);
}

class MockApiService {
  chunksResult: Observable<unknown[]> = of([]);
  readonly getInitiativeChunks = vi.fn((_id: string): Observable<unknown[]> => this.chunksResult);
}

const initiative = (id: string, cell?: string): Initiative => ({
  initiative_id: id,
  initiative_title: `Iniciativa ${id}`,
  cell,
});

describe('GIVEN PlanningManagementComponent', () => {
  let fixture: ComponentFixture<PlanningManagementComponent>;
  let internals: ManagementInternals;
  let state: MockStateService;
  let api: MockApiService;

  const rows = (): HTMLElement[] => Array.from(fixture.nativeElement.querySelectorAll('tbody tr'));

  /**
   * D-27 — La app corre en modo ZONELESS (`app.config.ts` no declara zona) y este componente guarda
   * la tabla en `editableInitiatives`, un CAMPO PLANO mutado desde una suscripción RxJS. Publicar
   * iniciativas NO marca la vista como sucia, así que el `@if/@else if` no se reevalúa y la tabla
   * no se repinta (ver la prueba «THEN publishing initiatives alone does NOT repaint»).
   *
   * En producción la tabla sí aparece, pero POR ACCIDENTE: `loadInitiatives()` conmuta el
   * BehaviorSubject `loading$`, que alimenta el signal `loading()` del componente, y ese cambio
   * reactivo es el único que fuerza el repintado. Este helper reproduce esa secuencia real.
   *
   * La FASE 05 debe migrar `editableInitiatives` a un signal ANTES de separar el `loading` por
   * flujo (D-16), o la tabla dejará de pintarse.
   */
  const publishAndRepaint = (initiatives: Initiative[]): void => {
    state.loading$.next(true);
    state.initiatives$.next(initiatives);
    state.loading$.next(false);
    fixture.detectChanges();
  };

  beforeEach(async () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    state = new MockStateService();
    api = new MockApiService();

    await TestBed.configureTestingModule({
      imports: [PlanningManagementComponent],
      providers: [
        { provide: DevopsAgentStateService, useValue: state as unknown as DevopsAgentStateService },
        { provide: DevopsAgentApiService, useValue: api as unknown as DevopsAgentApiService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PlanningManagementComponent);
    internals = fixture.componentInstance as unknown as ManagementInternals;
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('WHEN the component initialises', () => {
    it('THEN asks the state to load the initiatives', () => {
      expect(state.loadInitiatives).toHaveBeenCalledTimes(1);
    });

    it('THEN shows the empty state when there are no initiatives', () => {
      expect(fixture.nativeElement.textContent).toContain('No hay iniciativas indexadas');
    });

    it('THEN shows the spinner while the state reports loading', () => {
      state.loading$.next(true);

      fixture.detectChanges();

      expect(fixture.nativeElement.textContent).toContain('Cargando iniciativas...');
    });
  });

  // D-27 — Defecto vivo del modo zoneless. Esta prueba lo deja registrado con toda claridad.
  describe('WHEN initiatives arrive without any reactive signal changing (D-27, corregir en fase 05)', () => {
    it('THEN publishing initiatives alone does NOT repaint the table', () => {
      state.initiatives$.next([initiative('i1', 'Celula Core')]);
      fixture.detectChanges();

      expect(internals.editableInitiatives).toHaveLength(1);
      expect(rows()).toHaveLength(0);
      expect(fixture.nativeElement.textContent).toContain('No hay iniciativas indexadas');
    });

    it('THEN the table only repaints once the shared loading signal flips (acoplamiento accidental con D-16)', () => {
      publishAndRepaint([initiative('i1', 'Celula Core')]);

      expect(rows()).toHaveLength(1);
    });
  });

  describe('WHEN initiatives are published', () => {
    beforeEach(() => {
      publishAndRepaint([initiative('i1', 'Celula Core'), initiative('i2')]);
    });

    it('THEN renders one row per initiative', () => {
      expect(rows()).toHaveLength(2);
    });

    it('THEN maps a missing cell to an empty editable value', () => {
      expect(internals.editableInitiatives[1].cell).toBe('');
      expect(internals.editableInitiatives[1].tempCell).toBe('');
    });

    it('THEN seeds the editable buffer with the current cell', () => {
      expect(internals.editableInitiatives[0].tempCell).toBe('Celula Core');
    });

    it('THEN every action button declares an accessible label', () => {
      const buttons = Array.from(
        fixture.nativeElement.querySelectorAll('tbody button'),
      ) as HTMLElement[];

      expect(buttons.length).toBeGreaterThan(0);
      buttons.forEach((button) => expect(button.getAttribute('aria-label')).toBeTruthy());
    });
  });

  describe('WHEN a cell is saved', () => {
    beforeEach(() => {
      publishAndRepaint([initiative('i1', 'Celula Core')]);
    });

    it('THEN delegates the trimmed value to the state', () => {
      internals.saveCell({ initiative_id: 'i1', tempCell: '  Celula Nueva  ' });

      expect(state.updateInitiativeCell).toHaveBeenCalledWith('i1', 'Celula Nueva');
    });

    it('THEN the save button is disabled while the value has not changed', () => {
      const saveButton = fixture.nativeElement.querySelector(
        '[aria-label="Guardar célula de Iniciativa i1"]',
      ) as HTMLButtonElement;

      expect(saveButton.disabled).toBe(true);
    });
  });

  describe('WHEN an initiative is deleted', () => {
    it('THEN asks for confirmation before delegating', () => {
      const confirmSpy = vi.spyOn(globalThis, 'confirm').mockReturnValue(true);

      internals.deleteInitiative({ initiative_id: 'i1', initiative_title: 'Iniciativa i1' });

      expect(confirmSpy).toHaveBeenCalled();
      expect(state.deleteInitiative).toHaveBeenCalledWith('i1');
    });

    it('THEN a cancelled confirmation does not delete anything', () => {
      vi.spyOn(globalThis, 'confirm').mockReturnValue(false);

      internals.deleteInitiative({ initiative_id: 'i1', initiative_title: 'Iniciativa i1' });

      expect(state.deleteInitiative).not.toHaveBeenCalled();
    });
  });

  // D-26 — El componente llama a la API directamente, saltándose el servicio de estado.
  describe('WHEN previewing an initiative (D-26, mover al store en fase 05)', () => {
    it('THEN calls the API service directly and opens the modal', () => {
      api.chunksResult = of([{ content: 'trozo' }]);

      internals.previewInitiative({ initiative_id: 'i1', initiative_title: 'Iniciativa i1' });

      expect(api.getInitiativeChunks).toHaveBeenCalledWith('i1');
      expect(internals.isModalOpen()).toBe(true);
      expect(internals.selectedInitiativeTitle()).toBe('Iniciativa i1');
      expect(internals.selectedChunks()).toHaveLength(1);
      expect(internals.loadingChunks()).toBe(false);
    });

    it('THEN a null payload becomes an empty chunk list', () => {
      api.chunksResult = of(null as unknown as unknown[]);

      internals.previewInitiative({ initiative_id: 'i1', initiative_title: 'Iniciativa i1' });

      expect(internals.selectedChunks()).toEqual([]);
    });

    it('THEN a failing preview stops the spinner and keeps the modal open', () => {
      api.chunksResult = throwError(() => new Error('boom'));

      internals.previewInitiative({ initiative_id: 'i1', initiative_title: 'Iniciativa i1' });

      expect(internals.loadingChunks()).toBe(false);
      expect(internals.isModalOpen()).toBe(true);
    });

    it('THEN closing the modal clears the chunks', () => {
      api.chunksResult = of([{ content: 'trozo' }]);
      internals.previewInitiative({ initiative_id: 'i1', initiative_title: 'Iniciativa i1' });

      internals.closeModal();

      expect(internals.isModalOpen()).toBe(false);
      expect(internals.selectedChunks()).toEqual([]);
    });
  });
});
