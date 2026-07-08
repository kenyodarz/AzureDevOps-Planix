import {Component, EventEmitter, inject, OnDestroy, OnInit, Output} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {DevopsAgentStateService} from '../../services/devops-agent-state.service';
import {DashboardData, DashboardStoryItem} from '../../models/devops-agent.model';
import {Subscription} from 'rxjs';
import {MarkdownParserPipe} from '../../../../shared/pipes/markdown-parser.pipe';

@Component({
  selector: 'app-planning-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, MarkdownParserPipe],
  template: `
    <div class="flex-1 flex flex-col p-8 overflow-y-auto bg-transparent">

      <!-- HEADER & CONTROLS -->
      <div
        class="mb-8 bg-[rgba(17,24,39,0.5)] border border-[rgba(255,255,255,0.08)] rounded-xl p-6 backdrop-blur-md shadow-lg">
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

        <!-- FORMULARIO DE CONSULTA -->
        <div class="flex flex-col md:flex-row items-end gap-4">
          <div class="flex-1 flex flex-col gap-1.5 w-full">
            <label class="text-[0.7rem] text-[#9ca3af] font-semibold uppercase tracking-wider">Célula
              / Area Path</label>
            <div class="relative">
              <span
                class="pi pi-users absolute left-3 top-1/2 -translate-y-1/2 text-[#9ca3af]"></span>
              <input
                type="text"
                [(ngModel)]="cellInput"
                placeholder="Ej: Aegis Backend - Célula Arkham"
                class="w-full bg-[#0b0f19] border border-[rgba(255,255,255,0.12)] text-[#f3f4f6] pl-9 pr-4 py-2.5 rounded-lg text-sm outline-none focus:border-[#f2c94c] hover:border-[rgba(255,255,255,0.2)] transition-all font-sans"
              />
            </div>
          </div>

          <div class="flex-1 flex flex-col gap-1.5 w-full">
            <label class="text-[0.7rem] text-[#9ca3af] font-semibold uppercase tracking-wider">Sprint
              / Iteration Path</label>
            <div class="relative">
              <span
                class="pi pi-calendar absolute left-3 top-1/2 -translate-y-1/2 text-[#9ca3af]"></span>
              <input
                type="text"
                [(ngModel)]="sprintInput"
                placeholder="Ej: Sprint 3"
                class="w-full bg-[#0b0f19] border border-[rgba(255,255,255,0.12)] text-[#f3f4f6] pl-9 pr-4 py-2.5 rounded-lg text-sm outline-none focus:border-[#f2c94c] hover:border-[rgba(255,255,255,0.2)] transition-all font-sans"
              />
            </div>
          </div>

          <button
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

        <!-- ERROR ALERT -->
        @if (errorMessage) {
          <div
            class="mt-4 p-4 rounded-lg bg-[rgba(239,68,68,0.1)] border border-[rgba(239,68,68,0.25)] flex items-start gap-3 shadow-[0_4px_12px_rgba(239,68,68,0.1)]">
            <span class="pi pi-exclamation-circle text-[#ef4444] text-lg mt-0.5 shrink-0"></span>
            <div class="flex-1">
              <h4 class="text-sm font-bold text-[#ef4444] mb-0.5">Error en el análisis de
                Backlog</h4>
              <p class="text-xs text-[#fca5a5] leading-relaxed">{{ errorMessage }}</p>
            </div>
          </div>
        }
      </div>

      <!-- MAIN CONTENT VIEW -->
      @if (state.loading | async) {
        <!-- LOADER VIEW -->
        <div class="flex-1 flex flex-col items-center justify-center gap-3 py-16">
          <div
            class="w-10 h-10 border-4 border-[#f2c94c] border-t-transparent rounded-full animate-spin"></div>
          <span class="text-sm text-[#9ca3af]">Auditando backlog en Azure DevOps...</span>
          <p class="text-xs text-[#6b7280]">Esto puede demorar unos segundos mientras el agente
            ejecuta el análisis de calidad.</p>
        </div>
      } @else if (dashboardData) {

        <!-- DASHBOARD CONTAINER -->
        <div class="flex flex-col gap-6">

          <!-- FILTERS HEADER -->
          <div
            class="flex items-center gap-2 bg-[rgba(242,201,76,0.05)] border border-[rgba(242,201,76,0.15)] rounded-lg px-4 py-2 w-fit">
            <span class="w-2 h-2 rounded-full bg-[#f2c94c]"></span>
            <span class="text-xs text-[#9ca3af]">
              Mostrando análisis para Célula: <strong
              class="text-[#f3f4f6]">{{ activeCell }}</strong> | Sprint: <strong
              class="text-[#f3f4f6]">{{ activeSprint }}</strong>
            </span>
          </div>

          <!-- METRIC CARDS -->
          <div class="grid grid-cols-1 md:grid-cols-4 gap-5">

            <!-- CARD 1: VELOCITY -->
            <div
              class="bg-[rgba(17,24,39,0.4)] border border-[rgba(255,255,255,0.08)] rounded-xl p-5 backdrop-blur-md hover:border-[#2563eb] transition-all flex flex-col justify-between shadow-lg">
              <div class="flex justify-between items-start">
                <div>
                  <span
                    class="text-xs text-[#9ca3af] uppercase font-semibold">Progreso / Velocity</span>
                  <h3 class="text-2xl font-extrabold text-[#f3f4f6] mt-1">
                    {{ dashboardData.metrics.completedPoints }}
                    / {{ dashboardData.metrics.totalPoints }} SP
                  </h3>
                </div>
                <div
                  class="w-8 h-8 rounded-lg bg-[rgba(37,99,235,0.1)] flex items-center justify-center text-[#2563eb]">
                  <span class="pi pi-bolt"></span>
                </div>
              </div>
              <div class="mt-4">
                <div class="flex justify-between text-xs text-[#9ca3af] mb-1">
                  <span>Avance de entrega</span>
                  <span
                    class="font-bold text-[#f3f4f6]">{{ dashboardData.metrics.completedPercentage }}
                    %</span>
                </div>
                <div class="w-full bg-[rgba(255,255,255,0.05)] rounded-full h-1.5 overflow-hidden">
                  <div
                    class="bg-gradient-to-r from-[#2563eb] to-[#10b981] h-1.5 rounded-full transition-all duration-500"
                    [style.width.%]="dashboardData.metrics.completedPercentage"
                  ></div>
                </div>
              </div>
            </div>

            <!-- CARD 2: DOCUMENTATION QUALITY -->
            <div
              class="bg-[rgba(17,24,39,0.4)] border border-[rgba(255,255,255,0.08)] rounded-xl p-5 backdrop-blur-md hover:border-[#10b981] transition-all flex flex-col justify-between shadow-lg">
              <div class="flex justify-between items-start">
                <div>
                  <span
                    class="text-xs text-[#9ca3af] uppercase font-semibold">Calidad Documentación</span>
                  <h3 class="text-2xl font-extrabold text-[#f3f4f6] mt-1">
                    {{ dashboardData.metrics.avgQualityScore }}%
                  </h3>
                </div>
                <div
                  class="w-8 h-8 rounded-lg bg-[rgba(16,185,129,0.1)] flex items-center justify-center text-[#10b981]">
                  <span class="pi pi-verified"></span>
                </div>
              </div>
              <div class="mt-4">
                <div class="flex justify-between text-xs text-[#9ca3af] mb-1">
                  <span>Salud del DoD</span>
                  <span class="font-bold text-[#f3f4f6]">{{
                      getQualityLabel(dashboardData.metrics.avgQualityScore)
                    }}</span>
                </div>
                <div class="w-full bg-[rgba(255,255,255,0.05)] rounded-full h-1.5 overflow-hidden">
                  <div
                    [class]="getQualityBgClass(dashboardData.metrics.avgQualityScore)"
                    class="h-1.5 rounded-full transition-all duration-500"
                    [style.width.%]="dashboardData.metrics.avgQualityScore"
                  ></div>
                </div>
              </div>
            </div>

            <!-- CARD 3: UNDOCUMENTED STORIES -->
            <div
              class="bg-[rgba(17,24,39,0.4)] border border-[rgba(255,255,255,0.08)] rounded-xl p-5 backdrop-blur-md hover:border-[#ef4444] transition-all flex flex-col justify-between shadow-lg">
              <div class="flex justify-between items-start">
                <div>
                  <span
                    class="text-xs text-[#9ca3af] uppercase font-semibold">Historias sin Refinar</span>
                  <h3 class="text-2xl font-extrabold text-[#f3f4f6] mt-1">
                    {{ dashboardData.metrics.undocumentedCount }} HUs
                  </h3>
                </div>
                <div
                  class="w-8 h-8 rounded-lg bg-[rgba(239,68,68,0.1)] flex items-center justify-center text-[#ef4444]">
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
              class="bg-[rgba(17,24,39,0.4)] border border-[rgba(255,255,255,0.08)] rounded-xl p-5 backdrop-blur-md hover:border-[#f59e0b] transition-all flex flex-col justify-between shadow-lg">
              <div class="flex justify-between items-start">
                <div>
                  <span
                    class="text-xs text-[#9ca3af] uppercase font-semibold">Historias Complejas</span>
                  <h3 class="text-2xl font-extrabold text-[#f3f4f6] mt-1">
                    {{ getLargeStoriesCount() }} HUs
                  </h3>
                </div>
                <div
                  class="w-8 h-8 rounded-lg bg-[rgba(245,158,11,0.1)] flex items-center justify-center text-[#f59e0b]">
                  <span class="pi pi-clone"></span>
                </div>
              </div>
              <div class="mt-4">
                <p class="text-[0.75rem] text-[#f59e0b] flex items-center gap-1 font-medium">
                  <span class="pi pi-info-circle"></span> Historias con &ge; 13 SP que deben
                  dividirse.
                </p>
              </div>
            </div>

          </div>

          <!-- TABLE OF STORIES -->
          <div
            class="overflow-hidden border border-[rgba(255,255,255,0.08)] bg-[rgba(17,24,39,0.3)] backdrop-blur-md rounded-xl shadow-xl">
            <div
              class="p-4 border-b border-[rgba(255,255,255,0.08)] flex flex-col lg:flex-row justify-between items-start lg:items-center gap-4">
              <div class="flex flex-col gap-1">
                <span class="font-semibold text-sm text-[#f3f4f6]">Desglose de Historias y Habilitadores</span>
                <span class="text-xs text-[#9ca3af]">{{ filteredItems.length }}
                  de {{ dashboardData.items.length }} ítems encontrados</span>
              </div>

              <!-- FILTROS AVANZADOS -->
              <div class="flex flex-wrap items-center gap-4 w-full lg:w-auto">
                <!-- Filtro por Miembro -->
                <div class="flex items-center gap-2">
                  <label
                    class="text-xs text-[#9ca3af] font-medium whitespace-nowrap">Integrante:</label>
                  <select
                    [(ngModel)]="selectedMember"
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
                  <label
                    class="text-xs text-[#9ca3af] font-medium whitespace-nowrap">Estado:</label>
                  <select
                    [(ngModel)]="selectedState"
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
                  <label
                    class="text-xs text-[#9ca3af] font-medium whitespace-nowrap">Calidad:</label>
                  <select
                    [(ngModel)]="selectedQuality"
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
                class="border-b border-[rgba(255,255,255,0.08)] bg-[rgba(255,255,255,0.02)] text-[#9ca3af]">
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
                        class="bg-transparent border-none text-[#9ca3af] hover:text-[#f3f4f6]"
                        (click)="toggleExpand(item.id)"
                      >
                        <span class="pi"
                              [ngClass]="isExpanded(item.id) ? 'pi-chevron-down text-[#f2c94c]' : 'pi-chevron-right'"></span>
                      </button>
                    </td>

                    <!-- ID -->
                    <td class="p-4 font-mono text-[#2563eb] text-[0.75rem]">{{ item.id }}</td>

                    <!-- Title -->
                    <td class="p-4">
                      <div class="flex flex-col gap-1">
                        <span
                          class="text-[#f3f4f6] font-medium leading-snug">{{ item.title }}</span>

                        <!-- Warnings/Badges -->
                        <div class="flex items-center gap-2 mt-1">
                          @if (item.points >= 13) {
                            <span
                              class="bg-[rgba(245,158,11,0.1)] text-[#f59e0b] px-1.5 py-0.5 rounded text-[0.65rem] border border-[rgba(245,158,11,0.2)] font-semibold flex items-center gap-1">
                              <span class="pi pi-exclamation-triangle"
                                    style="font-size: 0.6rem"></span> Dividir Historia
                            </span>
                          }
                          <span class="text-[0.7rem] text-[#9ca3af] flex items-center gap-1">
                            <span class="pi pi-paperclip"
                                  style="font-size: 0.65rem"></span> {{ item.linkedTasksCount }}
                            tareas hijas
                          </span>

                          <!-- Mini quality progress -->
                          <div class="flex items-center gap-1.5 ml-2">
                            <span class="text-[0.65rem] text-[#9ca3af]">Calidad:</span>
                            @if (item.qualityScore === 0) {
                              <span class="pi pi-spin pi-spinner text-[#2563eb]"
                                    style="font-size: 0.65rem"></span>
                              <span
                                class="text-[0.65rem] text-[#2563eb] font-semibold animate-pulse">Auditando...</span>
                            } @else {
                              <div
                                class="w-12 bg-[rgba(255,255,255,0.05)] h-1 rounded-full overflow-hidden">
                                <div
                                  [class]="getQualityBgClass(item.qualityScore)"
                                  class="h-1 rounded-full"
                                  [style.width.%]="item.qualityScore"
                                ></div>
                              </div>
                              <span class="text-[0.65rem] font-bold"
                                    [class.text-[#ef4444]]="item.qualityScore < 50"
                                    [class.text-[#10b981]]="item.qualityScore >= 80"
                                    [class.text-[#f59e0b]]="item.qualityScore >= 50 && item.qualityScore < 80">
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
                        class="bg-[rgba(242,201,76,0.1)] text-[#f2c94c] px-2 py-0.5 rounded font-mono font-bold text-[0.75rem] border border-[rgba(242,201,76,0.2)] shadow-sm">
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
                        <span class="pi pi-spin pi-spinner text-[#6b7280]"
                              style="font-size: 0.9rem"></span>
                      } @else {
                        <span
                          [class]="item.hasAcceptanceCriteria ? 'pi pi-check-circle text-emerald-500' : 'pi pi-times-circle text-rose-500'"
                          style="font-size: 1.1rem"
                        ></span>
                      }
                    </td>

                    <td class="p-4 text-center">
                      @if (item.qualityScore === 0) {
                        <span class="pi pi-spin pi-spinner text-[#6b7280]"
                              style="font-size: 0.9rem"></span>
                      } @else {
                        <span
                          [class]="item.hasDoD ? 'pi pi-check-circle text-emerald-500' : 'pi pi-times-circle text-rose-500'"
                          style="font-size: 1.1rem"
                        ></span>
                      }
                    </td>

                    <!-- Action -->
                    <td class="p-4 text-center">
                      <button
                        class="bg-gradient-to-r from-[#2563eb] to-[#1d4ed8] hover:from-[#1d4ed8] hover:to-[#1e40af] text-white px-3 py-1.5 rounded-lg border-none text-[0.7rem] font-semibold cursor-pointer transition-all flex items-center justify-center gap-1.5 mx-auto shadow-[0_2px_4px_rgba(37,99,235,0.2)]"
                        (click)="refineStory(item)"
                      >
                        <span class="pi pi-sparkles" style="font-size: 0.75rem"></span> Refinar
                      </button>
                    </td>

                  </tr>
                  @if (isExpanded(item.id)) {
                    <tr
                      class="bg-[rgba(255,255,255,0.015)] border-b border-[rgba(255,255,255,0.04)]">
                      <td colspan="9" class="p-4 pl-12 text-[#d1d5db]">
                        <div class="flex flex-col gap-3 border-l-2 border-[#f2c94c] pl-4">
                          <div class="flex items-center justify-between gap-3">
                            <span class="text-xs font-bold text-[#f2c94c] flex items-center gap-1.5">
                              <span class="pi pi-comment"></span> Feedback de Calidad (Auditoría IA)
                            </span>
                            <button
                              type="button"
                              class="bg-[rgba(242,201,76,0.12)] hover:bg-[rgba(242,201,76,0.2)] text-[#f2c94c] border border-[rgba(242,201,76,0.2)] px-3 py-1.5 rounded-lg text-[0.7rem] font-semibold transition-all flex items-center gap-1.5"
                              (click)="runDetailedAudit(item.id)"
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
                                item.feedback
                                || 'Analizando o sin retroalimentación detallada todavía.'
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

        </div>

      } @else {

        <!-- BIENVENIDA / EN BLANCO -->
        <div
          class="flex-1 flex flex-col items-center justify-center text-center max-w-lg mx-auto py-16 gap-4">
          <div
            class="w-16 h-16 rounded-full bg-[rgba(242,201,76,0.1)] flex items-center justify-center text-[#f2c94c] mb-2">
            <span class="pi pi-chart-line text-2xl"></span>
          </div>
          <h3 class="text-lg font-bold text-[#f3f4f6]">Genera el Análisis de Backlog</h3>
          <p class="text-sm text-[#9ca3af] leading-relaxed">
            Ingresa la Célula (Ruta de Área) y el Sprint en el formulario superior para conectarse
            de forma segura con Azure DevOps, auditar la documentación y medir la velocidad del
            equipo.
          </p>
          <div
            class="flex items-center gap-2 mt-2 bg-[rgba(255,255,255,0.03)] border border-[rgba(255,255,255,0.05)] rounded-lg px-4 py-2 text-xs text-[#6b7280]">
            <span class="pi pi-lock"></span>
            <span>No se generarán llamadas automáticas ni consumo inútil de tokens.</span>
          </div>
        </div>

      }

    </div>
  `,
  styles: [`
    ::ng-deep .markdown-audit-content p {
      margin-bottom: 8px;
    }

    ::ng-deep .markdown-audit-content p:last-child {
      margin-bottom: 0;
    }

    ::ng-deep .markdown-audit-content pre {
      background: rgba(0, 0, 0, 0.25);
      padding: 12px 14px;
      border-radius: 8px;
      overflow-x: auto;
      font-family: monospace;
      font-size: 0.8rem;
      margin: 10px 0;
      border: 1px solid rgba(255, 255, 255, 0.05);
      white-space: pre;
    }

    ::ng-deep .markdown-audit-content code {
      font-family: monospace;
      font-size: 0.8rem;
      background: rgba(255, 255, 255, 0.05);
      padding: 2px 4px;
      border-radius: 4px;
    }

    ::ng-deep .markdown-audit-content ul,
    ::ng-deep .markdown-audit-content ol {
      margin-left: 20px;
      margin-top: 8px;
      margin-bottom: 8px;
    }

    ::ng-deep .markdown-audit-content li {
      margin-bottom: 4px;
    }

    ::ng-deep .markdown-audit-content table {
      width: 100%;
      border-collapse: collapse;
      margin: 10px 0;
      font-size: 0.75rem;
    }

    ::ng-deep .markdown-audit-content th,
    ::ng-deep .markdown-audit-content td {
      border: 1px solid rgba(255, 255, 255, 0.12);
      padding: 8px 10px;
      text-align: left;
    }

    ::ng-deep .markdown-audit-content th {
      background: rgba(255, 255, 255, 0.05);
      color: #f3f4f6;
      font-weight: 600;
    }

    ::ng-deep .markdown-audit-content strong {
      color: #f3f4f6;
    }

    ::ng-deep .markdown-audit-content h2,
    ::ng-deep .markdown-audit-content h3,
    ::ng-deep .markdown-audit-content h4 {
      margin: 12px 0 6px;
      color: #f2c94c;
      font-weight: 600;
    }
  `]
})
export class PlanningDashboardComponent implements OnInit, OnDestroy {
  @Output() refineRequested = new EventEmitter<void>();
  protected readonly state = inject(DevopsAgentStateService);
  protected dashboardData: DashboardData | null = null;
  protected errorMessage: string | null = null;

  // Inputs del formulario
  protected cellInput: string = '';
  protected sprintInput: string = '';

  // Filtros activos consultados
  protected activeCell: string = '';
  protected activeSprint: string = '';
  protected selectedMember: string = '';
  protected selectedState: string = '';
  protected selectedQuality: string = '';
  protected expandedItemIds: Set<string> = new Set();
  protected detailedAudits: Record<string, { loading: boolean; content?: string; error?: string }> = {};
  private sub: Subscription | null = null;
  private errorSub: Subscription | null = null;

  protected get filteredItems(): DashboardStoryItem[] {
    if (!this.dashboardData) return [];
    return this.dashboardData.items.filter(item => {
      if (this.selectedMember && item.assignedMember !== this.selectedMember) {
        return false;
      }
      if (this.selectedState && item.state !== this.selectedState) {
        return false;
      }
      if (this.selectedQuality) {
        if (this.selectedQuality === 'critical' && item.qualityScore >= 50) {
          return false;
        }
        if (this.selectedQuality === 'regular' && (item.qualityScore < 50 || item.qualityScore >= 80)) {
          return false;
        }
        if (this.selectedQuality === 'good' && item.qualityScore < 80) {
          return false;
        }
      }
      return true;
    });
  }

  protected get uniqueStates(): string[] {
    if (!this.dashboardData) return [];
    const states = this.dashboardData.items
    .map(item => item.state)
    .filter((state): state is string => !!state);
    return Array.from(new Set(states));
  }

  protected get uniqueMembers(): string[] {
    if (!this.dashboardData) return [];
    const members = this.dashboardData.items
    .map(item => item.assignedMember)
    .filter((member): member is string => !!member);
    return Array.from(new Set(members));
  }

  public ngOnInit(): void {
    // Escuchar el estado del dashboard
    this.sub = this.state.dashboardData.subscribe(data => {
      this.dashboardData = data;
      this.selectedMember = '';
      this.selectedState = '';
      this.selectedQuality = '';
      this.expandedItemIds.clear();
    });

    this.errorSub = this.state.dashboardError.subscribe(err => {
      this.errorMessage = err;
    });
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

  public ngOnDestroy(): void {
    if (this.sub) {
      this.sub.unsubscribe();
    }
    if (this.errorSub) {
      this.errorSub.unsubscribe();
    }
  }

  protected generateAnalysis(): void {
    if (this.cellInput.trim() && this.sprintInput.trim()) {
      this.activeCell = this.cellInput.trim();
      this.activeSprint = this.sprintInput.trim();
      this.state.loadDashboardData(this.activeCell, this.activeSprint);
    }
  }

  protected getQualityLabel(score: number): string {
    if (score >= 90) return 'Excelente';
    if (score >= 70) return 'Buena (Suficiente)';
    if (score >= 50) return 'Regular';
    return 'Deficiente (Requiere Refinar)';
  }

  protected getQualityBgClass(score: number): string {
    if (score >= 80) return 'bg-[#10b981]';
    if (score >= 50) return 'bg-[#f59e0b]';
    return 'bg-[#ef4444]';
  }

  protected getLargeStoriesCount(): number {
    if (!this.dashboardData) return 0;
    return this.dashboardData.items.filter(item => item.points >= 13).length;
  }

  protected getStateClass(state: string): string {
    const s = state ? state.toLowerCase() : '';
    if (s === 'done' || s === 'closed') {
      return 'bg-[rgba(16,185,129,0.1)] text-[#10b981] border border-[rgba(16,185,129,0.2)]';
    }
    if (s === 'committed' || s === 'active') {
      return 'bg-[rgba(37,99,235,0.1)] text-[#2563eb] border border-[rgba(37,99,235,0.2)]';
    }
    if (s === 'approved') {
      return 'bg-[rgba(242,201,76,0.1)] text-[#f2c94c] border border-[rgba(242,201,76,0.2)]';
    }
    return 'bg-[rgba(255,255,255,0.05)] text-[#9ca3af] border border-[rgba(255,255,255,0.08)]';
  }

  protected refineStory(item: DashboardStoryItem): void {
    this.state.refineStoryInChat(item.id, item.title);
    this.refineRequested.emit();
  }

  protected runDetailedAudit(storyId: string): void {
    this.detailedAudits[storyId] = {loading: true};
    this.state.auditStory(storyId).subscribe({
      next: (response) => {
        const replyText = response.message?.parts?.[0]?.text || 'No se obtuvo reporte.';
        this.detailedAudits[storyId] = {loading: false, content: replyText};
      },
      error: (error) => {
        console.error(error);
        this.detailedAudits[storyId] = {loading: false, error: 'Error al procesar la auditoría.'};
      }
    });
  }
}

