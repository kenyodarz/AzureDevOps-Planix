import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-info-cards',
  standalone: true,
  imports: [CommonModule],
  template: `
    <!-- PROTOCOLO -->
    <div class="bg-[rgba(255,255,255,0.02)] border border-[rgba(255,255,255,0.08)] rounded-xl p-4 flex flex-col gap-3">
      <h3 class="text-[0.9rem] text-[#f2c94c] font-semibold uppercase tracking-wider">Protocolo</h3>
      <p class="text-[0.85rem] text-[#9ca3af] leading-normal">
        <strong class="text-[#f3f4f6]">Fase 1:</strong> Recibe tu idea corta, la clasifica en HU o HA, y la enriquece.
      </p>
      <p class="text-[0.85rem] text-[#9ca3af] leading-normal">
        <strong class="text-[#f3f4f6]">Fase 2:</strong> Te muestra el borrador en Markdown para aprobación.
      </p>
      <p class="text-[0.85rem] text-[#9ca3af] leading-normal">
        <strong class="text-[#f3f4f6]">Fase 3:</strong> Tras confirmación (ej. "Aprobado" o "Crear"), la registra en Azure DevOps con sus tareas hijas.
      </p>
    </div>

    <!-- TECNOLOGÍAS -->
    <div class="bg-[rgba(255,255,255,0.02)] border border-[rgba(255,255,255,0.08)] rounded-xl p-4 flex flex-col gap-3">
      <h3 class="text-[0.9rem] text-[#f2c94c] font-semibold uppercase tracking-wider">Tecnologías</h3>
      <div class="flex flex-wrap gap-2 mt-1">
        @for (tag of tags; track tag) {
          <span class="bg-[rgba(255,255,255,0.05)] border border-[rgba(255,255,255,0.08)] text-[0.75rem] px-2 py-1 rounded text-[#f3f4f6]">
            {{ tag }}
          </span>
        }
      </div>
    </div>
  `
})
export class InfoCardsComponent {
  protected readonly tags = ['Spring AI', 'WebFlux', 'MCP', 'Azure DevOps', 'OpenAI'];
}
