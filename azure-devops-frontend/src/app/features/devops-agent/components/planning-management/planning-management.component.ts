import {Component, inject, OnDestroy, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {DevopsAgentStateService} from '../../services/devops-agent-state.service';
import {DevopsAgentApiService} from '../../services/devops-agent-api.service';
import {Subscription} from 'rxjs';

@Component({
  selector: 'app-planning-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="flex-1 flex flex-col p-8 overflow-y-auto">
      <div class="mb-6">
        <h2 class="text-xl font-bold text-[#f2c94c] flex items-center gap-2">
          <span class="pi pi-list" style="font-size: 1.2rem"></span>
          Gestión de Planeaciones Vectorizadas
        </h2>
        <p class="text-sm text-[#9ca3af] mt-1">
          Administra las iniciativas cargadas en la base de datos vectorial de PostgreSQL. Asocia cada iniciativa a una célula o elimina su indexación por completo.
        </p>
      </div>

      @if (loading()) {
        <div class="flex-1 flex flex-col items-center justify-center gap-3">
          <div class="w-8 h-8 border-4 border-[#f2c94c] border-t-transparent rounded-full animate-spin"></div>
          <span class="text-sm text-[#9ca3af]">Cargando iniciativas...</span>
        </div>
      } @else if (editableInitiatives.length === 0) {
        <div class="flex-1 border border-dashed border-[rgba(255,255,255,0.08)] bg-[rgba(255,255,255,0.01)] rounded-xl flex flex-col items-center justify-center p-12 text-center">
          <div class="w-12 h-12 rounded-full bg-[rgba(242,201,76,0.1)] flex items-center justify-center text-[#f2c94c] mb-4">
            <span class="pi pi-folder-open" style="font-size: 1.5rem"></span>
          </div>
          <h3 class="text-base font-semibold text-[#f3f4f6]">No hay iniciativas indexadas</h3>
          <p class="text-sm text-[#9ca3af] max-w-sm mt-1">
            Usa el panel de "Cargar Planeación" a la izquierda para subir y vectorizar tus primeros archivos de planeación.
          </p>
        </div>
      } @else {
        <div class="overflow-hidden border border-[rgba(255,255,255,0.08)] bg-[rgba(17,24,39,0.3)] backdrop-blur-md rounded-xl">
          <table class="w-full text-left border-collapse text-[0.85rem]">
            <thead>
              <tr class="border-b border-[rgba(255,255,255,0.08)] bg-[rgba(255,255,255,0.02)]">
                <th class="p-4 font-semibold text-[#f2c94c] w-[20%]">ID Iniciativa</th>
                <th class="p-4 font-semibold text-[#f2c94c] w-[35%]">Título de la Iniciativa</th>
                <th class="p-4 font-semibold text-[#f2c94c] w-[30%]">Célula / Equipo Asociado</th>
                <th class="p-4 font-semibold text-[#f2c94c] text-right w-[15%]">Acciones</th>
              </tr>
            </thead>
            <tbody>
              @for (init of editableInitiatives; track init.initiative_id) {
                <tr class="border-b border-[rgba(255,255,255,0.05)] hover:bg-[rgba(255,255,255,0.01)] transition-colors">
                  <td class="p-4 font-mono text-[#2563eb] text-[0.8rem] truncate">{{ init.initiative_id }}</td>
                  <td class="p-4 text-[#f3f4f6] font-medium">{{ init.initiative_title }}</td>
                  <td class="p-4">
                    <div class="flex items-center gap-2">
                      <input
                        [aria-label]="'Célula para ' + init.initiative_title"
                        placeholder="Ej: Célula Core, Canal App..."
                        class="bg-[rgba(255,255,255,0.05)] border border-[rgba(255,255,255,0.08)] rounded-md px-3 py-1.5 text-[0.8rem] text-[#f3f4f6] outline-none focus:border-[#f2c94c] w-full max-w-[240px] transition-all"
                        type="text"
                        [(ngModel)]="init.tempCell"
                      />
                    </div>
                  </td>
                  <td class="p-4 text-right">
                    <div class="flex items-center justify-end gap-2">
                      <button
                        [aria-label]="'Previsualizar iniciativa ' + init.initiative_title"
                        class="flex items-center justify-center bg-[#2563eb] hover:bg-[#1d4ed8] text-white rounded-md w-8 h-8 border-none cursor-pointer transition-all shadow-[0_2px_4px_rgba(0,0,0,0.2)]"
                        (click)="previewInitiative(init)"
                      >
                        <span class="pi pi-eye"></span>
                      </button>
                      <button
                        [aria-label]="'Guardar célula de ' + init.initiative_title"
                        [disabled]="init.cell === init.tempCell"
                        [style.opacity]="init.cell === init.tempCell ? 0.4 : 1"
                        class="flex items-center justify-center bg-[#10b981] hover:bg-[#059669] text-white rounded-md w-8 h-8 border-none cursor-pointer transition-all shadow-[0_2px_4px_rgba(0,0,0,0.2)]"
                        (click)="saveCell(init)"
                      >
                        <span class="pi pi-save"></span>
                      </button>
                      <button
                        [aria-label]="'Eliminar iniciativa ' + init.initiative_title"
                        class="flex items-center justify-center bg-[#ef4444] hover:bg-[#dc2626] text-white rounded-md w-8 h-8 border-none cursor-pointer transition-all shadow-[0_2px_4px_rgba(0,0,0,0.2)]"
                        (click)="deleteInitiative(init)"
                      >
                        <span class="pi pi-trash"></span>
                      </button>
                    </div>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }

      <!-- PREVIEW MODAL -->
      @if (isModalOpen()) {
        <div
          class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-[rgba(0,0,0,0.8)] backdrop-blur-md">
          <div
            class="bg-[#0b0f19] border border-[rgba(255,255,255,0.08)] rounded-xl max-w-3xl w-full max-h-[80vh] flex flex-col shadow-2xl">
            <!-- Modal Header -->
            <div
              class="p-5 border-b border-[rgba(255,255,255,0.08)] flex justify-between items-center bg-[rgba(17,24,39,0.5)]">
              <div>
                <h3 class="text-base font-bold text-[#f2c94c] flex items-center gap-2">
                  <span class="pi pi-eye"></span>
                  Previsualización de Fragmentos Vectorizados
                </h3>
                <p class="text-xs text-[#9ca3af] mt-1 leading-snug truncate max-w-[500px]">
                  Iniciativa: <span
                  class="text-[#f3f4f6] font-medium">{{ selectedInitiativeTitle() }}</span>
                </p>
              </div>
              <button
                [aria-label]="'Cerrar modal'"
                class="bg-transparent border-none text-[#9ca3af] hover:text-[#f3f4f6]"
                (click)="closeModal()"
              >
                <span class="pi pi-times text-lg"></span>
              </button>
            </div>

            <!-- Modal Content Chunks list -->
            <div class="p-6 overflow-y-auto flex-1 flex flex-col gap-4">
              @if (loadingChunks()) {
                <div class="flex-1 flex flex-col items-center justify-center gap-2 py-12">
                  <div
                    class="w-8 h-8 border-4 border-[#f2c94c] border-t-transparent rounded-full animate-spin"></div>
                  <span class="text-xs text-[#9ca3af]">Cargando fragmentos vectorizados de base de datos...</span>
                </div>
              } @else if (selectedChunks().length === 0) {
                <div class="text-center py-12 text-[#9ca3af] text-sm">
                  No se encontraron fragmentos vectorizados para esta iniciativa.
                </div>
              } @else {
                <div class="flex flex-col gap-4">
                  @for (chunk of selectedChunks(); track chunk.id; let idx = $index) {
                    <div
                      class="bg-[rgba(17,24,39,0.4)] border border-[rgba(255,255,255,0.06)] rounded-lg p-4 flex flex-col gap-2">
                      <div
                        class="flex justify-between items-center border-b border-[rgba(255,255,255,0.04)] pb-2 mb-1">
                        <span class="text-xs font-bold text-[#f2c94c] flex items-center gap-1.5">
                          <span class="pi pi-paperclip"></span> Bloque {{ idx + 1 }}
                        </span>
                        <span
                          class="text-[0.65rem] text-[#9ca3af] px-2 py-0.5 rounded-full bg-[rgba(255,255,255,0.05)] border border-[rgba(255,255,255,0.08)]">
                          Sección: {{ chunk.sectionName || 'N/A' }}
                        </span>
                      </div>
                      <p
                        class="text-xs text-[#d1d5db] leading-relaxed whitespace-pre-line text-left">
                        {{ chunk.content }}
                      </p>
                    </div>
                  }
                </div>
              }
            </div>

            <!-- Modal Footer -->
            <div
              class="p-4 border-t border-[rgba(255,255,255,0.08)] flex justify-end bg-[rgba(17,24,39,0.5)]">
              <button
                [aria-label]="'Cerrar previsualización'"
                class="px-5 py-2 rounded-lg bg-[rgba(255,255,255,0.08)] hover:bg-[rgba(255,255,255,0.12)] border border-[rgba(255,255,255,0.08)] text-[#f3f4f6] text-xs font-bold transition-all cursor-pointer"
                (click)="closeModal()"
              >
                Cerrar Previsualización
              </button>
            </div>
          </div>
        </div>
      }
    </div>
  `
})
export class PlanningManagementComponent implements OnInit, OnDestroy {
  protected readonly state = inject(DevopsAgentStateService);
  protected isModalOpen = signal<boolean>(false);
  private sub: Subscription | null = null;
  private loadingSub: Subscription | null = null;
  protected selectedChunks = signal<any[]>([]);
  protected loadingChunks = signal<boolean>(false);
  protected selectedInitiativeTitle = signal<string>('');
  private readonly api = inject(DevopsAgentApiService);

  protected editableInitiatives: {
    initiative_id: string;
    initiative_title: string;
    cell: string;
    tempCell: string;
  }[] = [];

  protected loading = signal<boolean>(false);

  public ngOnInit(): void {
    this.state.loadInitiatives();

    this.loadingSub = this.state.loading.subscribe(isLoading => {
      this.loading.set(isLoading);
    });

    this.sub = this.state.initiatives.subscribe(initiatives => {
      this.editableInitiatives = initiatives.map(init => ({
        initiative_id: init.initiative_id,
        initiative_title: init.initiative_title,
        cell: init.cell ?? '',
        tempCell: init.cell ?? ''
      }));
    });
  }

  public ngOnDestroy(): void {
    if (this.sub) {
      this.sub.unsubscribe();
    }
    if (this.loadingSub) {
      this.loadingSub.unsubscribe();
    }
  }

  protected saveCell(init: { initiative_id: string; tempCell: string }): void {
    this.state.updateInitiativeCell(init.initiative_id, init.tempCell.trim());
  }

  protected deleteInitiative(init: { initiative_id: string; initiative_title: string }): void {
    if (confirm(`¿Estás seguro de que deseas eliminar la iniciativa "${init.initiative_title}" y todas sus planeaciones vectorizadas? Esta acción no se puede deshacer.`)) {
      this.state.deleteInitiative(init.initiative_id);
    }
  }

  protected previewInitiative(init: { initiative_id: string; initiative_title: string }): void {
    this.selectedInitiativeTitle.set(init.initiative_title);
    this.isModalOpen.set(true);
    this.loadingChunks.set(true);
    this.selectedChunks.set([]);

    this.api.getInitiativeChunks(init.initiative_id).subscribe({
      next: (chunks) => {
        this.selectedChunks.set(chunks || []);
        this.loadingChunks.set(false);
      },
      error: (error) => {
        console.error('Error al previsualizar iniciativa', error);
        this.loadingChunks.set(false);
      }
    });
  }

  protected closeModal(): void {
    this.isModalOpen.set(false);
    this.selectedChunks.set([]);
  }
}
