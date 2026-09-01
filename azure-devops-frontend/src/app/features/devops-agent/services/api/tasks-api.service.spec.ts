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
});
