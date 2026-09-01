import { TestBed } from '@angular/core/testing';
import { EMPTY, Observable, of, throwError } from 'rxjs';
import { DashboardStateService } from './dashboard-state.service';
import { DevopsAgentApiService } from '../devops-agent-api.service';
import { DashboardData, DashboardStreamEvent } from '../../models/devops-agent.model';

class MockDevopsAgentApiService {
  dashboardStreamResult: Observable<DashboardStreamEvent> = EMPTY;

  readonly getDashboardDataStream = vi.fn(
    (_cell: string, _sprint: string): Observable<DashboardStreamEvent> =>
      this.dashboardStreamResult,
  );
}

const emptyDashboardData = (): DashboardData => ({
  metrics: {
    totalPoints: 10,
    completedPoints: 5,
    completedPercentage: 50,
    avgQualityScore: 85,
    undocumentedCount: 1,
  },
  items: [],
});

describe('GIVEN DashboardStateService', () => {
  let service: DashboardStateService;
  let mockApi: MockDevopsAgentApiService;

  const latest = <T>(source: Observable<T>): T => {
    let value!: T;
    const subscription = source.subscribe((emitted) => (value = emitted));
    subscription.unsubscribe();
    return value;
  };

  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    vi.spyOn(console, 'warn').mockImplementation(() => undefined);
    mockApi = new MockDevopsAgentApiService();

    TestBed.configureTestingModule({
      providers: [
        DashboardStateService,
        { provide: DevopsAgentApiService, useValue: mockApi as unknown as DevopsAgentApiService },
      ],
    });

    service = TestBed.inject(DashboardStateService);
  });

  afterEach(() => {
    TestBed.resetTestingModule();
    vi.restoreAllMocks();
  });

  describe('WHEN initialized', () => {
    it('THEN state variables start with default idle values', () => {
      expect(latest(service.dashboardData)).toBeNull();
      expect(latest(service.dashboardError)).toBeNull();
      expect(latest(service.loading)).toBe(false);
    });
  });

  describe('WHEN loadDashboardData is called', () => {
    it('THEN ignores call when cell or sprint is empty', () => {
      service.loadDashboardData('', 'Sprint 1');
      service.loadDashboardData('Cell A', '');

      expect(mockApi.getDashboardDataStream).not.toHaveBeenCalled();
    });

    it('THEN starts loading and updates data upon INITIAL event', () => {
      const data = emptyDashboardData();
      mockApi.dashboardStreamResult = of({ event: 'INITIAL', data });

      service.loadDashboardData('Cell A', 'Sprint 1');

      expect(mockApi.getDashboardDataStream).toHaveBeenCalledWith('Cell A', 'Sprint 1');
      expect(latest(service.dashboardData)).toEqual(data);
      expect(latest(service.loading)).toBe(false);
      expect(latest(service.dashboardError)).toBeNull();
    });

    it('THEN updates data upon BATCH_UPDATE event', () => {
      const data = emptyDashboardData();
      mockApi.dashboardStreamResult = of({ event: 'BATCH_UPDATE', data });

      service.loadDashboardData('Cell A', 'Sprint 1');

      expect(latest(service.dashboardData)).toEqual(data);
    });

    it('THEN surfaces error and resets data on stream failure', () => {
      mockApi.dashboardStreamResult = throwError(() => new Error('Error al conectar SSE'));

      service.loadDashboardData('Cell A', 'Sprint 1');

      expect(latest(service.dashboardData)).toBeNull();
      expect(latest(service.dashboardError)).toBe('Error al conectar SSE');
      expect(latest(service.loading)).toBe(false);
    });

    it('THEN uses fallback error message when error has no description', () => {
      mockApi.dashboardStreamResult = throwError(() => ({}));

      service.loadDashboardData('Cell A', 'Sprint 1');

      expect(latest(service.dashboardError)).toBe('Fallo en la comunicación con el agente o MCP.');
      expect(latest(service.loading)).toBe(false);
    });
  });
});
