import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardMetrics } from '../../models/devops-agent.model';
import { getQualityBgClass, getQualityLabel } from '../../domain/quality-score';

@Component({
  selector: 'app-quality-summary-cards',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (metrics) {
      <div class="grid grid-cols-1 md:grid-cols-4 gap-5">
        <!-- CARD 1: VELOCITY -->
        <div
          class="bg-[rgba(17,24,39,0.4)] border border-[rgba(255,255,255,0.08)] rounded-xl p-5 backdrop-blur-md hover:border-[#2563eb] transition-all flex flex-col justify-between shadow-lg"
        >
          <div class="flex justify-between items-start">
            <div>
              <span class="text-xs text-[#9ca3af] uppercase font-semibold"
                >Progreso / Velocity</span
              >
              <h3 class="text-2xl font-extrabold text-[#f3f4f6] mt-1">
                {{ metrics.completedPoints }} / {{ metrics.totalPoints }} SP
              </h3>
            </div>
            <div
              class="w-8 h-8 rounded-lg bg-[rgba(37,99,235,0.1)] flex items-center justify-center text-[#2563eb]"
            >
              <span class="pi pi-bolt"></span>
            </div>
          </div>
          <div class="mt-4">
            <div class="flex justify-between text-xs text-[#9ca3af] mb-1">
              <span>Avance de entrega</span>
              <span class="font-bold text-[#f3f4f6]">{{ metrics.completedPercentage }} %</span>
            </div>
            <div class="w-full bg-[rgba(255,255,255,0.05)] rounded-full h-1.5 overflow-hidden">
              <div
                class="bg-gradient-to-r from-[#2563eb] to-[#10b981] h-1.5 rounded-full transition-all duration-500"
                [style.width.%]="metrics.completedPercentage"
              ></div>
            </div>
          </div>
        </div>

        <!-- CARD 2: DOCUMENTATION QUALITY -->
        <div
          class="bg-[rgba(17,24,39,0.4)] border border-[rgba(255,255,255,0.08)] rounded-xl p-5 backdrop-blur-md hover:border-[#10b981] transition-all flex flex-col justify-between shadow-lg"
        >
          <div class="flex justify-between items-start">
            <div>
              <span class="text-xs text-[#9ca3af] uppercase font-semibold"
                >Calidad Documentación</span
              >
              <h3 class="text-2xl font-extrabold text-[#f3f4f6] mt-1">
                {{ metrics.avgQualityScore }}%
              </h3>
            </div>
            <div
              class="w-8 h-8 rounded-lg bg-[rgba(16,185,129,0.1)] flex items-center justify-center text-[#10b981]"
            >
              <span class="pi pi-verified"></span>
            </div>
          </div>
          <div class="mt-4">
            <div class="flex justify-between text-xs text-[#9ca3af] mb-1">
              <span>Salud del DoD</span>
              <span class="font-bold text-[#f3f4f6]">{{
                getQualityLabel(metrics.avgQualityScore)
              }}</span>
            </div>
            <div class="w-full bg-[rgba(255,255,255,0.05)] rounded-full h-1.5 overflow-hidden">
              <div
                [class]="getQualityBgClass(metrics.avgQualityScore)"
                class="h-1.5 rounded-full transition-all duration-500"
                [style.width.%]="metrics.avgQualityScore"
              ></div>
            </div>
          </div>
        </div>

        <!-- CARD 3: UNDOCUMENTED STORIES -->
        <div
          class="bg-[rgba(17,24,39,0.4)] border border-[rgba(255,255,255,0.08)] rounded-xl p-5 backdrop-blur-md hover:border-[#ef4444] transition-all flex flex-col justify-between shadow-lg"
        >
          <div class="flex justify-between items-start">
            <div>
              <span class="text-xs text-[#9ca3af] uppercase font-semibold"
                >Historias sin Refinar</span
              >
              <h3 class="text-2xl font-extrabold text-[#f3f4f6] mt-1">
                {{ metrics.undocumentedCount }} HUs
              </h3>
            </div>
            <div
              class="w-8 h-8 rounded-lg bg-[rgba(239,68,68,0.1)] flex items-center justify-center text-[#ef4444]"
            >
              <span class="pi pi-exclamation-triangle"></span>
            </div>
          </div>
          <div class="mt-4">
            <p class="text-[0.75rem] text-[#ef4444] flex items-center gap-1 font-medium">
              <span class="pi pi-info-circle"></span> Requieren criterios de aceptación o DoD.
            </p>
          </div>
        </div>

        <!-- CARD 4: STORIES IN RISK -->
        <div
          class="bg-[rgba(17,24,39,0.4)] border border-[rgba(255,255,255,0.08)] rounded-xl p-5 backdrop-blur-md hover:border-[#f59e0b] transition-all flex flex-col justify-between shadow-lg"
        >
          <div class="flex justify-between items-start">
            <div>
              <span class="text-xs text-[#9ca3af] uppercase font-semibold"
                >Historias Complejas</span
              >
              <h3 class="text-2xl font-extrabold text-[#f3f4f6] mt-1">
                {{ largeStoriesCount }} HUs
              </h3>
            </div>
            <div
              class="w-8 h-8 rounded-lg bg-[rgba(245,158,11,0.1)] flex items-center justify-center text-[#f59e0b]"
            >
              <span class="pi pi-clone"></span>
            </div>
          </div>
          <div class="mt-4">
            <p class="text-[0.75rem] text-[#f59e0b] flex items-center gap-1 font-medium">
              <span class="pi pi-info-circle"></span> Historias con &ge; 13 SP que deben dividirse.
            </p>
          </div>
        </div>
      </div>
    }
  `,
})
export class QualitySummaryCardsComponent {
  @Input() metrics: DashboardMetrics | null | undefined = null;
  @Input() largeStoriesCount: number = 0;

  protected readonly getQualityLabel = getQualityLabel;
  protected readonly getQualityBgClass = getQualityBgClass;
}
