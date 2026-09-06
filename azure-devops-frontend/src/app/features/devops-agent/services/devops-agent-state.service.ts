import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, combineLatest, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { NotificationService } from '../../../core';
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
  ProgramPlanRequestDTO,
  ProgramPlanResponseDTO,
  SendMessageResponse,
  SpecDocumentDTO,
} from '../models/devops-agent.model';

@Injectable({ providedIn: 'root' })
export class DevopsAgentStateService {
  private readonly api = inject(DevopsAgentApiService);
  private readonly notifications = inject(NotificationService);
  private readonly general = inject(GeneralChatStateService);
  public readonly generalMessages: Observable<Message[]> = this.general.generalMessages;
  private readonly refinement = inject(RefinementChatStateService);
  public readonly refinementMessages: Observable<Message[]> = this.refinement.refinementMessages;
  public readonly messages: Observable<Message[]> = this.refinement.messages;
  private readonly agentCard$ = new BehaviorSubject<AgentCard | null>(null);

  public readonly agentCard: Observable<AgentCard | null> = this.agentCard$.asObservable();
  private readonly planning = inject(PlanningStateService);
  public readonly uploading: Observable<boolean> = this.planning.uploading;
  public readonly uploadStatus: Observable<string | null> = this.planning.uploadStatus;
  public readonly initiatives: Observable<Initiative[]> = this.planning.initiatives;
  public readonly availableSpecs: Observable<string[]> = this.planning.availableSpecs;
  public readonly selectedSpec: Observable<SpecDocumentDTO | null> = this.planning.selectedSpec;
  public readonly loadingSpecs: Observable<boolean> = this.planning.loadingSpecs;
  public readonly planningRunning: Observable<boolean> = this.planning.planningRunning;
  private readonly dashboard = inject(DashboardStateService);
  public readonly dashboardData: Observable<DashboardData | null> = this.dashboard.dashboardData;
  public readonly dashboardError: Observable<string | null> = this.dashboard.dashboardError;
  public readonly loading: Observable<boolean> = combineLatest([
    this.general.loading,
    this.refinement.loading,
    this.planning.loading,
    this.dashboard.loading,
  ]).pipe(map(([g, r, p, d]) => g || r || p || d));
  private readonly tasksState = inject(TasksStateService);
  public readonly tasks: Observable<AgentTask[]> = this.tasksState.tasks;

  constructor() {
    this.loadAgentCard();
  }

  public loadAgentCard(): void {
    this.api.getAgentCard().subscribe({
      next: (card) => this.agentCard$.next(card),
      error: () => {
        this.notifications.error('No se pudo obtener la tarjeta del agente');
        this.agentCard$.next({
          name: 'Agente Local',
          version: '1.0.0',
          description: 'Conectado a la API local de simulación.',
        });
      },
    });
  }

  public clearGeneralChat(): void {
    this.general.clearGeneralChat();
  }
  public triggerImmediatePoll(): void {
    this.tasksState.triggerImmediatePoll();
  }
  public uploadPlanning(id: string, title: string, content: string): void {
    this.planning.uploadPlanning(id, title, content);
  }
  public sendGeneralMessage(text: string): void {
    this.general.sendGeneralMessage(text);
  }
  public sendRefinementMessage(text: string): void {
    this.refinement.sendRefinementMessage(text);
  }
  public sendMessage(text: string): void {
    this.refinement.sendMessage(text);
  }
  public loadInitiatives(): void {
    this.planning.loadInitiatives();
  }
  public updateInitiativeCell(id: string, cell: string): void {
    this.planning.updateInitiativeCell(id, cell);
  }
  public deleteInitiative(id: string): void {
    this.planning.deleteInitiative(id);
  }
  public clearRefinementChat(): void {
    this.refinement.clearRefinementChat();
  }
  public loadDashboardData(cell: string, sprint: string): void {
    this.dashboard.loadDashboardData(cell, sprint);
  }
  public loadTasks(): void {
    this.tasksState.loadTasks();
  }
  public clearChat(): void {
    this.refinement.clearChat();
  }
  public refineStoryInChat(id: string, title: string): void {
    this.refinement.refineStoryInChat(id, title);
  }
  public clearUploadStatus(): void {
    this.planning.clearUploadStatus();
  }
  public auditStory(id: string): Observable<SendMessageResponse> {
    return this.refinement.auditStory(id);
  }
  public cancelTask(id: string): void {
    this.tasksState.cancelTask(id);
  }
  public getInitiativeChunks(id: string): Observable<PlanningChunk[]> {
    return this.planning.getInitiativeChunks(id);
  }
  public loadAvailableSpecs(): void {
    this.planning.loadAvailableSpecs();
  }
  public selectSpec(name: string): void {
    this.planning.selectSpec(name);
  }
  public clearSelectedSpec(): void {
    this.planning.clearSelectedSpec();
  }
  public triggerProgramPlanning(
    request: ProgramPlanRequestDTO,
  ): Observable<ProgramPlanResponseDTO> {
    return this.planning.triggerProgramPlanning(request);
  }
}

