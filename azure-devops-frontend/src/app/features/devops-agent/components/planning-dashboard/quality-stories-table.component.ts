import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DashboardStoryItem } from '../../models/devops-agent.model';
import { MarkdownParserPipe } from '../../../../shared/pipes/markdown-parser.pipe';
import { getQualityBgClass, getStateClass } from '../../domain/quality-score';

@Component({
  selector: 'app-quality-stories-table',
  standalone: true,
  imports: [CommonModule, FormsModule, MarkdownParserPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div
      class="overflow-hidden border border-[rgba(255,255,255,0.08)] bg-[rgba(17,24,39,0.3)] backdrop-blur-md rounded-xl shadow-xl"
    >
      <div
        class="p-4 border-b border-[rgba(255,255,255,0.08)] flex flex-col lg:flex-row justify-between items-start lg:items-center gap-4"
      >
        <div class="flex flex-col gap-1">
          <span class="font-semibold text-sm text-[#f3f4f6]"
            >Desglose de Historias y Habilitadores</span
          >
          <span class="text-xs text-[#9ca3af]"
            >{{ filteredItems.length }} de {{ items.length }} ítems encontrados</span
          >
        </div>

        <!-- FILTROS AVANZADOS -->
        <div class="flex flex-wrap items-center gap-4 w-full lg:w-auto">
          <!-- Filtro por Miembro -->
          <div class="flex items-center gap-2">
            <label class="text-xs text-[#9ca3af] font-medium whitespace-nowrap">Integrante:</label>
            <select
              [ngModel]="selectedMember"
              (ngModelChange)="onMemberChange($event)"
              class="bg-[#0b0f19] border border-[rgba(255,255,255,0.12)] text-[#f3f4f6] px-3 py-1.5 rounded-lg text-xs outline-none focus:border-[#f2c94c] hover:border-[rgba(255,255,255,0.2)] transition-all"
            >
              <option value="">Todos</option>
              @for (member of uniqueMembers; track member) {
                <option [value]="member">{{ member }}</option>
              }
            </select>
          </div>

          <!-- Filtro por Estado -->
          <div class="flex items-center gap-2">
            <label class="text-xs text-[#9ca3af] font-medium whitespace-nowrap">Estado:</label>
            <select
              [ngModel]="selectedState"
              (ngModelChange)="onStateChange($event)"
              class="bg-[#0b0f19] border border-[rgba(255,255,255,0.12)] text-[#f3f4f6] px-3 py-1.5 rounded-lg text-xs outline-none focus:border-[#f2c94c] hover:border-[rgba(255,255,255,0.2)] transition-all"
            >
              <option value="">Todos</option>
              @for (state of uniqueStates; track state) {
                <option [value]="state">{{ state }}</option>
              }
            </select>
          </div>

          <!-- Filtro por Calidad -->
          <div class="flex items-center gap-2">
            <label class="text-xs text-[#9ca3af] font-medium whitespace-nowrap">Calidad:</label>
            <select
              [ngModel]="selectedQuality"
              (ngModelChange)="onQualityChange($event)"
              class="bg-[#0b0f19] border border-[rgba(255,255,255,0.12)] text-[#f3f4f6] px-3 py-1.5 rounded-lg text-xs outline-none focus:border-[#f2c94c] hover:border-[rgba(255,255,255,0.2)] transition-all"
            >
              <option value="">Todos</option>
              <option value="critical">Crítico (&lt; 50%)</option>
              <option value="regular">Regular (50% - 79%)</option>
              <option value="good">Bueno (&gt;= 80%)</option>
            </select>
          </div>
        </div>
      </div>

      <table class="w-full text-left border-collapse text-[0.8rem]">
        <thead>
          <tr
            class="border-b border-[rgba(255,255,255,0.08)] bg-[rgba(255,255,255,0.02)] text-[#9ca3af]"
          >
            <th class="p-4 font-semibold text-center w-[5%]"></th>
            <th class="p-4 font-semibold w-[10%]">ID</th>
            <th class="p-4 font-semibold w-[30%]">Título / Nombre</th>
            <th class="p-4 font-semibold w-[15%]">Miembro</th>
            <th class="p-4 font-semibold text-center w-[8%]">Puntos</th>
            <th class="p-4 font-semibold text-center w-[12%]">Estado</th>
            <th class="p-4 font-semibold text-center w-[6%]">Criterios</th>
            <th class="p-4 font-semibold text-center w-[6%]">DoD</th>
            <th class="p-4 font-semibold text-center w-[12%]">Acción</th>
          </tr>
        </thead>
        <tbody>
          @for (item of filteredItems; track item.id) {
            <tr
              [class.bg-[rgba(239,68,68,0.02)]]="item.qualityScore < 50"
              [class.bg-[rgba(245,158,11,0.01)]]="item.points >= 13"
              class="border-b border-[rgba(255,255,255,0.05)] hover:bg-[rgba(255,255,255,0.02)] transition-all"
            >
              <!-- Expand/Collapse Button -->
              <td class="p-4 text-center">
                <button
                  type="button"
                  class="bg-transparent border-none text-[#9ca3af] hover:text-[#f3f4f6] cursor-pointer"
                  (click)="toggleExpand.emit(item.id)"
                >
                  <span
                    class="pi"
                    [ngClass]="
                      isExpanded(item.id) ? 'pi-chevron-down text-[#f2c94c]' : 'pi-chevron-right'
                    "
                  ></span>
                </button>
              </td>

              <!-- ID -->
              <td class="p-4 font-mono text-[#2563eb] text-[0.75rem]">{{ item.id }}</td>

              <!-- Title -->
              <td class="p-4">
                <div class="flex flex-col gap-1">
                  <span class="text-[#f3f4f6] font-medium leading-snug">{{ item.title }}</span>

                  <!-- Warnings/Badges -->
                  <div class="flex items-center gap-2 mt-1">
                    @if (item.points >= 13) {
                      <span
                        class="bg-[rgba(245,158,11,0.1)] text-[#f59e0b] px-1.5 py-0.5 rounded text-[0.65rem] border border-[rgba(245,158,11,0.2)] font-semibold flex items-center gap-1"
                      >
                        <span class="pi pi-exclamation-triangle" style="font-size: 0.6rem"></span>
                        Dividir Historia
                      </span>
                    }
                    <span class="text-[0.7rem] text-[#9ca3af] flex items-center gap-1">
                      <span class="pi pi-paperclip" style="font-size: 0.65rem"></span>
                      {{ item.linkedTasksCount }} tareas hijas
                    </span>

                    <!-- Mini quality progress -->
                    <div class="flex items-center gap-1.5 ml-2">
                      <span class="text-[0.65rem] text-[#9ca3af]">Calidad:</span>
                      @if (item.qualityScore === 0) {
                        <span
                          class="pi pi-spin pi-spinner text-[#2563eb]"
                          style="font-size: 0.65rem"
                        ></span>
                        <span class="text-[0.65rem] text-[#2563eb] font-semibold animate-pulse"
                          >Auditando...</span
                        >
                      } @else {
                        <div
                          class="w-12 bg-[rgba(255,255,255,0.05)] h-1 rounded-full overflow-hidden"
                        >
                          <div
                            [class]="getQualityBgClass(item.qualityScore)"
                            class="h-1 rounded-full"
                            [style.width.%]="item.qualityScore"
                          ></div>
                        </div>
                        <span
                          class="text-[0.65rem] font-bold"
                          [class.text-[#ef4444]]="item.qualityScore < 50"
                          [class.text-[#10b981]]="item.qualityScore >= 80"
                          [class.text-[#f59e0b]]="item.qualityScore >= 50 && item.qualityScore < 80"
                        >
                          {{ item.qualityScore }}%
                        </span>
                      }
                    </div>
                  </div>
                </div>
              </td>

              <!-- Member -->
              <td class="p-4 text-[#f3f4f6]">
                {{ item.assignedMember || 'No asignado' }}
              </td>

              <!-- Points -->
              <td class="p-4 text-center">
                <span
                  class="bg-[rgba(242,201,76,0.1)] text-[#f2c94c] px-2 py-0.5 rounded font-mono font-bold text-[0.75rem] border border-[rgba(242,201,76,0.2)] shadow-sm"
                >
                  {{ item.points }} SP
                </span>
              </td>

              <!-- State -->
              <td class="p-4 text-center">
                <span
                  [class]="getStateClass(item.state)"
                  class="px-2.5 py-0.5 rounded-full font-bold text-[0.7rem] uppercase tracking-wider"
                >
                  {{ item.state }}
                </span>
              </td>

              <!-- Checks -->
              <td class="p-4 text-center">
                @if (item.qualityScore === 0) {
                  <span
                    class="pi pi-spin pi-spinner text-[#6b7280]"
                    style="font-size: 0.9rem"
                  ></span>
                } @else {
                  <span
                    [class]="
                      item.hasAcceptanceCriteria
                        ? 'pi pi-check-circle text-emerald-500'
                        : 'pi pi-times-circle text-rose-500'
                    "
                    style="font-size: 1.1rem"
                  ></span>
                }
              </td>

              <td class="p-4 text-center">
                @if (item.qualityScore === 0) {
                  <span
                    class="pi pi-spin pi-spinner text-[#6b7280]"
                    style="font-size: 0.9rem"
                  ></span>
                } @else {
                  <span
                    [class]="
                      item.hasDoD
                        ? 'pi pi-check-circle text-emerald-500'
                        : 'pi pi-times-circle text-rose-500'
                    "
                    style="font-size: 1.1rem"
                  ></span>
                }
              </td>

              <!-- Action -->
              <td class="p-4 text-center">
                <button
                  type="button"
                  class="bg-gradient-to-r from-[#2563eb] to-[#1d4ed8] hover:from-[#1d4ed8] hover:to-[#1e40af] text-white px-3 py-1.5 rounded-lg border-none text-[0.7rem] font-semibold cursor-pointer transition-all flex items-center justify-center gap-1.5 mx-auto shadow-[0_2px_4px_rgba(37,99,235,0.2)]"
                  (click)="refineStory.emit(item)"
                >
                  <span class="pi pi-sparkles" style="font-size: 0.75rem"></span> Refinar
                </button>
              </td>
            </tr>

            @if (isExpanded(item.id)) {
              <tr class="bg-[rgba(255,255,255,0.015)] border-b border-[rgba(255,255,255,0.04)]">
                <td colspan="9" class="p-4 pl-12 text-[#d1d5db]">
                  <div class="flex flex-col gap-3 border-l-2 border-[#f2c94c] pl-4">
                    <div class="flex items-center justify-between gap-3">
                      <span class="text-xs font-bold text-[#f2c94c] flex items-center gap-1.5">
                        <span class="pi pi-comment"></span> Feedback de Calidad (Auditoría IA)
                      </span>
                      <button
                        type="button"
                        class="bg-[rgba(242,201,76,0.12)] hover:bg-[rgba(242,201,76,0.2)] text-[#f2c94c] border border-[rgba(242,201,76,0.2)] px-3 py-1.5 rounded-lg text-[0.7rem] font-semibold transition-all flex items-center gap-1.5 cursor-pointer"
                        (click)="runDetailedAudit.emit(item.id)"
                        [disabled]="detailedAudits[item.id]?.loading"
                      >
                        @if (detailedAudits[item.id]?.loading) {
                          <span class="pi pi-spin pi-spinner"></span>
                          <span>Procesando...</span>
                        } @else {
                          <span class="pi pi-search"></span>
                          <span>Realizar Auditoría Detallada</span>
                        }
                      </button>
                    </div>

                    @if (detailedAudits[item.id]?.loading) {
                      <div class="flex items-center gap-2 text-xs text-[#f2c94c]">
                        <span class="pi pi-spin pi-spinner"></span>
                        <span>Generando reporte detallado...</span>
                      </div>
                    } @else if (detailedAudits[item.id]?.error) {
                      <p class="text-xs text-[#ef4444] leading-relaxed">
                        {{ detailedAudits[item.id].error }}
                      </p>
                    } @else if (detailedAudits[item.id]?.content) {
                      <div
                        class="markdown-audit-content text-xs leading-relaxed text-[#d1d5db]"
                        [innerHTML]="detailedAudits[item.id].content | markdownParser"
                      ></div>
                    } @else {
                      <p class="text-xs italic text-[#9ca3af] whitespace-pre-line leading-relaxed">
                        {{
                          item.feedback || 'Analizando o sin retroalimentación detallada todavía.'
                        }}
                      </p>
                    }
                  </div>
                </td>
              </tr>
            }
          }
        </tbody>
      </table>
    </div>
  `,
})
export class QualityStoriesTableComponent {
  @Input() items: DashboardStoryItem[] = [];
  @Input() filteredItems: DashboardStoryItem[] = [];
  @Input() uniqueMembers: string[] = [];
  @Input() uniqueStates: string[] = [];
  @Input() selectedMember: string = '';
  @Input() selectedState: string = '';
  @Input() selectedQuality: string = '';
  @Input() expandedItemIds: Set<string> = new Set();
  @Input() detailedAudits: Record<string, { loading: boolean; content?: string; error?: string }> =
    {};

  @Output() selectedMemberChange = new EventEmitter<string>();
  @Output() selectedStateChange = new EventEmitter<string>();
  @Output() selectedQualityChange = new EventEmitter<string>();
  @Output() toggleExpand = new EventEmitter<string>();
  @Output() refineStory = new EventEmitter<DashboardStoryItem>();
  @Output() runDetailedAudit = new EventEmitter<string>();

  protected readonly getQualityBgClass = getQualityBgClass;
  protected readonly getStateClass = getStateClass;

  protected isExpanded(itemId: string): boolean {
    return this.expandedItemIds.has(itemId);
  }

  protected onMemberChange(value: string): void {
    this.selectedMember = value;
    this.selectedMemberChange.emit(value);
  }

  protected onStateChange(value: string): void {
    this.selectedState = value;
    this.selectedStateChange.emit(value);
  }

  protected onQualityChange(value: string): void {
    this.selectedQuality = value;
    this.selectedQualityChange.emit(value);
  }
}
