import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal, WritableSignal } from '@angular/core';
import { By } from '@angular/platform-browser';
import { of, throwError } from 'rxjs';
import {
  DEFAULT_TECHNICAL_FRONTS,
  ProgramPlanningModalComponent,
} from './program-planning-modal.component';
import { PlanningStateService } from '../../services/state/planning-state.service';
import { NotificationService } from '../../../../core';
import { ProgramPlanRequestDTO, ProgramPlanResponseDTO } from '../../models/devops-agent.model';

class MockPlanningStateService {
  readonly planningRunningSignal: WritableSignal<boolean> = signal<boolean>(false);

  readonly triggerProgramPlanning = vi.fn((_request: ProgramPlanRequestDTO) =>
    of<ProgramPlanResponseDTO>({
      summary: 'Planeación trimestral generada con éxito',
      quarter: 'Q3-2026',
      sprintCount: 6,
      maxCapacityPerSprint: 34,
      targetFronts: ['Canales', 'Core', 'DevOps'],
      contextId: 'ctx-prog-1',
      task: {
        id: 'task-prog-1',
        contextId: 'ctx-prog-1',
        status: { state: 'submitted' },
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

describe('GIVEN ProgramPlanningModalComponent', () => {
  let component: ProgramPlanningModalComponent;
  let fixture: ComponentFixture<ProgramPlanningModalComponent>;
  let mockPlanningState: MockPlanningStateService;
  let mockNotifications: MockNotificationService;

  beforeEach(async () => {
    mockPlanningState = new MockPlanningStateService();
    mockNotifications = new MockNotificationService();

    await TestBed.configureTestingModule({
      imports: [ProgramPlanningModalComponent],
      providers: [
        { provide: PlanningStateService, useValue: mockPlanningState },
        { provide: NotificationService, useValue: mockNotifications },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProgramPlanningModalComponent);
    component = fixture.componentInstance;
  });

  describe('WHEN el modal está oculto (visible = false)', () => {
    it('THEN no debe renderizar el diálogo modal en el DOM', () => {
      component.visible = false;
      fixture.detectChanges();

      const dialog = fixture.debugElement.query(By.css('[role="dialog"]'));
      expect(dialog).toBeNull();
    });
  });

  describe('WHEN el modal está visible (visible = true)', () => {
    beforeEach(() => {
      component.visible = true;
      fixture.detectChanges();
    });

    it('THEN debe renderizar el diálogo modal con sus atributos de accesibilidad', () => {
      const dialog = fixture.debugElement.query(By.css('[role="dialog"]'));
      expect(dialog).not.toBeNull();
      expect(dialog.attributes['aria-modal']).toBe('true');
      expect(dialog.attributes['aria-labelledby']).toBe('program-planning-modal-title');

      const title = fixture.debugElement.query(By.css('#program-planning-modal-title'));
      expect(title.nativeElement.textContent.trim()).toBe('Lanzar Program Planning');
    });

    it('THEN debe inicializar los valores por defecto del formulario', () => {
      expect(component.form.value.quarter).toBe('Q3-2026');
      expect(component.form.value.sprintCount).toBe(6);
      expect(component.form.value.maxCapacityPerSprint).toBe(34);
      expect(component.form.value.objectives).toBe('');
      expect(component.selectedFronts()).toEqual(['Canales', 'Core', 'DevOps']);
    });

    it('THEN debe validar el formato estricto del Quarter (Q[1-4]-YYYY)', () => {
      const quarterControl = component.form.get('quarter');

      quarterControl?.setValue('2026');
      expect(quarterControl?.invalid).toBe(true);

      quarterControl?.setValue('Q5-2026');
      expect(quarterControl?.invalid).toBe(true);

      quarterControl?.setValue('Q0-2026');
      expect(quarterControl?.invalid).toBe(true);

      quarterControl?.setValue('Q3');
      expect(quarterControl?.invalid).toBe(true);

      quarterControl?.setValue('Q1-2026');
      expect(quarterControl?.valid).toBe(true);

      quarterControl?.setValue('Q4-2027');
      expect(quarterControl?.valid).toBe(true);
    });

    it('THEN debe validar el rango de Sprints entre 1 y 24', () => {
      const sprintControl = component.form.get('sprintCount');

      sprintControl?.setValue(0);
      expect(sprintControl?.invalid).toBe(true);

      sprintControl?.setValue(-3);
      expect(sprintControl?.invalid).toBe(true);

      sprintControl?.setValue(25);
      expect(sprintControl?.invalid).toBe(true);

      sprintControl?.setValue(1);
      expect(sprintControl?.valid).toBe(true);

      sprintControl?.setValue(24);
      expect(sprintControl?.valid).toBe(true);

      sprintControl?.setValue(12);
      expect(sprintControl?.valid).toBe(true);
    });

    it('THEN debe validar el rango de Capacidad Máxima por Sprint entre 1 y 500 SP', () => {
      const capControl = component.form.get('maxCapacityPerSprint');

      capControl?.setValue(0);
      expect(capControl?.invalid).toBe(true);

      capControl?.setValue(501);
      expect(capControl?.invalid).toBe(true);

      capControl?.setValue(1);
      expect(capControl?.valid).toBe(true);

      capControl?.setValue(500);
      expect(capControl?.valid).toBe(true);

      capControl?.setValue(45);
      expect(capControl?.valid).toBe(true);
    });

    it('THEN debe validar que los objetivos tengan al menos 5 caracteres', () => {
      const objControl = component.form.get('objectives');

      objControl?.setValue('');
      expect(objControl?.invalid).toBe(true);

      objControl?.setValue('Hola');
      expect(objControl?.invalid).toBe(true);

      objControl?.setValue('Objetivos válidos de prueba para el roadmap');
      expect(objControl?.valid).toBe(true);
    });

    it('THEN debe permitir seleccionar y deseleccionar frentes técnicos interactivos', () => {
      expect(component.isFrontSelected('Seguridad')).toBe(false);
      component.toggleFront('Seguridad');
      expect(component.isFrontSelected('Seguridad')).toBe(true);
      expect(component.selectedFronts()).toContain('Seguridad');

      component.toggleFront('Canales');
      expect(component.isFrontSelected('Canales')).toBe(false);
      expect(component.selectedFronts()).not.toContain('Canales');
    });

    it('THEN debe deshabilitar el botón submit si no hay ningún frente seleccionado', () => {
      component.selectedFronts.set([]);
      component.form.patchValue({
        quarter: 'Q3-2026',
        sprintCount: 6,
        maxCapacityPerSprint: 34,
        objectives: 'Objetivos estratégicos completos',
      });
      component.frontsTouched.set(true);
      fixture.detectChanges();

      expect(component.frontsError()).toBe(true);
      expect(component.isSubmitDisabled()).toBe(true);

      const submitBtn = fixture.debugElement.query(By.css('button[type="submit"]'));
      expect(submitBtn.nativeElement.disabled).toBe(true);
    });

    it('THEN debe despachar la planeación al enviar un formulario válido y cerrar el modal al completar', () => {
      const visibleSpy = vi.spyOn(component.visibleChange, 'emit');

      component.form.patchValue({
        quarter: 'Q4-2026',
        sprintCount: 8,
        maxCapacityPerSprint: 40,
        objectives: 'Desplegar nuevos canales móviles e integración BFF en Azure',
      });
      component.selectedFronts.set(['Canales', 'Core']);
      fixture.detectChanges();

      expect(component.isSubmitDisabled()).toBe(false);

      component.submit();

      expect(mockPlanningState.triggerProgramPlanning).toHaveBeenCalledTimes(1);
      expect(mockPlanningState.triggerProgramPlanning).toHaveBeenCalledWith({
        quarter: 'Q4-2026',
        sprintCount: 8,
        maxCapacityPerSprint: 40,
        targetFronts: ['Canales', 'Core'],
        objectives: 'Desplegar nuevos canales móviles e integración BFF en Azure',
      });

      expect(component.visible).toBe(false);
      expect(visibleSpy).toHaveBeenCalledWith(false);
    });

    it('THEN no debe despachar si el formulario es inválido al llamar a submit()', () => {
      component.form.patchValue({
        quarter: 'invalido',
        objectives: '',
      });

      component.submit();

      expect(mockPlanningState.triggerProgramPlanning).not.toHaveBeenCalled();
    });

    it('THEN debe capturar el error de la API sin cerrar el modal ni romper el componente', () => {
      mockPlanningState.triggerProgramPlanning.mockReturnValueOnce(
        throwError(() => new Error('Fallo de red')),
      );

      const visibleSpy = vi.spyOn(component.visibleChange, 'emit');

      component.form.patchValue({
        quarter: 'Q3-2026',
        sprintCount: 6,
        maxCapacityPerSprint: 34,
        objectives: 'Objetivos válidos de prueba',
      });

      component.submit();

      expect(mockPlanningState.triggerProgramPlanning).toHaveBeenCalledTimes(1);
      expect(component.visible).toBe(true);
      expect(visibleSpy).not.toHaveBeenCalled();
    });

    it('THEN debe cerrarse al hacer clic en el botón de cerrar (X)', () => {
      const visibleSpy = vi.spyOn(component.visibleChange, 'emit');

      const closeBtn = fixture.debugElement.query(
        By.css('button[aria-label="Cerrar modal de planeación"]'),
      );
      closeBtn.nativeElement.click();

      expect(component.visible).toBe(false);
      expect(visibleSpy).toHaveBeenCalledWith(false);
    });

    it('THEN debe cerrarse al hacer clic en el botón Cancelar', () => {
      const visibleSpy = vi.spyOn(component.visibleChange, 'emit');

      const cancelBtn = fixture.debugElement.query(
        By.css('button[aria-label="Cancelar y cerrar modal"]'),
      );
      cancelBtn.nativeElement.click();

      expect(component.visible).toBe(false);
      expect(visibleSpy).toHaveBeenCalledWith(false);
    });

    it('THEN debe cerrarse con la tecla Escape', () => {
      const visibleSpy = vi.spyOn(component.visibleChange, 'emit');

      component.onEscape();

      expect(component.visible).toBe(false);
      expect(visibleSpy).toHaveBeenCalledWith(false);
    });

    it('THEN debe cerrarse al hacer clic en el backdrop', () => {
      const visibleSpy = vi.spyOn(component.visibleChange, 'emit');

      const dialogEl = fixture.debugElement.query(By.css('[role="dialog"]')).nativeElement;
      const fakeEvent = {
        target: dialogEl,
        currentTarget: dialogEl,
      } as unknown as MouseEvent;

      component.onBackdropClick(fakeEvent);

      expect(component.visible).toBe(false);
      expect(visibleSpy).toHaveBeenCalledWith(false);
    });

    it('THEN no debe cerrarse con backdrop ni Escape ni botón de cerrar si planningRunning está activo', () => {
      mockPlanningState.planningRunningSignal.set(true);
      fixture.detectChanges();

      const visibleSpy = vi.spyOn(component.visibleChange, 'emit');

      component.onEscape();
      expect(component.visible).toBe(true);

      component.close();
      expect(component.visible).toBe(true);
      expect(visibleSpy).not.toHaveBeenCalled();

      const closeBtn = fixture.debugElement.query(
        By.css('button[aria-label="Cerrar modal de planeación"]'),
      );
      expect(closeBtn.nativeElement.disabled).toBe(true);
    });

    it('THEN debe mostrar spinner e inhabilitar envío cuando planningRunning sea true', () => {
      mockPlanningState.planningRunningSignal.set(true);
      fixture.detectChanges();

      const submitBtn = fixture.debugElement.query(
        By.css('button[aria-label="Iniciar planeación del programa"]'),
      );
      expect(submitBtn.nativeElement.disabled).toBe(true);
      expect(submitBtn.nativeElement.textContent).toContain('Planificando Programa...');
    });
  });

  describe('WHEN se cargan los frentes por defecto', () => {
    it('THEN debe tener definidos los frentes técnicos estándar', () => {
      expect(DEFAULT_TECHNICAL_FRONTS).toEqual([
        'Canales',
        'Core',
        'DevOps',
        'Seguridad',
        'Datos',
        'Arquitectura',
      ]);
    });
  });
});
