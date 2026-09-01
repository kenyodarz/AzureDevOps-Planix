import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardMetrics, DashboardStoryItem } from '../../models/devops-agent.model';
import { COLOR_AMBER_MIN, COLOR_GREEN_MIN } from '../../domain/quality-score';

@Component({
  selector: 'app-quality-chart',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (metrics && items.length > 0) {
      <div
        class="bg-[rgba(17,24,39,0.3)] border border-[rgba(255,255,255,0.08)] rounded-xl p-5 backdrop-blur-md shadow-lg flex flex-col gap-4"
      >
        <div class="flex items-center justify-between">
          <span
            class="text-xs font-semibold text-[#f3f4f6] uppercase tracking-wider flex items-center gap-2"
          >
            <span class="pi pi-chart-pie text-[#f2c94c]"></span> Distribución de Calidad del Sprint
          </span>
          <span class="text-xs text-[#9ca3af]">{{ items.length }} Historias evaluadas</span>
        </div>

        <div class="w-full bg-[rgba(255,255,255,0.05)] h-3 rounded-full overflow-hidden flex">
          <div
            class="bg-[#10b981] h-full transition-all duration-500"
            [style.width.%]="goodPercent"
            title="Buenas / Excelentes (&ge; 80%)"
          ></div>
          <div
            class="bg-[#f59e0b] h-full transition-all duration-500"
            [style.width.%]="regularPercent"
            title="Regulares (50% - 79%)"
          ></div>
          <div
            class="bg-[#ef4444] h-full transition-all duration-500"
            [style.width.%]="criticalPercent"
            title="Críticas (&lt; 50%)"
          ></div>
        </div>

        <div class="flex flex-wrap items-center justify-between gap-4 text-xs">
          <div class="flex items-center gap-2">
            <span class="w-2.5 h-2.5 rounded-full bg-[#10b981]"></span>
            <span class="text-[#9ca3af]">Buenas (&ge; 80%):</span>
            <strong class="text-[#f3f4f6]"
              >{{ goodCount }} ({{ goodPercent | number: '1.0-0' }}%)</strong
            >
          </div>
          <div class="flex items-center gap-2">
            <span class="w-2.5 h-2.5 rounded-full bg-[#f59e0b]"></span>
            <span class="text-[#9ca3af]">Regulares (50-79%):</span>
            <strong class="text-[#f3f4f6]"
              >{{ regularCount }} ({{ regularPercent | number: '1.0-0' }}%)</strong
            >
          </div>
          <div class="flex items-center gap-2">
            <span class="w-2.5 h-2.5 rounded-full bg-[#ef4444]"></span>
            <span class="text-[#9ca3af]">Críticas (&lt; 50%):</span>
            <strong class="text-[#f3f4f6]"
              >{{ criticalCount }} ({{ criticalPercent | number: '1.0-0' }}%)</strong
            >
          </div>
        </div>
      </div>
    }
  `,
})
export class QualityChartComponent {
  @Input() metrics: DashboardMetrics | null | undefined = null;
  @Input() items: DashboardStoryItem[] = [];

  protected get goodCount(): number {
    return this.items.filter((i) => i.qualityScore >= COLOR_GREEN_MIN).length;
  }

  protected get regularCount(): number {
    return this.items.filter(
      (i) => i.qualityScore >= COLOR_AMBER_MIN && i.qualityScore < COLOR_GREEN_MIN,
    ).length;
  }

  protected get criticalCount(): number {
    return this.items.filter((i) => i.qualityScore < COLOR_AMBER_MIN).length;
  }

  protected get goodPercent(): number {
    if (!this.items.length) return 0;
    return (this.goodCount / this.items.length) * 100;
  }

  protected get regularPercent(): number {
    if (!this.items.length) return 0;
    return (this.regularCount / this.items.length) * 100;
  }

  protected get criticalPercent(): number {
    if (!this.items.length) return 0;
    return (this.criticalCount / this.items.length) * 100;
  }
}
