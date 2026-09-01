import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { DevopsAgentApiService } from './devops-agent-api.service';
import { POLLING_INTERVAL_ACTIVE_MS, POLLING_INTERVAL_IDLE_MS } from '../../../core';
import {
  AgentCard,
  AgentTask,
  DashboardData,
  Initiative,
  Message,
  PlanningChunk,
  SendMessageRequest,
  SendMessageResponse,
} from '../models/devops-agent.model';
import {
  buildAuditPrompt,
  buildRefinementPrompt,
  GENERAL_GREETING,
  GENERAL_RESET_GREETING,
  REFINEMENT_GREETING,
  REFINEMENT_RESET_GREETING,
} from '../domain';

@Injectable({
  providedIn: 'root',
})
export class DevopsAgentStateService {
  private readonly api = inject(DevopsAgentApiService);

  private generalContextId = `general-${crypto.randomUUID()}`;
  private refinementContextId = `refinement-${crypto.randomUUID()}`;

  private pollingTimer: ReturnType<typeof setTimeout> | null = null;

  // ---------------------------------------------------------------------------------------------
  // BLOQUE 1 — Configuración e intervalos.
  //
  // Tras la externalización a la capa domain (Fase 03), los saludos residen en constantes
  // inmutables fuera de la clase. Se conserva la estructura de bloques para respetar el orden
  // de inicialización de TypeScript y evitar TS2729.
  private pollingInterval = POLLING_INTERVAL_IDLE_MS;

  // ---------------------------------------------------------------------------------------------
  // BLOQUE 2 — Subjects privados. Consumen las constantes importadas del dominio.
  // ---------------------------------------------------------------------------------------------
  private readonly generalMessages$ = new BehaviorSubject<Message[]>([GENERAL_GREETING]);
  // BLOQUE 3 — Observables públicos. Consumen los Subjects del bloque 2, por eso van los últimos.
  public readonly generalMessages: Observable<Message[]> = this.generalMessages$.asObservable();
  private readonly refinementMessages$ = new BehaviorSubject<Message[]>([REFINEMENT_GREETING]);
  private readonly agentCard$ = new BehaviorSubject<AgentCard | null>(null);
  private readonly loading$ = new BehaviorSubject<boolean>(false);

  // ---------------------------------------------------------------------------------------------
  private readonly uploading$ = new BehaviorSubject<boolean>(false);
  public readonly refinementMessages: Observable<Message[]> =
    this.refinementMessages$.asObservable();
  public readonly agentCard: Observable<AgentCard | null> = this.agentCard$.asObservable();
  public readonly loading: Observable<boolean> = this.loading$.asObservable();
  public readonly uploading: Observable<boolean> = this.uploading$.asObservable();
  private readonly uploadStatus$ = new BehaviorSubject<string | null>(null);
  public readonly uploadStatus: Observable<string | null> = this.uploadStatus$.asObservable();
  public readonly messages: Observable<Message[]> = this.refinementMessages; // Por compatibilidad con tests antiguos
  private readonly initiatives$ = new BehaviorSubject<Initiative[]>([]);
  public readonly initiatives: Observable<Initiative[]> = this.initiatives$.asObservable();
  private readonly dashboardData$ = new BehaviorSubject<DashboardData | null>(null);
  public readonly dashboardData: Observable<DashboardData | null> =
    this.dashboardData$.asObservable();
  private readonly dashboardError$ = new BehaviorSubject<string | null>(null);
  private readonly tasks$ = new BehaviorSubject<AgentTask[]>([]);
  public readonly dashboardError: Observable<string | null> = this.dashboardError$.asObservable();
  public readonly tasks: Observable<AgentTask[]> = this.tasks$.asObservable();

  constructor() {
    this.loadAgentCard();
    this.startDynamicPolling();
  }

  private startDynamicPolling(): void {
    this.loadTasks();
    this.scheduleNextPoll();
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
    this.generalContextId = `general-${crypto.randomUUID()}`;
    this.generalMessages$.next([GENERAL_RESET_GREETING]);
  }

  public triggerImmediatePoll(): void {
    this.pollingInterval = POLLING_INTERVAL_ACTIVE_MS;
    this.startDynamicPolling();
  }

  public uploadPlanning(initiativeId: string, title: string, content: string): void {
    this.uploading$.next(true);
    this.uploadStatus$.next('Vectorizando planeación...');

    this.api
      .uploadPlanning({ initiativeId, title, markdownContent: content })
      .pipe(finalize(() => this.uploading$.next(false)))
      .subscribe({
        next: () => {
          this.uploadStatus$.next('¡Planeación indexada con éxito!');
          // Recargar iniciativas para que aparezca la nueva en la lista de gestión
          this.loadInitiatives();
        },
        error: (error) => {
          console.error(error);
          const errText = error.error || error.message || 'Error desconocido';
          this.uploadStatus$.next(`Error: ${errText}`);
        },
      });
  }

  public sendGeneralMessage(text: string): void {
    this.sendChatMessage(text, this.generalContextId, this.generalMessages$);
  }

  public sendRefinementMessage(text: string): void {
    this.sendChatMessage(text, this.refinementContextId, this.refinementMessages$);
  }

  public sendMessage(text: string): void {
    this.sendRefinementMessage(text);
  }

  public loadInitiatives(): void {
    this.loading$.next(true);
    this.api
      .getInitiatives()
      .pipe(finalize(() => this.loading$.next(false)))
      .subscribe({
        next: (initiatives) => {
          this.initiatives$.next(initiatives);
        },
        error: (error) => {
          console.error('Error al cargar iniciativas', error);
        },
      });
  }

  public updateInitiativeCell(id: string, cell: string): void {
    this.loading$.next(true);
    this.api
      .updateInitiativeCell(id, cell)
      .pipe(finalize(() => this.loading$.next(false)))
      .subscribe({
        next: () => {
          const updated = this.initiatives$.value.map((init) =>
            init.initiative_id === id ? { ...init, cell } : init,
          );
          this.initiatives$.next(updated);
        },
        error: (error) => {
          console.error(`Error al actualizar la célula de la iniciativa ${id}`, error);
        },
      });
  }

  public deleteInitiative(id: string): void {
    this.loading$.next(true);
    this.api
      .deleteInitiative(id)
      .pipe(finalize(() => this.loading$.next(false)))
      .subscribe({
        next: () => {
          const filtered = this.initiatives$.value.filter((init) => init.initiative_id !== id);
          this.initiatives$.next(filtered);
        },
        error: (error) => {
          console.error(`Error al eliminar la iniciativa ${id}`, error);
        },
      });
  }

  public clearRefinementChat(): void {
    this.refinementContextId = `refinement-${crypto.randomUUID()}`;
    this.refinementMessages$.next([REFINEMENT_RESET_GREETING]);
  }

  public loadDashboardData(cell: string, sprint: string): void {
    if (!cell || !sprint) {
      console.warn('Célula y Sprint son requeridos para cargar el dashboard.');
      return;
    }
    this.loading$.next(true);
    this.dashboardError$.next(null);

    this.api.getDashboardDataStream(cell, sprint).subscribe({
      next: (event) => {
        if (event.event === 'INITIAL') {
          this.loading$.next(false);
        }
        if (event.data) {
          this.dashboardData$.next(event.data);
        }
      },
      // El evento `ERROR` del stream llega por aquí convertido en un error del observable (D-40):
      // el mensaje que compuso el backend es el que ve el usuario, en vez de un fallo mudo.
      error: (error) => {
        console.error('Error al cargar datos del dashboard por SSE', error);
        this.loading$.next(false);
        this.dashboardData$.next(null);
        const errText =
          error.error || error.message || 'Fallo en la comunicación con el agente o MCP.';
        this.dashboardError$.next(errText);
      },
      complete: () => {
        this.loading$.next(false);
      },
    });
  }

  public loadTasks(): void {
    this.api.getTasks().subscribe({
      next: (tasks) => {
        this.tasks$.next(tasks || []);
      },
      error: (error) => {
        console.error('Error al cargar tareas', error);
      },
    });
  }

  public clearChat(): void {
    this.clearRefinementChat();
  }

  public refineStoryInChat(storyId: string, title: string): void {
    const prompt = buildRefinementPrompt(storyId, title);
    this.sendRefinementMessage(prompt);
  }

  public clearUploadStatus(): void {
    this.uploadStatus$.next(null);
  }

  public auditStory(id: string): Observable<SendMessageResponse> {
    const contextId = `audit-story-${id}`;
    const payload: SendMessageRequest = {
      message: {
        role: 'user',
        messageId: 'msg-audit-' + Date.now(),
        contextId,
        parts: [{ text: buildAuditPrompt(id) }],
      },
    };
    this.triggerImmediatePoll();
    return this.api.sendMessage(payload);
  }

  public cancelTask(id: string): void {
    this.api.cancelTask(id).subscribe({
      next: () => {
        this.loadTasks();
      },
      error: (error) => {
        console.error(`Error al cancelar tarea ${id}`, error);
      },
    });
  }

  public getInitiativeChunks(id: string): Observable<PlanningChunk[]> {
    return this.api.getInitiativeChunks(id);
  }

  private scheduleNextPoll(): void {
    if (this.pollingTimer) {
      clearTimeout(this.pollingTimer);
    }

    this.pollingTimer = setTimeout(() => {
      this.api.getTasks().subscribe({
        next: (tasks) => {
          this.tasks$.next(tasks || []);
          this.adjustPollingInterval(tasks || []);
          this.scheduleNextPoll();
        },
        error: (error) => {
          console.error('Error al cargar tareas', error);
          this.pollingInterval = POLLING_INTERVAL_IDLE_MS;
          this.scheduleNextPoll();
        },
      });
    }, this.pollingInterval);
  }

  private sendChatMessage(
    text: string,
    contextId: string,
    subject$: BehaviorSubject<Message[]>,
  ): void {
    if (!text.trim()) {
      return;
    }

    const userMsg: Message = {
      role: 'user',
      messageId: 'msg-' + Date.now(),
      contextId,
      parts: [{ text }],
    };

    const currentMessages = subject$.value;
    subject$.next([...currentMessages, userMsg]);
    this.loading$.next(true);

    const payload: SendMessageRequest = {
      message: {
        role: 'user',
        messageId: userMsg.messageId || 'msg-' + Date.now(),
        contextId,
        parts: [{ text }],
      },
    };

    this.api
      .sendMessage(payload)
      .pipe(finalize(() => this.loading$.next(false)))
      .subscribe({
        next: (response) => {
          const replyText = response.message?.parts?.[0]?.text || 'No obtuve respuesta del modelo.';
          const agentMsg: Message = {
            role: 'agent',
            messageId: response.message?.messageId || 'msg-reply-' + Date.now(),
            contextId,
            parts: [{ text: replyText }],
          };
          subject$.next([...subject$.value, agentMsg]);
        },
        error: (error) => {
          console.error(error);
          const errorMsg: Message = {
            role: 'agent',
            parts: [
              {
                text: `❌ Error de red: No se pudo conectar con el agente. Asegúrate de que corre en el puerto 8081.`,
              },
            ],
          };
          subject$.next([...subject$.value, errorMsg]);
        },
      });
  }

  private adjustPollingInterval(tasks: AgentTask[]): void {
    const hasActiveTasks = tasks.some(
      (task) => task.status?.state === 'submitted' || task.status?.state === 'working',
    );
    this.pollingInterval = hasActiveTasks ? POLLING_INTERVAL_ACTIVE_MS : POLLING_INTERVAL_IDLE_MS;
  }
}
