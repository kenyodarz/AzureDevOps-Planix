import {Component, EventEmitter, Input, Output} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';

@Component({
  selector: 'app-chat-input',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <!-- Sugerencias rápidas -->
    <div class="flex gap-3 p-3 px-8 overflow-x-auto w-full max-w-[1000px] mx-auto min-w-0">
      @for (sug of suggestions; track sug.label) {
        <button
          [disabled]="loading"
          class="bg-[rgba(255,255,255,0.03)] border border-[rgba(255,255,255,0.08)] text-[#9ca3af] px-4 py-2 rounded-full text-[0.8rem] cursor-pointer whitespace-nowrap hover:bg-[rgba(242,201,76,0.1)] hover:border-[#f2c94c] hover:text-[#f3f4f6] transition-all disabled:opacity-50"
          (click)="sendSuggestion(sug.value)"
        >
          {{ sug.label }}
        </button>
      }
    </div>

    <!-- Input Box -->
    <div class="p-8 pt-0 w-full max-w-[1000px] mx-auto min-w-0">
      <div class="bg-[rgba(17,24,39,0.7)] border border-[rgba(255,255,255,0.08)] rounded-2xl p-2 px-4 flex items-center gap-3 backdrop-blur-md shadow-2xl focus-within:border-[#f2c94c] transition-colors">
        <textarea
          aria-label="Responder o escribir idea"
          class="flex-1 bg-transparent border-none text-[#f3f4f6] text-[0.95rem] py-3 outline-none resize-none h-[48px] max-h-[150px] overflow-y-auto"
          placeholder="Escribe tu idea o responde aquí..."
          [(ngModel)]="messageText"
          (keydown)="onKeyDown($event)"
          (input)="onInput($event)"
          #textarea
        ></textarea>
        <button
          [disabled]="loading || !messageText.trim()"
          class="bg-[#f2c94c] text-[#000] border-none px-6 py-3 rounded-lg font-semibold text-[0.9rem] cursor-pointer hover:bg-[#e0b83b] transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          (click)="submitMessage()"
        >
          Enviar
        </button>
      </div>
    </div>
  `
})
export class ChatInputComponent {
  @Input() loading = false;
  @Output() sendMessage = new EventEmitter<string>();

  protected messageText = '';

  protected readonly suggestions = [
    {
      label: '💡 CRUD Aprobadores (Idea corta)',
      value: 'AI Admin panel - Crear la funcion de carga en batch para poblar la tabla de aprovadores de MCP y todo el tema de CRUD'
    },
    { label: '✅ Aprobado', value: 'Aprobado' },
    { label: '🚀 Crear', value: 'Crear' }
  ];

  protected onInput(event: Event): void {
    const textarea = event.target as HTMLTextAreaElement;
    textarea.style.height = 'auto';
    textarea.style.height = Math.min(textarea.scrollHeight, 150) + 'px';
  }

  protected onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.submitMessage();
    }
  }

  protected submitMessage(): void {
    const text = this.messageText.trim();
    if (text && !this.loading) {
      this.sendMessage.emit(text);
      this.messageText = '';

      // Reset height
      setTimeout(() => {
        const textareas = document.getElementsByTagName('textarea');
        if (textareas.length > 0) {
          textareas[0].style.height = '48px';
        }
      }, 0);
    }
  }

  protected sendSuggestion(text: string): void {
    if (this.loading) {
      return;
    }
    this.messageText = text;
    this.submitMessage();
  }
}
