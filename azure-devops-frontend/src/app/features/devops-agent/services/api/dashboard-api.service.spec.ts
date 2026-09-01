import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DashboardApiService } from './dashboard-api.service';
import { DashboardData } from '../../models/devops-agent.model';

describe('GIVEN DashboardApiService', () => {
  let service: DashboardApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DashboardApiService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DashboardApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('WHEN getDashboardData is called', () => {
    it('THEN performs a GET request to /api/devops/dashboard with cell and sprint params', () => {
      const mockData: DashboardData = {
        metrics: {
          totalPoints: 20,
          completedPoints: 10,
          completedPercentage: 50,
          avgQualityScore: 85,
          undocumentedCount: 0,
        },
        items: [],
      };

      service.getDashboardData('EQU1096', 'Sprint 247').subscribe((data) => {
        expect(data).toEqual(mockData);
      });

      const req = httpMock.expectOne((candidate) => candidate.url === '/api/devops/dashboard');
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('cell')).toBe('EQU1096');
      expect(req.request.params.get('sprint')).toBe('Sprint 247');
      req.flush(mockData);
    });
  });

  describe('WHEN getDashboardDataStream receives an ERROR event (D-40)', () => {
    let originalEventSource: typeof EventSource;

    beforeEach(() => {
      originalEventSource = globalThis.EventSource;
      globalThis.EventSource = EventSourceStub as unknown as typeof EventSource;
    });

    afterEach(() => {
      globalThis.EventSource = originalEventSource;
    });

    it('THEN propagates the backend message instead of completing in silence', () => {
      let received: string | null = null;
      let completed = false;
      service.getDashboardDataStream('EQU1096', 'Sprint 247').subscribe({
        error: (error: Error) => (received = error.message),
        complete: () => (completed = true),
      });

      EventSourceStub.last?.emit(
        'ERROR',
        JSON.stringify({ event: 'ERROR', data: null, message: 'agente caído' }),
      );

      expect(received).toBe('agente caído');
      expect(completed).toBe(false);
      expect(EventSourceStub.last?.closed).toBe(true);
    });

    it('THEN falls back to a readable message when the event carries no detail', () => {
      let received: string | null = null;
      service
        .getDashboardDataStream('EQU1096', 'Sprint 247')
        .subscribe({ error: (error: Error) => (received = error.message) });

      EventSourceStub.last?.emit('ERROR', JSON.stringify({ event: 'ERROR', data: null }));

      expect(received).toBe('Fallo en la comunicación con el agente o MCP.');
    });

    it('THEN still forwards INITIAL events as data', () => {
      const events: unknown[] = [];
      service
        .getDashboardDataStream('EQU1096', 'Sprint 247')
        .subscribe({ next: (event) => events.push(event) });

      EventSourceStub.last?.emit(
        'INITIAL',
        JSON.stringify({ event: 'INITIAL', data: { metrics: {}, items: [] } }),
      );

      expect(events).toHaveLength(1);
    });

    it('THEN forwards BATCH_UPDATE events as data', () => {
      const events: unknown[] = [];
      service
        .getDashboardDataStream('EQU1096', 'Sprint 247')
        .subscribe({ next: (event) => events.push(event) });

      EventSourceStub.last?.emit(
        'BATCH_UPDATE',
        JSON.stringify({ event: 'BATCH_UPDATE', data: { metrics: {}, items: [] } }),
      );

      expect(events).toHaveLength(1);
    });

    it('THEN encodes cell and sprint into the stream URL', () => {
      service.getDashboardDataStream('Celula A&B', 'Sprint 247/1').subscribe();

      expect(EventSourceStub.last?.url).toBe(
        '/api/devops/dashboard/stream?cell=Celula%20A%26B&sprint=Sprint%20247%2F1',
      );
    });

    it('THEN ignores events whose payload is not valid JSON', () => {
      const events: unknown[] = [];
      let failed = false;
      service.getDashboardDataStream('EQU1096', 'Sprint 247').subscribe({
        next: (event) => events.push(event),
        error: () => (failed = true),
      });

      EventSourceStub.last?.emit('INITIAL', 'esto no es json');

      expect(events).toHaveLength(0);
      expect(failed).toBe(false);
    });

    it('THEN completes silently when the transport itself errors', () => {
      let completed = false;
      let failed = false;
      service.getDashboardDataStream('EQU1096', 'Sprint 247').subscribe({
        complete: () => (completed = true),
        error: () => (failed = true),
      });

      EventSourceStub.last?.onerror?.();

      expect(completed).toBe(true);
      expect(failed).toBe(false);
      expect(EventSourceStub.last?.closed).toBe(true);
    });

    it('THEN closes the EventSource when the consumer unsubscribes', () => {
      const subscription = service.getDashboardDataStream('EQU1096', 'Sprint 247').subscribe();

      subscription.unsubscribe();

      expect(EventSourceStub.last?.closed).toBe(true);
    });
  });
});

class EventSourceStub {
  static last: EventSourceStub | null = null;

  onerror: (() => void) | null = null;
  closed = false;

  private readonly listeners = new Map<string, (event: MessageEvent<string>) => void>();

  constructor(public readonly url: string) {
    EventSourceStub.last = this;
  }

  addEventListener(type: string, handler: (event: MessageEvent<string>) => void): void {
    this.listeners.set(type, handler);
  }

  close(): void {
    this.closed = true;
  }

  emit(type: string, data: string): void {
    this.listeners.get(type)?.({ data } as MessageEvent<string>);
  }
}
