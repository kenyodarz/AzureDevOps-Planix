import { TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';
import { RefinementChatStateService } from './refinement-chat-state.service';
import { TasksStateService } from './tasks-state.service';
import { DevopsAgentApiService } from '../devops-agent-api.service';
import { SendMessageRequest, SendMessageResponse } from '../../models/devops-agent.model';
import { REFINEMENT_GREETING, REFINEMENT_RESET_GREETING } from '../../domain';

class MockDevopsAgentApiService {
  sendMessageResult: Observable<SendMessageResponse> = of({});
  readonly sendMessage = vi.fn(
    (_payload: SendMessageRequest): Observable<SendMessageResponse> => this.sendMessageResult,
  );
}

class MockTasksStateService {
  readonly triggerImmediatePoll = vi.fn((): void => undefined);
}

describe('GIVEN RefinementChatStateService', () => {
  let service: RefinementChatStateService;
  let mockApi: MockDevopsAgentApiService;
  let mockTasksState: MockTasksStateService;

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

    TestBed.configureTestingModule({
      providers: [
        RefinementChatStateService,
        { provide: DevopsAgentApiService, useValue: mockApi as unknown as DevopsAgentApiService },
        { provide: TasksStateService, useValue: mockTasksState as unknown as TasksStateService },
      ],
    });

    service = TestBed.inject(RefinementChatStateService);
  });

  afterEach(() => {
    TestBed.resetTestingModule();
    vi.restoreAllMocks();
  });

  describe('WHEN initialized', () => {
    it('THEN seeds messages with refinement greeting and mirrors messages alias', () => {
      const messages = latest(service.refinementMessages);
      expect(messages).toHaveLength(1);
      expect(messages[0]).toEqual(REFINEMENT_GREETING);
      expect(latest(service.messages)).toEqual(messages);
      expect(latest(service.loading)).toBe(false);
    });
  });

  describe('WHEN sendRefinementMessage or sendMessage is called', () => {
    it('THEN sendMessage routes to sendRefinementMessage and appends messages', () => {
      mockApi.sendMessageResult = of({
        message: {
          role: 'agent',
          parts: [{ text: 'Respuesta refinamiento' }],
        },
      });

      service.sendMessage('Refinar HU 100');

      expect(mockApi.sendMessage).toHaveBeenCalledTimes(1);
      const messages = latest(service.refinementMessages);
      expect(messages).toHaveLength(3);
      expect(messages[1].parts[0].text).toBe('Refinar HU 100');
      expect(messages[2].parts[0].text).toBe('Respuesta refinamiento');
    });

    it('THEN ignores whitespace-only message', () => {
      service.sendRefinementMessage('   ');

      expect(mockApi.sendMessage).not.toHaveBeenCalled();
      expect(latest(service.refinementMessages)).toHaveLength(1);
    });

    it('THEN handles network error appending error copy', () => {
      mockApi.sendMessageResult = throwError(() => new Error('Offline'));

      service.sendRefinementMessage('Prueba error');

      const messages = latest(service.refinementMessages);
      expect(messages).toHaveLength(3);
      expect(messages[2].parts[0].text).toContain('Error de red');
    });
  });

  describe('WHEN clearRefinementChat or clearChat is called', () => {
    it('THEN resets the refinement messages to reset greeting', () => {
      service.sendMessage('Mensaje previo');
      service.clearChat();

      const messages = latest(service.refinementMessages);
      expect(messages).toHaveLength(1);
      expect(messages[0]).toEqual(REFINEMENT_RESET_GREETING);
    });
  });

  describe('WHEN refineStoryInChat is called', () => {
    it('THEN sends formatted refinement prompt with story id and title', () => {
      service.refineStoryInChat('US-101', 'Exportar reportes');

      expect(mockApi.sendMessage).toHaveBeenCalledTimes(1);
      const payload = mockApi.sendMessage.mock.calls[0][0];
      expect(payload.message.parts[0].text).toContain('US-101');
      expect(payload.message.parts[0].text).toContain('Exportar reportes');
    });
  });

  describe('WHEN auditStory is called', () => {
    it('THEN triggers immediate poll on tasks state and calls sendMessage', () => {
      service.auditStory('US-202').subscribe();

      expect(mockTasksState.triggerImmediatePoll).toHaveBeenCalledTimes(1);
      expect(mockApi.sendMessage).toHaveBeenCalledTimes(1);
      const payload = mockApi.sendMessage.mock.calls[0][0];
      expect(payload.message.contextId).toContain('US-202');
      expect(payload.message.parts[0].text).toContain('US-202');
    });
  });

  describe('WHEN prefillPrompt or consumePrefilledPrompt is called', () => {
    it('THEN sets prefilled prompt and clears it when consumed', () => {
      expect(latest(service.prefilledPrompt)).toBeNull();

      service.prefillPrompt('Prompt de refinamiento precargado');
      expect(latest(service.prefilledPrompt)).toBe('Prompt de refinamiento precargado');

      const consumed = service.consumePrefilledPrompt();
      expect(consumed).toBe('Prompt de refinamiento precargado');
      expect(latest(service.prefilledPrompt)).toBeNull();
    });
  });
});
