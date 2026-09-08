import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal, WritableSignal } from '@angular/core';
import { By } from '@angular/platform-browser';
import { of, throwError } from 'rxjs';
import {
  AVAILABLE_FRONTS,
  ProgramPlanningWizardComponent,
} from './program-planning-wizard.component';
import { PlanningStateService } from '../../services/state/planning-state.service';
import { DevopsAgentApiService } from '../../services/devops-agent-api.service';
import { NotificationService } from '../../../../core';
import {
  ProgramPlanRequestDTO,
  ProgramPlanResponseDTO,
  SendMessageResponse,
} from '../../models/devops-agent.model';

class MockPlanningStateService {
  readonly planningRunningSignal: WritableSignal<boolean> = signal<boolean>(false);
  readonly loadAvailableSpecs = vi.fn();

  readonly triggerProgramPlanning = vi.fn((_request: ProgramPlanRequestDTO) =>
    of<ProgramPlanResponseDTO>({
      summary: 'Planeación trimestral generada con éxito',
      quarter: 'Q3-2026',
      sprintCount: 6,
      maxCapacityPerSprint: 28,
      targetFronts: ['Canales', 'BFF', 'Core', 'Datos', 'DevOps'],
      contextId: 'ctx-prog-wizard-1',
      task: {
        id: 'task-prog-wizard-1',
        contextId: 'ctx-prog-wizard-1',
        status: { state: 'submitted' },
      },
    }),
  );
}

class MockDevopsAgentApiService {
  readonly sendMessage = vi.fn(() =>
    of<SendMessageResponse>({
      message: {
        role: 'agent',
        messageId: 'reply-123',
        contextId: 'ctx-123',
        parts: [{ text: 'Ajusté el frente de Kafka para adelantar sus contratos.' }],
      },
    }),
  );
}

class MockNotificationService {
  readonly success = vi.fn();
  readonly error = vi.fn();
  readonly info = vi.fn();
  readonly warn = vi.fn();
}

describe('GIVEN ProgramPlanningWizardComponent', () => {
  let component: ProgramPlanningWizardComponent;
  let fixture: ComponentFixture<ProgramPlanningWizardComponent>;
  let mockPlanningState: MockPlanningStateService;
  let mockDevopsApi: MockDevopsAgentApiService;
  let mockNotifications: MockNotificationService;

  beforeEach(async () => {
    mockPlanningState = new MockPlanningStateService();
    mockDevopsApi = new MockDevopsAgentApiService();
    mockNotifications = new MockNotificationService();

    await TestBed.configureTestingModule({
      imports: [ProgramPlanningWizardComponent],
      providers: [
        { provide: PlanningStateService, useValue: mockPlanningState },
        { provide: DevopsAgentApiService, useValue: mockDevopsApi },
        { provide: NotificationService, useValue: mockNotifications },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProgramPlanningWizardComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('WHEN visible is false', () => {
    it('THEN the wizard dialog is NOT rendered in the DOM', () => {
      component.visible = false;
      fixture.detectChanges();

      const dialog = fixture.debugElement.query(By.css('[role="dialog"]'));
      expect(dialog).toBeNull();
    });
  });

  describe('WHEN visible is true', () => {
    beforeEach(() => {
      component.visible = true;
      fixture.detectChanges();
    });

    it('THEN renders the wizard dialog with title and step navigation', () => {
      const title = fixture.debugElement.query(By.css('#wizard-modal-title'));
      expect(title).not.toBeNull();
      expect(title.nativeElement.textContent.trim()).toBe('Asistente de Planeación de Iniciativa');
      expect(component.currentStep()).toBe(1);
    });

    it('THEN validates Step 1 (Iniciativa & Negocio) inputs', () => {
      component.initiativeForm.patchValue({
        title: '',
        problemAndValue: '',
      });
      component.initiativeForm.markAllAsTouched();
      fixture.detectChanges();

      expect(component.initiativeForm.invalid).toBe(true);
      expect(component.isNextDisabled()).toBe(true);

      component.initiativeForm.patchValue({
        title: 'Guardián de la Experiencia',
        problemAndValue: 'Solución para resolver incidentes transaccionales con reglas híbridas.',
      });
      fixture.detectChanges();

      expect(component.initiativeForm.valid).toBe(true);
      expect(component.isNextDisabled()).toBe(false);
    });

    it('THEN advances to Step 2 and toggles technical fronts', () => {
      component.nextStep();
      fixture.detectChanges();

      expect(component.currentStep()).toBe(2);

      const initialCount = component.selectedFronts().length;
      component.toggleFront('Broker');
      fixture.detectChanges();

      expect(component.selectedFronts().includes('Broker')).toBe(true);
      expect(component.selectedFronts().length).toBe(initialCount + 1);

      // Deseleccionar
      component.toggleFront('Broker');
      fixture.detectChanges();
      expect(component.selectedFronts().includes('Broker')).toBe(false);
    });

    it('THEN validates Step 2 requires at least one technical front', () => {
      component.currentStep.set(2);
      component.selectedFronts.set([]);
      fixture.detectChanges();

      expect(component.isNextDisabled()).toBe(true);

      component.selectedFronts.set(['Canales']);
      fixture.detectChanges();

      expect(component.isNextDisabled()).toBe(false);
    });

    it('THEN navigates to Step 3 and validates quarter & sprints', () => {
      component.currentStep.set(3);
      component.horizonForm.patchValue({
        quarter: 'INVALID',
        sprintCount: 0,
      });
      fixture.detectChanges();

      expect(component.horizonForm.invalid).toBe(true);
      expect(component.isNextDisabled()).toBe(true);

      component.horizonForm.patchValue({
        quarter: 'Q3-2026',
        sprintCount: 6,
      });
      fixture.detectChanges();

      expect(component.horizonForm.valid).toBe(true);
      expect(component.isNextDisabled()).toBe(false);
    });

    it('THEN computes capacity diagnosis in Step 4 automatically without PO manual capacity input', () => {
      vi.useFakeTimers();
      try {
        component.currentStep.set(3);
        component.maxVisitedStep.set(3);
        component.nextStep();
        fixture.detectChanges();

        expect(component.currentStep()).toBe(4);
        expect(component.calculatingDiagnosis()).toBe(true);

        vi.advanceTimersByTime(500);
        fixture.detectChanges();

        expect(component.calculatingDiagnosis()).toBe(false);
        const diag = component.diagnosis();
        expect(diag).not.toBeNull();
        expect(diag!.totalStoryPoints).toBeGreaterThan(0);
        expect(diag!.recommendedCapacityPerSprint).toBeGreaterThan(0);
        expect(diag!.viabilityScore).toBe(94);
        expect(diag!.frontsBreakdown.length).toBe(component.selectedFronts().length);
      } finally {
        vi.useRealTimers();
      }
    });

    it('THEN renders the SPEC Markdown in Step 5 and enables Copilot chat via BFF', () => {
      vi.useFakeTimers();
      try {
        component.initiativeForm.patchValue({
          title: 'Guardián de la Experiencia',
          problemAndValue: 'Solución para resolver incidentes transaccionales con reglas híbridas.',
        });
        component.selectedFronts.set(['Canales', 'BFF', 'Broker']);
        component.currentStep.set(4);
        component.runCapacityCalculation();
        vi.advanceTimersByTime(500);

        component.nextStep();
        fixture.detectChanges();

        expect(component.currentStep()).toBe(5);
        expect(component.generatedSpec()).toContain('# Ideas de Planeación - Q3-2026');
        expect(component.generatedSpec()).toContain('## 1. Visión y Objetivos de Negocio');
        expect(component.generatedSpec()).toContain('## 4. Distribución del Roadmap por Sprints');
        // Debe incluir Broker en la tabla
        expect(component.generatedSpec()).toContain('Broker');
        expect(component.copilotMessages().length).toBeGreaterThanOrEqual(1);

        // Enviar mensaje a Copilot
        component.copilotPrompt = 'Mover Kafka al sprint 1';
        component.sendCopilotMessage();
        fixture.detectChanges();

        expect(mockDevopsApi.sendMessage).toHaveBeenCalledTimes(1);
        expect(component.copilotThinking()).toBe(false);
        const lastMsg = component.copilotMessages()[component.copilotMessages().length - 1];
        expect(lastMsg.sender).toBe('agent');
        expect(lastMsg.text).toContain('Kafka');
      } finally {
        vi.useRealTimers();
      }
    });

    it('THEN copies generated markdown when clicking copy button', () => {
      const writeTextSpy = vi.fn();
      Object.assign(navigator, {
        clipboard: { writeText: writeTextSpy },
      });

      component.currentStep.set(5);
      component.ensureSpecGenerated();
      fixture.detectChanges();

      component.copySpecMarkdown();

      expect(writeTextSpy).toHaveBeenCalledWith(component.generatedSpec());
      expect(mockNotifications.success).toHaveBeenCalledWith('Contenido Markdown copiado al portapapeles');
    });

    it('THEN approving in Step 5 calls triggerProgramPlanning and emits initiativeSaved', () => {
      const savedSpy = vi.fn();
      component.initiativeSaved.subscribe(savedSpy);

      component.currentStep.set(5);
      component.ensureSpecGenerated();
      fixture.detectChanges();

      component.approveAndSave();

      expect(mockPlanningState.triggerProgramPlanning).toHaveBeenCalledTimes(1);
      expect(savedSpy).toHaveBeenCalled();
      expect(mockNotifications.success).toHaveBeenCalledWith(
        expect.stringMatching(/aprobada.*(encolada en el agente|persistida con éxito)/),
      );
      expect(mockPlanningState.loadAvailableSpecs).toHaveBeenCalledTimes(1);
      expect(component.visible).toBe(false);
    });

    it('THEN closes on backdrop click if planning is not running', () => {
      const closeSpy = vi.fn();
      component.visibleChange.subscribe(closeSpy);

      const fakeEvent = {
        target: fixture.nativeElement.querySelector('[role="dialog"]'),
        currentTarget: fixture.nativeElement.querySelector('[role="dialog"]'),
      } as unknown as MouseEvent;

      component.onBackdropClick(fakeEvent);

      expect(component.visible).toBe(false);
      expect(closeSpy).toHaveBeenCalledWith(false);
    });

    it('THEN closes on Escape key', () => {
      const closeSpy = vi.fn();
      component.visibleChange.subscribe(closeSpy);

      component.onEscape();

      expect(component.visible).toBe(false);
      expect(closeSpy).toHaveBeenCalledWith(false);
    });
  });
});
