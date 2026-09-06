import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  EventEmitter,
  HostListener,
  inject,
  Input,
  OnInit,
  Output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { PlanningStateService } from '../../services/state/planning-state.service';
import { NotificationService } from '../../../../core';
import { ProgramPlanRequestDTO } from '../../models/devops-agent.model';

export const DEFAULT_TECHNICAL_FRONTS: readonly string[] = [
  'Canales',
  'Core',
  'DevOps',
  'Seguridad',
  'Datos',
  'Arquitectura',
];

@Component({
  selector: 'app-program-planning-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (visible) {
      <div
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4 overflow-y-auto"
        role="dialog"
        aria-modal="true"
        aria-labelledby="program-planning-modal-title"
        (click)="onBackdropClick($event)"
      >
        <div
          class="relative w-full max-w-2xl bg-[#0e131f] border border-[rgba(255,255,255,0.12)] rounded-2xl shadow-2xl overflow-hidden flex flex-col my-auto transition-all"
          (click)="$event.stopPropagation()"
        >
          <!-- CABECERA DEL DIÁLOGO -->
          <div
            class="px-6 py-4 border-b border-[rgba(255,255,255,0.08)] bg-[rgba(17,24,39,0.5)] flex items-center justify-between"
          >
            <div class="flex items-center gap-3">
              <div
                class="w-8 h-8 rounded-lg bg-[rgba(242,201,76,0.15)] border border-[rgba(242,201,76,0.3)] flex items-center justify-center text-[#f2c94c]"
              >
                <span class="pi pi-compass text-base"></span>
              </div>
              <div>
                <h2
                  id="program-planning-modal-title"
                  class="text-base font-semibold text-[#f3f4f6] tracking-wide m-0"
                >
                  Lanzar Program Planning
                </h2>
                <p class="text-xs text-[#9ca3af] m-0">
                  Configura y despacha la planeación macro del programa trimestral
                </p>
              </div>
            </div>

            <button
              type="button"
              aria-label="Cerrar modal de planeación"
              [disabled]="planningState.planningRunningSignal()"
              (click)="close()"
              class="text-[#9ca3af] hover:text-[#f3f4f6] bg-transparent border-none cursor-pointer p-1.5 rounded-lg transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
            >
              <span class="pi pi-times text-sm"></span>
            </button>
          </div>

          <!-- CUERPO DEL FORMULARIO -->
          <form [formGroup]="form" (ngSubmit)="submit()" class="p-6 flex flex-col gap-5">
            <!-- FILA 1: QUARTER, SPRINTS Y CAPACIDAD -->
            <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
              <!-- QUARTER -->
              <div class="flex flex-col gap-1.5">
                <label
                  for="quarter-input"
                  class="text-xs font-semibold text-[#d1d5db] flex items-center gap-1"
                >
                  <span>Quarter</span>
                  <span class="text-[#f87171]">*</span>
                </label>
                <input
                  id="quarter-input"
                  type="text"
                  formControlName="quarter"
                  placeholder="ej. Q3-2026"
                  aria-label="Quarter en formato Q1 a Q4 guión año"
                  aria-describedby="quarter-hint quarter-error"
                  class="bg-[rgba(255,255,255,0.05)] border border-[rgba(255,255,255,0.12)] rounded-lg px-3 py-2 text-xs text-[#f3f4f6] outline-none transition-colors focus:border-[#f2c94c]"
                  [class.border-[#f87171]]="isFieldInvalid('quarter')"
                />
                <span id="quarter-hint" class="text-[0.65rem] text-[#9ca3af]"
                  >Formato: Q1-2026 a Q4-2099</span
                >
                @if (isFieldInvalid('quarter')) {
                  <span id="quarter-error" class="text-[0.65rem] text-[#f87171]">
                    Quarter inválido. Usa el formato Q[1-4]-YYYY (ej. Q3-2026).
                  </span>
                }
              </div>

              <!-- SPRINTS -->
              <div class="flex flex-col gap-1.5">
                <label
                  for="sprint-count-input"
                  class="text-xs font-semibold text-[#d1d5db] flex items-center gap-1"
                >
                  <span>Sprints</span>
                  <span class="text-[#f87171]">*</span>
                </label>
                <input
                  id="sprint-count-input"
                  type="number"
                  min="1"
                  max="24"
                  formControlName="sprintCount"
                  placeholder="6"
                  aria-label="Número de sprints del trimestre"
                  aria-describedby="sprint-error"
                  class="bg-[rgba(255,255,255,0.05)] border border-[rgba(255,255,255,0.12)] rounded-lg px-3 py-2 text-xs text-[#f3f4f6] outline-none transition-colors focus:border-[#f2c94c]"
                  [class.border-[#f87171]]="isFieldInvalid('sprintCount')"
                />
                <span class="text-[0.65rem] text-[#9ca3af]">Entre 1 y 24 sprints</span>
                @if (isFieldInvalid('sprintCount')) {
                  <span id="sprint-error" class="text-[0.65rem] text-[#f87171]">
                    Debe ser un entero entre 1 y 24.
                  </span>
                }
              </div>

              <!-- CAPACIDAD MÁXIMA -->
              <div class="flex flex-col gap-1.5">
                <label
                  for="capacity-input"
                  class="text-xs font-semibold text-[#d1d5db] flex items-center gap-1"
                >
                  <span>Capacidad Sprint (SP)</span>
                  <span class="text-[#f87171]">*</span>
                </label>
                <input
                  id="capacity-input"
                  type="number"
                  min="1"
                  max="500"
                  formControlName="maxCapacityPerSprint"
                  placeholder="34"
                  aria-label="Capacidad máxima de Story Points por sprint"
                  aria-describedby="capacity-error"
                  class="bg-[rgba(255,255,255,0.05)] border border-[rgba(255,255,255,0.12)] rounded-lg px-3 py-2 text-xs text-[#f3f4f6] outline-none transition-colors focus:border-[#f2c94c]"
                  [class.border-[#f87171]]="isFieldInvalid('maxCapacityPerSprint')"
                />
                <span class="text-[0.65rem] text-[#9ca3af]">Límite por sprint (1 - 500)</span>
                @if (isFieldInvalid('maxCapacityPerSprint')) {
                  <span id="capacity-error" class="text-[0.65rem] text-[#f87171]">
                    Capacidad debe estar entre 1 y 500 SP.
                  </span>
                }
              </div>
            </div>

            <!-- FILA 2: SELECCIÓN DE FRENTES TÉCNICOS -->
            <div class="flex flex-col gap-2">
              <label class="text-xs font-semibold text-[#d1d5db] flex items-center justify-between">
                <span class="flex items-center gap-1">
                  <span>Frentes Técnicos Involucrados</span>
                  <span class="text-[#f87171]">*</span>
                </span>
                <span class="text-[0.65rem] text-[#9ca3af] font-normal">
                  {{ selectedFronts().length }} seleccionado(s)
                </span>
              </label>

              <div class="flex flex-wrap gap-2 items-center">
                @for (front of availableFronts; track front) {
                  <button
                    type="button"
                    [attr.aria-label]="'Alternar frente técnico ' + front"
                    [attr.aria-pressed]="isFrontSelected(front)"
                    (click)="toggleFront(front)"
                    [disabled]="planningState.planningRunningSignal()"
                    class="px-3 py-1.5 rounded-full text-xs font-medium border transition-all cursor-pointer flex items-center gap-1.5 disabled:opacity-40"
                    [class.bg-[rgba(242,201,76,0.15)]]="isFrontSelected(front)"
                    [class.text-[#f2c94c]]="isFrontSelected(front)"
                    [class.border-[#f2c94c]]="isFrontSelected(front)"
                    [class.bg-[rgba(255,255,255,0.04)]]="!isFrontSelected(front)"
                    [class.text-[#9ca3af]]="!isFrontSelected(front)"
                    [class.border-[rgba(255,255,255,0.1)]]="!isFrontSelected(front)"
                    [class.hover:border-[rgba(242,201,76,0.5)]]="!isFrontSelected(front)"
                  >
                    <span
                      class="pi text-[0.65rem]"
                      [class.pi-check]="isFrontSelected(front)"
                      [class.pi-plus]="!isFrontSelected(front)"
                    ></span>
                    <span>{{ front }}</span>
                  </button>
                }
              </div>

              @if (frontsError()) {
                <span class="text-[0.65rem] text-[#f87171]">
                  Debes seleccionar al menos un frente técnico.
                </span>
              }
            </div>

            <!-- FILA 3: OBJETIVOS DE NEGOCIO -->
            <div class="flex flex-col gap-1.5">
              <label
                for="objectives-input"
                class="text-xs font-semibold text-[#d1d5db] flex items-center gap-1"
              >
                <span>Objetivos de Negocio del Trimestre</span>
                <span class="text-[#f87171]">*</span>
              </label>
              <textarea
                id="objectives-input"
                rows="4"
                formControlName="objectives"
                placeholder="Describe los lineamientos estratégicos, epics prioritarias y resultados clave que deberán guiar la descomposición en sprints y frentes..."
                aria-label="Objetivos de negocio y directrices del trimestre"
                aria-describedby="objectives-error"
                class="bg-[rgba(255,255,255,0.05)] border border-[rgba(255,255,255,0.12)] rounded-lg p-3 text-xs text-[#f3f4f6] outline-none transition-colors focus:border-[#f2c94c] resize-y min-h-[90px]"
                [class.border-[#f87171]]="isFieldInvalid('objectives')"
              ></textarea>
              @if (isFieldInvalid('objectives')) {
                <span id="objectives-error" class="text-[0.65rem] text-[#f87171]">
                  Los objetivos del trimestre son obligatorios (mínimo 5 caracteres).
                </span>
              }
            </div>

            <!-- ACCIONES DEL FORMULARIO -->
            <div
              class="pt-3 border-t border-[rgba(255,255,255,0.08)] flex items-center justify-end gap-3 mt-1"
            >
              <button
                type="button"
                aria-label="Cancelar y cerrar modal"
                [disabled]="planningState.planningRunningSignal()"
                (click)="close()"
                class="px-4 py-2 rounded-lg text-xs font-medium text-[#9ca3af] hover:text-[#f3f4f6] hover:bg-[rgba(255,255,255,0.05)] transition-colors border border-transparent cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
              >
                Cancelar
              </button>

              <button
                type="submit"
                aria-label="Iniciar planeación del programa"
                [disabled]="isSubmitDisabled()"
                class="px-5 py-2 rounded-lg text-xs font-semibold bg-[#f2c94c] text-[#0b0f19] hover:bg-[#dfb73e] transition-all flex items-center gap-2 cursor-pointer shadow-md disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-[#f2c94c]"
              >
                @if (planningState.planningRunningSignal()) {
                  <span class="pi pi-spin pi-spinner text-xs"></span>
                  <span>Planificando Programa...</span>
                } @else {
                  <span class="pi pi-send text-xs"></span>
                  <span>Iniciar Planeación</span>
                }
              </button>
            </div>
          </form>
        </div>
      </div>
    }
  `,
})
export class ProgramPlanningModalComponent implements OnInit {
  @Input() visible = false;
  @Output() visibleChange = new EventEmitter<boolean>();

  public readonly planningState = inject(PlanningStateService);
  public readonly availableFronts: readonly string[] = DEFAULT_TECHNICAL_FRONTS;
  public readonly selectedFronts = signal<string[]>(['Canales', 'Core', 'DevOps']);
  public readonly frontsTouched = signal<boolean>(false);
  private readonly notifications = inject(NotificationService);
  private readonly fb = inject(FormBuilder);
  public readonly form: FormGroup = this.fb.group({
    quarter: ['Q3-2026', [Validators.required, Validators.pattern(/^Q[1-4]-\d{4}$/)]],
    sprintCount: [6, [Validators.required, Validators.min(1), Validators.max(24)]],
    maxCapacityPerSprint: [34, [Validators.required, Validators.min(1), Validators.max(500)]],
    objectives: ['', [Validators.required, Validators.minLength(5)]],
  });
  private readonly destroyRef = inject(DestroyRef);

  public ngOnInit(): void {
    // Si se requiere sincronización o reactividad adicional
  }

  @HostListener('document:keydown.escape')
  public onEscape(): void {
    if (this.visible && !this.planningState.planningRunningSignal()) {
      this.close();
    }
  }

  public onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget && !this.planningState.planningRunningSignal()) {
      this.close();
    }
  }

  public isFieldInvalid(fieldName: string): boolean {
    const control = this.form.get(fieldName);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  public frontsError(): boolean {
    return this.selectedFronts().length === 0 && this.frontsTouched();
  }

  public isFrontSelected(front: string): boolean {
    return this.selectedFronts().includes(front);
  }

  public toggleFront(front: string): void {
    this.frontsTouched.set(true);
    const current = this.selectedFronts();
    if (current.includes(front)) {
      this.selectedFronts.set(current.filter((item) => item !== front));
    } else {
      this.selectedFronts.set([...current, front]);
    }
  }

  public isSubmitDisabled(): boolean {
    return (
      this.form.invalid ||
      this.selectedFronts().length === 0 ||
      this.planningState.planningRunningSignal()
    );
  }

  public close(): void {
    if (this.planningState.planningRunningSignal()) {
      return;
    }
    this.visible = false;
    this.visibleChange.emit(false);
  }

  public submit(): void {
    this.frontsTouched.set(true);
    if (this.isSubmitDisabled()) {
      this.form.markAllAsTouched();
      return;
    }

    const val = this.form.value;
    const request: ProgramPlanRequestDTO = {
      quarter: (val.quarter as string).trim().toUpperCase(),
      sprintCount: Number(val.sprintCount),
      maxCapacityPerSprint: Number(val.maxCapacityPerSprint),
      targetFronts: [...this.selectedFronts()],
      objectives: (val.objectives as string).trim(),
    };

    this.planningState
      .triggerProgramPlanning(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.close();
        },
        error: () => {
          // El error ya es manejado y notificado en PlanningStateService
        },
      });
  }
}
