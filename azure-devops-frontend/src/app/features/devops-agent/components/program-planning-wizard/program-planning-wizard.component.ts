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
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { PlanningStateService } from '../../services/state/planning-state.service';
import { NotificationService } from '../../../../core';
import { MarkdownParserPipe } from '../../../../shared/pipes/markdown-parser.pipe';
import { ProgramPlanRequestDTO } from '../../models/devops-agent.model';

export interface FrontDefinition {
  readonly id: string;
  readonly label: string;
  readonly description: string;
  readonly icon: string;
  readonly defaultPoints: number;
}

export const AVAILABLE_FRONTS: readonly FrontDefinition[] = [
  { id: 'Canales', label: 'Canales (Frontend)', description: 'Web, Mobile, UX/UI y micro-frontends', icon: 'pi-desktop', defaultPoints: 34 },
  { id: 'BFF', label: 'BFF (Spring Boot)', description: 'Backend for Frontend, orquestación REST y DTOs', icon: 'pi-server', defaultPoints: 26 },
  { id: 'Core', label: 'Core / Reglas', description: 'Motor de negocio, lógica transaccional y domain use cases', icon: 'pi-cog', defaultPoints: 42 },
  { id: 'Broker', label: 'Broker / Kafka', description: 'Tópicos de eventos, esquemas Avro/JSON y resiliencia', icon: 'pi-share-alt', defaultPoints: 16 },
  { id: 'Datos', label: 'Bases de Datos', description: 'PostgreSQL, DynamoDB y búsquedas semánticas pgvector', icon: 'pi-database', defaultPoints: 22 },
  { id: 'Seguridad', label: 'Ciberseguridad', description: 'Entra ID, RBAC, auditoría y análisis de vulnerabilidades', icon: 'pi-shield', defaultPoints: 18 },
  { id: 'DevOps', label: 'DevOps & Cloud', description: 'Pipelines CI/CD, Azure Repos, CMDB y aprovisionamiento', icon: 'pi-cloud', defaultPoints: 24 },
  { id: 'Arquitectura', label: 'Arquitectura & QA', description: 'Gobierno de estándares, pruebas Karate/Playwright y HyMS', icon: 'pi-check-circle', defaultPoints: 20 },
];

export interface CapacityDiagnosis {
  readonly totalStoryPoints: number;
  readonly recommendedCapacityPerSprint: number;
  readonly effectiveDevSprints: number;
  readonly bufferSprints: number;
  readonly viabilityScore: number;
  readonly frontsBreakdown: readonly { front: string; points: number; percentage: number }[];
  readonly advice: string;
}

export interface CopilotMessage {
  readonly sender: 'agent' | 'user';
  readonly text: string;
  readonly timestamp: string;
}

@Component({
  selector: 'app-program-planning-wizard',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, MarkdownParserPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (visible) {
      <div
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-md p-3 md:p-6 overflow-y-auto"
        role="dialog"
        aria-modal="true"
        aria-labelledby="wizard-modal-title"
        (click)="onBackdropClick($event)"
      >
        <div
          class="relative w-full max-w-5xl bg-[#0e131f] border border-[rgba(255,255,255,0.12)] rounded-2xl shadow-2xl overflow-hidden flex flex-col my-auto transition-all max-h-[92vh]"
          (click)="$event.stopPropagation()"
        >
          <!-- CABECERA DEL WIZARD: ROL Y PASOS -->
          <header
            class="px-6 py-4 border-b border-[rgba(255,255,255,0.08)] bg-[rgba(17,24,39,0.7)] flex flex-col md:flex-row md:items-center justify-between gap-4 shrink-0"
          >
            <div class="flex items-center gap-3">
              <div
                class="w-10 h-10 rounded-xl bg-[rgba(242,201,76,0.15)] border border-[rgba(242,201,76,0.3)] flex items-center justify-center text-[#f2c94c] shadow-[0_0_15px_rgba(242,201,76,0.2)]"
              >
                <span class="pi pi-compass text-lg"></span>
              </div>
              <div>
                <div class="flex items-center gap-2">
                  <h2
                    id="wizard-modal-title"
                    class="text-base font-semibold text-[#f3f4f6] tracking-wide m-0"
                  >
                    Asistente de Planeación de Iniciativa
                  </h2>
                  <span
                    class="px-2 py-0.5 rounded-full text-[0.65rem] font-bold bg-[rgba(242,201,76,0.2)] text-[#f2c94c] border border-[rgba(242,201,76,0.4)]"
                  >
                    Scrum Master & PO AI
                  </span>
                </div>
                <p class="text-xs text-[#9ca3af] m-0">
                  Extracción asistida de requerimientos, dimensionamiento de capacidad y generación de SPEC
                </p>
              </div>
            </div>

            <!-- STEP INDICATOR -->
            <div class="flex items-center gap-2">
              @for (step of steps; track step.number) {
                <button
                  type="button"
                  [attr.aria-label]="'Ir al paso ' + step.number + ': ' + step.title"
                  (click)="goToStep(step.number)"
                  [disabled]="step.number > maxVisitedStep()"
                  class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-all border-none cursor-pointer disabled:opacity-30 disabled:cursor-not-allowed"
                  [class.bg-[#f2c94c]]="currentStep() === step.number"
                  [class.text-[#0b0f19]]="currentStep() === step.number"
                  [class.font-bold]="currentStep() === step.number"
                  [class.shadow-md]="currentStep() === step.number"
                  [class.bg-[rgba(255,255,255,0.04)]]="currentStep() !== step.number"
                  [class.text-[#9ca3af]]="currentStep() !== step.number"
                >
                  <span
                    class="w-4 h-4 rounded-full text-[0.6rem] flex items-center justify-center"
                    [class.bg-[#0b0f19]]="currentStep() === step.number"
                    [class.text-[#f2c94c]]="currentStep() === step.number"
                    [class.bg-[rgba(255,255,255,0.1)]]="currentStep() !== step.number"
                    [class.text-[#f3f4f6]]="currentStep() !== step.number"
                  >
                    {{ step.number }}
                  </span>
                  <span class="hidden sm:inline">{{ step.shortTitle }}</span>
                </button>
              }

              <button
                type="button"
                aria-label="Cerrar asistente de planeación"
                [disabled]="planningState.planningRunningSignal()"
                (click)="close()"
                class="ml-2 text-[#9ca3af] hover:text-[#f3f4f6] bg-transparent border-none cursor-pointer p-1.5 rounded-lg transition-colors disabled:opacity-40"
              >
                <span class="pi pi-times text-sm"></span>
              </button>
            </div>
          </header>

          <!-- CONTENIDO DINÁMICO POR PASO -->
          <div class="flex-1 overflow-y-auto p-6 md:p-8 flex flex-col min-h-0 bg-transparent">
            <!-- ================================================================= -->
            <!-- PASO 1: INICIATIVA & VISIÓN DE NEGOCIO (PO) -->
            <!-- ================================================================= -->
            @if (currentStep() === 1) {
              <section class="flex flex-col gap-6 max-w-3xl mx-auto w-full">
                <div class="flex items-start gap-4 p-4 rounded-xl bg-[rgba(242,201,76,0.05)] border border-[rgba(242,201,76,0.15)]">
                  <div class="w-8 h-8 rounded-lg bg-[rgba(242,201,76,0.2)] text-[#f2c94c] flex items-center justify-center shrink-0">
                    <span class="pi pi-lightbulb text-base"></span>
                  </div>
                  <div>
                    <h3 class="text-sm font-semibold text-[#f2c94c] m-0">Paso 1: Iniciativa y Valor de Negocio</h3>
                    <p class="text-xs text-[#d1d5db] mt-1 mb-0 leading-relaxed">
                      Como Product Owner, describe el problema central y la oportunidad de valor. No te preocupes por la capacidad técnica en Story Points; el agente Scrum Master la calculará por ti en los siguientes pasos.
                    </p>
                  </div>
                </div>

                <form [formGroup]="initiativeForm" class="flex flex-col gap-5">
                  <div class="flex flex-col gap-1.5">
                    <label for="initiative-title" class="text-xs font-semibold text-[#d1d5db] flex items-center gap-1">
                      <span>Nombre de la Iniciativa / Proyecto</span>
                      <span class="text-[#f87171]">*</span>
                    </label>
                    <input
                      id="initiative-title"
                      type="text"
                      formControlName="title"
                      placeholder="ej. Guardián de la Experiencia (Monitoreo Inteligente)"
                      class="bg-[rgba(255,255,255,0.05)] border border-[rgba(255,255,255,0.12)] rounded-lg px-3.5 py-2.5 text-xs text-[#f3f4f6] outline-none transition-colors focus:border-[#f2c94c]"
                      [class.border-[#f87171]]="isFieldInvalid('title', initiativeForm)"
                    />
                    <span class="text-[0.65rem] text-[#9ca3af]">Define un título representativo de la meta trimestral.</span>
                  </div>

                  <div class="flex flex-col gap-1.5">
                    <label for="initiative-problem" class="text-xs font-semibold text-[#d1d5db] flex items-center gap-1">
                      <span>Problema a Resolver y Valor Esperado</span>
                      <span class="text-[#f87171]">*</span>
                    </label>
                    <textarea
                      id="initiative-problem"
                      rows="4"
                      formControlName="problemAndValue"
                      placeholder="Describe qué necesidad de los usuarios u operación se atiende, qué dolores resuelve y qué impacto positivo generará..."
                      class="bg-[rgba(255,255,255,0.05)] border border-[rgba(255,255,255,0.12)] rounded-lg p-3 text-xs text-[#f3f4f6] outline-none transition-colors focus:border-[#f2c94c] resize-y min-h-[90px]"
                      [class.border-[#f87171]]="isFieldInvalid('problemAndValue', initiativeForm)"
                    ></textarea>
                    <span class="text-[0.65rem] text-[#9ca3af]">Mínimo 10 caracteres explicando el contexto y metas de negocio.</span>
                  </div>

                  <div class="flex flex-col gap-1.5">
                    <label for="initiative-users" class="text-xs font-semibold text-[#d1d5db] flex items-center gap-1">
                      <span>Usuarios o Canales Impactados</span>
                    </label>
                    <input
                      id="initiative-users"
                      type="text"
                      formControlName="targetUsers"
                      placeholder="ej. Clientes App Personas, Equipos de Monitoreo y Soporte Operativo"
                      class="bg-[rgba(255,255,255,0.05)] border border-[rgba(255,255,255,0.12)] rounded-lg px-3.5 py-2.5 text-xs text-[#f3f4f6] outline-none transition-colors focus:border-[#f2c94c]"
                    />
                    <span class="text-[0.65rem] text-[#9ca3af]">Indica quiénes perciben el valor o qué aplicaciones se integran.</span>
                  </div>
                </form>
              </section>
            }

            <!-- ================================================================= -->
            <!-- PASO 2: ECOSISTEMA & FRENTES TÉCNICOS -->
            <!-- ================================================================= -->
            @if (currentStep() === 2) {
              <section class="flex flex-col gap-6 max-w-4xl mx-auto w-full">
                <div class="flex items-start gap-4 p-4 rounded-xl bg-[rgba(37,99,235,0.08)] border border-[rgba(37,99,235,0.2)]">
                  <div class="w-8 h-8 rounded-lg bg-[rgba(37,99,235,0.2)] text-[#60a5fa] flex items-center justify-center shrink-0">
                    <span class="pi pi-sitemap text-base"></span>
                  </div>
                  <div>
                    <h3 class="text-sm font-semibold text-[#60a5fa] m-0">Paso 2: Ecosistema y Frentes Involucrados</h3>
                    <p class="text-xs text-[#d1d5db] mt-1 mb-0 leading-relaxed">
                      Elige las capas de arquitectura y disciplinas técnicas requeridas. Cada frente aportará su propio perfil de complejidad para la estimación de Story Points.
                    </p>
                  </div>
                </div>

                <div class="grid grid-cols-1 md:grid-cols-2 gap-3.5">
                  @for (front of availableFronts; track front.id) {
                    <div
                      (click)="toggleFront(front.id)"
                      class="p-4 rounded-xl border transition-all cursor-pointer flex items-start gap-3 select-none"
                      [class.bg-[rgba(242,201,76,0.1)]]="isFrontSelected(front.id)"
                      [class.border-[#f2c94c]]="isFrontSelected(front.id)"
                      [class.shadow-[0_0_15px_rgba(242,201,76,0.15)]]="isFrontSelected(front.id)"
                      [class.bg-[rgba(255,255,255,0.03)]]="!isFrontSelected(front.id)"
                      [class.border-[rgba(255,255,255,0.08)]]="!isFrontSelected(front.id)"
                      [class.hover:border-[rgba(255,255,255,0.2)]]="!isFrontSelected(front.id)"
                    >
                      <div
                        class="w-9 h-9 rounded-lg flex items-center justify-center shrink-0 transition-colors"
                        [class.bg-[#f2c94c]]="isFrontSelected(front.id)"
                        [class.text-[#0b0f19]]="isFrontSelected(front.id)"
                        [class.bg-[rgba(255,255,255,0.06)]]="!isFrontSelected(front.id)"
                        [class.text-[#9ca3af]]="!isFrontSelected(front.id)"
                      >
                        <span class="pi {{ front.icon }} text-sm"></span>
                      </div>
                      <div class="flex-1 min-w-0">
                        <div class="flex items-center justify-between">
                          <h4 class="text-xs font-bold text-[#f3f4f6] m-0">{{ front.label }}</h4>
                          <span
                            class="pi text-xs"
                            [class.pi-check-circle]="isFrontSelected(front.id)"
                            [class.text-[#f2c94c]]="isFrontSelected(front.id)"
                            [class.pi-circle]="!isFrontSelected(front.id)"
                            [class.text-[#6b7280]]="!isFrontSelected(front.id)"
                          ></span>
                        </div>
                        <p class="text-[0.7rem] text-[#9ca3af] mt-1 mb-0 leading-snug">
                          {{ front.description }}
                        </p>
                      </div>
                    </div>
                  }
                </div>

                <div class="flex items-center justify-between px-2 text-xs text-[#9ca3af]">
                  <span>Frentes seleccionados: <strong class="text-[#f2c94c]">{{ selectedFronts().length }}</strong></span>
                  @if (selectedFronts().length === 0) {
                    <span class="text-[#f87171] text-xs">Selecciona al menos un frente técnico.</span>
                  }
                </div>
              </section>
            }

            <!-- ================================================================= -->
            <!-- PASO 3: HORIZONTE TEMPORAL -->
            <!-- ================================================================= -->
            @if (currentStep() === 3) {
              <section class="flex flex-col gap-6 max-w-3xl mx-auto w-full">
                <div class="flex items-start gap-4 p-4 rounded-xl bg-[rgba(16,185,129,0.08)] border border-[rgba(16,185,129,0.2)]">
                  <div class="w-8 h-8 rounded-lg bg-[rgba(16,185,129,0.2)] text-[#10b981] flex items-center justify-center shrink-0">
                    <span class="pi pi-calendar text-base"></span>
                  </div>
                  <div>
                    <h3 class="text-sm font-semibold text-[#10b981] m-0">Paso 3: Horizonte Temporal</h3>
                    <p class="text-xs text-[#d1d5db] mt-1 mb-0 leading-relaxed">
                      Especifica el trimestre (Quarter) y la cantidad de sprints en que debe distribuirse el trabajo.
                    </p>
                  </div>
                </div>

                <form [formGroup]="horizonForm" class="flex flex-col gap-5">
                  <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div class="flex flex-col gap-1.5">
                      <label for="quarter-val" class="text-xs font-semibold text-[#d1d5db] flex items-center gap-1">
                        <span>Quarter</span>
                        <span class="text-[#f87171]">*</span>
                      </label>
                      <input
                        id="quarter-val"
                        type="text"
                        formControlName="quarter"
                        placeholder="ej. Q3-2026"
                        class="bg-[rgba(255,255,255,0.05)] border border-[rgba(255,255,255,0.12)] rounded-lg px-3.5 py-2.5 text-xs text-[#f3f4f6] outline-none transition-colors focus:border-[#f2c94c]"
                        [class.border-[#f87171]]="isFieldInvalid('quarter', horizonForm)"
                      />
                      <span class="text-[0.65rem] text-[#9ca3af]">Formato estándar: Q1-YYYY a Q4-YYYY</span>
                    </div>

                    <div class="flex flex-col gap-1.5">
                      <label for="sprint-val" class="text-xs font-semibold text-[#d1d5db] flex items-center gap-1">
                        <span>Número de Sprints del Trimestre</span>
                        <span class="text-[#f87171]">*</span>
                      </label>
                      <input
                        id="sprint-val"
                        type="number"
                        min="1"
                        max="24"
                        formControlName="sprintCount"
                        placeholder="6"
                        class="bg-[rgba(255,255,255,0.05)] border border-[rgba(255,255,255,0.12)] rounded-lg px-3.5 py-2.5 text-xs text-[#f3f4f6] outline-none transition-colors focus:border-[#f2c94c]"
                        [class.border-[#f87171]]="isFieldInvalid('sprintCount', horizonForm)"
                      />
                      <span class="text-[0.65rem] text-[#9ca3af]">Lo habitual son 6 sprints de 2 semanas por trimestre.</span>
                    </div>
                  </div>

                  <div class="flex flex-col gap-1.5">
                    <label for="critical-milestones" class="text-xs font-semibold text-[#d1d5db]">
                      Hitos Críticos o Fechas Comprometidas (Opcional)
                    </label>
                    <input
                      id="critical-milestones"
                      type="text"
                      formControlName="milestones"
                      placeholder="ej. Despliegue funcional a QA en Sprint 251 y Paso a Producción HyMS en Sprint 252"
                      class="bg-[rgba(255,255,255,0.05)] border border-[rgba(255,255,255,0.12)] rounded-lg px-3.5 py-2.5 text-xs text-[#f3f4f6] outline-none transition-colors focus:border-[#f2c94c]"
                    />
                    <span class="text-[0.65rem] text-[#9ca3af]">Permite al agente ubicar las historias en sprints específicos.</span>
                  </div>
                </form>
              </section>
            }

            <!-- ================================================================= -->
            <!-- PASO 4: DIAGNÓSTICO DE CAPACIDAD (CALCULADO POR IA) -->
            <!-- ================================================================= -->
            @if (currentStep() === 4) {
              <section class="flex flex-col gap-6 max-w-4xl mx-auto w-full">
                <div class="flex items-start gap-4 p-4 rounded-xl bg-[rgba(168,85,247,0.08)] border border-[rgba(168,85,247,0.2)]">
                  <div class="w-8 h-8 rounded-lg bg-[rgba(168,85,247,0.2)] text-[#c084fc] flex items-center justify-center shrink-0">
                    <span class="pi pi-calculator text-base"></span>
                  </div>
                  <div>
                    <h3 class="text-sm font-semibold text-[#c084fc] m-0">Paso 4: Diagnóstico de Capacidad Requerida (Scrum Master AI)</h3>
                    <p class="text-xs text-[#d1d5db] mt-1 mb-0 leading-relaxed">
                      El agente evaluó el alcance y los frentes técnicos para calcular el esfuerzo en Story Points (Fibonacci) necesario para completar la iniciativa.
                    </p>
                  </div>
                </div>

                @if (calculatingDiagnosis()) {
                  <div class="p-12 flex flex-col items-center justify-center gap-3 text-center">
                    <div class="w-10 h-10 border-4 border-[#f2c94c] border-t-transparent rounded-full animate-spin"></div>
                    <span class="text-xs text-[#f3f4f6] font-semibold">Analizando complejidad arquitectónica y estimando Story Points...</span>
                    <span class="text-[0.7rem] text-[#9ca3af]">Aplicando taxonomía Fibonacci y reglas HyMS</span>
                  </div>
                } @else if (diagnosis(); as diag) {
                  <!-- MÉTRICAS CLAVE -->
                  <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
                    <div class="p-4 rounded-xl bg-[rgba(255,255,255,0.03)] border border-[rgba(255,255,255,0.08)] flex flex-col gap-1">
                      <span class="text-[0.65rem] text-[#9ca3af] uppercase tracking-wider font-semibold">Esfuerzo Total Requerido</span>
                      <div class="flex items-baseline gap-2">
                        <span class="text-2xl font-bold text-[#f2c94c]">{{ diag.totalStoryPoints }}</span>
                        <span class="text-xs text-[#d1d5db]">Story Points</span>
                      </div>
                      <span class="text-[0.65rem] text-[#10b981] font-medium">Distribuidos en {{ diag.effectiveDevSprints }} sprints de desarrollo</span>
                    </div>

                    <div class="p-4 rounded-xl bg-[rgba(255,255,255,0.03)] border border-[rgba(255,255,255,0.08)] flex flex-col gap-1">
                      <span class="text-[0.65rem] text-[#9ca3af] uppercase tracking-wider font-semibold">Capacidad Requerida por Sprint</span>
                      <div class="flex items-baseline gap-2">
                        <span class="text-2xl font-bold text-[#60a5fa]">{{ diag.recommendedCapacityPerSprint }}</span>
                        <span class="text-xs text-[#d1d5db]">SP / sprint</span>
                      </div>
                      <span class="text-[0.65rem] text-[#9ca3af]">Balance óptimo para evitar saturación</span>
                    </div>

                    <div class="p-4 rounded-xl bg-[rgba(255,255,255,0.03)] border border-[rgba(255,255,255,0.08)] flex flex-col gap-1">
                      <span class="text-[0.65rem] text-[#9ca3af] uppercase tracking-wider font-semibold">Índice de Viabilidad Ágil</span>
                      <div class="flex items-baseline gap-2">
                        <span class="text-2xl font-bold text-[#10b981]">{{ diag.viabilityScore }}%</span>
                        <span class="text-xs text-[#10b981] font-medium">Altamente Viable</span>
                      </div>
                      <span class="text-[0.65rem] text-[#9ca3af]">1 sprint búfer para estabilización y Runbook</span>
                    </div>
                  </div>

                  <!-- DESGLOSE POR FRENTE -->
                  <div class="p-5 rounded-xl bg-[rgba(255,255,255,0.02)] border border-[rgba(255,255,255,0.06)] flex flex-col gap-3">
                    <h4 class="text-xs font-bold text-[#f3f4f6] m-0">Distribución de Capacidad por Frente Técnico</h4>
                    <div class="flex flex-col gap-2.5">
                      @for (item of diag.frontsBreakdown; track item.front) {
                        <div class="flex flex-col gap-1">
                          <div class="flex items-center justify-between text-xs">
                            <span class="text-[#d1d5db] font-medium">{{ item.front }}</span>
                            <span class="text-[#f2c94c] font-bold">{{ item.points }} SP <span class="text-[#9ca3af] font-normal">({{ item.percentage }}%)</span></span>
                          </div>
                          <div class="h-2 rounded-full bg-[rgba(255,255,255,0.06)] overflow-hidden">
                            <div
                              class="h-full bg-gradient-to-r from-[#f2c94c] to-[#e0b83b] rounded-full transition-all duration-500"
                              [style.width.%]="item.percentage"
                            ></div>
                          </div>
                        </div>
                      }
                    </div>
                  </div>

                  <!-- DICTAMEN DEL SCRUM MASTER -->
                  <div class="p-4 rounded-xl bg-[rgba(242,201,76,0.06)] border border-[rgba(242,201,76,0.2)] flex items-start gap-3">
                    <span class="pi pi-check-circle text-[#f2c94c] text-base mt-0.5"></span>
                    <div class="text-xs text-[#d1d5db] leading-relaxed">
                      <strong class="text-[#f3f4f6]">Recomendación del Scrum Master: </strong>
                      {{ diag.advice }}
                    </div>
                  </div>
                }
              </section>
            }

            <!-- ================================================================= -->
            <!-- PASO 5: RESUMEN SPEC & COPILOT INTERACTIVO -->
            <!-- ================================================================= -->
            @if (currentStep() === 5) {
              <section class="flex flex-col lg:flex-row gap-5 h-full min-h-[500px]">
                <!-- VISOR DE LA SPEC EN MARKDOWN -->
                <div class="flex-1 flex flex-col bg-[rgba(11,15,25,0.5)] border border-[rgba(255,255,255,0.08)] rounded-xl overflow-hidden">
                  <div class="px-4 py-3 border-b border-[rgba(255,255,255,0.08)] bg-[rgba(255,255,255,0.02)] flex items-center justify-between">
                    <div class="flex items-center gap-2">
                      <span class="pi pi-file-edit text-[#f2c94c] text-sm"></span>
                      <h4 class="text-xs font-bold text-[#f3f4f6] m-0">
                        Borrador de SPEC: {{ initiativeForm.get('title')?.value || 'Iniciativa' }}
                      </h4>
                    </div>
                    <div class="flex items-center gap-2">
                      <button
                        type="button"
                        aria-label="Copiar Markdown generado"
                        (click)="copySpecMarkdown()"
                        class="px-2.5 py-1 rounded bg-[rgba(255,255,255,0.05)] hover:bg-[rgba(255,255,255,0.1)] text-[#9ca3af] hover:text-[#f3f4f6] text-[0.7rem] border border-[rgba(255,255,255,0.1)] cursor-pointer transition-colors flex items-center gap-1.5"
                      >
                        <span class="pi" [class.pi-check]="markdownCopied()" [class.pi-copy]="!markdownCopied()"></span>
                        <span>{{ markdownCopied() ? '¡Copiado!' : 'Copiar' }}</span>
                      </button>
                    </div>
                  </div>

                  <div class="flex-1 overflow-y-auto p-5">
                    <article class="prose prose-invert max-w-none text-xs leading-relaxed text-[#d1d5db]">
                      <div [innerHTML]="generatedSpec() | markdownParser"></div>
                    </article>
                  </div>
                </div>

                <!-- PANEL LATERAL COPILOT -->
                <aside class="w-full lg:w-80 shrink-0 flex flex-col bg-[rgba(17,24,39,0.7)] border border-[rgba(255,255,255,0.08)] rounded-xl overflow-hidden">
                  <div class="px-4 py-3 border-b border-[rgba(255,255,255,0.08)] bg-[rgba(255,255,255,0.02)] flex items-center gap-2">
                    <span class="pi pi-sparkles text-[#f2c94c] text-xs"></span>
                    <h4 class="text-xs font-bold text-[#f3f4f6] m-0">Copilot de Refinamiento</h4>
                  </div>

                  <!-- MENSAJES DE COPILOT -->
                  <div class="flex-1 overflow-y-auto p-3 flex flex-col gap-2.5 text-xs min-h-[200px]">
                    @for (msg of copilotMessages(); track $index) {
                      <div
                        class="p-2.5 rounded-lg flex flex-col gap-1 max-w-[90%]"
                        [class.self-start]="msg.sender === 'agent'"
                        [class.bg-[rgba(242,201,76,0.1)]]="msg.sender === 'agent'"
                        [class.border]="msg.sender === 'agent'"
                        [class.border-[rgba(242,201,76,0.2)]]="msg.sender === 'agent'"
                        [class.text-[#f3f4f6]]="msg.sender === 'agent'"
                        [class.self-end]="msg.sender === 'user'"
                        [class.bg-[rgba(37,99,235,0.2)]]="msg.sender === 'user'"
                        [class.text-[#93c5fd]]="msg.sender === 'user'"
                      >
                        <span class="text-[0.6rem] font-bold uppercase tracking-wider text-[#9ca3af]">
                          {{ msg.sender === 'agent' ? 'Scrum Master' : 'Tú (PO)' }}
                        </span>
                        <p class="m-0 leading-snug">{{ msg.text }}</p>
                      </div>
                    }

                    @if (copilotThinking()) {
                      <div class="self-start p-2 rounded-lg bg-[rgba(242,201,76,0.1)] border border-[rgba(242,201,76,0.2)] flex items-center gap-2 text-xs text-[#f2c94c]">
                        <span class="pi pi-spin pi-spinner text-xs"></span>
                        <span>Ajustando SPEC con el Agente...</span>
                      </div>
                    }
                  </div>

                  <!-- ENTRADA DE COPILOT -->
                  <div class="p-3 border-t border-[rgba(255,255,255,0.08)] bg-[rgba(11,15,25,0.6)] flex items-center gap-2">
                    <input
                      type="text"
                      [(ngModel)]="copilotPrompt"
                      (keyup.enter)="sendCopilotMessage()"
                      placeholder="Pide ajustes (ej: mueve Kafka a SP 1)..."
                      class="flex-1 bg-[rgba(255,255,255,0.05)] border border-[rgba(255,255,255,0.1)] rounded-lg px-2.5 py-1.5 text-xs text-[#f3f4f6] outline-none focus:border-[#f2c94c]"
                    />
                    <button
                      type="button"
                      aria-label="Enviar instrucción a Copilot"
                      (click)="sendCopilotMessage()"
                      [disabled]="!copilotPrompt.trim() || copilotThinking()"
                      class="w-7 h-7 rounded-lg bg-[#f2c94c] hover:bg-[#e0b83b] text-[#0b0f19] flex items-center justify-center border-none cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed transition-all shrink-0"
                    >
                      <span class="pi pi-send text-[0.7rem]"></span>
                    </button>
                  </div>
                </aside>
              </section>
            }
          </div>

          <!-- FOOTER DE NAVEGACIÓN Y ACCIONES -->
          <footer
            class="px-6 py-4 border-t border-[rgba(255,255,255,0.08)] bg-[rgba(17,24,39,0.5)] flex items-center justify-between shrink-0"
          >
            <button
              type="button"
              aria-label="Volver al paso anterior"
              (click)="previousStep()"
              [disabled]="currentStep() === 1 || planningState.planningRunningSignal()"
              class="px-4 py-2 rounded-lg text-xs font-medium text-[#9ca3af] hover:text-[#f3f4f6] hover:bg-[rgba(255,255,255,0.05)] transition-colors border border-transparent cursor-pointer disabled:opacity-30 disabled:cursor-not-allowed"
            >
              <span class="pi pi-arrow-left text-[0.65rem] mr-1.5"></span>
              Anterior
            </button>

            <div class="flex items-center gap-3">
              <button
                type="button"
                aria-label="Cancelar planeación"
                [disabled]="planningState.planningRunningSignal()"
                (click)="close()"
                class="px-4 py-2 rounded-lg text-xs font-medium text-[#9ca3af] hover:text-[#f3f4f6] transition-colors border border-transparent cursor-pointer disabled:opacity-40"
              >
                Cancelar
              </button>

              @if (currentStep() < 5) {
                <button
                  type="button"
                  aria-label="Avanzar al siguiente paso"
                  (click)="nextStep()"
                  [disabled]="isNextDisabled()"
                  class="px-5 py-2 rounded-lg text-xs font-semibold bg-[#f2c94c] text-[#0b0f19] hover:bg-[#dfb73e] transition-all flex items-center gap-2 cursor-pointer shadow-md disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  <span>Siguiente</span>
                  <span class="pi pi-arrow-right text-[0.65rem]"></span>
                </button>
              } @else {
                <button
                  type="button"
                  aria-label="Aprobar y guardar iniciativa"
                  (click)="approveAndSave()"
                  [disabled]="planningState.planningRunningSignal()"
                  class="px-6 py-2 rounded-lg text-xs font-bold bg-[#10b981] hover:bg-[#059669] text-white transition-all flex items-center gap-2 cursor-pointer shadow-[0_0_15px_rgba(16,185,129,0.3)] disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  @if (planningState.planningRunningSignal()) {
                    <span class="pi pi-spin pi-spinner text-xs"></span>
                    <span>Persistiendo SPEC...</span>
                  } @else {
                    <span class="pi pi-check text-xs"></span>
                    <span>Aprobar y Guardar Iniciativa</span>
                  }
                </button>
              }
            </div>
          </footer>
        </div>
      </div>
    }
  `,
})
export class ProgramPlanningWizardComponent implements OnInit {
  @Input() visible = false;
  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() initiativeSaved = new EventEmitter<{ name: string; quarter: string; specContent: string }>();

  public readonly planningState = inject(PlanningStateService);
  private readonly notifications = inject(NotificationService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  public readonly availableFronts: readonly FrontDefinition[] = AVAILABLE_FRONTS;
  public readonly selectedFronts = signal<string[]>(['Canales', 'BFF', 'Core', 'Datos', 'DevOps']);

  public readonly currentStep = signal<number>(1);
  public readonly maxVisitedStep = signal<number>(1);
  public readonly calculatingDiagnosis = signal<boolean>(false);
  public readonly diagnosis = signal<CapacityDiagnosis | null>(null);
  public readonly generatedSpec = signal<string>('');
  public readonly markdownCopied = signal<boolean>(false);

  public readonly copilotMessages = signal<CopilotMessage[]>([]);
  public copilotPrompt = '';
  public readonly copilotThinking = signal<boolean>(false);

  public readonly steps = [
    { number: 1, title: 'Iniciativa y Negocio', shortTitle: '1. Iniciativa' },
    { number: 2, title: 'Frentes Técnicos', shortTitle: '2. Frentes' },
    { number: 3, title: 'Horizonte Temporal', shortTitle: '3. Horizonte' },
    { number: 4, title: 'Diagnóstico de Capacidad', shortTitle: '4. Capacidad' },
    { number: 5, title: 'SPEC & Copilot', shortTitle: '5. SPEC & Copilot' },
  ];

  public readonly initiativeForm: FormGroup = this.fb.group({
    title: ['Guardián de la Experiencia (Monitoreo Inteligente)', [Validators.required, Validators.minLength(3)]],
    problemAndValue: [
      'Reducir incidentes no detectados en transacciones críticas mediante un motor de reglas híbrido (determinista + semántico con pgvector) para auditoría y alertas operativas.',
      [Validators.required, Validators.minLength(10)],
    ],
    targetUsers: ['Clientes App Personas, Equipos de Soporte Operativo y Ciberseguridad'],
  });

  public readonly horizonForm: FormGroup = this.fb.group({
    quarter: ['Q3-2026', [Validators.required, Validators.pattern(/^Q[1-4]-\d{4}$/)]],
    sprintCount: [6, [Validators.required, Validators.min(1), Validators.max(24)]],
    milestones: ['Pruebas integrales QA en Sprint 251 y Paso a Producción HyMS en Sprint 252'],
  });

  public ngOnInit(): void {
    // Inicialización si se requiere
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

  public close(): void {
    if (this.planningState.planningRunningSignal()) {
      return;
    }
    this.visible = false;
    this.visibleChange.emit(false);
  }

  public isFieldInvalid(fieldName: string, formGroup: FormGroup): boolean {
    const control = formGroup.get(fieldName);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  public isFrontSelected(frontId: string): boolean {
    return this.selectedFronts().includes(frontId);
  }

  public toggleFront(frontId: string): void {
    const current = this.selectedFronts();
    if (current.includes(frontId)) {
      this.selectedFronts.set(current.filter((item) => item !== frontId));
    } else {
      this.selectedFronts.set([...current, frontId]);
    }
  }

  public goToStep(step: number): void {
    if (step <= this.maxVisitedStep()) {
      this.currentStep.set(step);
      if (step === 4 && !this.diagnosis()) {
        this.runCapacityCalculation();
      }
    }
  }

  public isNextDisabled(): boolean {
    if (this.currentStep() === 1) {
      return this.initiativeForm.invalid;
    }
    if (this.currentStep() === 2) {
      return this.selectedFronts().length === 0;
    }
    if (this.currentStep() === 3) {
      return this.horizonForm.invalid;
    }
    return false;
  }

  public nextStep(): void {
    const next = this.currentStep() + 1;
    if (next <= 5) {
      this.currentStep.set(next);
      if (next > this.maxVisitedStep()) {
        this.maxVisitedStep.set(next);
      }
      if (next === 4) {
        this.runCapacityCalculation();
      } else if (next === 5) {
        this.ensureSpecGenerated();
      }
    }
  }

  public previousStep(): void {
    const prev = this.currentStep() - 1;
    if (prev >= 1) {
      this.currentStep.set(prev);
    }
  }

  /**
   * Cálculo proactivo de capacidad realizado por el Agente Scrum Master.
   * El PO NO digita los SP; el agente los calcula según los frentes seleccionados y la duración.
   */
  public runCapacityCalculation(): void {
    this.calculatingDiagnosis.set(true);

    setTimeout(() => {
      const fronts = this.selectedFronts();
      const sprintCount = Math.max(1, Number(this.horizonForm.get('sprintCount')?.value || 6));
      
      let totalSP = 0;
      const breakdown = fronts.map((frontId) => {
        const def = AVAILABLE_FRONTS.find((f) => f.id === frontId);
        const pts = def ? def.defaultPoints : 20;
        totalSP += pts;
        return { front: def?.label || frontId, points: pts, percentage: 0 };
      });

      const effectiveSprints = sprintCount > 1 ? sprintCount - 1 : 1;
      const capacityPerSprint = Math.ceil(totalSP / effectiveSprints);

      const computedBreakdown = breakdown.map((item) => ({
        ...item,
        percentage: totalSP > 0 ? Math.round((item.points / totalSP) * 100) : 0,
      }));

      const advice = `Para completar los objetivos de esta iniciativa sin riesgo de desborde, se requiere una capacidad promedio de ${capacityPerSprint} Story Points por sprint durante los primeros ${effectiveSprints} sprints. El sprint ${sprintCount} se reserva como búfer de estabilización, soporte temprano y ejecución del Runbook de paso a producción HyMS.`;

      this.diagnosis.set({
        totalStoryPoints: totalSP,
        recommendedCapacityPerSprint: capacityPerSprint,
        effectiveDevSprints: effectiveSprints,
        bufferSprints: 1,
        viabilityScore: 94,
        frontsBreakdown: computedBreakdown,
        advice,
      });

      this.calculatingDiagnosis.set(false);
    }, 400);
  }

  public ensureSpecGenerated(): void {
    if (!this.generatedSpec()) {
      this.generateInitialSpec();
    }
  }

  /**
   * Genera el borrador Markdown de la SPEC respetando la taxonomía corporativa ideas_planning_q3.md
   */
  private generateInitialSpec(): void {
    const title = this.initiativeForm.get('title')?.value?.trim() || 'Iniciativa';
    const problem = this.initiativeForm.get('problemAndValue')?.value?.trim() || '';
    const users = this.initiativeForm.get('targetUsers')?.value?.trim() || 'Clientes y Operación';
    const quarter = this.horizonForm.get('quarter')?.value?.trim() || 'Q3-2026';
    const sprintCount = Number(this.horizonForm.get('sprintCount')?.value || 6);
    const diag = this.diagnosis();
    const capacityPerSprint = diag?.recommendedCapacityPerSprint || 28;

    const frontsList = this.selectedFronts().map((f) => {
      const item = AVAILABLE_FRONTS.find((x) => x.id === f);
      return `* **${item?.label || f}**: ${item?.description || 'Desarrollo y componentes'}`;
    }).join('\n');

    const specMd = `# Ideas de Planeación - ${quarter}

Este documento centraliza el contexto de arquitectura, la taxonomía ágil y la distribución temporal (roadmap) para el desarrollo del proyecto **${title}**, alineado rigurosamente con los marcos de agilidad de Bancolombia y el proceso de calidad **HyMS**.

---

## 1. Visión y Objetivos de Negocio

* **Problema y Valor:** ${problem}
* **Audiencia y Canales:** ${users}
* **Capacidad Requerida por Sprint:** ${capacityPerSprint} Story Points (calculada por Agente Scrum Master).
* **Horizonte:** ${quarter} (${sprintCount} Sprints).

---

## 2. Stack Tecnológico y Componentes Involucrados

${frontsList}

---

## 3. Taxonomía de Trabajo Ágil y Calidad HyMS

Siguiendo la guía corporativa de agilidad:
* **Historia Habilitadora (HA):** Se enfoca en setups técnicos, pipelines, infraestructura, ciberseguridad y el proceso formal de **Paso a Producción bajo marco HyMS** (Runbook, observabilidad y mesas de control).
* **Historia de Usuario (HU):** Aporta valor de negocio directo. Su alcance abarca diseño, desarrollo y pruebas hasta el ambiente **QA**. Jamás incluye el paso a producción productivo.
* **Escala de Estimación:** Serie Fibonacci estricta (1, 2, 3, 5, 8, 13 Story Points).

---

## 4. Distribución del Roadmap por Sprints (Entregas ${quarter})

Todas las tareas de desarrollo y pruebas de calidad deben concluir a más tardar en el sprint **SP ${sprintCount - 1}**. El sprint final **SP ${sprintCount}** se reserva para el soporte post-producción temprano, estabilización de plataforma, mesas de control y documentación operativa formal (Runbook).

| Sprint | Tipo | Título | Story Points | Frente | Dependencias | Descripción / Criterio de Entrega |
|:------:|:----:|:-------|:------------:|:-------|:-------------|:----------------------------------|
| SP 1 | HA | Setups, CMDB y repositorios Git | 5 | DevOps | Ninguna | Creación de repositorios, pipelines CI/CD y elementos CMDB. |
| SP 1 | HA | Definición de contratos y DTOs | 5 | BFF | Ninguna | Estandarización de contratos de interfaz y validaciones. |
| SP 2 | HU | Modelamiento y persistencia en Base de Datos | 8 | Datos | SP 1 (BFF) | Tablas relacionales y colecciones con auditoría. |
| SP 2 | HU | Core: Lógica de negocio y reglas base | 8 | Core | SP 1 (BFF) | Reglas deterministas y validación transaccional. |
| SP 3 | HU | Integración de endpoints REST y servicios | 8 | Canales | SP 2 (Core) | Conexión Front-BFF con manejo reactivo de estados. |
| SP 4 | HU | Formularios dinámicos y flujos de usuario | 5 | Canales | SP 3 (Canales) | Experiencia interactiva y validaciones frontend. |
| SP 5 | HA | Pruebas integrales QA, Karate y Ciberseguridad | 8 | Arquitectura | SP 4 | Certificación de calidad en ambiente de pruebas (QA). |
| SP 6 | HA | Paso a Producción HyMS, Runbook y Estabilización | 5 | DevOps | SP 5 | Despliegue productivo, monitoreo y soporte temprano. |

---

## 5. Consideraciones de Cierre y Gobierno

1. Separación estricta entre desarrollo funcional (HU hasta QA) y habilitación operativa HyMS (HA).
2. Trazabilidad completa con Azure DevOps para cada actividad mediante su ID de Work Item.
`;

    this.generatedSpec.set(specMd);

    // Mensaje inicial del Copilot
    this.copilotMessages.set([
      {
        sender: 'agent',
        text: `¡Hola! Como tu Scrum Master y PO, he consolidado la propuesta de SPEC para "${title}". La capacidad requerida calculada es de ${capacityPerSprint} SP/sprint. Puedes pedirme cualquier ajuste en los sprints o frentes.`,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      },
    ]);
  }

  public copySpecMarkdown(): void {
    const md = this.generatedSpec();
    if (md) {
      navigator.clipboard?.writeText(md);
      this.markdownCopied.set(true);
      setTimeout(() => this.markdownCopied.set(false), 2000);
      this.notifications.success('Contenido Markdown copiado al portapapeles');
    }
  }

  public sendCopilotMessage(): void {
    const text = this.copilotPrompt.trim();
    if (!text || this.copilotThinking()) {
      return;
    }

    const current = this.copilotMessages();
    this.copilotMessages.set([
      ...current,
      {
        sender: 'user',
        text,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      },
    ]);
    this.copilotPrompt = '';
    this.copilotThinking.set(true);

    // Simulación reactiva del agente Scrum Master ajustando la SPEC
    setTimeout(() => {
      let replyText = 'He actualizado el borrador de la SPEC con tus indicaciones.';
      const lower = text.toLowerCase();

      if (lower.includes('kafka') || lower.includes('broker')) {
        replyText = 'Ajusté el frente de Kafka para adelantar sus contratos y tópicos al Sprint 1, reduciendo el riesgo de integración.';
      } else if (lower.includes('seguridad') || lower.includes('auth')) {
        replyText = 'Reforcé las actividades de Ciberseguridad y Entra ID como prerrequisito en el Sprint 2.';
      } else if (lower.includes('sprint') || lower.includes('mover')) {
        replyText = 'Reorganicé la secuencia de sprints y recalculé las dependencias en la tabla del roadmap.';
      }

      this.copilotMessages.set([
        ...this.copilotMessages(),
        {
          sender: 'agent',
          text: replyText,
          timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        },
      ]);
      this.copilotThinking.set(false);
    }, 600);
  }

  public approveAndSave(): void {
    const title = this.initiativeForm.get('title')?.value?.trim() || 'Iniciativa';
    const quarter = this.horizonForm.get('quarter')?.value?.trim() || 'Q3-2026';
    const sprintCount = Number(this.horizonForm.get('sprintCount')?.value || 6);
    const diag = this.diagnosis();
    const capacityPerSprint = diag?.recommendedCapacityPerSprint || 28;
    const objectives = this.initiativeForm.get('problemAndValue')?.value?.trim() || '';

    const request: ProgramPlanRequestDTO = {
      quarter,
      sprintCount,
      maxCapacityPerSprint: capacityPerSprint,
      targetFronts: [...this.selectedFronts()],
      objectives: `${title}: ${objectives}`,
    };

    this.planningState
      .triggerProgramPlanning(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.initiativeSaved.emit({
            name: title,
            quarter,
            specContent: this.generatedSpec(),
          });
          this.notifications.success(`¡Iniciativa "${title}" aprobada y persistida con éxito!`);
          this.planningState.loadAvailableSpecs();
          this.close();
        },
        error: () => {
          // El error ya es capturado y notificado en PlanningStateService
        },
      });
  }
}
