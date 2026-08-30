import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DevopsAgentApiService } from './devops-agent-api.service';
import { AgentCard, IngestPayload, SendMessageRequest } from '../models/devops-agent.model';

describe('GIVEN DevopsAgentApiService', () => {
  let service: DevopsAgentApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DevopsAgentApiService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DevopsAgentApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('WHEN getAgentCard is called', () => {
    it('THEN performs a GET request to /.well-known/agent-card.json', () => {
      const mockCard: AgentCard = {
        name: 'Test Scrum Master',
        version: '1.2.3',
        description: 'Test Description',
      };

      service.getAgentCard().subscribe((card) => {
        expect(card.name).toBe('Test Scrum Master');
        expect(card.version).toBe('1.2.3');
      });

      const req = httpMock.expectOne('/.well-known/agent-card.json');
      expect(req.request.method).toBe('GET');
      req.flush(mockCard);
    });
  });

  describe('WHEN sendMessage is called', () => {
    it('THEN performs a POST request to /message:send', () => {
      const payload: SendMessageRequest = {
        message: {
          role: 'user',
          messageId: 'msg-123',
          contextId: 'session-456',
          parts: [{ text: 'hello' }],
        },
      };

      const mockResponse = {
        message: {
          role: 'agent',
          parts: [{ text: 'response text' }],
        },
      };

      service.sendMessage(payload).subscribe((res) => {
        expect(res.message?.parts?.[0]?.text).toBe('response text');
      });

      const req = httpMock.expectOne('/message:send');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(payload);
      req.flush(mockResponse);
    });
  });

  describe('WHEN uploadPlanning is called', () => {
    it('THEN performs a POST request to /api/planning/ingest', () => {
      const payload: IngestPayload = {
        initiativeId: 'guardian-q3',
        title: 'Guardián Q3',
        markdownContent: '# Planeación',
      };

      service.uploadPlanning(payload).subscribe((res) => {
        expect(res).toBeTruthy();
      });

      const req = httpMock.expectOne('/api/planning/ingest');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(payload);
      req.flush({ status: 'success' });
    });
  });

  describe('WHEN getInitiatives is called', () => {
    it('THEN performs a GET request to /api/planning/initiatives', () => {
      const mockList = [{ initiative_id: 'i1', initiative_title: 'Title 1', cell: 'c1' }];

      service.getInitiatives().subscribe((res) => {
        expect(res).toEqual(mockList);
      });

      const req = httpMock.expectOne('/api/planning/initiatives');
      expect(req.request.method).toBe('GET');
      req.flush(mockList);
    });
  });

  describe('WHEN deleteInitiative is called', () => {
    it('THEN performs a DELETE request to /api/planning/initiatives/:id', () => {
      service.deleteInitiative('i1').subscribe();

      const req = httpMock.expectOne('/api/planning/initiatives/i1');
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('WHEN updateInitiativeCell is called', () => {
    it('THEN performs a PUT request to /api/planning/initiatives/:id/cell', () => {
      service.updateInitiativeCell('i1', 'new-cell').subscribe();

      const req = httpMock.expectOne('/api/planning/initiatives/i1/cell');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ cell: 'new-cell' });
      req.flush(null);
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
      // GIVEN
      let received: string | null = null;
      let completed = false;
      service.getDashboardDataStream('EQU1096', 'Sprint 247').subscribe({
        error: (error: Error) => (received = error.message),
        complete: () => (completed = true),
      });

      // WHEN
      EventSourceStub.last?.emit(
        'ERROR',
        JSON.stringify({ event: 'ERROR', data: null, message: 'agente caído' }),
      );

      // THEN
      expect(received).toBe('agente caído');
      expect(completed).toBe(false);
      expect(EventSourceStub.last?.closed).toBe(true);
    });

    it('THEN falls back to a readable message when the event carries no detail', () => {
      // GIVEN
      let received: string | null = null;
      service
        .getDashboardDataStream('EQU1096', 'Sprint 247')
        .subscribe({ error: (error: Error) => (received = error.message) });

      // WHEN
      EventSourceStub.last?.emit('ERROR', JSON.stringify({ event: 'ERROR', data: null }));

      // THEN
      expect(received).toBe('Fallo en la comunicación con el agente o MCP.');
    });

    it('THEN still forwards INITIAL events as data', () => {
      // GIVEN
      const events: unknown[] = [];
      service
        .getDashboardDataStream('EQU1096', 'Sprint 247')
        .subscribe({ next: (event) => events.push(event) });

      // WHEN
      EventSourceStub.last?.emit(
        'INITIAL',
        JSON.stringify({ event: 'INITIAL', data: { metrics: {}, items: [] } }),
      );

      // THEN
      expect(events).toHaveLength(1);
    });
  });
});

/**
 * Doble de `EventSource`: el entorno de pruebas no abre conexiones reales, así que el stub permite
 * disparar los eventos del stream a mano y comprobar qué hace el servicio con cada uno.
 */
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
