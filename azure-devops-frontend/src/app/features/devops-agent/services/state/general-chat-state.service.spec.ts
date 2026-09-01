import { TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';
import { GeneralChatStateService } from './general-chat-state.service';
import { DevopsAgentApiService } from '../devops-agent-api.service';
import { SendMessageRequest, SendMessageResponse } from '../../models/devops-agent.model';
import { GENERAL_GREETING, GENERAL_RESET_GREETING } from '../../domain';

class MockDevopsAgentApiService {
  sendMessageResult: Observable<SendMessageResponse> = of({});
  readonly sendMessage = vi.fn(
    (_payload: SendMessageRequest): Observable<SendMessageResponse> => this.sendMessageResult,
  );
}

describe('GIVEN GeneralChatStateService', () => {
  let service: GeneralChatStateService;
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
        GeneralChatStateService,
        { provide: DevopsAgentApiService, useValue: mockApi as unknown as DevopsAgentApiService },
      ],
    });

    service = TestBed.inject(GeneralChatStateService);
  });

  afterEach(() => {
    TestBed.resetTestingModule();
    vi.restoreAllMocks();
  });

  describe('WHEN initialized', () => {
    it('THEN seeds messages with the general greeting and loading is false', () => {
      const messages = latest(service.generalMessages);
      expect(messages).toHaveLength(1);
      expect(messages[0]).toEqual(GENERAL_GREETING);
      expect(latest(service.loading)).toBe(false);
    });
  });

  describe('WHEN sendGeneralMessage is called', () => {
    it('THEN ignores empty or whitespace-only messages', () => {
      service.sendGeneralMessage('   ');

      expect(mockApi.sendMessage).not.toHaveBeenCalled();
      expect(latest(service.generalMessages)).toHaveLength(1);
    });

    it('THEN sends user message and appends agent reply on success', () => {
      mockApi.sendMessageResult = of({
        message: {
          role: 'agent',
          messageId: 'reply-1',
          parts: [{ text: 'Hola, soy el agente general.' }],
        },
      });

      service.sendGeneralMessage('Hola agente');

      expect(mockApi.sendMessage).toHaveBeenCalledTimes(1);
      const messages = latest(service.generalMessages);
      expect(messages).toHaveLength(3);
      expect(messages[1].role).toBe('user');
      expect(messages[1].parts[0].text).toBe('Hola agente');
      expect(messages[2].role).toBe('agent');
      expect(messages[2].parts[0].text).toBe('Hola, soy el agente general.');
      expect(latest(service.loading)).toBe(false);
    });

    it('THEN handles network error gracefully with fallback message', () => {
      mockApi.sendMessageResult = throwError(() => new Error('Connection failed'));

      service.sendGeneralMessage('Mensaje con fallo');

      const messages = latest(service.generalMessages);
      expect(messages).toHaveLength(3);
      expect(messages[2].role).toBe('agent');
      expect(messages[2].parts[0].text).toContain('Error de red');
      expect(latest(service.loading)).toBe(false);
    });
  });

  describe('WHEN clearGeneralChat is called', () => {
    it('THEN resets the chat messages to the reset greeting and generates new context', () => {
      service.sendGeneralMessage('primer mensaje');
      const firstContextId = mockApi.sendMessage.mock.calls[0][0].message.contextId;

      service.clearGeneralChat();

      const messages = latest(service.generalMessages);
      expect(messages).toHaveLength(1);
      expect(messages[0]).toEqual(GENERAL_RESET_GREETING);

      service.sendGeneralMessage('segundo mensaje');
      const secondContextId = mockApi.sendMessage.mock.calls[1][0].message.contextId;
      expect(secondContextId).not.toBe(firstContextId);
    });
  });
});
