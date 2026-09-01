import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AgentCard,
  AgentTask,
  CancelTaskResponse,
  DashboardData,
  DashboardStreamEvent,
  IngestPayload,
  IngestResponse,
  Initiative,
  PlanningChunk,
  SendMessageRequest,
  SendMessageResponse,
} from '../models/devops-agent.model';
import {
  AgentChatApiService,
  DashboardApiService,
  PlanningApiService,
  TasksApiService,
} from './api';

@Injectable({
  providedIn: 'root',
})
export class DevopsAgentApiService {
  private readonly agentChatApi = inject(AgentChatApiService);
  private readonly planningApi = inject(PlanningApiService);
  private readonly dashboardApi = inject(DashboardApiService);
  private readonly tasksApi = inject(TasksApiService);

  getAgentCard(): Observable<AgentCard> {
    return this.agentChatApi.getAgentCard();
  }

  sendMessage(payload: SendMessageRequest): Observable<SendMessageResponse> {
    return this.agentChatApi.sendMessage(payload);
  }

  uploadPlanning(payload: IngestPayload): Observable<IngestResponse> {
    return this.planningApi.uploadPlanning(payload);
  }

  getInitiatives(): Observable<Initiative[]> {
    return this.planningApi.getInitiatives();
  }

  deleteInitiative(id: string): Observable<void> {
    return this.planningApi.deleteInitiative(id);
  }

  updateInitiativeCell(id: string, cell: string): Observable<void> {
    return this.planningApi.updateInitiativeCell(id, cell);
  }

  getDashboardData(cell: string, sprint: string): Observable<DashboardData> {
    return this.dashboardApi.getDashboardData(cell, sprint);
  }

  getDashboardDataStream(cell: string, sprint: string): Observable<DashboardStreamEvent> {
    return this.dashboardApi.getDashboardDataStream(cell, sprint);
  }

  getInitiativeChunks(id: string): Observable<PlanningChunk[]> {
    return this.planningApi.getInitiativeChunks(id);
  }

  getTasks(): Observable<AgentTask[]> {
    return this.tasksApi.getTasks();
  }

  cancelTask(id: string): Observable<CancelTaskResponse> {
    return this.tasksApi.cancelTask(id);
  }
}


