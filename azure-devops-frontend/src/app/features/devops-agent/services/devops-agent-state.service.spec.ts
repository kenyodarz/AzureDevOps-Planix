import { TestBed } from '@angular/core/testing';
import { EMPTY, Observable, of, Subject, throwError } from 'rxjs';
import { DevopsAgentStateService } from './devops-agent-state.service';
import { DevopsAgentApiService } from './devops-agent-api.service';
import {
  AgentCard,
  AgentTask,
  DashboardData,
  DashboardMetrics,
  DashboardStoryItem,
  DashboardStreamEvent,
  Initiative,
  SendMessageRequest,
  SendMessageResponse
} from '../models/devops-agent.model';

/**
 * PRUEBAS DE CARACTERIZACIÓN — FASE 01
 *
 * Este archivo NO valida que el diseño sea correcto: **congela el comportamiento observable actual**
 * del servicio para que las fases 02 a 08 puedan refactorizarlo con red de seguridad.
 *
 * Varios casos afirman explícitamente comportamientos que el plan maestro considera deuda técnica
 * (D-02, D-13, D-16, D-18). Están marcados con un comentario y la fase que los invertirá. Si en el
 * futuro uno de esos casos falla, NO es un error de la prueba: es la señal de que el refactor
 * cambió el comportamiento a propósito y toca actualizar la prueba de forma consciente.
 */

// --- Constantes de prueba: sin números mágicos (angular-rules.md §5) --------------------------
const POLL_IDLE_MS = 30000;
const POLL_ACTIVE_MS = 5000;
const ONE_MS = 1;
const GREETING_ONLY = 1;
const GREETING_USER_AND_REPLY = 3;

const AGENT_CARD: AgentCard = { name: 'Real Agent', version: '1.2', description: 'Desc' };
const FALLBACK_AGENT_NAME = 'Agente Local';

const workingTask = (id: string): AgentTask => ({ id, status: { state: 'working' } });
const submittedTask = (id: string): AgentTask => ({ id, status: { state: 'submitted' } });
const completedTask = (id: string): AgentTask => ({ id, status: { state: 'completed' } });

const initiative = (id: string, cell: string): Initiative => ({
  initiative_id: id,
  initiative_title: `Titulo ${id}`,
  cell,
});

const emptyMetrics = (): DashboardMetrics => ({
  totalPoints: 0,
  completedPoints: 0,
  completedPercentage: 0,
  avgQualityScore: 0,
  undocumentedCount: 0,
});

const storyItem = (id: string, qualityScore: number): DashboardStoryItem => ({
  id,
  title: `Historia ${id}`,
  points: 3,
  state: 'Active',
  hasAcceptanceCriteria: true,
  hasDoD: true,
  qualityScore,
  linkedTasksCount: 0,
});

const dashboard = (items: DashboardStoryItem[]): DashboardData => ({
  metrics: emptyMetrics(),
  items,
});

/** Doble tipado del cliente de API. No usa `any` (angular-rules.md §5). */
class MockDevopsAgentApiService {
  getAgentCardResult: Observable<AgentCard> = of(AGENT_CARD);
  sendMessageResult: Observable<SendMessageResponse> = of({});
  uploadPlanningResult: Observable<unknown> = of({});
  getInitiativesResult: Observable<Initiative[]> = of([]);
  deleteInitiativeResult: Observable<void> = of(undefined);
  updateInitiativeCellResult: Observable<void> = of(undefined);
  getTasksResult: Observable<AgentTask[]> = of([]);
  cancelTaskResult: Observable<unknown> = of({});
  dashboardStreamResult: Observable<DashboardStreamEvent> = EMPTY;

  readonly getAgentCard = vi.fn((): Observable<AgentCard> => this.getAgentCardResult);
  readonly sendMessage = vi.fn(
    (_payload: SendMessageRequest): Observable<SendMessageResponse> => this.sendMessageResult,
  );
  readonly uploadPlanning = vi.fn(
    (_payload: unknown): Observable<unknown> => this.uploadPlanningResult,
  );
  readonly getInitiatives = vi.fn((): Observable<Initiative[]> => this.getInitiativesResult);
  readonly deleteInitiative = vi.fn((_id: string): Observable<void> => this.deleteInitiativeResult);
  readonly updateInitiativeCell = vi.fn(
    (_id: string, _cell: string): Observable<void> => this.updateInitiativeCellResult,
  );
  readonly getTasks = vi.fn((): Observable<AgentTask[]> => this.getTasksResult);
  readonly cancelTask = vi.fn((_id: string): Observable<unknown> => this.cancelTaskResult);
  readonly getDashboardDataStream = vi.fn(
    (_cell: string, _sprint: string): Observable<DashboardStreamEvent> => this.dashboardStreamResult,
  );
}

describe('GIVEN DevopsAgentStateService (caracterizacion de los 5 flujos)', () => {
  let service: DevopsAgentStateService;
  let mockApi: MockDevopsAgentApiService;

  /** Lee el valor actual de un Observable respaldado por BehaviorSubject (emision sincrona). */
  const latest = <T>(source: Observable<T>): T => {
    let value!: T;
    const subscription = source.subscribe((emitted) => (value = emitted));
    subscription.unsubscribe();
    return value;
  };

  const buildService = (): void => {
    service = TestBed.inject(DevopsAgentStateService);
  };

  beforeEach(() => {
    vi.useFakeTimers();
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    vi.spyOn(console, 'warn').mockImplementation(() => undefined);

    mockApi = new MockDevopsAgentApiService();
    TestBed.configureTestingModule({
      providers: [
        DevopsAgentStateService,
        { provide: DevopsAgentApiService, useValue: mockApi as unknown as DevopsAgentApiService },
      ],
    });
  });

  afterEach(() => {
    TestBed.resetTestingModule();
    vi.clearAllTimers();
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  // ===========================================================================================
  // ARRANQUE
  // ===========================================================================================
  describe('WHEN the service is constructed', () => {
    it('THEN requests the agent card and exposes it', () => {
      buildService();

      expect(mockApi.getAgentCard).toHaveBeenCalledTimes(1);
      expect(latest(service.agentCard)?.name).toBe('Real Agent');
    });

    it('THEN falls back to local agent card metadata when the request fails', () => {
      mockApi.getAgentCardResult = throwError(() => new Error('Net error'));

      buildService();

      expect(latest(service.agentCard)?.name).toBe(FALLBACK_AGENT_NAME);
    });

    it('THEN seeds each chat with exactly one agent greeting', () => {
      buildService();

      const general = latest(service.generalMessages);
      const refinement = latest(service.refinementMessages);

      expect(general).toHaveLength(GREETING_ONLY);
      expect(general[0].role).toBe('agent');
      expect(refinement).toHaveLength(GREETING_ONLY);
      expect(refinement[0].role).toBe('agent');
    });

    it('THEN the two greetings are different texts', () => {
      buildService();

      expect(latest(service.generalMessages)[0].parts[0].text).not.toBe(
        latest(service.refinementMessages)[0].parts[0].text,
      );
    });

    it('THEN loading, uploading and dashboard state start idle', () => {
      buildService();

      expect(latest(service.loading)).toBe(false);
      expect(latest(service.uploading)).toBe(false);
      expect(latest(service.uploadStatus)).toBeNull();
      expect(latest(service.dashboardError)).toBeNull();
      expect(latest(service.dashboardData)).toBeNull();
      expect(latest(service.initiatives)).toEqual([]);
    });
  });

  // ===========================================================================================
  // F1 y F2 - Chat General y Asistente de Refinamiento
  // ===========================================================================================
  describe('WHEN the chat flows are used (F1 / F2)', () => {
    beforeEach(() => buildService());

    it('THEN a general message lands only in the general chat', () => {
      service.sendGeneralMessage('hola');

      expect(latest(service.generalMessages)).toHaveLength(GREETING_USER_AND_REPLY);
      expect(latest(service.refinementMessages)).toHaveLength(GREETING_ONLY);
    });

    it('THEN a refinement message lands only in the refinement chat', () => {
      service.sendRefinementMessage('hola');

      expect(latest(service.refinementMessages)).toHaveLength(GREETING_USER_AND_REPLY);
      expect(latest(service.generalMessages)).toHaveLength(GREETING_ONLY);
    });

    it('THEN the user message and the agent reply are appended in order', () => {
      mockApi.sendMessageResult = of({
        message: { role: 'agent', parts: [{ text: 'response text' }] },
      });

      service.sendGeneralMessage('hi');

      const messages = latest(service.generalMessages);
      expect(messages).toHaveLength(GREETING_USER_AND_REPLY);
      expect(messages[1].role).toBe('user');
      expect(messages[1].parts[0].text).toBe('hi');
      expect(messages[2].role).toBe('agent');
      expect(messages[2].parts[0].text).toBe('response text');
    });

    // D-16 - El `loading` es unico y compartido por los 5 flujos. Enviar en el Chat General
    // enciende el spinner del Refinamiento. La FASE 05 invertira este caso (ver DP-08).
    it('THEN a pending general message ALSO blocks the refinement flow (D-16, invertir en fase 05)', () => {
      mockApi.sendMessageResult = new Subject<SendMessageResponse>();

      service.sendGeneralMessage('hola');

      expect(latest(service.loading)).toBe(true);
    });

    it('THEN blank text is ignored and no request is issued', () => {
      service.sendGeneralMessage('   ');

      expect(mockApi.sendMessage).not.toHaveBeenCalled();
      expect(latest(service.generalMessages)).toHaveLength(GREETING_ONLY);
    });

    it('THEN a network failure appends a readable error message to the same chat', () => {
      mockApi.sendMessageResult = throwError(() => new Error('Failed'));

      service.sendGeneralMessage('hi');

      const messages = latest(service.generalMessages);
      expect(messages).toHaveLength(GREETING_USER_AND_REPLY);
      expect(messages[messages.length - 1].parts[0].text).toContain('Error de red');
    });

    it('THEN consecutive messages of the same flow share the conversation context', () => {
      service.sendGeneralMessage('uno');
      service.sendGeneralMessage('dos');

      const [first, second] = mockApi.sendMessage.mock.calls;
      expect(first[0].message.contextId).toBe(second[0].message.contextId);
      expect(first[0].message.contextId).toContain('general-');
    });

    it('THEN the general and refinement flows use different context identifiers', () => {
      service.sendGeneralMessage('uno');
      service.sendRefinementMessage('dos');

      const [general, refinement] = mockApi.sendMessage.mock.calls;
      expect(general[0].message.contextId).toContain('general-');
      expect(refinement[0].message.contextId).toContain('refinement-');
      expect(general[0].message.contextId).not.toBe(refinement[0].message.contextId);
    });

    it('THEN clearGeneralChat resets its own list and renews the context', () => {
      service.sendGeneralMessage('uno');
      const previousContext = mockApi.sendMessage.mock.calls[0][0].message.contextId;

      service.clearGeneralChat();
      service.sendGeneralMessage('dos');

      expect(latest(service.generalMessages)).toHaveLength(GREETING_USER_AND_REPLY);
      expect(mockApi.sendMessage.mock.calls[1][0].message.contextId).not.toBe(previousContext);
    });

    it('THEN clearGeneralChat does not touch the refinement chat', () => {
      service.sendRefinementMessage('idea');
      const before = latest(service.refinementMessages).length;

      service.clearGeneralChat();

      expect(latest(service.refinementMessages)).toHaveLength(before);
    });

    it('THEN clearRefinementChat resets the refinement list to its reset greeting', () => {
      service.sendRefinementMessage('idea');

      service.clearRefinementChat();

      const messages = latest(service.refinementMessages);
      expect(messages).toHaveLength(GREETING_ONLY);
      expect(messages[0].parts[0].text).toContain('Chat limpio');
    });

    it('THEN clearChat is an alias of clearRefinementChat', () => {
      service.sendRefinementMessage('idea');

      service.clearChat();

      expect(latest(service.refinementMessages)).toHaveLength(GREETING_ONLY);
    });

    // D-13 - Alias publico mantenido por compatibilidad con tests antiguos. Ver DP-01.
    it('THEN the legacy `messages` alias mirrors the refinement flow (D-13, ver DP-01)', () => {
      service.sendRefinementMessage('idea');

      expect(latest(service.messages)).toEqual(latest(service.refinementMessages));
      expect(latest(service.messages)).not.toEqual(latest(service.generalMessages));
    });

    it('THEN sendMessage routes to the refinement flow, not the general one', () => {
      service.sendMessage('idea');

      expect(latest(service.refinementMessages)).toHaveLength(GREETING_USER_AND_REPLY);
      expect(latest(service.generalMessages)).toHaveLength(GREETING_ONLY);
    });

    it('THEN refineStoryInChat pushes a refinement prompt carrying id and title', () => {
      service.refineStoryInChat('123', 'Carga masiva');

      const sent = mockApi.sendMessage.mock.calls[0][0].message;
      expect(sent.contextId).toContain('refinement-');
      expect(sent.parts[0].text).toContain('123');
      expect(sent.parts[0].text).toContain('Carga masiva');
    });

    it('THEN a reply without text falls back to a default agent message', () => {
      mockApi.sendMessageResult = of({});

      service.sendGeneralMessage('hi');

      const messages = latest(service.generalMessages);
      expect(messages[messages.length - 1].parts[0].text).toBe('No obtuve respuesta del modelo.');
    });
  });

  // ===========================================================================================
  // F3 - Tablero de Calidad
  // ===========================================================================================
  describe('WHEN the quality dashboard flow is used (F3)', () => {
    beforeEach(() => buildService());

    it('THEN a blank cell prevents opening the stream', () => {
      service.loadDashboardData('', 'Sprint 1');

      expect(mockApi.getDashboardDataStream).not.toHaveBeenCalled();
    });

    it('THEN a blank sprint prevents opening the stream', () => {
      service.loadDashboardData('EQU1096', '');

      expect(mockApi.getDashboardDataStream).not.toHaveBeenCalled();
    });

    it('THEN valid filters open the stream and clear the previous error', () => {
      service.loadDashboardData('EQU1096', 'Sprint 247');

      expect(mockApi.getDashboardDataStream).toHaveBeenCalledWith('EQU1096', 'Sprint 247');
      expect(latest(service.dashboardError)).toBeNull();
    });

    it('THEN an INITIAL event publishes the data and stops the loading indicator', () => {
      mockApi.dashboardStreamResult = of({ event: 'INITIAL', data: dashboard([]) });

      service.loadDashboardData('EQU1096', 'Sprint 247');

      expect(latest(service.dashboardData)?.items).toEqual([]);
      expect(latest(service.loading)).toBe(false);
    });

    it('THEN a BATCH_UPDATE event republishes the refreshed data', () => {
      mockApi.dashboardStreamResult = of({
        event: 'BATCH_UPDATE',
        data: dashboard([storyItem('S-1', 80)]),
      });

      service.loadDashboardData('EQU1096', 'Sprint 247');

      expect(latest(service.dashboardData)?.items).toHaveLength(1);
    });

    it('THEN a stream error surfaces the backend message and drops the data', () => {
      mockApi.dashboardStreamResult = throwError(() => new Error('agente caido'));

      service.loadDashboardData('EQU1096', 'Sprint 247');

      expect(latest(service.dashboardData)).toBeNull();
      expect(latest(service.dashboardError)).toBe('agente caido');
      expect(latest(service.loading)).toBe(false);
    });

    it('THEN a stream error without detail surfaces the default message', () => {
      mockApi.dashboardStreamResult = throwError(() => ({}) as Error);

      service.loadDashboardData('EQU1096', 'Sprint 247');

      expect(latest(service.dashboardError)).toBe('Fallo en la comunicación con el agente o MCP.');
    });

    it('THEN completing the stream stops the loading indicator', () => {
      service.loadDashboardData('EQU1096', 'Sprint 247');

      expect(latest(service.loading)).toBe(false);
    });

    it('THEN auditStory sends an audit prompt scoped to the story id', () => {
      service.auditStory('42').subscribe();

      const sent = mockApi.sendMessage.mock.calls[0][0].message;
      expect(sent.contextId).toContain('42');
      expect(sent.parts[0].text).toContain('42');
    });

    // D-18 - Efecto cruzado F3 -> F5: auditar una historia reinicia el sondeo global de tareas.
    // La FASE 06 lo eliminara convirtiendolo en un evento explicito del store de tareas.
    it('THEN auditStory ALSO restarts the global task polling (D-18, eliminar en fase 06)', () => {
      const callsBefore = mockApi.getTasks.mock.calls.length;

      service.auditStory('42').subscribe();

      expect(mockApi.getTasks.mock.calls.length).toBeGreaterThan(callsBefore);
    });
  });

  // ===========================================================================================
  // F4 - Gestion de Planeaciones
  // ===========================================================================================
  describe('WHEN the planning management flow is used (F4)', () => {
    beforeEach(() => buildService());

    it('THEN uploadPlanning forwards the ingest payload', () => {
      service.uploadPlanning('guardian-q3', 'Guardian Q3', '# Planeacion');

      expect(mockApi.uploadPlanning).toHaveBeenCalledWith({
        initiativeId: 'guardian-q3',
        title: 'Guardian Q3',
        markdownContent: '# Planeacion',
      });
    });

    it('THEN a successful upload reports success and reloads the initiatives', () => {
      const callsBefore = mockApi.getInitiatives.mock.calls.length;

      service.uploadPlanning('id', 'title', 'content');

      expect(latest(service.uploadStatus)).toBe('¡Planeación indexada con éxito!');
      expect(mockApi.getInitiatives.mock.calls.length).toBe(callsBefore + 1);
    });

    it('THEN a failed upload reports the error detail', () => {
      mockApi.uploadPlanningResult = throwError(() => ({ error: 'Bad file format' }));

      service.uploadPlanning('id', 'title', 'content');

      expect(latest(service.uploadStatus)).toContain('Error: Bad file format');
    });

    it('THEN the uploading flag always returns to false', () => {
      mockApi.uploadPlanningResult = throwError(() => new Error('boom'));

      service.uploadPlanning('id', 'title', 'content');

      expect(latest(service.uploading)).toBe(false);
    });

    it('THEN clearUploadStatus wipes the status text', () => {
      service.uploadPlanning('id', 'title', 'content');

      service.clearUploadStatus();

      expect(latest(service.uploadStatus)).toBeNull();
    });

    it('THEN loadInitiatives publishes the list and releases the loading flag', () => {
      const list = [initiative('i1', 'c1')];
      mockApi.getInitiativesResult = of(list);

      service.loadInitiatives();

      expect(latest(service.initiatives)).toEqual(list);
      expect(latest(service.loading)).toBe(false);
    });

    it('THEN a failed loadInitiatives keeps the previous list intact', () => {
      mockApi.getInitiativesResult = of([initiative('i1', 'c1')]);
      service.loadInitiatives();

      mockApi.getInitiativesResult = throwError(() => new Error('boom'));
      service.loadInitiatives();

      expect(latest(service.initiatives)).toHaveLength(1);
      expect(latest(service.loading)).toBe(false);
    });

    it('THEN updateInitiativeCell replaces only the target item immutably', () => {
      const original = initiative('i1', 'c1');
      mockApi.getInitiativesResult = of([original, initiative('i2', 'c2')]);
      service.loadInitiatives();

      service.updateInitiativeCell('i1', 'nueva-celula');

      const list = latest(service.initiatives);
      expect(list[0].cell).toBe('nueva-celula');
      expect(list[0]).not.toBe(original);
      expect(list[1].cell).toBe('c2');
    });

    it('THEN a failed updateInitiativeCell leaves the list untouched', () => {
      mockApi.getInitiativesResult = of([initiative('i1', 'c1')]);
      service.loadInitiatives();
      mockApi.updateInitiativeCellResult = throwError(() => new Error('boom'));

      service.updateInitiativeCell('i1', 'nueva-celula');

      expect(latest(service.initiatives)[0].cell).toBe('c1');
    });

    it('THEN deleteInitiative removes the target item', () => {
      mockApi.getInitiativesResult = of([initiative('i1', 'c1'), initiative('i2', 'c2')]);
      service.loadInitiatives();

      service.deleteInitiative('i1');

      const list = latest(service.initiatives);
      expect(list).toHaveLength(1);
      expect(list[0].initiative_id).toBe('i2');
    });

    it('THEN a failed deleteInitiative leaves the list untouched', () => {
      mockApi.getInitiativesResult = of([initiative('i1', 'c1')]);
      service.loadInitiatives();
      mockApi.deleteInitiativeResult = throwError(() => new Error('boom'));

      service.deleteInitiative('i1');

      expect(latest(service.initiatives)).toHaveLength(1);
    });
  });

  // ===========================================================================================
  // F5 - Tareas del Agente y sondeo dinamico
  // ===========================================================================================
  describe('WHEN the agent task polling runs (F5)', () => {
    it('THEN tasks are requested once immediately on construction', () => {
      buildService();

      expect(mockApi.getTasks).toHaveBeenCalledTimes(1);
    });

    it('THEN a null task payload is normalised to an empty array', () => {
      mockApi.getTasksResult = of(null as unknown as AgentTask[]);

      buildService();

      expect(latest(service.tasks)).toEqual([]);
    });

    it('THEN with no active tasks the next poll happens after the idle interval', () => {
      buildService();

      vi.advanceTimersByTime(POLL_IDLE_MS - ONE_MS);
      expect(mockApi.getTasks).toHaveBeenCalledTimes(1);

      vi.advanceTimersByTime(ONE_MS);
      expect(mockApi.getTasks).toHaveBeenCalledTimes(2);
    });

    it('THEN a working task shortens the interval to the active one', () => {
      buildService();
      mockApi.getTasksResult = of([workingTask('t1')]);

      vi.advanceTimersByTime(POLL_IDLE_MS);
      expect(mockApi.getTasks).toHaveBeenCalledTimes(2);

      vi.advanceTimersByTime(POLL_ACTIVE_MS);
      expect(mockApi.getTasks).toHaveBeenCalledTimes(3);
    });

    it('THEN a submitted task also shortens the interval', () => {
      buildService();
      mockApi.getTasksResult = of([submittedTask('t1')]);

      vi.advanceTimersByTime(POLL_IDLE_MS);
      vi.advanceTimersByTime(POLL_ACTIVE_MS);

      expect(mockApi.getTasks).toHaveBeenCalledTimes(3);
    });

    it('THEN once every task completes the interval returns to idle', () => {
      buildService();
      mockApi.getTasksResult = of([workingTask('t1')]);
      vi.advanceTimersByTime(POLL_IDLE_MS);

      mockApi.getTasksResult = of([completedTask('t1')]);
      vi.advanceTimersByTime(POLL_ACTIVE_MS);
      const callsAfterCompletion = mockApi.getTasks.mock.calls.length;

      vi.advanceTimersByTime(POLL_ACTIVE_MS);
      expect(mockApi.getTasks).toHaveBeenCalledTimes(callsAfterCompletion);

      vi.advanceTimersByTime(POLL_IDLE_MS - POLL_ACTIVE_MS);
      expect(mockApi.getTasks).toHaveBeenCalledTimes(callsAfterCompletion + 1);
    });

    it('THEN a failing request resets to the idle interval and keeps polling', () => {
      buildService();
      mockApi.getTasksResult = throwError(() => new Error('boom'));

      vi.advanceTimersByTime(POLL_IDLE_MS);
      expect(mockApi.getTasks).toHaveBeenCalledTimes(2);

      vi.advanceTimersByTime(POLL_IDLE_MS);
      expect(mockApi.getTasks).toHaveBeenCalledTimes(3);
    });

    it('THEN triggerImmediatePoll refreshes now and switches to the active interval', () => {
      buildService();

      service.triggerImmediatePoll();
      expect(mockApi.getTasks).toHaveBeenCalledTimes(2);

      vi.advanceTimersByTime(POLL_ACTIVE_MS);
      expect(mockApi.getTasks).toHaveBeenCalledTimes(3);
    });

    it('THEN cancelTask calls the API and reloads the list', () => {
      buildService();
      const callsBefore = mockApi.getTasks.mock.calls.length;

      service.cancelTask('t1');

      expect(mockApi.cancelTask).toHaveBeenCalledWith('t1');
      expect(mockApi.getTasks.mock.calls.length).toBe(callsBefore + 1);
    });

    it('THEN a failing cancelTask does not reload the list', () => {
      buildService();
      mockApi.cancelTaskResult = throwError(() => new Error('boom'));
      const callsBefore = mockApi.getTasks.mock.calls.length;

      service.cancelTask('t1');

      expect(mockApi.getTasks.mock.calls.length).toBe(callsBefore);
    });

    // D-02 - FUGA DE MEMORIA REAL. El servicio es `providedIn:'root'`, arranca el temporizador en
    // el constructor y nunca lo cancela: no implementa `ngOnDestroy` ni usa `DestroyRef`.
    // Destruir el inyector NO detiene el sondeo. La FASE 06 invertira este caso.
    it('THEN destroying the injector leaves the polling timer alive (D-02, corregir en fase 06)', () => {
      buildService();

      TestBed.resetTestingModule();

      expect(vi.getTimerCount()).toBeGreaterThan(0);
    });
  });
});

