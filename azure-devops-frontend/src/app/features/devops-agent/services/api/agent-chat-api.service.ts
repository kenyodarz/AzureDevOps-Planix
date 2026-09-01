import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_ENDPOINTS } from '../../../../core';
import {
  AgentCard,
  SendMessageRequest,
  SendMessageResponse,
} from '../../models/devops-agent.model';

@Injectable({
  providedIn: 'root',
})
export class AgentChatApiService {
  private readonly http = inject(HttpClient);

  getAgentCard(): Observable<AgentCard> {
    return this.http.get<AgentCard>(API_ENDPOINTS.AGENT_CARD);
  }

  sendMessage(payload: SendMessageRequest): Observable<SendMessageResponse> {
    return this.http.post<SendMessageResponse>(API_ENDPOINTS.MESSAGE_SEND, payload);
  }
}
