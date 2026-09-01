import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { DevopsAgentApiService } from '../devops-agent-api.service';
import { TasksStateService } from './tasks-state.service';
import { Message, SendMessageRequest, SendMessageResponse } from '../../models/devops-agent.model';
import {
  buildAuditPrompt,
  buildRefinementPrompt,
  REFINEMENT_GREETING,
  REFINEMENT_RESET_GREETING,
} from '../../domain';

@Injectable({
  providedIn: 'root',
})
export class RefinementChatStateService {
  public readonly messages: Observable<Message[]> = this.refinementMessages;
  private readonly api = inject(DevopsAgentApiService);
  private readonly tasksState = inject(TasksStateService);
  private refinementContextId = `refinement-${crypto.randomUUID()}`;
  private readonly refinementMessages$ = new BehaviorSubject<Message[]>([REFINEMENT_GREETING]);
  public readonly refinementMessages: Observable<Message[]> =
    this.refinementMessages$.asObservable();
  private readonly loading$ = new BehaviorSubject<boolean>(false);
  public readonly loading: Observable<boolean> = this.loading$.asObservable();

  public clearRefinementChat(): void {
    this.refinementContextId = `refinement-${crypto.randomUUID()}`;
    this.refinementMessages$.next([REFINEMENT_RESET_GREETING]);
  }

  public clearChat(): void {
    this.clearRefinementChat();
  }

  public sendRefinementMessage(text: string): void {
    if (!text.trim()) {
      return;
    }

    const userMsg: Message = {
      role: 'user',
      messageId: 'msg-' + Date.now(),
      contextId: this.refinementContextId,
      parts: [{ text }],
    };

    const currentMessages = this.refinementMessages$.value;
    this.refinementMessages$.next([...currentMessages, userMsg]);
    this.loading$.next(true);

    const payload: SendMessageRequest = {
      message: {
        role: 'user',
        messageId: userMsg.messageId || 'msg-' + Date.now(),
        contextId: this.refinementContextId,
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
            contextId: this.refinementContextId,
            parts: [{ text: replyText }],
          };
          this.refinementMessages$.next([...this.refinementMessages$.value, agentMsg]);
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
          this.refinementMessages$.next([...this.refinementMessages$.value, errorMsg]);
        },
      });
  }

  public sendMessage(text: string): void {
    this.sendRefinementMessage(text);
  }

  public refineStoryInChat(storyId: string, title: string): void {
    const prompt = buildRefinementPrompt(storyId, title);
    this.sendRefinementMessage(prompt);
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
    this.tasksState.triggerImmediatePoll();
    return this.api.sendMessage(payload);
  }
}
