import { TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';
import { PlanningStateService } from './planning-state.service';
import { DevopsAgentApiService } from '../devops-agent-api.service';
import { TasksStateService } from './tasks-state.service';
import { NotificationService } from '../../../../core';
import {
  Initiative,
  PlanningChunk,
  ProgramPlanRequestDTO,
  ProgramPlanResponseDTO,
  SpecDocumentDTO,
  SpecListDTO,
} from '../../models/devops-agent.model';

class MockTasksStateService {
  readonly triggerImmediatePoll = vi.fn();
}

class MockNotificationService {
  readonly success = vi.fn();
  readonly error = vi.fn();
  readonly info = vi.fn();
  readonly warn = vi.fn();
}

class MockDevopsAgentApiService {
  getInitiativesResult: Observable<Initiative[]> = of([]);
  uploadPlanningResult: Observable<unknown> = of({});
  updateInitiativeCellResult: Observable<void> = of(undefined);
  deleteInitiativeResult: Observable<void> = of(undefined);
  getInitiativeChunksResult: Observable<PlanningChunk[]> = of([]);
  triggerProgramPlanningResult: Observable<ProgramPlanResponseDTO> = of({
    summary: 'OK',
    quarter: 'Q3',
    sprintCount: 6,
    maxCapacityPerSprint: 45,
    targetFronts: ['Canales'],
    contextId: 'ctx-1',
    task: {
      id: 't-1',
      contextId: 'ctx-1',
      status: { state: 'submitted' },
    },
  });
  getAvailableSpecsResult: Observable<SpecListDTO> = of({ specs: [], total: 0 });
  getSpecDocumentResult: Observable<SpecDocumentDTO> = of({
    name: 'test.md',
    content: '',
    path: 'specs/test.md',
  });

  readonly getInitiatives = vi.fn((): Observable<Initiative[]> => this.getInitiativesResult);
  readonly uploadPlanning = vi.fn(
    (_payload: unknown): Observable<unknown> => this.uploadPlanningResult,
  );
  readonly updateInitiativeCell = vi.fn(
    (_id: string, _cell: string): Observable<void> => this.updateInitiativeCellResult,
  );
  readonly deleteInitiative = vi.fn((_id: string): Observable<void> => this.deleteInitiativeResult);
  readonly getInitiativeChunks = vi.fn(
    (_id: string): Observable<PlanningChunk[]> => this.getInitiativeChunksResult,
  );
  readonly triggerProgramPlanning = vi.fn(
    (_request: ProgramPlanRequestDTO): Observable<ProgramPlanResponseDTO> =>
      this.triggerProgramPlanningResult,
  );
  readonly getAvailableSpecs = vi.fn((): Observable<SpecListDTO> => this.getAvailableSpecsResult);
  readonly getSpecDocument = vi.fn(
    (_name: string): Observable<SpecDocumentDTO> => this.getSpecDocumentResult,
  );
}

const initiative = (id: string, cell: string): Initiative => ({
  initiative_id: id,
  initiative_title: `Iniciativa ${id}`,
  cell,
});

describe('GIVEN PlanningStateService', () => {
  let service: PlanningStateService;
  let mockApi: MockDevopsAgentApiService;
  let mockTasksState: MockTasksStateService;
  let mockNotifications: MockNotificationService;

  const latest = <T>(source: Observable<T>): T => {
    let value!: T;
    const subscription = source.subscribe((emitted) => (value = emitted));
    subscription.unsubscribe();
    return value;
  };

  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    mockApi = new MockDevopsAgentApiService();
    mockTasksState = new MockTasksStateService();
    mockNotifications = new MockNotificationService();

    TestBed.configureTestingModule({
      providers: [
        PlanningStateService,
        { provide: DevopsAgentApiService, useValue: mockApi as unknown as DevopsAgentApiService },
        { provide: TasksStateService, useValue: mockTasksState as unknown as TasksStateService },
        {
          provide: NotificationService,
          useValue: mockNotifications as unknown as NotificationService,
        },
      ],
    });

    service = TestBed.inject(PlanningStateService);
  });

  afterEach(() => {
    TestBed.resetTestingModule();
    vi.restoreAllMocks();
  });

  describe('WHEN initialized', () => {
    it('THEN state variables start in idle values', () => {
      expect(latest(service.initiatives)).toEqual([]);
      expect(latest(service.loading)).toBe(false);
      expect(latest(service.uploading)).toBe(false);
      expect(latest(service.uploadStatus)).toBeNull();

      expect(latest(service.availableSpecs)).toEqual([]);
      expect(service.availableSpecsSignal()).toEqual([]);
      expect(latest(service.selectedSpec)).toBeNull();
      expect(service.selectedSpecSignal()).toBeNull();
      expect(latest(service.loadingSpecs)).toBe(false);
      expect(service.loadingSpecsSignal()).toBe(false);
      expect(latest(service.planningRunning)).toBe(false);
      expect(service.planningRunningSignal()).toBe(false);
    });
  });

  describe('WHEN loadInitiatives is called', () => {
    it('THEN updates initiatives and toggles loading flag', () => {
      const list = [initiative('i-1', 'Core')];
      mockApi.getInitiativesResult = of(list);

      service.loadInitiatives();

      expect(mockApi.getInitiatives).toHaveBeenCalledTimes(1);
      expect(latest(service.initiatives)).toEqual(list);
      expect(latest(service.loading)).toBe(false);
    });

    it('THEN handles error by keeping previous list and resetting loading flag', () => {
      mockApi.getInitiativesResult = of([initiative('i-1', 'Core')]);
      service.loadInitiatives();

      mockApi.getInitiativesResult = throwError(() => new Error('Error de red'));
      service.loadInitiatives();

      expect(latest(service.initiatives)).toHaveLength(1);
      expect(latest(service.loading)).toBe(false);
      expect(mockNotifications.error).toHaveBeenCalledWith('Error al cargar iniciativas');
    });
  });

  describe('WHEN uploadPlanning is called', () => {
    it('THEN uploads planning, sets success status and reloads initiatives', () => {
      mockApi.uploadPlanningResult = of({ message: 'ok' });

      service.uploadPlanning('init-1', 'Título 1', '# Contenido');

      expect(mockApi.uploadPlanning).toHaveBeenCalledWith({
        initiativeId: 'init-1',
        title: 'Título 1',
        markdownContent: '# Contenido',
      });
      expect(latest(service.uploadStatus)).toBe('¡Planeación indexada con éxito!');
      expect(mockApi.getInitiatives).toHaveBeenCalled();
      expect(latest(service.uploading)).toBe(false);
    });

    it('THEN handles upload failure reporting the error message', () => {
      mockApi.uploadPlanningResult = throwError(() => ({ error: 'Formato inválido' }));

      service.uploadPlanning('init-1', 'Título 1', '# Contenido');

      expect(latest(service.uploadStatus)).toContain('Error: Formato inválido');
      expect(latest(service.uploading)).toBe(false);
      expect(mockNotifications.error).toHaveBeenCalledWith('Error al cargar planeación');
    });

    it('THEN clearUploadStatus wipes the status message', () => {
      service.uploadPlanning('init-1', 'Título 1', '# Contenido');
      service.clearUploadStatus();

      expect(latest(service.uploadStatus)).toBeNull();
    });
  });

  describe('WHEN updateInitiativeCell is called', () => {
    it('THEN updates the cell for matching initiative', () => {
      mockApi.getInitiativesResult = of([initiative('i-1', 'Vieja'), initiative('i-2', 'Otra')]);
      service.loadInitiatives();

      service.updateInitiativeCell('i-1', 'Nueva Célula');

      const list = latest(service.initiatives);
      expect(list[0].cell).toBe('Nueva Célula');
      expect(list[1].cell).toBe('Otra');
    });

    it('THEN keeps previous list on error', () => {
      mockApi.getInitiativesResult = of([initiative('i-1', 'Vieja')]);
      service.loadInitiatives();

      mockApi.updateInitiativeCellResult = throwError(() => new Error('Error al actualizar'));
      service.updateInitiativeCell('i-1', 'Nueva');

      expect(latest(service.initiatives)[0].cell).toBe('Vieja');
      expect(mockNotifications.error).toHaveBeenCalledWith(
        'Error al actualizar la célula de la iniciativa i-1',
      );
    });
  });

  describe('WHEN deleteInitiative is called', () => {
    it('THEN removes the initiative from the list', () => {
      mockApi.getInitiativesResult = of([initiative('i-1', 'Core'), initiative('i-2', 'Canales')]);
      service.loadInitiatives();

      service.deleteInitiative('i-1');

      const list = latest(service.initiatives);
      expect(list).toHaveLength(1);
      expect(list[0].initiative_id).toBe('i-2');
    });

    it('THEN keeps previous list on error', () => {
      mockApi.getInitiativesResult = of([initiative('i-1', 'Core')]);
      service.loadInitiatives();

      mockApi.deleteInitiativeResult = throwError(() => new Error('Error al eliminar'));
      service.deleteInitiative('i-1');

      expect(latest(service.initiatives)).toHaveLength(1);
      expect(mockNotifications.error).toHaveBeenCalledWith('Error al eliminar la iniciativa i-1');
    });
  });

  describe('WHEN getInitiativeChunks is called', () => {
    it('THEN delegates to the API service', () => {
      const chunks: PlanningChunk[] = [
        { id: 'c1', initiativeId: 'i-1', sectionName: 'Sec', content: 'Cont', metadata: {} },
      ];
      mockApi.getInitiativeChunksResult = of(chunks);

      let result: PlanningChunk[] = [];
      service.getInitiativeChunks('i-1').subscribe((data) => (result = data));

      expect(mockApi.getInitiativeChunks).toHaveBeenCalledWith('i-1');
      expect(result).toEqual(chunks);
    });
  });

  describe('WHEN loadAvailableSpecs is called', () => {
    it('THEN updates availableSpecs and toggles loadingSpecs', () => {
      const mockList: SpecListDTO = {
        specs: ['ideas_planning_Q3.md', 'frente_canales.md'],
        total: 2,
      };
      mockApi.getAvailableSpecsResult = of(mockList);

      service.loadAvailableSpecs();

      expect(mockApi.getAvailableSpecs).toHaveBeenCalledTimes(1);
      expect(latest(service.availableSpecs)).toEqual(mockList.specs);
      expect(service.availableSpecsSignal()).toEqual(mockList.specs);
      expect(latest(service.loadingSpecs)).toBe(false);
      expect(service.loadingSpecsSignal()).toBe(false);
    });

    it('THEN handles error by keeping previous specs and resetting loading flag', () => {
      mockApi.getAvailableSpecsResult = throwError(() => new Error('Fallo de red'));

      service.loadAvailableSpecs();

      expect(latest(service.availableSpecs)).toEqual([]);
      expect(latest(service.loadingSpecs)).toBe(false);
      expect(mockNotifications.error).toHaveBeenCalledWith(
        'Error al cargar especificaciones documentales',
      );
    });
  });

  describe('WHEN selectSpec is called', () => {
    it('THEN does not call API if spec name is empty or only whitespace', () => {
      service.selectSpec('   ');
      expect(mockApi.getSpecDocument).not.toHaveBeenCalled();
    });

    it('THEN loads and updates selectedSpec and toggles loading flag', () => {
      const mockDoc: SpecDocumentDTO = {
        name: 'ideas_planning_Q3.md',
        content: '# Ideas Q3',
        path: 'specs/ideas_planning_Q3.md',
      };
      mockApi.getSpecDocumentResult = of(mockDoc);

      service.selectSpec('ideas_planning_Q3.md');

      expect(mockApi.getSpecDocument).toHaveBeenCalledWith('ideas_planning_Q3.md');
      expect(latest(service.selectedSpec)).toEqual(mockDoc);
      expect(service.selectedSpecSignal()).toEqual(mockDoc);
      expect(latest(service.loadingSpecs)).toBe(false);
    });

    it('THEN handles error by resetting loading flag and notifying error', () => {
      mockApi.getSpecDocumentResult = throwError(() => new Error('Doc no encontrado'));

      service.selectSpec('non-existent.md');

      expect(latest(service.selectedSpec)).toBeNull();
      expect(latest(service.loadingSpecs)).toBe(false);
      expect(mockNotifications.error).toHaveBeenCalledWith(
        'Error al cargar el documento non-existent.md',
      );
    });
  });

  describe('WHEN clearSelectedSpec is called', () => {
    it('THEN resets selectedSpec to null', () => {
      const mockDoc: SpecDocumentDTO = {
        name: 'ideas_planning_Q3.md',
        content: '# Ideas Q3',
        path: 'specs/ideas_planning_Q3.md',
      };
      mockApi.getSpecDocumentResult = of(mockDoc);
      service.selectSpec('ideas_planning_Q3.md');

      service.clearSelectedSpec();

      expect(latest(service.selectedSpec)).toBeNull();
      expect(service.selectedSpecSignal()).toBeNull();
    });
  });

  describe('WHEN triggerProgramPlanning is called', () => {
    const request: ProgramPlanRequestDTO = {
      quarter: 'Q3',
      sprintCount: 6,
      maxCapacityPerSprint: 45,
      targetFronts: ['Canales', 'Core'],
      objectives: 'Objetivos Q3',
    };

    it('THEN executes planning, notifies TasksStateService.triggerImmediatePoll, and updates planningRunning', () => {
      const mockResponse: ProgramPlanResponseDTO = {
        summary: 'Encolado',
        quarter: 'Q3',
        sprintCount: 6,
        maxCapacityPerSprint: 45,
        targetFronts: ['Canales', 'Core'],
        contextId: 'ctx-123',
        task: {
          id: 't-123',
          contextId: 'ctx-123',
          status: { state: 'submitted' },
        },
      };
      mockApi.triggerProgramPlanningResult = of(mockResponse);

      let emittedResponse: ProgramPlanResponseDTO | undefined;
      service.triggerProgramPlanning(request).subscribe((res) => {
        emittedResponse = res;
      });

      expect(mockApi.triggerProgramPlanning).toHaveBeenCalledWith(request);
      expect(emittedResponse).toEqual(mockResponse);
      expect(mockNotifications.success).toHaveBeenCalledWith(
        'Planeación para Q3 encolada exitosamente',
      );
      expect(mockTasksState.triggerImmediatePoll).toHaveBeenCalledTimes(1);
      expect(latest(service.planningRunning)).toBe(false);
      expect(service.planningRunningSignal()).toBe(false);
    });

    it('THEN handles error by turning off planningRunning, notifying and rethrowing', () => {
      const networkError = new Error('BFF no responde');
      mockApi.triggerProgramPlanningResult = throwError(() => networkError);

      let errorCaught: unknown;
      service.triggerProgramPlanning(request).subscribe({
        error: (err) => {
          errorCaught = err;
        },
      });

      expect(errorCaught).toBe(networkError);
      expect(mockNotifications.error).toHaveBeenCalledWith(
        'Error al solicitar planeación de programa',
      );
      expect(mockTasksState.triggerImmediatePoll).not.toHaveBeenCalled();
      expect(latest(service.planningRunning)).toBe(false);
      expect(service.planningRunningSignal()).toBe(false);
    });
  });
});
