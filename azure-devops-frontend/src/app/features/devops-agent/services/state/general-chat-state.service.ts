import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { DevopsAgentApiService } from '../devops-agent-api.service';
import { Message, SendMessageRequest } from '../../models/devops-agent.model';
import { GENERAL_GREETING, GENERAL_RESET_GREETING } from '../../domain';

@Injectable({
  providedIn: 'root',
})
export class GeneralChatStateService {
  private readonly api = inject(DevopsAgentApiService);

  private generalContextId = `general-${crypto.randomUUID()}`;

  private readonly generalMessages$ = new BehaviorSubject<Message[]>([GENERAL_GREETING]);
  public readonly generalMessages: Observable<Message[]> = this.generalMessages$.asObservable();

  private readonly loading$ = new BehaviorSubject<boolean>(false);
  public readonly loading: Observable<boolean> = this.loading$.asObservable();

  public clearGeneralChat(): void {
    this.generalContextId = `general-${crypto.randomUUID()}`;
    this.generalMessages$.next([GENERAL_RESET_GREETING]);
  }

  public sendGeneralMessage(text: string): void {
    if (!text.trim()) {
      return;
    }

    const userMsg: Message = {
      role: 'user',
      messageId: 'msg-' + Date.now(),
      contextId: this.generalContextId,
      parts: [{ text }],
    };

    const currentMessages = this.generalMessages$.value;
    this.generalMessages$.next([...currentMessages, userMsg]);
    this.loading$.next(true);

    const payload: SendMessageRequest = {
      message: {
        role: 'user',
        messageId: userMsg.messageId || 'msg-' + Date.now(),
        contextId: this.generalContextId,
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
            contextId: this.generalContextId,
            parts: [{ text: replyText }],
          };
          this.generalMessages$.next([...this.generalMessages$.value, agentMsg]);
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
          this.generalMessages$.next([...this.generalMessages$.value, errorMsg]);
        },
      });
  }
}
