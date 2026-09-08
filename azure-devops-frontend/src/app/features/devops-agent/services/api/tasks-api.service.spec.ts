import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TasksApiService } from './tasks-api.service';
import { AgentTask } from '../../models/devops-agent.model';

describe('GIVEN TasksApiService', () => {
  let service: TasksApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [TasksApiService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TasksApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('WHEN getTasks is called', () => {
    it('THEN performs a GET request to /api/tasks', () => {
      const mockTasks: AgentTask[] = [{ id: 't1', contextId: 'ctx-1' }];

      service.getTasks().subscribe((tasks) => {
        expect(tasks).toEqual(mockTasks);
      });

      const req = httpMock.expectOne('/api/tasks');
      expect(req.request.method).toBe('GET');
      req.flush(mockTasks);
    });
  });

  describe('WHEN cancelTask is called', () => {
    it('THEN posts a JSON-RPC envelope to the ROOT path (D-08, ver DP-03)', () => {
      service.cancelTask('t1').subscribe();

      const req = httpMock.expectOne('/');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({
        jsonrpc: '2.0',
        method: 'tasks/cancel',
        params: { taskId: 't1' },
        id: 'cancel-t1',
      });
      req.flush({});
    });
  });

  describe('WHEN getTasksStream is called', () => {
    let originalEventSource: typeof EventSource;

    beforeEach(() => {
      originalEventSource = globalThis.EventSource;
      globalThis.EventSource = EventSourceStub as unknown as typeof EventSource;
    });

    afterEach(() => {
      globalThis.EventSource = originalEventSource;
    });

    it('THEN opens EventSource to /api/tasks/stream and emits INITIAL events', () => {
      let emittedData: unknown = null;
      const sub = service.getTasksStream().subscribe((event) => {
        emittedData = event;
      });

      expect(EventSourceStub.last?.url).toBe('/api/tasks/stream');

      EventSourceStub.last?.emit(
        'INITIAL',
        JSON.stringify({ event: 'INITIAL', data: [{ id: 't-1' }] }),
      );

      expect(emittedData).toEqual({
        event: 'INITIAL',
        data: [{ id: 't-1' }],
      });

      sub.unsubscribe();
      expect(EventSourceStub.last?.closed).toBe(true);
    });

    it('THEN emits TASKS_UPDATE events when received', () => {
      let emittedData: unknown = null;
      service.getTasksStream().subscribe((event) => {
        emittedData = event;
      });

      EventSourceStub.last?.emit(
        'TASKS_UPDATE',
        JSON.stringify({ event: 'TASKS_UPDATE', data: [{ id: 't-2' }] }),
      );

      expect(emittedData).toEqual({
        event: 'TASKS_UPDATE',
        data: [{ id: 't-2' }],
      });
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
