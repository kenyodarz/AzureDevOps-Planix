import { Component, EventEmitter, inject, OnDestroy, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { DevopsAgentStateService } from '../../services/devops-agent-state.service';
import { DashboardData, DashboardStoryItem } from '../../models/devops-agent.model';
import {
  filterStories,
  getLargeStoriesCount,
  getQualityBgClass,
  getQualityLabel,
  getStateClass,
  getUniqueMembers,
  getUniqueStates,
} from '../../domain/quality-score';
import { QualitySummaryCardsComponent } from './quality-summary-cards.component';
import { QualityStoriesTableComponent } from './quality-stories-table.component';
import { QualityChartComponent } from './quality-chart.component';
import { StoryAuditModalComponent } from './story-audit-modal.component';

@Component({
  selector: 'app-planning-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    QualitySummaryCardsComponent,
    QualityStoriesTableComponent,
    QualityChartComponent,
    StoryAuditModalComponent,
  ],
  template: `
    <div class="flex-1 flex flex-col p-8 overflow-y-auto bg-transparent">
      <!-- HEADER & CONTROLS -->
      <div
        class="mb-8 bg-[rgba(17,24,39,0.5)] border border-[rgba(255,255,255,0.08)] rounded-xl p-6 backdrop-blur-md shadow-lg"
      >
        <div class="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
          <div>
            <h2 class="text-xl font-bold text-[#f2c94c] flex items-center gap-2">
              <span class="pi pi-chart-bar" style="font-size: 1.2rem"></span>
              Tablero de Calidad y Velocidad (DevOps Dashboard)
            </h2>
            <p class="text-xs text-[#9ca3af] mt-1">
              Monitorea de forma no invasiva la salud del backlog de Azure DevOps de tu equipo y
              evalúa la cobertura de DoD.
            </p>
          </div>
        </div>

        <div class="flex flex-col md:flex-row items-end gap-4">
          <div class="flex-1 flex flex-col gap-1.5 w-full">
            <label class="text-[0.7rem] text-[#9ca3af] font-semibold uppercase tracking-wider"
              >Célula / Area Path</label
            >
            <div class="relative">
              <span
                class="pi pi-users absolute left-3 top-1/2 -translate-y-1/2 text-[#9ca3af]"
              ></span>
              <input
                type="text"
                [(ngModel)]="cellInput"
                placeholder="Ej: Aegis Backend - Célula Arkham"
                class="w-full bg-[#0b0f19] border border-[rgba(255,255,255,0.12)] text-[#f3f4f6] pl-9 pr-4 py-2.5 rounded-lg text-sm outline-none focus:border-[#f2c94c] hover:border-[rgba(255,255,255,0.2)] transition-all font-sans"
              />
            </div>
          </div>

          <div class="flex-1 flex flex-col gap-1.5 w-full">
            <label class="text-[0.7rem] text-[#9ca3af] font-semibold uppercase tracking-wider"
              >Sprint / Iteration Path</label
            >
            <div class="relative">
              <span
                class="pi pi-calendar absolute left-3 top-1/2 -translate-y-1/2 text-[#9ca3af]"
              ></span>
              <input
                type="text"
                [(ngModel)]="sprintInput"
                placeholder="Ej: Sprint 3"
                class="w-full bg-[#0b0f19] border border-[rgba(255,255,255,0.12)] text-[#f3f4f6] pl-9 pr-4 py-2.5 rounded-lg text-sm outline-none focus:border-[#f2c94c] hover:border-[rgba(255,255,255,0.2)] transition-all font-sans"
              />
            </div>
          </div>

          <button
            type="button"
            [disabled]="(state.loading | async) || !cellInput || !sprintInput"
            class="bg-gradient-to-r from-[#f2c94c] to-[#e0b230] text-[#0b0f19] hover:from-[#f5d56e] hover:to-[#f2c94c] disabled:opacity-50 disabled:cursor-not-allowed px-6 py-2.5 rounded-lg font-bold text-sm transition-all shadow-[0_4px_12px_rgba(242,201,76,0.15)] flex items-center justify-center gap-2 cursor-pointer border-none w-full md:w-auto h-[42px]"
            (click)="generateAnalysis()"
          >
            @if (state.loading | async) {
              <span class="pi pi-spin pi-spinner"></span> Analizando...
            } @else {
              <span class="pi pi-search"></span> Generar Análisis
            }
          </button>
        </div>

        @if (errorMessage) {
          <div
            class="mt-4 p-4 rounded-lg bg-[rgba(239,68,68,0.1)] border border-[rgba(239,68,68,0.25)] flex items-start gap-3 shadow-[0_4px_12px_rgba(239,68,68,0.1)]"
          >
            <span class="pi pi-exclamation-circle text-[#ef4444] text-lg mt-0.5 shrink-0"></span>
            <div class="flex-1">
              <h4 class="text-sm font-bold text-[#ef4444] mb-0.5">
                Error en el análisis de Backlog
              </h4>
              <p class="text-xs text-[#fca5a5] leading-relaxed">{{ errorMessage }}</p>
            </div>
          </div>
        }
      </div>

      <!-- MAIN CONTENT VIEW -->
      @if (state.loading | async) {
        <div class="flex-1 flex flex-col items-center justify-center gap-3 py-16">
          <div
            class="w-10 h-10 border-4 border-[#f2c94c] border-t-transparent rounded-full animate-spin"
          ></div>
          <span class="text-sm text-[#9ca3af]">Auditando backlog en Azure DevOps...</span>
          <p class="text-xs text-[#6b7280]">
            Esto puede demorar unos segundos mientras el agente ejecuta el análisis de calidad.
          </p>
        </div>
      } @else if (dashboardData) {
        <div class="flex flex-col gap-6">
          <div
            class="flex items-center gap-2 bg-[rgba(242,201,76,0.05)] border border-[rgba(242,201,76,0.15)] rounded-lg px-4 py-2 w-fit"
          >
            <span class="w-2 h-2 rounded-full bg-[#f2c94c]"></span>
            <span class="text-xs text-[#9ca3af]"
              >Mostrando análisis para Célula:
              <strong class="text-[#f3f4f6]">{{ activeCell }}</strong> | Sprint:
              <strong class="text-[#f3f4f6]">{{ activeSprint }}</strong></span
            >
          </div>
          <app-quality-summary-cards
            [metrics]="dashboardData.metrics"
            [largeStoriesCount]="getLargeStoriesCount()"
          />
          <app-quality-chart [metrics]="dashboardData.metrics" [items]="dashboardData.items" />
          <app-quality-stories-table
            [items]="dashboardData.items"
            [filteredItems]="filteredItems"
            [uniqueMembers]="uniqueMembers"
            [uniqueStates]="uniqueStates"
            [selectedMember]="selectedMember"
            [selectedState]="selectedState"
            [selectedQuality]="selectedQuality"
            [expandedItemIds]="expandedItemIds"
            [detailedAudits]="detailedAudits"
            (selectedMemberChange)="selectedMember = $event"
            (selectedStateChange)="selectedState = $event"
            (selectedQualityChange)="selectedQuality = $event"
            (toggleExpand)="toggleExpand($event)"
            (refineStory)="refineStory($event)"
            (runDetailedAudit)="runDetailedAudit($event)"
          />
          <app-story-audit-modal
            [visible]="isAuditModalOpen"
            [story]="selectedAuditStory"
            [loading]="
              selectedAuditStory ? !!detailedAudits[selectedAuditStory.id]?.loading : false
            "
            [error]="selectedAuditStory ? detailedAudits[selectedAuditStory.id]?.error : undefined"
            [auditContent]="
              selectedAuditStory ? detailedAudits[selectedAuditStory.id]?.content : undefined
            "
            (close)="isAuditModalOpen = false"
            (runAudit)="runDetailedAudit($event)"
          />
        </div>
      } @else {
        <div
          class="flex-1 flex flex-col items-center justify-center text-center max-w-lg mx-auto py-16 gap-4"
        >
          <div
            class="w-16 h-16 rounded-full bg-[rgba(242,201,76,0.1)] flex items-center justify-center text-[#f2c94c] mb-2"
          >
            <span class="pi pi-chart-line text-2xl"></span>
          </div>
          <h3 class="text-lg font-bold text-[#f3f4f6]">Genera el Análisis de Backlog</h3>
          <p class="text-sm text-[#9ca3af] leading-relaxed">
            Ingresa la Célula (Ruta de Área) y el Sprint en el formulario superior para conectarse
            de forma segura con Azure DevOps, auditar la documentación y medir la velocidad del
            equipo.
          </p>
          <div
            class="flex items-center gap-2 mt-2 bg-[rgba(255,255,255,0.03)] border border-[rgba(255,255,255,0.05)] rounded-lg px-4 py-2 text-xs text-[#6b7280]"
          >
            <span class="pi pi-lock"></span>
            <span>No se generarán llamadas automáticas ni consumo inútil de tokens.</span>
          </div>
        </div>
      }
    </div>
  `,
})
export class PlanningDashboardComponent implements OnInit, OnDestroy {
  @Output() refineRequested = new EventEmitter<void>();

  protected readonly state = inject(DevopsAgentStateService);
  protected dashboardData: DashboardData | null = null;
  protected errorMessage: string | null = null;
  protected cellInput: string = '';
  protected sprintInput: string = '';
  protected activeCell: string = '';
  protected activeSprint: string = '';
  protected selectedMember: string = '';
  protected selectedState: string = '';
  protected selectedQuality: string = '';
  protected expandedItemIds: Set<string> = new Set();
  protected detailedAudits: Record<string, { loading: boolean; content?: string; error?: string }> =
    {};
  protected selectedAuditStory: DashboardStoryItem | null = null;
  protected isAuditModalOpen: boolean = false;
  private sub: Subscription | null = null;
  private errorSub: Subscription | null = null;

  protected get filteredItems(): DashboardStoryItem[] {
    return filterStories(this.dashboardData?.items, {
      member: this.selectedMember,
      state: this.selectedState,
      quality: this.selectedQuality,
    });
  }

  protected get uniqueStates(): string[] {
    return getUniqueStates(this.dashboardData?.items);
  }
  protected get uniqueMembers(): string[] {
    return getUniqueMembers(this.dashboardData?.items);
  }

  public ngOnInit(): void {
    this.sub = this.state.dashboardData.subscribe((data) => {
      this.dashboardData = data;
      this.selectedMember = '';
      this.selectedState = '';
      this.selectedQuality = '';
      this.expandedItemIds.clear();
    });
    this.errorSub = this.state.dashboardError.subscribe((err) => (this.errorMessage = err));
  }

  public ngOnDestroy(): void {
    this.sub?.unsubscribe();
    this.errorSub?.unsubscribe();
  }

  protected toggleExpand(itemId: string): void {
    if (this.expandedItemIds.has(itemId)) {
      this.expandedItemIds.delete(itemId);
    } else {
      this.expandedItemIds.add(itemId);
    }
  }

  protected isExpanded(itemId: string): boolean {
    return this.expandedItemIds.has(itemId);
  }

  protected generateAnalysis(): void {
    if (this.cellInput.trim() && this.sprintInput.trim()) {
      this.activeCell = this.cellInput.trim();
      this.activeSprint = this.sprintInput.trim();
      this.state.loadDashboardData(this.activeCell, this.activeSprint);
    }
  }

  protected getQualityLabel(score: number): string {
    return getQualityLabel(score);
  }
  protected getQualityBgClass(score: number): string {
    return getQualityBgClass(score);
  }
  protected getLargeStoriesCount(): number {
    return getLargeStoriesCount(this.dashboardData?.items);
  }
  protected getStateClass(state: string): string {
    return getStateClass(state);
  }

  protected refineStory(item: DashboardStoryItem): void {
    this.state.refineStoryInChat(item.id, item.title);
    this.refineRequested.emit();
  }

  protected runDetailedAudit(storyId: string): void {
    this.detailedAudits[storyId] = { loading: true };
    this.state.auditStory(storyId).subscribe({
      next: (response) => {
        const replyText = response.message?.parts?.[0]?.text || 'No se obtuvo reporte.';
        this.detailedAudits[storyId] = { loading: false, content: replyText };
      },
      error: (error) => {
        console.error(error);
        this.detailedAudits[storyId] = { loading: false, error: 'Error al procesar la auditoría.' };
      },
    });
  }
}
