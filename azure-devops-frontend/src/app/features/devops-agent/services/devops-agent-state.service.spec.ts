import {TestBed} from '@angular/core/testing';
import {Observable, of, throwError} from 'rxjs';
import {DevopsAgentStateService} from './devops-agent-state.service';
import {DevopsAgentApiService} from './devops-agent-api.service';
import {AgentCard, SendMessageResponse} from '../models/devops-agent.model';

class MockDevopsAgentApiService {
  getAgentCardResult: Observable<AgentCard> = of({ name: 'Real Agent', version: '1.2', description: 'Desc' });
  sendMessageResult: Observable<SendMessageResponse> = of({});
  uploadPlanningResult: Observable<any> = of({});
  getInitiativesResult: Observable<any[]> = of([]);
  deleteInitiativeResult: Observable<void> = of(undefined);
  updateInitiativeCellResult: Observable<void> = of(undefined);
  getTasksResult: Observable<any[]> = of([]);
  cancelTaskResult: Observable<any> = of({});

  getAgentCard(): Observable<AgentCard> {
    return this.getAgentCardResult;
  }

  sendMessage(): Observable<SendMessageResponse> {
    return this.sendMessageResult;
  }

  uploadPlanning(): Observable<any> {
    return this.uploadPlanningResult;
  }

  getInitiatives(): Observable<any[]> {
    return this.getInitiativesResult;
  }

  deleteInitiative(): Observable<void> {
    return this.deleteInitiativeResult;
  }

  updateInitiativeCell(): Observable<void> {
    return this.updateInitiativeCellResult;
  }

  getTasks(): Observable<any[]> {
    return this.getTasksResult;
  }

  cancelTask(): Observable<any> {
    return this.cancelTaskResult;
  }
}

describe('GIVEN DevopsAgentStateService', () => {
  let service: DevopsAgentStateService;
  let mockApi: MockDevopsAgentApiService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        DevopsAgentStateService,
        { provide: DevopsAgentApiService, useClass: MockDevopsAgentApiService }
      ]
    });

    mockApi = TestBed.inject(DevopsAgentApiService) as unknown as MockDevopsAgentApiService;
    service = TestBed.inject(DevopsAgentStateService);
  });

  describe('WHEN initialized', () => {
    it('THEN fetches agent card and establishes initial greeting messages', () => {
      service.agentCard.subscribe((card) => {
        expect(card).toBeTruthy();
        expect(card?.name).toBe('Real Agent');
      });

      service.messages.subscribe((msgs) => {
        expect(msgs.length).toBe(1); // greeting message
        expect(msgs[0].role).toBe('agent');
      });
    });
  });

  describe('WHEN loadAgentCard fails', () => {
    it('THEN falls back to local agent card metadata', () => {
      mockApi.getAgentCardResult = throwError(() => new Error('Net error'));

      service.loadAgentCard();

      service.agentCard.subscribe((card) => {
        expect(card).toBeTruthy();
        expect(card?.name).toBe('Agente Local');
      });
    });
  });

  describe('WHEN sendMessage is invoked', () => {
    it('THEN appends user message, toggles loading state, and appends agent reply on success', () => {
      const mockReply: SendMessageResponse = {
        message: {
          role: 'agent',
          parts: [{ text: 'response text' }]
        }
      };
      mockApi.sendMessageResult = of(mockReply);

      service.sendMessage('hi');

      service.messages.subscribe((msgs) => {
        expect(msgs.length).toBe(3);
        expect(msgs[1].role).toBe('user');
        expect(msgs[1].parts[0].text).toBe('hi');
        expect(msgs[2].role).toBe('agent');
        expect(msgs[2].parts[0].text).toBe('response text');
      });
    });

    it('THEN appends error alert message when the network fails', () => {
      mockApi.sendMessageResult = throwError(() => new Error('Failed'));

      service.sendMessage('hi');

      service.messages.subscribe((msgs) => {
        expect(msgs.length).toBe(3);
        expect(msgs[2].parts[0].text).toContain('Error de red');
      });
    });
  });

  describe('WHEN uploadPlanning is invoked', () => {
    it('THEN triggers upload on API and updates status text on success', () => {
      mockApi.uploadPlanningResult = of({});

      service.uploadPlanning('id', 'title', 'content');

      service.uploadStatus.subscribe((status) => {
        expect(status).toBe('¡Planeación indexada con éxito!');
      });
    });

    it('THEN updates status with error description on failure', () => {
      mockApi.uploadPlanningResult = throwError(() => ({ error: 'Bad file format' }));

      service.uploadPlanning('id', 'title', 'content');

      service.uploadStatus.subscribe((status) => {
        expect(status).toContain('Error: Bad file format');
      });
    });
  });

  describe('WHEN clearChat is invoked', () => {
    it('THEN resets the message list to a solitary reset alert message', () => {
      service.clearChat();

      service.messages.subscribe((msgs) => {
        expect(msgs.length).toBe(1);
        expect(msgs[0].parts[0].text).toContain('Chat limpio');
      });
    });
  });

  describe('WHEN loadInitiatives is invoked', () => {
    it('THEN updates the initiatives list on success', () => {
      const mockList = [{ initiative_id: 'i1', initiative_title: 'Title 1', cell: 'c1' }];
      mockApi.getInitiativesResult = of(mockList);

      service.loadInitiatives();

      service.initiatives.subscribe((list) => {
        expect(list).toEqual(mockList);
      });
    });
  });

  describe('WHEN updateInitiativeCell is invoked', () => {
    it('THEN triggers update on API and updates the local item', () => {
      const initialList = [{ initiative_id: 'i1', initiative_title: 'Title 1', cell: 'c1' }];
      mockApi.getInitiativesResult = of(initialList);
      service.loadInitiatives();

      mockApi.updateInitiativeCellResult = of(undefined);
      service.updateInitiativeCell('i1', 'new-cell');

      service.initiatives.subscribe((list) => {
        expect(list[0].cell).toBe('new-cell');
      });
    });
  });

  describe('WHEN deleteInitiative is invoked', () => {
    it('THEN triggers delete on API and filters the local item', () => {
      const initialList = [{ initiative_id: 'i1', initiative_title: 'Title 1', cell: 'c1' }];
      mockApi.getInitiativesResult = of(initialList);
      service.loadInitiatives();

      mockApi.deleteInitiativeResult = of(undefined);
      service.deleteInitiative('i1');

      service.initiatives.subscribe((list) => {
        expect(list.length).toBe(0);
      });
    });
  });
});
