import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject, Observable } from 'rxjs';
import { DevopsAgentHomePage } from './devops-agent-home.page';
import { DevopsAgentStateService } from '../services/devops-agent-state.service';
import { DevopsAgentApiService } from '../services/devops-agent-api.service';
import { AgentCard, AgentTask, DashboardData, Initiative, Message } from '../models/devops-agent.model';

/** Caracterización FASE 01 — layout, pestañas y panel de tareas de la página principal. */

interface HomeInternals {
  activeTab: string;
  isTasksPanelOpen(): boolean;
  workingTasksCount(): number;
  toggleTasksPanel(): void;
}

const greeting = (text: string): Message => ({ role: 'agent', parts: [{ text }] });

class MockStateService {
  readonly agentCard$ = new BehaviorSubject<AgentCard | null>(null);
  readonly tasks$ = new BehaviorSubject<AgentTask[]>([]);
  readonly generalMessages$ = new BehaviorSubject<Message[]>([greeting('saludo general')]);
  readonly refinementMessages$ = new BehaviorSubject<Message[]>([greeting('saludo refinamiento')]);

  readonly agentCard: Observable<AgentCard | null> = this.agentCard$.asObservable();
  readonly tasks: Observable<AgentTask[]> = this.tasks$.asObservable();
  readonly generalMessages: Observable<Message[]> = this.generalMessages$.asObservable();
  readonly refinementMessages: Observable<Message[]> = this.refinementMessages$.asObservable();
  readonly loading: Observable<boolean> = new BehaviorSubject<boolean>(false).asObservable();
  readonly uploading: Observable<boolean> = new BehaviorSubject<boolean>(false).asObservable();
  readonly uploadStatus: Observable<string | null> = new BehaviorSubject<string | null>(
    null,
  ).asObservable();
  readonly initiatives: Observable<Initiative[]> = new BehaviorSubject<Initiative[]>(
    [],
  ).asObservable();
  readonly dashboardData: Observable<DashboardData | null> =
    new BehaviorSubject<DashboardData | null>(null).asObservable();
  readonly dashboardError: Observable<string | null> = new BehaviorSubject<string | null>(
    null,
  ).asObservable();

  readonly loadInitiatives = vi.fn((): void => undefined);
  readonly clearGeneralChat = vi.fn((): void => undefined);
  readonly clearRefinementChat = vi.fn((): void => undefined);
  readonly sendGeneralMessage = vi.fn((_text: string): void => undefined);
  readonly sendRefinementMessage = vi.fn((_text: string): void => undefined);
  readonly uploadPlanning = vi.fn(
    (_id: string, _title: string, _content: string): void => undefined,
  );
  readonly cancelTask = vi.fn((_id: string): void => undefined);
  readonly loadDashboardData = vi.fn((_cell: string, _sprint: string): void => undefined);
  readonly refineStoryInChat = vi.fn((_id: string, _title: string): void => undefined);
}

class MockApiService {
  readonly getInitiativeChunks = vi.fn(() => new BehaviorSubject<unknown[]>([]).asObservable());
}

describe('GIVEN DevopsAgentHomePage', () => {
  let fixture: ComponentFixture<DevopsAgentHomePage>;
  let internals: HomeInternals;
  let state: MockStateService;

  const textOf = (): string => fixture.nativeElement.textContent as string;

  const tabButton = (label: string): HTMLButtonElement =>
    Array.from(fixture.nativeElement.querySelectorAll('header button')).find(
      (element) => (element as HTMLElement).textContent?.trim() === label,
    ) as HTMLButtonElement;

  /**
   * D-27 — `activeTab` es un CAMPO PLANO y la app corre en modo ZONELESS (`app.config.ts` no
   * declara zona). Asignarlo desde TypeScript no marca la vista como sucia y el `@if` no se
   * reevalúa. En la aplicación real la pestaña cambia por un CLIC, y es el evento del DOM el que
   * notifica al planificador y fuerza el repintado. Esta prueba reproduce esa vía real.
   * La FASE 05 migrará `activeTab` a un signal.
   */
  const activateTab = (label: string): void => {
    tabButton(label).click();
    fixture.detectChanges();
  };

  const TAB_GENERAL = 'Chat General';
  const TAB_REFINEMENT = 'Refinar HU/HA';
  const TAB_DASHBOARD = 'Tablero de Calidad';
  const TAB_MANAGEMENT = 'Gestión de Planeaciones';

  beforeEach(async () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    state = new MockStateService();

    await TestBed.configureTestingModule({
      imports: [DevopsAgentHomePage],
      providers: [
        { provide: DevopsAgentStateService, useValue: state as unknown as DevopsAgentStateService },
        {
          provide: DevopsAgentApiService,
          useValue: new MockApiService() as unknown as DevopsAgentApiService,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DevopsAgentHomePage);
    internals = fixture.componentInstance as unknown as HomeInternals;
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('WHEN the page loads', () => {
    it('THEN the general chat tab is active', () => {
      expect(internals.activeTab).toBe('general');
      expect(textOf()).toContain('saludo general');
    });

    it('THEN the four tabs are offered', () => {
      expect(tabButton('Chat General')).toBeTruthy();
      expect(tabButton('Refinar HU/HA')).toBeTruthy();
      expect(tabButton('Tablero de Calidad')).toBeTruthy();
      expect(tabButton('Gestión de Planeaciones')).toBeTruthy();
    });

    it('THEN the task panel starts expanded', () => {
      expect(internals.isTasksPanelOpen()).toBe(true);
      expect(textOf()).toContain('No hay tareas en ejecución.');
    });
  });

  describe('WHEN switching tabs', () => {
    it('THEN the refinement tab renders the refinement conversation', () => {
      tabButton('Refinar HU/HA').click();
      fixture.detectChanges();

      expect(internals.activeTab).toBe('refinement');
      expect(textOf()).toContain('saludo refinamiento');
    });

    it('THEN the quality dashboard tab renders the dashboard', () => {
      tabButton('Tablero de Calidad').click();
      fixture.detectChanges();

      expect(internals.activeTab).toBe('dashboard');
      expect(fixture.nativeElement.querySelector('app-planning-dashboard')).toBeTruthy();
    });

    it('THEN the management tab renders the management view and reload action', () => {
      tabButton('Gestión de Planeaciones').click();
      fixture.detectChanges();

      expect(internals.activeTab).toBe('management');
      expect(fixture.nativeElement.querySelector('app-planning-management')).toBeTruthy();
    });
  });

  describe('WHEN deciding which side panels to show', () => {
    it('THEN the upload panel is hidden on the general tab', () => {
      activateTab(TAB_GENERAL);

      expect(fixture.nativeElement.querySelector('app-planning-upload')).toBeNull();
    });

    it('THEN the upload panel is shown on the refinement and management tabs', () => {
      activateTab(TAB_REFINEMENT);
      expect(fixture.nativeElement.querySelector('app-planning-upload')).toBeTruthy();

      activateTab(TAB_MANAGEMENT);
      expect(fixture.nativeElement.querySelector('app-planning-upload')).toBeTruthy();
    });

    it('THEN the protocol info cards are shown only on the refinement tab', () => {
      expect(fixture.nativeElement.querySelector('app-info-cards')).toBeNull();

      activateTab(TAB_REFINEMENT);

      expect(fixture.nativeElement.querySelector('app-info-cards')).toBeTruthy();
    });

    it('THEN the task panel is hidden on the management tab', () => {
      activateTab(TAB_MANAGEMENT);

      expect(textOf()).not.toContain('Tareas del Agente');
    });
  });

  describe('WHEN the task list changes', () => {
    it('THEN the active task counter reflects working and submitted tasks', () => {
      state.tasks$.next([
        { id: 't1', status: { state: 'working' } },
        { id: 't2', status: { state: 'submitted' } },
        { id: 't3', status: { state: 'completed' } },
      ]);

      fixture.detectChanges();

      expect(internals.workingTasksCount()).toBe(2);
    });

    it('THEN each task row is rendered with its identifier', () => {
      state.tasks$.next([{ id: 't1', status: { state: 'working' } }]);

      fixture.detectChanges();

      expect(textOf()).toContain('t1');
    });

    it('THEN a cancellable task exposes an accessible cancel action', () => {
      state.tasks$.next([{ id: 't1', status: { state: 'working' } }]);
      fixture.detectChanges();

      const cancelButton = fixture.nativeElement.querySelector(
        '[aria-label="Cancelar tarea t1"]',
      ) as HTMLButtonElement;
      cancelButton.click();

      expect(state.cancelTask).toHaveBeenCalledWith('t1');
    });

    it('THEN a completed task exposes no cancel action', () => {
      state.tasks$.next([{ id: 't1', status: { state: 'completed' } }]);

      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector('[aria-label="Cancelar tarea t1"]')).toBeNull();
    });
  });

  describe('WHEN the task panel header is toggled', () => {
    it('THEN it collapses and expands', () => {
      internals.toggleTasksPanel();
      fixture.detectChanges();
      expect(internals.isTasksPanelOpen()).toBe(false);

      internals.toggleTasksPanel();
      fixture.detectChanges();
      expect(internals.isTasksPanelOpen()).toBe(true);
    });

    it('THEN the toggle declares an accessible label in both states', () => {
      const toggle = (): HTMLElement =>
        fixture.nativeElement.querySelector('aside button') as HTMLElement;

      expect(toggle().getAttribute('aria-label')).toBe('Colapsar panel de tareas');

      internals.toggleTasksPanel();
      fixture.detectChanges();

      expect(toggle().getAttribute('aria-label')).toBe('Expandir panel de tareas');
    });
  });

  describe('WHEN the header actions are used', () => {
    it('THEN the general tab offers clearing the general chat only', () => {
      activateTab(TAB_GENERAL);

      const button = Array.from(fixture.nativeElement.querySelectorAll('header button')).find(
        (element) => (element as HTMLElement).textContent?.includes('Limpiar Chat General'),
      ) as HTMLButtonElement;
      button.click();

      expect(state.clearGeneralChat).toHaveBeenCalledTimes(1);
      expect(state.clearRefinementChat).not.toHaveBeenCalled();
    });

    it('THEN the refinement tab offers clearing the refinement chat only', () => {
      activateTab(TAB_REFINEMENT);

      const button = Array.from(fixture.nativeElement.querySelectorAll('header button')).find(
        (element) => (element as HTMLElement).textContent?.includes('Limpiar Asistente'),
      ) as HTMLButtonElement;
      button.click();

      expect(state.clearRefinementChat).toHaveBeenCalledTimes(1);
      expect(state.clearGeneralChat).not.toHaveBeenCalled();
    });
  });

  describe('WHEN the dashboard asks to refine a story', () => {
    it('THEN the page switches to the refinement tab', () => {
      activateTab(TAB_DASHBOARD);

      const dashboard = fixture.debugElement.query(
        (node) => node.name === 'app-planning-dashboard',
      );
      dashboard.componentInstance.refineRequested.emit();
      fixture.detectChanges();

      expect(internals.activeTab).toBe('refinement');
    });
  });

  // D-03 — La página mantiene una `Subscription` manual con `ngOnDestroy` artesanal en lugar de
  // `takeUntilDestroyed()`. La FASE 08 lo corregirá; aquí solo se verifica que hoy sí cierra.
  describe('WHEN the page is destroyed (D-03, migrar a takeUntilDestroyed en fase 08)', () => {
    it('THEN the manual task subscription is closed', () => {
      state.tasks$.next([{ id: 't1', status: { state: 'working' } }]);
      fixture.detectChanges();
      expect(internals.workingTasksCount()).toBe(1);

      fixture.destroy();
      state.tasks$.next([
        { id: 't1', status: { state: 'working' } },
        { id: 't2', status: { state: 'working' } },
      ]);

      expect(internals.workingTasksCount()).toBe(1);
    });
  });
});
