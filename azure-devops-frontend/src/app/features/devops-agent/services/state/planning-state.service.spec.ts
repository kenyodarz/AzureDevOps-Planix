import { TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';
import { PlanningStateService } from './planning-state.service';
import { DevopsAgentApiService } from '../devops-agent-api.service';
import { Initiative, PlanningChunk } from '../../models/devops-agent.model';

class MockDevopsAgentApiService {
  getInitiativesResult: Observable<Initiative[]> = of([]);
  uploadPlanningResult: Observable<unknown> = of({});
  updateInitiativeCellResult: Observable<void> = of(undefined);
  deleteInitiativeResult: Observable<void> = of(undefined);
  getInitiativeChunksResult: Observable<PlanningChunk[]> = of([]);

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
}

const initiative = (id: string, cell: string): Initiative => ({
  initiative_id: id,
  initiative_title: `Iniciativa ${id}`,
  cell,
});

describe('GIVEN PlanningStateService', () => {
  let service: PlanningStateService;
  let mockApi: MockDevopsAgentApiService;

  const latest = <T>(source: Observable<T>): T => {
    let value!: T;
    const subscription = source.subscribe((emitted) => (value = emitted));
    subscription.unsubscribe();
    return value;
  };

  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    mockApi = new MockDevopsAgentApiService();

    TestBed.configureTestingModule({
      providers: [
        PlanningStateService,
        { provide: DevopsAgentApiService, useValue: mockApi as unknown as DevopsAgentApiService },
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
});
