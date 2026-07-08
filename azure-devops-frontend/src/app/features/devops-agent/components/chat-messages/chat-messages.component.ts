import { Component, ElementRef, Input, OnChanges, SimpleChanges, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Message } from '../../models/devops-agent.model';
import { MarkdownParserPipe } from '../../../../shared/pipes/markdown-parser.pipe';

@Component({
  selector: 'app-chat-messages',
  standalone: true,
  imports: [CommonModule, MarkdownParserPipe],
  template: `
    <div #chatBox class="flex-1 p-8 overflow-y-auto flex flex-col gap-5 w-full max-w-[1000px] mx-auto min-w-0">
      @for (msg of messages; track msg.messageId || $index) {
        <div class="flex w-full animate-fadeIn" [class.justify-end]="msg.role === 'user'" [class.justify-start]="msg.role === 'agent'">
          <div 
            [class.bg-[#1f2937]]="msg.role === 'user'"
            [class.border]="msg.role === 'user'"
            [class.border-[rgba(255,255,255,0.08)]]="msg.role === 'user'"
            [class.rounded-br-sm]="msg.role === 'user'"
            [class.bg-[rgba(242,201,76,0.08)]]="msg.role === 'agent'"
            [class.border-[#f2c94c33]]="msg.role === 'agent'"
            [class.border]="msg.role === 'agent'"
            [class.rounded-bl-sm]="msg.role === 'agent'"
            class="max-w-[90%] p-4 px-5 rounded-2xl text-[0.95rem] leading-relaxed relative word-break overflow-wrap text-[#f3f4f6]"
          >
            @for (part of msg.parts; track $index) {
              <div [innerHTML]="part.text | markdownParser" class="message-part-container"></div>
            }
          </div>
        </div>
      }
      
      @if (loading) {
        <div class="flex w-full justify-start">
          <div class="bg-[rgba(242,201,76,0.08)] border border-[#f2c94c33] rounded-2xl rounded-bl-sm max-w-[90%] p-4 px-5">
            <div class="flex gap-[5px] p-2 items-center">
              <div class="w-[7px] h-[7px] bg-[#9ca3af] rounded-full animate-bounce delay-1"></div>
              <div class="w-[7px] h-[7px] bg-[#9ca3af] rounded-full animate-bounce delay-2"></div>
              <div class="w-[7px] h-[7px] bg-[#9ca3af] rounded-full animate-bounce delay-3"></div>
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(8px); }
      to { opacity: 1; transform: translateY(0); }
    }
    .animate-fadeIn {
      animation: fadeIn 0.3s ease;
    }
    .word-break { word-break: break-word; }
    .overflow-wrap { overflow-wrap: break-word; }
    
    /* Custom Bounce Delays */
    .delay-1 { animation: bounce 1.4s infinite ease-in-out both; animation-delay: -0.32s; }
    .delay-2 { animation: bounce 1.4s infinite ease-in-out both; animation-delay: -0.16s; }
    .delay-3 { animation: bounce 1.4s infinite ease-in-out both; }
    
    @keyframes bounce {
      0%, 80%, 100% { transform: scale(0); }
      40% { transform: scale(1.0); }
    }

    ::ng-deep .message-part-container p {
      margin-bottom: 8px;
    }
    ::ng-deep .message-part-container p:last-child {
      margin-bottom: 0;
    }
    ::ng-deep .message-part-container pre {
      background: rgba(0, 0, 0, 0.25);
      padding: 14px 18px;
      border-radius: 8px;
      overflow-x: auto;
      font-family: monospace;
      font-size: 0.85rem;
      margin: 12px 0;
      border: 1px solid rgba(255, 255, 255, 0.05);
      max-width: 100%;
      white-space: pre;
    }
    ::ng-deep .message-part-container code {
      font-family: monospace;
      font-size: 0.85rem;
      background: rgba(255, 255, 255, 0.05);
      padding: 2px 4px;
      border-radius: 4px;
    }
    ::ng-deep .message-part-container ul, ::ng-deep .message-part-container ol {
      margin-left: 20px;
      margin-top: 8px;
      margin-bottom: 8px;
    }
    ::ng-deep .message-part-container li {
      margin-bottom: 4px;
    }
  `]
})
export class ChatMessagesComponent implements OnChanges {
  @Input() messages: Message[] | null = [];
  @Input() loading = false;
  
  @ViewChild('chatBox') private chatBoxRef!: ElementRef;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['messages'] || changes['loading']) {
      this.scrollToBottom();
    }
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      if (this.chatBoxRef) {
        const element = this.chatBoxRef.nativeElement;
        element.scrollTop = element.scrollHeight;
      }
    }, 50);
  }
}
