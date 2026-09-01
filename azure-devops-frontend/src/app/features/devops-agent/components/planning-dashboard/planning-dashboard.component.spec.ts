import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';
import { PlanningDashboardComponent } from './planning-dashboard.component';
import { DevopsAgentStateService } from '../../services/devops-agent-state.service';
import {
  DashboardData,
  DashboardMetrics,
  DashboardStoryItem,
  SendMessageResponse
} from '../../models/devops-agent.model';

/**
 * PRUEBAS DE CARACTERIZACIÓN — FASE 01 · Tablero de Calidad
 *
 * Este archivo congela las **reglas de negocio embebidas en la vista** (D-04) tal y como están hoy,
 * incluida la discrepancia entre los umbrales de la etiqueta y los del filtro documentada en DP-02.
 * La FASE 07 las trasladará a `domain/quality-score.ts`; hasta que DP-02 se resuelva, está
 * PROHIBIDO unificarlas.
 */

// --- Umbrales actuales, extraídos literalmente del componente ---------------------------------
const LABEL_EXCELLENT_MIN = 90; // :703
const LABEL_GOOD_MIN = 70; // :704
const LABEL_REGULAR_MIN = 50; // :705
const COLOR_GREEN_MIN = 80; // :710
const COLOR_AMBER_MIN = 50; // :711
const LARGE_STORY_POINTS = 13; // :717

const LABEL_EXCELLENT = 'Excelente';
const LABEL_GOOD = 'Buena (Suficiente)';
const LABEL_REGULAR = 'Regular';
const LABEL_POOR = 'Deficiente (Requiere Refinar)';

const CLASS_GREEN = 'bg-[#10b981]';
const CLASS_AMBER = 'bg-[#f59e0b]';
const CLASS_RED = 'bg-[#ef4444]';

/** Acceso tipado a los miembros `protected` que la prueba de caracterización necesita observar. */
interface DashboardInternals {
  cellInput: string;
  sprintInput: string;
  selectedMember: string;
  selectedState: string;
  selectedQuality: string;
  expandedItemIds: Set<string>;
  detailedAudits: Record<string, { loading: boolean; content?: string; error?: string }>;
  readonly filteredItems: DashboardStoryItem[];
  readonly uniqueStates: string[];
  readonly uniqueMembers: string[];
  getQualityLabel(score: number): string;
  getQualityBgClass(score: number): string;
  getStateClass(state: string): string;
  getLargeStoriesCount(): number;
  generateAnalysis(): void;
  refineStory(item: DashboardStoryItem): void;
  runDetailedAudit(storyId: string): void;
  toggleExpand(itemId: string): void;
  isExpanded(itemId: string): boolean;
}

class MockDevopsAgentStateService {
  readonly dashboardData$ = new BehaviorSubject<DashboardData | null>(null);
  readonly dashboardError$ = new BehaviorSubject<string | null>(null);

  readonly dashboardData: Observable<DashboardData | null> = this.dashboardData$.asObservable();
  readonly dashboardError: Observable<string | null> = this.dashboardError$.asObservable();
  readonly loading: Observable<boolean> = new BehaviorSubject<boolean>(false).asObservable();

  auditStoryResult: Observable<SendMessageResponse> = of({});

  readonly loadDashboardData = vi.fn((_cell: string, _sprint: string): void => undefined);
  readonly refineStoryInChat = vi.fn((_id: string, _title: string): void => undefined);
  readonly auditStory = vi.fn(
    (_id: string): Observable<SendMessageResponse> => this.auditStoryResult,
  );
}

const metrics = (): DashboardMetrics => ({
  totalPoints: 0,
  completedPoints: 0,
  completedPercentage: 0,
  avgQualityScore: 0,
  undocumentedCount: 0,
});

const item = (overrides: Partial<DashboardStoryItem> & { id: string }): DashboardStoryItem => ({
  title: `Historia ${overrides.id}`,
  points: 3,
  state: 'Active',
  hasAcceptanceCriteria: true,
  hasDoD: true,
  qualityScore: 100,
  linkedTasksCount: 0,
  ...overrides,
});

describe('GIVEN PlanningDashboardComponent (caracterizacion de las reglas de negocio)', () => {
  let fixture: ComponentFixture<PlanningDashboardComponent>;
  let internals: DashboardInternals;
  let state: MockDevopsAgentStateService;

  beforeEach(async () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    state = new MockDevopsAgentStateService();

    await TestBed.configureTestingModule({
      imports: [PlanningDashboardComponent],
      providers: [
        {
          provide: DevopsAgentStateService,
          useValue: state as unknown as DevopsAgentStateService,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PlanningDashboardComponent);
    internals = fixture.componentInstance as unknown as DashboardInternals;
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  // ===========================================================================================
  // Etiqueta de calidad — umbrales 90 / 70 / 50
  // ===========================================================================================
  describe('WHEN classifying the quality label', () => {
    it(`THEN a score of ${LABEL_EXCELLENT_MIN} is "${LABEL_EXCELLENT}"`, () => {
      expect(internals.getQualityLabel(LABEL_EXCELLENT_MIN)).toBe(LABEL_EXCELLENT);
    });

    it(`THEN a score just below ${LABEL_EXCELLENT_MIN} drops to "${LABEL_GOOD}"`, () => {
      expect(internals.getQualityLabel(LABEL_EXCELLENT_MIN - 1)).toBe(LABEL_GOOD);
    });

    it(`THEN a score of ${LABEL_GOOD_MIN} is still "${LABEL_GOOD}"`, () => {
      expect(internals.getQualityLabel(LABEL_GOOD_MIN)).toBe(LABEL_GOOD);
    });

    it(`THEN a score just below ${LABEL_GOOD_MIN} drops to "${LABEL_REGULAR}"`, () => {
      expect(internals.getQualityLabel(LABEL_GOOD_MIN - 1)).toBe(LABEL_REGULAR);
    });

    it(`THEN a score of ${LABEL_REGULAR_MIN} is still "${LABEL_REGULAR}"`, () => {
      expect(internals.getQualityLabel(LABEL_REGULAR_MIN)).toBe(LABEL_REGULAR);
    });

    it(`THEN a score just below ${LABEL_REGULAR_MIN} drops to "${LABEL_POOR}"`, () => {
      expect(internals.getQualityLabel(LABEL_REGULAR_MIN - 1)).toBe(LABEL_POOR);
    });
  });

  // ===========================================================================================
  // Color de la barra — umbrales 80 / 50 (distintos de los de la etiqueta)
  // ===========================================================================================
  describe('WHEN choosing the quality bar colour', () => {
    it(`THEN a score of ${COLOR_GREEN_MIN} is green`, () => {
      expect(internals.getQualityBgClass(COLOR_GREEN_MIN)).toBe(CLASS_GREEN);
    });

    it(`THEN a score just below ${COLOR_GREEN_MIN} is amber`, () => {
      expect(internals.getQualityBgClass(COLOR_GREEN_MIN - 1)).toBe(CLASS_AMBER);
    });

    it(`THEN a score of ${COLOR_AMBER_MIN} is still amber`, () => {
      expect(internals.getQualityBgClass(COLOR_AMBER_MIN)).toBe(CLASS_AMBER);
    });

    it(`THEN a score just below ${COLOR_AMBER_MIN} is red`, () => {
      expect(internals.getQualityBgClass(COLOR_AMBER_MIN - 1)).toBe(CLASS_RED);
    });
  });

  // ===========================================================================================
  // DP-02 — Evidencia dura de la discrepancia entre umbrales
  // ===========================================================================================
  describe('WHEN the label thresholds and the filter thresholds disagree (DP-02)', () => {
    beforeEach(() => {
      state.dashboardData$.next({
        metrics: metrics(),
        items: [item({ id: 'S-75', qualityScore: 75 })],
      });
      fixture.detectChanges();
    });

    // ⚠️ Una historia con 75 se ETIQUETA como "Buena (Suficiente)" pero cae en el filtro
    // "regular", no en "good". Los umbrales de la etiqueta (90/70/50) y los del filtro (80/50)
    // no coinciden. Esto NO se corrige aquí: es la evidencia que alimenta DP-02.
    it('THEN a score of 75 is labelled GOOD but filtered as REGULAR (discrepancia viva)', () => {
      expect(internals.getQualityLabel(75)).toBe(LABEL_GOOD);

      internals.selectedQuality = 'good';
      expect(internals.filteredItems).toHaveLength(0);

      internals.selectedQuality = 'regular';
      expect(internals.filteredItems).toHaveLength(1);
    });
  });

  // ===========================================================================================
  // Filtros
  // ===========================================================================================
  describe('WHEN filtering the story list', () => {
    beforeEach(() => {
      state.dashboardData$.next({
        metrics: metrics(),
        items: [
          item({ id: 'A', qualityScore: 30, state: 'Active', assignedMember: 'Ana' }),
          item({ id: 'B', qualityScore: 60, state: 'Done', assignedMember: 'Bruno' }),
          item({ id: 'C', qualityScore: 95, state: 'Active', assignedMember: 'Ana' }),
        ],
      });
      fixture.detectChanges();
    });

    it('THEN no filter returns every item', () => {
      expect(internals.filteredItems).toHaveLength(3);
    });

    it('THEN the critical filter keeps only scores below 50', () => {
      internals.selectedQuality = 'critical';

      expect(internals.filteredItems.map((entry) => entry.id)).toEqual(['A']);
    });

    it('THEN the regular filter keeps only scores between 50 and 79', () => {
      internals.selectedQuality = 'regular';

      expect(internals.filteredItems.map((entry) => entry.id)).toEqual(['B']);
    });

    it('THEN the good filter keeps only scores of 80 or more', () => {
      internals.selectedQuality = 'good';

      expect(internals.filteredItems.map((entry) => entry.id)).toEqual(['C']);
    });

    it('THEN the member filter narrows the list', () => {
      internals.selectedMember = 'Ana';

      expect(internals.filteredItems.map((entry) => entry.id)).toEqual(['A', 'C']);
    });

    it('THEN the state filter narrows the list', () => {
      internals.selectedState = 'Done';

      expect(internals.filteredItems.map((entry) => entry.id)).toEqual(['B']);
    });

    it('THEN combined filters are applied as a conjunction', () => {
      internals.selectedMember = 'Ana';
      internals.selectedQuality = 'good';

      expect(internals.filteredItems.map((entry) => entry.id)).toEqual(['C']);
    });

    it('THEN the unique members and states are deduplicated', () => {
      expect(internals.uniqueMembers).toEqual(['Ana', 'Bruno']);
      expect(internals.uniqueStates).toEqual(['Active', 'Done']);
    });
  });

  // ===========================================================================================
  // Métricas derivadas y traducción de estados
  // ===========================================================================================
  describe('WHEN counting large stories', () => {
    it(`THEN only stories with ${LARGE_STORY_POINTS} points or more are counted`, () => {
      state.dashboardData$.next({
        metrics: metrics(),
        items: [
          item({ id: 'A', points: LARGE_STORY_POINTS - 1 }),
          item({ id: 'B', points: LARGE_STORY_POINTS }),
          item({ id: 'C', points: LARGE_STORY_POINTS + 8 }),
        ],
      });
      fixture.detectChanges();

      expect(internals.getLargeStoriesCount()).toBe(2);
    });

    it('THEN with no data the count is zero', () => {
      expect(internals.getLargeStoriesCount()).toBe(0);
    });
  });

  describe('WHEN translating an Azure DevOps state into styles', () => {
    it('THEN done and closed share the completed style', () => {
      expect(internals.getStateClass('Done')).toBe(internals.getStateClass('closed'));
      expect(internals.getStateClass('Done')).toContain('#10b981');
    });

    it('THEN committed and active share the in-progress style', () => {
      expect(internals.getStateClass('Committed')).toBe(internals.getStateClass('active'));
      expect(internals.getStateClass('Committed')).toContain('#2563eb');
    });

    it('THEN approved has its own style', () => {
      expect(internals.getStateClass('approved')).toContain('#f2c94c');
    });

    it('THEN an unknown state falls back to the neutral style', () => {
      expect(internals.getStateClass('cualquier-otro')).toContain('#9ca3af');
    });

    it('THEN an empty state does not crash', () => {
      expect(internals.getStateClass('')).toContain('#9ca3af');
    });
  });

  // ===========================================================================================
  // Reacción al estado y acciones
  // ===========================================================================================
  describe('WHEN new dashboard data arrives', () => {
    it('THEN every filter is reset and the expanded rows collapse', () => {
      state.dashboardData$.next({ metrics: metrics(), items: [item({ id: 'A' })] });
      fixture.detectChanges();
      internals.selectedMember = 'Ana';
      internals.selectedState = 'Done';
      internals.selectedQuality = 'good';
      internals.toggleExpand('A');

      state.dashboardData$.next({ metrics: metrics(), items: [item({ id: 'B' })] });
      fixture.detectChanges();

      expect(internals.selectedMember).toBe('');
      expect(internals.selectedState).toBe('');
      expect(internals.selectedQuality).toBe('');
      expect(internals.isExpanded('A')).toBe(false);
    });
  });

  describe('WHEN a dashboard error is published', () => {
    it('THEN the message is rendered to the user', () => {
      state.dashboardError$.next('el agente no responde');

      fixture.detectChanges();

      expect(fixture.nativeElement.textContent).toContain('el agente no responde');
    });
  });

  describe('WHEN the analysis is requested', () => {
    it('THEN blank filters do not trigger a load', () => {
      internals.cellInput = '   ';
      internals.sprintInput = 'Sprint 1';

      internals.generateAnalysis();

      expect(state.loadDashboardData).not.toHaveBeenCalled();
    });

    it('THEN valid filters trigger a trimmed load', () => {
      internals.cellInput = '  EQU1096  ';
      internals.sprintInput = '  Sprint 247  ';

      internals.generateAnalysis();

      expect(state.loadDashboardData).toHaveBeenCalledWith('EQU1096', 'Sprint 247');
    });
  });

  describe('WHEN a row is expanded and collapsed', () => {
    it('THEN the expansion toggles', () => {
      expect(internals.isExpanded('A')).toBe(false);

      internals.toggleExpand('A');
      expect(internals.isExpanded('A')).toBe(true);

      internals.toggleExpand('A');
      expect(internals.isExpanded('A')).toBe(false);
    });
  });

  describe('WHEN a story is sent to refinement', () => {
    it('THEN it delegates to the state and asks the parent to switch tabs', () => {
      let requested = false;
      fixture.componentInstance.refineRequested.subscribe(() => (requested = true));

      internals.refineStory(item({ id: 'S-1', title: 'Carga masiva' }));

      expect(state.refineStoryInChat).toHaveBeenCalledWith('S-1', 'Carga masiva');
      expect(requested).toBe(true);
    });
  });

  // D-04 / M-15 — La auditoría detallada se suscribe a la red DESDE EL COMPONENTE.
  // La FASE 07 la moverá al store; hasta entonces esta prueba congela el comportamiento.
  describe('WHEN a detailed audit is run (D-04, mover al store en fase 07)', () => {
    it('THEN a successful audit stores the report text', () => {
      state.auditStoryResult = of({ message: { parts: [{ text: '## Informe' }] } });

      internals.runDetailedAudit('S-1');

      expect(internals.detailedAudits['S-1']).toEqual({ loading: false, content: '## Informe' });
    });

    it('THEN an audit without text falls back to a default report message', () => {
      state.auditStoryResult = of({});

      internals.runDetailedAudit('S-1');

      expect(internals.detailedAudits['S-1'].content).toBe('No se obtuvo reporte.');
    });

    it('THEN a failing audit stores the error message', () => {
      state.auditStoryResult = throwError(() => new Error('boom'));

      internals.runDetailedAudit('S-1');

      expect(internals.detailedAudits['S-1']).toEqual({
        loading: false,
        error: 'Error al procesar la auditoría.',
      });
    });
  });

  // D-23 — El componente más grande del proyecto (753 líneas) no declara ni un solo `aria-label`.
  // La FASE 07 lo corregirá.
  describe('WHEN auditing accessibility (D-23, corregir en fase 07)', () => {
    it('THEN the template currently declares no aria-label at all', () => {
      state.dashboardData$.next({ metrics: metrics(), items: [item({ id: 'A' })] });
      fixture.detectChanges();

      const labelled = fixture.nativeElement.querySelectorAll('[aria-label]');

      expect(labelled).toHaveLength(0);
    });
  });
});
