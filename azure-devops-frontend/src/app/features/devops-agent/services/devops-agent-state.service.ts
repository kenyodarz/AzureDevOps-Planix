import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { DevopsAgentApiService } from './devops-agent-api.service';
import {
  AgentCard,
  AgentTask,
  DashboardData,
  Initiative,
  Message,
  SendMessageRequest,
} from '../models/devops-agent.model';

@Injectable({
  providedIn: 'root',
})
export class DevopsAgentStateService {
  private readonly api = inject(DevopsAgentApiService);

  private generalContextId = `general-${crypto.randomUUID()}`;
  private refinementContextId = `refinement-${crypto.randomUUID()}`;

  public readonly refinementMessages: Observable<Message[]> =
    this.refinementMessages$.asObservable();
  private readonly agentCard$ = new BehaviorSubject<AgentCard | null>(null);
  public readonly agentCard: Observable<AgentCard | null> = this.agentCard$.asObservable();
  private readonly loading$ = new BehaviorSubject<boolean>(false);
  public readonly loading: Observable<boolean> = this.loading$.asObservable();
  private readonly uploading$ = new BehaviorSubject<boolean>(false);
  public readonly uploading: Observable<boolean> = this.uploading$.asObservable();
  private readonly uploadStatus$ = new BehaviorSubject<string | null>(null);
  public readonly uploadStatus: Observable<string | null> = this.uploadStatus$.asObservable();
  private readonly initiatives$ = new BehaviorSubject<Initiative[]>([]);
  public readonly initiatives: Observable<Initiative[]> = this.initiatives$.asObservable();
  private readonly dashboardData$ = new BehaviorSubject<DashboardData | null>(null);
  public readonly dashboardData: Observable<DashboardData | null> =
    this.dashboardData$.asObservable();
  private readonly dashboardError$ = new BehaviorSubject<string | null>(null);
  private readonly tasks$ = new BehaviorSubject<AgentTask[]>([]);
  private pollingInterval = 30000;
  private pollingTimer: ReturnType<typeof setTimeout> | null = null;
  // Estos dos Subject y los Observables que los exponen dependen de `generalGreeting` e
  // `initialGreeting`, así que se declaran DESPUÉS de ellos. Los campos de una clase se inicializan
  // en orden de declaración: usar uno antes de declararlo no es un aviso de estilo, lo deja en
  private readonly generalGreeting: Message = {
    role: 'agent',
    parts: [
      {
        text: `### ¡Hola! Soy tu asistente y Scrum Master virtual de Bancolombia. 🤖

Estoy aquí para ayudarte en el **Chat General**. Aquí puedes hacer consultas libres, pedir reportes, listados de DevOps, soporte general y más.`,
      },
    ],
  };
  private readonly refinementMessages$ = new BehaviorSubject<Message[]>([this.initialGreeting]);
  private readonly initialGreeting: Message = {
    role: 'agent',
    parts: [
      {
        text: `### ¡Hola! Soy tu asistente y Scrum Master virtual de Bancolombia. 🤖

Estoy aquí para ayudarte a redactar e instanciar tus **Historias de Usuario (HU)** o **Historias Habilitadoras (HA)** en Azure DevOps, siguiendo estrictamente la plantilla corporativa.

---

### 📝 ¿Cómo enviarme tu idea?
Puedes escribir una descripción breve, pero obtendrás un resultado ideal si me proporcionas una estructura clara.

**Ejemplo de mensaje perfecto:**
* **Título**: Carga masiva de aprobadores
* **Equipo**: Canales Digitales - Célula Core
* **Descripción**: Crear la función de carga en batch (.csv) para poblar la tabla de aprobadores del sistema de control de accesos (MCP). Esto incluye el CRUD completo para gestionar los registros individuales desde el panel de administración, asegurando que solo usuarios con rol de SuperAdmin puedan operarlo.
* **Criterios de Aceptación**: Debe validar que los campos requeridos no estén vacíos, que el formato de correo sea válido y que el proceso se ejecute de forma asíncrona informando el resultado al finalizar.

---

### 📘 ¿Cómo funciona la sección "Cargar Planeación"?
En el panel lateral izquierdo tienes la opción de **Cargar Planeación**. Aquí puedes subir archivos de planeación en formato Markdown (\`.md\`).

**¿Cómo ayuda esto al proceso?**
1. **Contexto Semántico**: Al subir un documento (como la planeación de un Q o los lineamientos de arquitectura), el contenido se procesa y se almacena en nuestra **base de datos vectorial**.
2. **Generación Alineada**: Cuando me pidas redactar una HU o HA, realizaré una **búsqueda semántica** automática en ese archivo cargado. De este modo, la historia generada adoptará automáticamente los detalles de negocio, restricciones técnicas, criterios técnicos u objetivos previamente acordados en tu planeación.
3. **Menos esfuerzo**: No necesitas redactar todo desde cero ni copiar y pegar extensos documentos en el chat; el agente recuperará la información relevante por ti.
`,
      },
    ],
  };
  // `undefined` y el `.asObservable()` revienta al construir el servicio (TS2729).
  private readonly generalMessages$ = new BehaviorSubject<Message[]>([this.generalGreeting]);
  public readonly messages: Observable<Message[]> = this.refinementMessages; // Por compatibilidad con tests antiguos
  public readonly generalMessages: Observable<Message[]> = this.generalMessages$.asObservable();
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
    const resetGreeting: Message = {
      role: 'agent',
      parts: [
        {
          text: 'Chat general limpio. Entrégame una consulta libre, listado de DevOps o reporte para comenzar.',
        },
      ],
    };
    this.generalMessages$.next([resetGreeting]);
  }

  public triggerImmediatePoll(): void {
    this.pollingInterval = 5000;
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
    const resetGreeting: Message = {
      role: 'agent',
      parts: [
        {
          text: 'Chat limpio (Asistente de Refinamiento). Entrégame una nueva idea de Historia de Usuario o Historia Habilitadora para comenzar.',
        },
      ],
    };
    this.refinementMessages$.next([resetGreeting]);
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
    const prompt = `Asistente, quiero que analicemos y refinemos la Historia de Usuario: "${title}" (ID: ${storyId}). Ayúdame a revisar sus criterios de aceptación y calidad de documentación.`;
    this.sendRefinementMessage(prompt);
  }

  public clearUploadStatus(): void {
    this.uploadStatus$.next(null);
  }

  public auditStory(id: string): Observable<any> {
    const contextId = `audit-story-${id}`;
    const payload: SendMessageRequest = {
      message: {
        role: 'user',
        messageId: 'msg-audit-' + Date.now(),
        contextId,
        parts: [{ text: `audita la calidad de (ID:${id})` }],
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
          this.pollingInterval = 30000;
          this.scheduleNextPoll();
        },
      });
    }, this.pollingInterval);
  }

  private adjustPollingInterval(tasks: AgentTask[]): void {
    const hasActiveTasks = tasks.some(
      (task) => task.status?.state === 'submitted' || task.status?.state === 'working',
    );
    this.pollingInterval = hasActiveTasks ? 5000 : 30000;
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

  public getInitiativeChunks(id: string): Observable<any[]> {
    return this.api.getInitiativeChunks(id);
  }
}
