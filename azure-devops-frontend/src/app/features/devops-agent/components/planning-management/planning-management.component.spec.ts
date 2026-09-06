import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal, WritableSignal } from '@angular/core';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';
import { PlanningManagementComponent } from './planning-management.component';
import { DevopsAgentStateService } from '../../services/devops-agent-state.service';
import { DevopsAgentApiService } from '../../services/devops-agent-api.service';
import { PlanningStateService } from '../../services/state/planning-state.service';
import { RefinementChatStateService } from '../../services/state/refinement-chat-state.service';
import { NotificationService } from '../../../../core';
import { Initiative, PlanningActiveView, SpecDocumentDTO } from '../../models/devops-agent.model';

/**
 * Caracterización FASE 01 y FASE 05 — Centro de Gestión de Planeaciones.
 */

interface ManagementInternals {
  activeView: WritableSignal<PlanningActiveView>;
  showPlanningModal: WritableSignal<boolean>;
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

class MockPlanningStateService {
  readonly availableSpecsSignal: WritableSignal<string[]> = signal<string[]>([]);
  readonly selectedSpecSignal: WritableSignal<SpecDocumentDTO | null> =
    signal<SpecDocumentDTO | null>(null);
  readonly loadingSpecsSignal: WritableSignal<boolean> = signal<boolean>(false);
  readonly planningRunningSignal: WritableSignal<boolean> = signal<boolean>(false);

  readonly loadAvailableSpecs = vi.fn((): void => undefined);
  readonly selectSpec = vi.fn((_name: string): void => undefined);
  readonly triggerProgramPlanning = vi.fn(() => of({}));
}

class MockRefinementChatStateService {
  readonly prefillPrompt = vi.fn((_prompt: string): void => undefined);
}

class MockNotificationService {
  readonly info = vi.fn();
  readonly success = vi.fn();
  readonly error = vi.fn();
  readonly warn = vi.fn();
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
  let mockPlanningState: MockPlanningStateService;
  let mockRefinementChatState: MockRefinementChatStateService;
  let mockNotifications: MockNotificationService;

  const rows = (): HTMLElement[] => Array.from(fixture.nativeElement.querySelectorAll('tbody tr'));

  const publishAndRepaint = (initiatives: Initiative[]): void => {
    fixture.componentInstance.activeView.set('legacy-vector');
    state.loading$.next(true);
    state.initiatives$.next(initiatives);
    state.loading$.next(false);
    fixture.detectChanges();
  };

  beforeEach(async () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    state = new MockStateService();
    api = new MockApiService();
    mockPlanningState = new MockPlanningStateService();
    mockRefinementChatState = new MockRefinementChatStateService();
    mockNotifications = new MockNotificationService();

    await TestBed.configureTestingModule({
      imports: [PlanningManagementComponent],
      providers: [
        { provide: DevopsAgentStateService, useValue: state as unknown as DevopsAgentStateService },
        { provide: DevopsAgentApiService, useValue: api as unknown as DevopsAgentApiService },
        { provide: PlanningStateService, useValue: mockPlanningState },
        { provide: RefinementChatStateService, useValue: mockRefinementChatState },
        { provide: NotificationService, useValue: mockNotifications },
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

    it('THEN defaults to the specs explorer view (DP-FE-01)', () => {
      expect(fixture.componentInstance.activeView()).toBe('explorer');
      expect(fixture.nativeElement.querySelector('app-planning-specs-explorer')).toBeTruthy();
    });

    it('THEN allows switching to the legacy vectorized initiatives view', () => {
      const legacyBtn = fixture.nativeElement.querySelector(
        '[aria-label="Ver iniciativas vectorizadas legadas"]',
      ) as HTMLButtonElement;
      legacyBtn.click();
      fixture.detectChanges();

      expect(fixture.componentInstance.activeView()).toBe('legacy-vector');
      expect(fixture.nativeElement.textContent).toContain('No hay iniciativas indexadas');
    });

    it('THEN shows the spinner while the state reports loading in legacy view', () => {
      fixture.componentInstance.activeView.set('legacy-vector');
      state.loading$.next(true);

      fixture.detectChanges();

      expect(fixture.nativeElement.textContent).toContain('Cargando iniciativas...');
    });
  });

  describe('WHEN launching program planning', () => {
    it('THEN opens the program planning modal', () => {
      const launchBtn = fixture.nativeElement.querySelector(
        '[aria-label="Abrir modal para lanzar nueva planeación de programa"]',
      ) as HTMLButtonElement;
      launchBtn.click();
      fixture.detectChanges();

      expect(fixture.componentInstance.showPlanningModal()).toBe(true);
    });
  });

  describe('WHEN specs explorer requests refinement', () => {
    it('THEN forwards the refineRequested event to parent container', () => {
      const spy = vi.fn();
      fixture.componentInstance.refineRequested.subscribe(spy);

      const explorer = fixture.debugElement.query(
        (node) => node.name === 'app-planning-specs-explorer',
      );
      explorer.componentInstance.refineRequested.emit('Prompt para refinar frente');

      expect(spy).toHaveBeenCalledWith('Prompt para refinar frente');
    });
  });

  describe('WHEN initiatives arrive without any reactive signal changing (D-27, corregir en fase 05)', () => {
    beforeEach(() => {
      fixture.componentInstance.activeView.set('legacy-vector');
      fixture.detectChanges();
    });

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
