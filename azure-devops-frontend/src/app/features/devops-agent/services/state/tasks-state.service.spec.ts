import { TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';
import { TasksStateService } from './tasks-state.service';
import { DevopsAgentApiService } from '../devops-agent-api.service';
import { AgentTask } from '../../models/devops-agent.model';
import { POLLING_INTERVAL_ACTIVE_MS, POLLING_INTERVAL_IDLE_MS } from '../../../../core';

class MockDevopsAgentApiService {
  getTasksResult: Observable<AgentTask[]> = of([]);
  cancelTaskResult: Observable<unknown> = of({});

  readonly getTasks = vi.fn((): Observable<AgentTask[]> => this.getTasksResult);
  readonly cancelTask = vi.fn((_id: string): Observable<unknown> => this.cancelTaskResult);
}

const workingTask = (id: string): AgentTask => ({ id, status: { state: 'working' } });
const submittedTask = (id: string): AgentTask => ({ id, status: { state: 'submitted' } });
const completedTask = (id: string): AgentTask => ({ id, status: { state: 'completed' } });

describe('GIVEN TasksStateService', () => {
  let service: TasksStateService;
  let mockApi: MockDevopsAgentApiService;

  const latest = <T>(source: Observable<T>): T => {
    let value!: T;
    const subscription = source.subscribe((emitted) => (value = emitted));
    subscription.unsubscribe();
    return value;
  };

  beforeEach(() => {
    vi.useFakeTimers();
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    mockApi = new MockDevopsAgentApiService();

    TestBed.configureTestingModule({
      providers: [
        TasksStateService,
        { provide: DevopsAgentApiService, useValue: mockApi as unknown as DevopsAgentApiService },
      ],
    });

    service = TestBed.inject(TasksStateService);
  });

  afterEach(() => {
    TestBed.resetTestingModule();
    vi.clearAllTimers();
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  describe('WHEN initialized', () => {
    it('THEN loads tasks immediately on construction', () => {
      expect(mockApi.getTasks).toHaveBeenCalledTimes(1);
      expect(latest(service.tasks)).toEqual([]);
    });

    it('THEN normalizes a null payload to empty array', () => {
      mockApi.getTasksResult = of(null as unknown as AgentTask[]);
      service.loadTasks();

      expect(latest(service.tasks)).toEqual([]);
    });
  });

  describe('WHEN cancelTask is called', () => {
    it('THEN cancels task and reloads tasks on success', () => {
      const callsBefore = mockApi.getTasks.mock.calls.length;

      service.cancelTask('task-1');

      expect(mockApi.cancelTask).toHaveBeenCalledWith('task-1');
      expect(mockApi.getTasks.mock.calls.length).toBe(callsBefore + 1);
    });

    it('THEN does not reload tasks if cancellation fails', () => {
      mockApi.cancelTaskResult = throwError(() => new Error('Error al cancelar'));
      const callsBefore = mockApi.getTasks.mock.calls.length;

      service.cancelTask('task-1');

      expect(mockApi.getTasks.mock.calls.length).toBe(callsBefore);
    });
  });

  describe('WHEN dynamic polling runs', () => {
    it('THEN polls with idle interval when no active tasks exist', () => {
      vi.advanceTimersByTime(POLLING_INTERVAL_IDLE_MS);

      expect(mockApi.getTasks).toHaveBeenCalledTimes(2);
    });

    it('THEN shortens interval to active interval when working task is found', () => {
      mockApi.getTasksResult = of([workingTask('t-1')]);

      vi.advanceTimersByTime(POLLING_INTERVAL_IDLE_MS);
      expect(mockApi.getTasks).toHaveBeenCalledTimes(2);

      vi.advanceTimersByTime(POLLING_INTERVAL_ACTIVE_MS);
      expect(mockApi.getTasks).toHaveBeenCalledTimes(3);
    });

    it('THEN shortens interval to active interval when submitted task is found', () => {
      mockApi.getTasksResult = of([submittedTask('t-2')]);

      vi.advanceTimersByTime(POLLING_INTERVAL_IDLE_MS);
      vi.advanceTimersByTime(POLLING_INTERVAL_ACTIVE_MS);

      expect(mockApi.getTasks).toHaveBeenCalledTimes(3);
    });

    it('THEN returns to idle interval once tasks complete', () => {
      mockApi.getTasksResult = of([workingTask('t-1')]);
      vi.advanceTimersByTime(POLLING_INTERVAL_IDLE_MS);

      mockApi.getTasksResult = of([completedTask('t-1')]);
      vi.advanceTimersByTime(POLLING_INTERVAL_ACTIVE_MS);
      const callsAfterCompleted = mockApi.getTasks.mock.calls.length;

      vi.advanceTimersByTime(POLLING_INTERVAL_ACTIVE_MS);
      expect(mockApi.getTasks).toHaveBeenCalledTimes(callsAfterCompleted);

      vi.advanceTimersByTime(POLLING_INTERVAL_IDLE_MS - POLLING_INTERVAL_ACTIVE_MS);
      expect(mockApi.getTasks).toHaveBeenCalledTimes(callsAfterCompleted + 1);
    });

    it('THEN triggerImmediatePoll immediately polls and uses active interval', () => {
      service.triggerImmediatePoll();
      expect(mockApi.getTasks).toHaveBeenCalledTimes(2);

      vi.advanceTimersByTime(POLLING_INTERVAL_ACTIVE_MS);
      expect(mockApi.getTasks).toHaveBeenCalledTimes(3);
    });

    it('THEN resets to idle interval on polling failure', () => {
      mockApi.getTasksResult = throwError(() => new Error('Polling error'));

      vi.advanceTimersByTime(POLLING_INTERVAL_IDLE_MS);
      expect(mockApi.getTasks).toHaveBeenCalledTimes(2);

      vi.advanceTimersByTime(POLLING_INTERVAL_IDLE_MS);
      expect(mockApi.getTasks).toHaveBeenCalledTimes(3);
    });
  });
});
