import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AgentChatApiService } from './agent-chat-api.service';
import { AgentCard, SendMessageRequest } from '../../models/devops-agent.model';

describe('GIVEN AgentChatApiService', () => {
  let service: AgentChatApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AgentChatApiService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AgentChatApiService);
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
});
