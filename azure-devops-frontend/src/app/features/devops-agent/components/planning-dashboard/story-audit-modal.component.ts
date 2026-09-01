import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardStoryItem } from '../../models/devops-agent.model';
import { MarkdownParserPipe } from '../../../../shared/pipes/markdown-parser.pipe';

@Component({
  selector: 'app-story-audit-modal',
  standalone: true,
  imports: [CommonModule, MarkdownParserPipe],
  template: `
    @if (visible && story) {
      <div
        class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in"
      >
        <div
          class="bg-[#0f172a] border border-[rgba(255,255,255,0.12)] rounded-2xl w-full max-w-2xl max-h-[85vh] flex flex-col shadow-2xl overflow-hidden"
        >
          <!-- MODAL HEADER -->
          <div
            class="p-5 border-b border-[rgba(255,255,255,0.08)] flex items-center justify-between bg-[rgba(17,24,39,0.5)]"
          >
            <div class="flex items-center gap-2.5">
              <span class="pi pi-shield text-[#f2c94c] text-lg"></span>
              <div>
                <h3 class="text-sm font-bold text-[#f3f4f6]">
                  Auditoría Detallada — {{ story.id }}
                </h3>
                <p class="text-xs text-[#9ca3af] truncate max-w-md">{{ story.title }}</p>
              </div>
            </div>
            <button
              type="button"
              (click)="close.emit()"
              class="text-[#9ca3af] hover:text-[#f3f4f6] bg-transparent border-none p-1.5 rounded-lg cursor-pointer transition-colors"
            >
              <span class="pi pi-times text-base"></span>
            </button>
          </div>

          <!-- MODAL BODY -->
          <div
            class="p-6 overflow-y-auto flex-1 flex flex-col gap-4 text-xs text-[#d1d5db] leading-relaxed"
          >
            @if (loading) {
              <div class="flex flex-col items-center justify-center py-12 gap-3">
                <span class="pi pi-spin pi-spinner text-[#f2c94c] text-2xl"></span>
                <span class="text-sm text-[#9ca3af]">Generando auditoría mediante IA...</span>
              </div>
            } @else if (error) {
              <div
                class="p-4 rounded-lg bg-[rgba(239,68,68,0.1)] border border-[rgba(239,68,68,0.25)] flex items-start gap-3"
              >
                <span
                  class="pi pi-exclamation-circle text-[#ef4444] text-lg mt-0.5 shrink-0"
                ></span>
                <div class="flex-1">
                  <h4 class="text-xs font-bold text-[#ef4444] mb-0.5">Error en la auditoría</h4>
                  <p class="text-xs text-[#fca5a5]">{{ error }}</p>
                </div>
              </div>
            } @else if (auditContent) {
              <div
                class="markdown-audit-content bg-[rgba(255,255,255,0.02)] p-4 rounded-xl border border-[rgba(255,255,255,0.05)]"
                [innerHTML]="auditContent | markdownParser"
              ></div>
            } @else {
              <div class="text-center py-8">
                <p class="text-xs text-[#9ca3af] mb-4">
                  {{ story.feedback || 'No hay reporte detallado generado para esta historia.' }}
                </p>
                <button
                  type="button"
                  (click)="runAudit.emit(story.id)"
                  class="bg-gradient-to-r from-[#f2c94c] to-[#e0b230] text-[#0b0f19] px-4 py-2 rounded-lg font-bold text-xs cursor-pointer border-none shadow-md"
                >
                  <span class="pi pi-search"></span> Iniciar Auditoría con IA
                </button>
              </div>
            }
          </div>

          <!-- MODAL FOOTER -->
          <div
            class="p-4 border-t border-[rgba(255,255,255,0.08)] flex justify-end bg-[rgba(17,24,39,0.3)]"
          >
            <button
              type="button"
              (click)="close.emit()"
              class="px-4 py-2 bg-[rgba(255,255,255,0.05)] hover:bg-[rgba(255,255,255,0.1)] text-[#f3f4f6] rounded-lg text-xs font-semibold cursor-pointer border border-[rgba(255,255,255,0.1)] transition-all"
            >
              Cerrar
            </button>
          </div>
        </div>
      </div>
    }
  `,
})
export class StoryAuditModalComponent {
  @Input() visible: boolean = false;
  @Input() story: DashboardStoryItem | null = null;
  @Input() auditContent: string | null | undefined = null;
  @Input() loading: boolean = false;
  @Input() error: string | null | undefined = null;

  @Output() close = new EventEmitter<void>();
  @Output() runAudit = new EventEmitter<string>();
}
