import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  OnInit,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PlanningStateService } from '../../services/state/planning-state.service';
import { RefinementChatStateService } from '../../services/state/refinement-chat-state.service';
import { SpecDocumentDTO } from '../../models/devops-agent.model';
import { MarkdownParserPipe } from '../../../../shared/pipes/markdown-parser.pipe';
import { NotificationService } from '../../../../core';

@Component({
  selector: 'app-planning-specs-explorer',
  standalone: true,
  imports: [CommonModule, FormsModule, MarkdownParserPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="h-full w-full flex overflow-hidden bg-transparent font-sans">
      <!-- PANEL LATERAL: SELECTOR DE SPECS -->
      <aside
        class="w-80 shrink-0 bg-[rgba(17,24,39,0.7)] border-r border-[rgba(255,255,255,0.08)] backdrop-blur-md flex flex-col h-full"
      >
        <!-- CABECERA DEL PANEL -->
        <div class="p-4 border-b border-[rgba(255,255,255,0.08)] flex items-center justify-between">
          <div class="flex items-center gap-2">
            <span class="pi pi-folder text-[#f2c94c] text-base"></span>
            <h3 class="text-sm font-semibold text-[#f3f4f6]">Especificaciones</h3>
          </div>
          <button
            type="button"
            aria-label="Recargar catálogo de especificaciones"
            class="bg-transparent border-none text-[#9ca3af] hover:text-[#f3f4f6] cursor-pointer p-1 rounded transition-colors"
            (click)="refreshSpecs()"
          >
            <span
              class="pi pi-refresh text-xs"
              [class.animate-spin]="planningState.loadingSpecsSignal()"
            ></span>
          </button>
        </div>

        <!-- BUSCADOR -->
        <div class="p-3 border-b border-[rgba(255,255,255,0.06)]">
          <div
            class="flex items-center gap-2 bg-[rgba(11,15,25,0.6)] border border-[rgba(255,255,255,0.08)] rounded-lg px-2.5 py-1.5 focus-within:border-[#f2c94c] transition-colors"
          >
            <span class="pi pi-search text-xs text-[#9ca3af]"></span>
            <input
              type="text"
              aria-label="Buscar especificación"
              placeholder="Buscar documento..."
              [(ngModel)]="filterText"
              class="bg-transparent border-none text-xs text-[#f3f4f6] placeholder-[#6b7280] outline-none w-full"
            />
            @if (filterText()) {
              <button
                type="button"
                aria-label="Limpiar filtro de búsqueda"
                (click)="filterText.set('')"
                class="bg-transparent border-none text-[#9ca3af] hover:text-[#f3f4f6] p-0 cursor-pointer"
              >
                <span class="pi pi-times text-[0.65rem]"></span>
              </button>
            }
          </div>
        </div>

        <!-- LISTA DE ESPECIFICACIONES -->
        <div class="flex-1 overflow-y-auto p-3 flex flex-col gap-2">
          @if (
            planningState.loadingSpecsSignal() && planningState.availableSpecsSignal().length === 0
          ) {
            <div class="flex flex-col items-center justify-center py-12 text-[#9ca3af] gap-2">
              <span class="pi pi-spin pi-spinner text-xl text-[#f2c94c]"></span>
              <span class="text-xs">Cargando especificaciones...</span>
            </div>
          } @else if (filteredSpecs().length === 0) {
            <div
              class="flex flex-col items-center justify-center py-12 text-[#9ca3af] text-center px-4"
            >
              <span class="pi pi-inbox text-2xl mb-2 text-slate-600"></span>
              <p class="text-xs font-medium">No se encontraron especificaciones</p>
              <p class="text-[0.7rem] text-[#6b7280] mt-1">
                Genera una planeación para visualizar las especificaciones del trimestre.
              </p>
            </div>
          } @else {
            @for (spec of filteredSpecs(); track spec) {
              <button
                type="button"
                [attr.aria-label]="'Seleccionar especificación ' + spec"
                (click)="selectSpec(spec)"
                [class.bg-[rgba(242,201,76,0.12)]]="
                  planningState.selectedSpecSignal()?.name === spec
                "
                [class.border-[#f2c94c]]="planningState.selectedSpecSignal()?.name === spec"
                [class.shadow-[0_0_12px_rgba(242,201,76,0.15)]]="
                  planningState.selectedSpecSignal()?.name === spec
                "
                [class.bg-[rgba(255,255,255,0.02)]]="
                  planningState.selectedSpecSignal()?.name !== spec
                "
                [class.border-[rgba(255,255,255,0.06)]]="
                  planningState.selectedSpecSignal()?.name !== spec
                "
                class="w-full text-left p-3 rounded-xl border transition-all cursor-pointer flex flex-col gap-1.5 hover:border-[rgba(255,255,255,0.15)] hover:bg-[rgba(255,255,255,0.04)]"
              >
                <div class="flex items-center justify-between gap-2">
                  <span
                    class="text-xs font-semibold truncate"
                    [class.text-[#f2c94c]]="planningState.selectedSpecSignal()?.name === spec"
                    [class.text-[#f3f4f6]]="planningState.selectedSpecSignal()?.name !== spec"
                  >
                    {{ formatSpecTitle(spec) }}
                  </span>
                  <span
                    class="text-[0.65rem] px-1.5 py-0.5 rounded font-medium shrink-0 uppercase tracking-wide"
                    [ngClass]="getBadgeClass(spec)"
                  >
                    {{ formatCategoryLabel(spec) }}
                  </span>
                </div>
                <span class="font-mono text-[0.68rem] text-[#9ca3af] truncate">
                  {{ spec }}
                </span>
              </button>
            }
          }
        </div>
      </aside>

      <!-- PANEL PRINCIPAL: VISOR DOCUMENTAL -->
      <main class="flex-1 flex flex-col min-w-0 h-full overflow-hidden bg-transparent">
        @if (planningState.loadingSpecsSignal() && !planningState.selectedSpecSignal()) {
          <div class="flex-1 flex flex-col items-center justify-center text-[#9ca3af] gap-3">
            <span class="pi pi-spin pi-spinner text-3xl text-[#f2c94c]"></span>
            <p class="text-sm font-medium">Cargando contenido de la especificación...</p>
          </div>
        } @else if (planningState.selectedSpecSignal(); as doc) {
          <!-- BARRA SUPERIOR DE HERRAMIENTAS -->
          <header
            class="h-16 px-6 border-b border-[rgba(255,255,255,0.08)] bg-[rgba(11,15,25,0.6)] backdrop-blur-md flex items-center justify-between gap-4 shrink-0 z-10"
          >
            <div class="flex items-center gap-3 min-w-0">
              <div
                class="w-8 h-8 rounded-lg flex items-center justify-center shrink-0"
                [ngClass]="
                  isMasterSpec(doc.name)
                    ? 'bg-[#f2c94c]/15 text-[#f2c94c]'
                    : 'bg-[#2563eb]/15 text-[#60a5fa]'
                "
              >
                <span
                  class="pi text-sm"
                  [ngClass]="isMasterSpec(doc.name) ? 'pi-compass' : 'pi-box'"
                ></span>
              </div>
              <div class="min-w-0">
                <div class="flex items-center gap-2">
                  <h2 class="text-sm font-bold text-[#f3f4f6] truncate">
                    {{ formatSpecTitle(doc.name) }}
                  </h2>
                  <span
                    class="text-[0.65rem] px-2 py-0.5 rounded font-semibold uppercase tracking-wider"
                    [ngClass]="getBadgeClass(doc.name)"
                  >
                    {{ formatCategoryLabel(doc.name) }}
                  </span>
                </div>
                <p class="text-[0.7rem] text-[#9ca3af] font-mono truncate">
                  {{ doc.path || doc.name }}
                </p>
              </div>
            </div>

            <!-- ACCIONES DEL VISOR -->
            <div class="flex items-center gap-2.5 shrink-0">
              @if (isFrontSpec(doc.name)) {
                <button
                  type="button"
                  aria-label="Refinar HU en este Frente"
                  (click)="refineFront(doc)"
                  class="bg-[#f2c94c] hover:bg-[#e0b83b] text-black font-semibold px-3.5 py-1.5 rounded-lg text-xs flex items-center gap-1.5 cursor-pointer transition-all shadow-[0_0_15px_rgba(242,201,76,0.25)] hover:shadow-[0_0_20px_rgba(242,201,76,0.4)]"
                >
                  <span class="pi pi-sparkles text-xs"></span>
                  <span>Refinar HU en este Frente</span>
                </button>
              }

              <button
                type="button"
                aria-label="Copiar contenido Markdown"
                (click)="copyMarkdown(doc.content)"
                class="bg-[rgba(255,255,255,0.04)] hover:bg-[rgba(255,255,255,0.08)] border border-[rgba(255,255,255,0.08)] text-[#f3f4f6] px-3 py-1.5 rounded-lg text-xs flex items-center gap-1.5 cursor-pointer transition-colors"
              >
                <span
                  class="pi text-xs"
                  [ngClass]="copied() ? 'pi-check text-[#10b981]' : 'pi-copy text-[#9ca3af]'"
                ></span>
                <span>{{ copied() ? '¡Copiado!' : 'Copiar' }}</span>
              </button>

              <button
                type="button"
                aria-label="Descargar archivo Markdown"
                (click)="downloadMarkdown(doc)"
                class="bg-[rgba(255,255,255,0.04)] hover:bg-[rgba(255,255,255,0.08)] border border-[rgba(255,255,255,0.08)] text-[#f3f4f6] px-3 py-1.5 rounded-lg text-xs flex items-center gap-1.5 cursor-pointer transition-colors"
              >
                <span class="pi pi-download text-xs text-[#9ca3af]"></span>
                <span>Descargar</span>
              </button>
            </div>
          </header>

          <!-- ÁREA DE CONTENIDO MARKDOWN -->
          <div class="flex-1 overflow-y-auto p-6 md:p-8 bg-transparent">
            <article
              class="max-w-4xl mx-auto bg-[rgba(17,24,39,0.4)] border border-[rgba(255,255,255,0.06)] rounded-2xl p-6 md:p-8 shadow-2xl backdrop-blur-md"
            >
              <div
                class="markdown-body text-slate-200 text-xs md:text-sm leading-relaxed font-sans"
                [innerHTML]="doc.content | markdownParser"
              ></div>
            </article>
          </div>
        } @else {
          <!-- ESTADO VACÍO -->
          <div
            class="flex-1 flex flex-col items-center justify-center text-center p-8 text-[#9ca3af]"
          >
            <div
              class="w-16 h-16 rounded-2xl bg-[rgba(255,255,255,0.02)] border border-[rgba(255,255,255,0.08)] flex items-center justify-center mb-4 text-slate-500"
            >
              <span class="pi pi-file text-2xl"></span>
            </div>
            <h3 class="text-base font-semibold text-[#f3f4f6] mb-1">
              Selecciona una especificación documental
            </h3>
            <p class="text-xs text-[#9ca3af] max-w-sm">
              Selecciona un documento en el panel lateral para explorar el roadmap macro o el
              desglose técnico de cada frente.
            </p>
          </div>
        }
      </main>
    </div>
  `,
})
export class PlanningSpecsExplorerComponent implements OnInit {
  public readonly refineRequested = output<string>();
  protected readonly planningState = inject(PlanningStateService);
  protected readonly refinementChatState = inject(RefinementChatStateService);
  protected readonly filterText = signal<string>('');
  protected readonly copied = signal<boolean>(false);
  protected readonly filteredSpecs = computed<string[]>(() => {
    const specs = this.planningState.availableSpecsSignal();
    const filter = this.filterText().trim().toLowerCase();
    if (!filter) {
      return specs;
    }
    return specs.filter((s) => s.toLowerCase().includes(filter));
  });
  private readonly notifications = inject(NotificationService);

  constructor() {
    effect(() => {
      const specs = this.planningState.availableSpecsSignal();
      const current = this.planningState.selectedSpecSignal();
      if (specs.length > 0 && !current) {
        const masterSpec = specs.find((s) => s.startsWith('ideas_planning_'));
        const target = masterSpec || specs[0];
        this.planningState.selectSpec(target);
      }
    });
  }

  public ngOnInit(): void {
    this.planningState.loadAvailableSpecs();
  }

  public refreshSpecs(): void {
    this.planningState.loadAvailableSpecs();
  }

  public selectSpec(name: string): void {
    this.planningState.selectSpec(name);
  }

  public isMasterSpec(name: string): boolean {
    return name.startsWith('ideas_planning_');
  }

  public isFrontSpec(name: string): boolean {
    return name.startsWith('frente_');
  }

  public formatSpecTitle(name: string): string {
    const clean = name.replace(/\.md$/i, '');
    if (clean.startsWith('ideas_planning_')) {
      const quarter = clean.replace('ideas_planning_', '').toUpperCase();
      return `Roadmap Maestro (${quarter})`;
    }
    if (clean.startsWith('frente_')) {
      const front = clean.replace('frente_', '');
      return `Frente ${front.charAt(0).toUpperCase() + front.slice(1)}`;
    }
    return clean;
  }

  public formatCategoryLabel(name: string): string {
    if (this.isMasterSpec(name)) {
      return 'Maestro';
    }
    if (this.isFrontSpec(name)) {
      return 'Frente';
    }
    return 'Doc';
  }

  public getBadgeClass(name: string): string {
    if (this.isMasterSpec(name)) {
      return 'bg-[rgba(242,201,76,0.15)] text-[#f2c94c] border border-[rgba(242,201,76,0.3)]';
    }
    if (this.isFrontSpec(name)) {
      return 'bg-[rgba(37,99,235,0.15)] text-[#60a5fa] border border-[rgba(37,99,235,0.3)]';
    }
    return 'bg-[rgba(156,163,175,0.15)] text-[#9ca3af] border border-[rgba(156,163,175,0.3)]';
  }

  public refineFront(doc: SpecDocumentDTO): void {
    const frontTitle = this.formatSpecTitle(doc.name);
    const prompt = `Por favor refina las Historias de Usuario para el ${frontTitle} basadas en la especificación ${doc.name}.`;
    this.refinementChatState.prefillPrompt(prompt);
    this.refineRequested.emit(prompt);
    this.notifications.info(`Frente ${frontTitle} cargado para refinamiento`);
  }

  public copyMarkdown(content: string): void {
    if (!content) {
      return;
    }
    if (typeof navigator !== 'undefined' && navigator.clipboard) {
      navigator.clipboard.writeText(content).then(() => {
        this.copied.set(true);
        this.notifications.success('Especificación copiada al portapapeles');
        setTimeout(() => this.copied.set(false), 2000);
      });
    }
  }

  public downloadMarkdown(doc: SpecDocumentDTO): void {
    if (!doc.content) {
      return;
    }
    const blob = new Blob([doc.content], { type: 'text/markdown;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = doc.name.endsWith('.md') ? doc.name : `${doc.name}.md`;
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
    URL.revokeObjectURL(url);
    this.notifications.success(`Archivo ${anchor.download} descargado`);
  }
}
