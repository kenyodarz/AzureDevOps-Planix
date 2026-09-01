import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, combineLatest, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { DevopsAgentApiService } from './devops-agent-api.service';
import {
  DashboardStateService,
  GeneralChatStateService,
  PlanningStateService,
  RefinementChatStateService,
  TasksStateService,
} from './state';
import {
  AgentCard,
  AgentTask,
  DashboardData,
  Initiative,
  Message,
  PlanningChunk,
  SendMessageResponse,
} from '../models/devops-agent.model';

@Injectable({
  providedIn: 'root',
})
export class DevopsAgentStateService {
  private readonly api = inject(DevopsAgentApiService);
  private readonly generalChatState = inject(GeneralChatStateService);
  public readonly generalMessages: Observable<Message[]> = this.generalChatState.generalMessages;
  private readonly refinementChatState = inject(RefinementChatStateService);
  public readonly refinementMessages: Observable<Message[]> =
    this.refinementChatState.refinementMessages;
  public readonly messages: Observable<Message[]> = this.refinementChatState.messages;

  private readonly agentCard$ = new BehaviorSubject<AgentCard | null>(null);
  public readonly agentCard: Observable<AgentCard | null> = this.agentCard$.asObservable();
  private readonly planningState = inject(PlanningStateService);
  public readonly uploading: Observable<boolean> = this.planningState.uploading;
  public readonly uploadStatus: Observable<string | null> = this.planningState.uploadStatus;
  public readonly initiatives: Observable<Initiative[]> = this.planningState.initiatives;
  private readonly dashboardState = inject(DashboardStateService);
  public readonly loading: Observable<boolean> = combineLatest([
    this.generalChatState.loading,
    this.refinementChatState.loading,
    this.planningState.loading,
    this.dashboardState.loading,
  ]).pipe(map(([g, r, p, d]) => g || r || p || d));
  public readonly dashboardData: Observable<DashboardData | null> =
    this.dashboardState.dashboardData;
  public readonly dashboardError: Observable<string | null> = this.dashboardState.dashboardError;
  private readonly tasksState = inject(TasksStateService);
  public readonly tasks: Observable<AgentTask[]> = this.tasksState.tasks;

  constructor() {
    this.loadAgentCard();
  }

  public loadAgentCard(): void {
    this.api.getAgentCard().subscribe({
      next: (card) => {
        this.agentCard$.next(card);
      },
      error: (error) => {
        console.error('No se pudo obtener la tarjeta del agente', error);
        this.agentCard$.next({
          name: 'Agente Local',
          version: '1.0.0',
          description: 'Conectado a la API local de simulación.',
        });
      },
    });
  }

  public clearGeneralChat(): void {
    this.generalChatState.clearGeneralChat();
  }

  public triggerImmediatePoll(): void {
    this.tasksState.triggerImmediatePoll();
  }

  public uploadPlanning(initiativeId: string, title: string, content: string): void {
    this.planningState.uploadPlanning(initiativeId, title, content);
  }

  public sendGeneralMessage(text: string): void {
    this.generalChatState.sendGeneralMessage(text);
  }

  public sendRefinementMessage(text: string): void {
    this.refinementChatState.sendRefinementMessage(text);
  }

  public sendMessage(text: string): void {
    this.refinementChatState.sendMessage(text);
  }

  public loadInitiatives(): void {
    this.planningState.loadInitiatives();
  }

  public updateInitiativeCell(id: string, cell: string): void {
    this.planningState.updateInitiativeCell(id, cell);
  }

  public deleteInitiative(id: string): void {
    this.planningState.deleteInitiative(id);
  }

  public clearRefinementChat(): void {
    this.refinementChatState.clearRefinementChat();
  }

  public loadDashboardData(cell: string, sprint: string): void {
    this.dashboardState.loadDashboardData(cell, sprint);
  }

  public loadTasks(): void {
    this.tasksState.loadTasks();
  }

  public clearChat(): void {
    this.refinementChatState.clearChat();
  }

  public refineStoryInChat(storyId: string, title: string): void {
    this.refinementChatState.refineStoryInChat(storyId, title);
  }

  public clearUploadStatus(): void {
    this.planningState.clearUploadStatus();
  }

  public auditStory(id: string): Observable<SendMessageResponse> {
    return this.refinementChatState.auditStory(id);
  }

  public cancelTask(id: string): void {
    this.tasksState.cancelTask(id);
  }

  public getInitiativeChunks(id: string): Observable<PlanningChunk[]> {
    return this.planningState.getInitiativeChunks(id);
  }
}

