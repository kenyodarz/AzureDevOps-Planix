import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AgentCard } from '../../models/devops-agent.model';

@Component({
  selector: 'app-agent-info',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="bg-[rgba(255,255,255,0.02)] border border-[rgba(255,255,255,0.08)] rounded-xl p-4 flex flex-col gap-3">
      <h3 class="text-[0.9rem] text-[#f2c94c] font-semibold uppercase tracking-wider">Información del Agente</h3>
      @if (card) {
        <p class="text-[0.85rem] text-[#9ca3af] leading-normal">
          <strong class="text-[#f3f4f6]">Nombre:</strong> {{ card.name }}
        </p>
        <p class="text-[0.85rem] text-[#9ca3af] leading-normal">
          <strong class="text-[#f3f4f6]">Versión:</strong> {{ card.version }}
        </p>
        <p class="text-[0.85rem] text-[#9ca3af] leading-normal">{{ card.description }}</p>
      } @else {
        <p class="text-[0.85rem] text-[#9ca3af] leading-normal">Cargando...</p>
      }
    </div>
  `
})
export class AgentInfoComponent {
  @Input({ required: true }) card!: AgentCard | null;
}
