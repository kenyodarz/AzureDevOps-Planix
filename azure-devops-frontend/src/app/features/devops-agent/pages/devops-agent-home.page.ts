import {Component, inject, OnDestroy, OnInit, signal} from '@angular/core';
import {AsyncPipe, CommonModule} from '@angular/common';
import {DevopsAgentStateService} from '../services/devops-agent-state.service';
import {Subscription} from 'rxjs';
import {
  AgentInfoComponent,
  ChatInputComponent,
  ChatMessagesComponent,
  InfoCardsComponent,
  PlanningDashboardComponent,
  PlanningManagementComponent,
  PlanningUploadComponent
} from '../components';

@Component({
  selector: 'app-devops-agent-home',
  standalone: true,
  imports: [
    CommonModule,
    AsyncPipe,
    AgentInfoComponent,
    PlanningUploadComponent,
    InfoCardsComponent,
    ChatMessagesComponent,
    ChatInputComponent,
    PlanningManagementComponent,
    PlanningDashboardComponent
  ],
  template: `
    <div class="h-screen w-screen flex overflow-hidden bg-[#0b0f19] text-[#f3f4f6] bg-[radial-gradient(at_0%_0%,rgba(242,201,76,0.05)_0px,transparent_50%),radial-gradient(at_100%_100%,rgba(37,99,235,0.05)_0px,transparent_50%)] font-sans">

      <!-- SIDEBAR -->
      <aside class="w-[320px] bg-[rgba(17,24,39,0.7)] border-r border-[rgba(255,255,255,0.08)] backdrop-blur-md p-6 flex flex-col gap-6 overflow-y-auto h-full shrink-0">
        <div class="logo-container flex items-center gap-3 pb-5 border-b border-[rgba(255,255,255,0.08)]">
          <div class="logo-icon w-9 h-9 bg-[#f2c94c] rounded-lg flex items-center justify-center text-[#000] font-bold text-lg shadow-[0_0_15px_rgba(242,201,76,0.3)]">A</div>
          <div class="logo-text">
            <h1 class="font-semibold text-lg leading-none">DevOps Agent</h1>
            <span class="text-[0.75rem] text-[#9ca3af] uppercase font-medium">Bancolombia</span>
          </div>
        </div>

        <app-agent-info [card]="state.agentCard | async"></app-agent-info>

        @if (activeTab === 'refinement' || activeTab === 'management') {
          <app-planning-upload
            [uploading]="(state.uploading | async) ?? false"
            [uploadStatus]="state.uploadStatus | async"
            (upload)="state.uploadPlanning($event.initiativeId, $event.title, $event.content)"
          ></app-planning-upload>
        }

        <!-- MONITOREO DE TAREAS EN BACKGROUND -->
        @if (activeTab === 'general' || activeTab === 'refinement' || activeTab === 'dashboard') {
          <div
            class="border border-[rgba(255,255,255,0.08)] bg-[rgba(255,255,255,0.02)] rounded-xl p-4 flex flex-col gap-3 shadow-md">
            <button
              type="button"
              [aria-label]="isTasksPanelOpen() ? 'Colapsar panel de tareas' : 'Expandir panel de tareas'"
              class="flex items-center justify-between bg-transparent border-none text-[#f3f4f6] font-bold text-xs cursor-pointer outline-none w-full p-0"
              (click)="toggleTasksPanel()"
            >
              <span class="flex items-center gap-1.5 text-left">
                @if (workingTasksCount() > 0) {
                  <span class="pi pi-cog animate-spin text-[#f2c94c]"></span>
                } @else {
                  <span class="pi pi-list"></span>
                }
                Tareas del Agente ({{ (state.tasks | async)?.length || 0 }})
              </span>
              <span class="pi text-xs text-[#9ca3af]"
                    [ngClass]="isTasksPanelOpen() ? 'pi-chevron-down' : 'pi-chevron-right'"></span>
            </button>

            @if (isTasksPanelOpen()) {
              <div class="flex flex-col gap-2 mt-2 transition-all">
                @if (!(state.tasks | async) || (state.tasks | async)?.length === 0) {
                  <span
                    class="text-[0.7rem] text-[#9ca3af] italic">No hay tareas en ejecución.</span>
                } @else {
                  <div class="flex flex-col gap-2 max-h-[200px] overflow-y-auto pr-1">
                    @for (task of state.tasks | async; track task.id) {
                      <div
                        class="flex flex-col gap-1 bg-[#0b0f19] border border-[rgba(255,255,255,0.06)] rounded p-2.5 text-[0.7rem]">
                        <div class="flex justify-between items-center">
                          <span
                            class="font-mono text-[#2563eb] truncate max-w-[130px] font-bold">#{{ task.id }}</span>
                          <div class="flex items-center gap-1.5">
                            <span
                              class="px-1.5 py-0.5 rounded text-[0.6rem] font-bold uppercase"
                              [ngClass]="{
                                'bg-[rgba(16,185,129,0.1)] text-[#10b981]': task.status?.state === 'completed',
                                'bg-[rgba(245,158,11,0.1)] text-[#f59e0b] animate-pulse': task.status?.state === 'working' || task.status?.state === 'submitted',
                                'bg-[rgba(239,68,68,0.1)] text-[#ef4444]': task.status?.state === 'failed' || task.status?.state === 'canceled' || task.status?.state === 'rejected'
                              }"
                            >
                              {{ task.status?.state || 'Enviado' }}
                            </span>
                            @if (task.status?.state === 'working' || task.status?.state
                            === 'submitted') {
                              <button
                                [aria-label]="'Cancelar tarea ' + task.id"
                                class="bg-transparent border-none text-[#ef4444] hover:text-[#dc2626] cursor-pointer p-0 flex items-center justify-center align-middle"
                                (click)="state.cancelTask(task.id)"
                              >
                                <span class="pi pi-times-circle" style="font-size: 0.9rem"></span>
                              </button>
                            }
                          </div>
                        </div>
                        @if (task.status?.message?.parts?.[0]?.text) {
                          <p class="text-[0.65rem] text-[#9ca3af] mt-1 leading-snug line-clamp-3">
                            {{ task.status?.message?.parts?.[0]?.text }}
                          </p>
                        }
                        @if (task.status?.timestamp) {
                          <span class="text-[0.62rem] text-[#6b7280] mt-0.5">
                            Actualizado: {{ task.status?.timestamp }}
                          </span>
                        }
                      </div>
                    }
                  </div>
                }
              </div>
            }
          </div>
        }

        @if (activeTab === 'refinement') {
          <app-info-cards></app-info-cards>
        }
      </aside>

      <!-- MAIN CHAT CONTAINER -->
      <main class="flex-1 flex flex-col h-full relative min-w-0 bg-transparent">
        <header class="h-[70px] border-b border-[rgba(255,255,255,0.08)] flex items-center justify-between px-8 bg-[rgba(11,15,25,0.5)] backdrop-blur-sm z-10 shrink-0">
          <div class="flex items-center gap-3">
            <div class="w-2 h-2 bg-[#10b981] rounded-full shadow-[0_0_8px_#10b981]"></div>
            <div>
              <h2 class="text-base font-semibold text-[#f3f4f6]">Agente Scrum Master</h2>
              <span class="text-[0.75rem] text-[#9ca3af]">En línea - Consola de Pruebas</span>
            </div>
          </div>

          <!-- TABS -->
          <div class="flex items-center gap-6 h-full">
            <button
              [class.text-[#f2c94c]]="activeTab === 'general'"
              [class.border-[#f2c94c]]="activeTab === 'general'"
              [class.text-[#9ca3af]]="activeTab !== 'general'"
              [class.border-transparent]="activeTab !== 'general'"
              class="bg-transparent border-b-2 border-t-0 border-l-0 border-r-0 h-full px-2 font-semibold text-sm cursor-pointer transition-all hover:text-[#f3f4f6] outline-none"
              (click)="activeTab = 'general'"
            >
              Chat General
            </button>
            <button
              [class.text-[#f2c94c]]="activeTab === 'refinement'"
              [class.border-[#f2c94c]]="activeTab === 'refinement'"
              [class.text-[#9ca3af]]="activeTab !== 'refinement'"
              [class.border-transparent]="activeTab !== 'refinement'"
              class="bg-transparent border-b-2 border-t-0 border-l-0 border-r-0 h-full px-2 font-semibold text-sm cursor-pointer transition-all hover:text-[#f3f4f6] outline-none"
              (click)="activeTab = 'refinement'"
            >
              Refinar HU/HA
            </button>
            <button
              [class.text-[#f2c94c]]="activeTab === 'dashboard'"
              [class.border-[#f2c94c]]="activeTab === 'dashboard'"
              [class.text-[#9ca3af]]="activeTab !== 'dashboard'"
              [class.border-transparent]="activeTab !== 'dashboard'"
              class="bg-transparent border-b-2 border-t-0 border-l-0 border-r-0 h-full px-2 font-semibold text-sm cursor-pointer transition-all hover:text-[#f3f4f6] outline-none"
              (click)="activeTab = 'dashboard'"
            >
              Tablero de Calidad
            </button>
            <button
              [class.text-[#f2c94c]]="activeTab === 'management'"
              [class.border-[#f2c94c]]="activeTab === 'management'"
              [class.text-[#9ca3af]]="activeTab !== 'management'"
              [class.border-transparent]="activeTab !== 'management'"
              class="bg-transparent border-b-2 border-t-0 border-l-0 border-r-0 h-full px-2 font-semibold text-sm cursor-pointer transition-all hover:text-[#f3f4f6] outline-none"
              (click)="activeTab = 'management'"
            >
              Gestión de Planeaciones
            </button>
          </div>

          <div>
            @if (activeTab === 'general') {
              <button
                class="bg-[rgba(255,255,255,0.03)] border border-[rgba(255,255,255,0.08)] text-[#9ca3af] hover:bg-[rgba(242,201,76,0.1)] hover:border-[#f2c94c] hover:text-[#f3f4f6] px-4 py-2 rounded-lg text-sm transition-all cursor-pointer"
                (click)="state.clearGeneralChat()"
              >
                Limpiar Chat General
              </button>
            } @else if (activeTab === 'refinement') {
              <button
                class="bg-[rgba(255,255,255,0.03)] border border-[rgba(255,255,255,0.08)] text-[#9ca3af] hover:bg-[rgba(242,201,76,0.1)] hover:border-[#f2c94c] hover:text-[#f3f4f6] px-4 py-2 rounded-lg text-sm transition-all cursor-pointer"
                (click)="state.clearRefinementChat()"
              >
                Limpiar Asistente de Refinamiento
              </button>
            } @else if (activeTab === 'management') {
              <button
                class="bg-[rgba(255,255,255,0.03)] border border-[rgba(255,255,255,0.08)] text-[#9ca3af] hover:bg-[rgba(242,201,76,0.1)] hover:border-[#f2c94c] hover:text-[#f3f4f6] px-4 py-2 rounded-lg text-sm transition-all cursor-pointer flex items-center gap-2"
                (click)="state.loadInitiatives()"
              >
                <span class="pi pi-refresh"></span> Actualizar
              </button>
            }
          </div>
        </header>

        @if (activeTab === 'general') {
          <app-chat-messages
            [messages]="state.generalMessages | async"
            [loading]="(state.loading | async) ?? false"
            class="flex-1 flex flex-col overflow-hidden min-h-0"
          ></app-chat-messages>

          <app-chat-input
            [loading]="(state.loading | async) ?? false"
            (sendMessage)="state.sendGeneralMessage($event)"
            class="block shrink-0"
          ></app-chat-input>
        } @else if (activeTab === 'refinement') {
          <app-chat-messages
            [messages]="state.refinementMessages | async"
            [loading]="(state.loading | async) ?? false"
            class="flex-1 flex flex-col overflow-hidden min-h-0"
          ></app-chat-messages>

          <app-chat-input
            [loading]="(state.loading | async) ?? false"
            (sendMessage)="state.sendRefinementMessage($event)"
            class="block shrink-0"
          ></app-chat-input>
        } @else if (activeTab === 'dashboard') {
          <app-planning-dashboard
            (refineRequested)="activeTab = 'refinement'"
            class="flex-1 flex flex-col overflow-hidden min-h-0"
          ></app-planning-dashboard>
        } @else {
          <app-planning-management class="flex-1 flex flex-col overflow-hidden min-h-0"></app-planning-management>
        }
      </main>

    </div>
  `
})
export class DevopsAgentHomePage implements OnInit, OnDestroy {
  protected readonly state = inject(DevopsAgentStateService);
  protected activeTab = 'general';

  protected isTasksPanelOpen = signal<boolean>(true);
  protected workingTasksCount = signal<number>(0);
  private tasksSub: Subscription | null = null;

  public ngOnInit(): void {
    this.tasksSub = this.state.tasks.subscribe(tasks => {
      const active = (tasks || []).filter(t => t.status?.state === 'working' || t.status?.state === 'submitted');
      this.workingTasksCount.set(active.length);
    });
  }

  public ngOnDestroy(): void {
    if (this.tasksSub) {
      this.tasksSub.unsubscribe();
    }
  }

  protected toggleTasksPanel(): void {
    this.isTasksPanelOpen.update(val => !val);
  }
}
