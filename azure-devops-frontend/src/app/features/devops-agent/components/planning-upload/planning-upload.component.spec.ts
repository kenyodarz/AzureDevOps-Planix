import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PlanningUploadComponent } from './planning-upload.component';

interface UploadPayload {
  initiativeId: string;
  title: string;
  content: string;
}

/** Caracterización FASE 01 — congela el contrato de carga de planeaciones. */
describe('GIVEN PlanningUploadComponent', () => {
  // El DOM normaliza los colores hexadecimales del componente a notación `rgb()`.
  const SUCCESS_COLOR = 'rgb(16, 185, 129)'; // #10b981
  const ERROR_COLOR = 'rgb(239, 68, 68)'; // #ef4444
  const NEUTRAL_COLOR = 'rgb(156, 163, 175)'; // #9ca3af

  let fixture: ComponentFixture<PlanningUploadComponent>;
  let emitted: UploadPayload[];

  const inputs = (): HTMLInputElement[] =>
    Array.from(fixture.nativeElement.querySelectorAll('input[type="text"]'));

  const fileInput = (): HTMLInputElement =>
    fixture.nativeElement.querySelector('input[type="file"]') as HTMLInputElement;

  const vectorizeButton = (): HTMLButtonElement =>
    fixture.nativeElement.querySelector('button') as HTMLButtonElement;

  const statusColor = (): string | null =>
    (fixture.nativeElement.querySelector('.text-center.mt-1') as HTMLElement | null)?.style.color ??
    null;

  const fillText = (element: HTMLInputElement, value: string): void => {
    element.value = value;
    element.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  };

  /** Simula la selección de un archivo sin depender de la API real de `FileList`. */
  const selectFile = (name: string, content: string): void => {
    const file = new File([content], name, { type: 'text/markdown' });
    Object.defineProperty(fileInput(), 'files', { value: [file], configurable: true });
    fileInput().dispatchEvent(new Event('change'));
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PlanningUploadComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(PlanningUploadComponent);
    emitted = [];
    fixture.componentInstance.upload.subscribe((payload: UploadPayload) => emitted.push(payload));
    fixture.detectChanges();
  });

  describe('WHEN the form is incomplete', () => {
    it('THEN the vectorize button stays disabled', () => {
      expect(vectorizeButton().disabled).toBe(true);
    });

    it('THEN filling only the identifier keeps it disabled', () => {
      fillText(inputs()[0], 'guardian-q3');

      expect(vectorizeButton().disabled).toBe(true);
    });

    it('THEN filling both texts without a file keeps it disabled', () => {
      fillText(inputs()[0], 'guardian-q3');
      fillText(inputs()[1], 'Guardian Q3');

      expect(vectorizeButton().disabled).toBe(true);
    });
  });

  describe('WHEN a file is selected', () => {
    it('THEN the label shows the file name', () => {
      selectFile('planeacion.md', '# contenido');

      expect(fixture.nativeElement.textContent).toContain('planeacion.md');
    });

    it('THEN the placeholder is shown again when the selection is cleared', () => {
      selectFile('planeacion.md', '# contenido');

      Object.defineProperty(fileInput(), 'files', { value: [], configurable: true });
      fileInput().dispatchEvent(new Event('change'));
      fixture.detectChanges();

      expect(fixture.nativeElement.textContent).toContain('Seleccionar .md');
    });
  });

  describe('WHEN the form is complete and submitted', () => {
    it('THEN emits the trimmed identifier, title and file content', async () => {
      fillText(inputs()[0], '  guardian-q3  ');
      fillText(inputs()[1], '  Guardian Q3  ');
      selectFile('planeacion.md', '# contenido del archivo');

      vectorizeButton().click();
      await fixture.whenStable();

      expect(emitted).toEqual([
        {
          initiativeId: 'guardian-q3',
          title: 'Guardian Q3',
          content: '# contenido del archivo',
        },
      ]);
    });

    it('THEN resets the form after emitting', async () => {
      fillText(inputs()[0], 'guardian-q3');
      fillText(inputs()[1], 'Guardian Q3');
      selectFile('planeacion.md', '# contenido');

      vectorizeButton().click();
      await fixture.whenStable();
      fixture.detectChanges();

      expect(fixture.nativeElement.textContent).toContain('Seleccionar .md');
      expect(vectorizeButton().disabled).toBe(true);
    });
  });

  describe('WHEN uploading is true', () => {
    it('THEN the button is disabled and shows the progress label', () => {
      fixture.componentRef.setInput('uploading', true);

      fixture.detectChanges();

      expect(vectorizeButton().disabled).toBe(true);
      expect(vectorizeButton().textContent?.trim()).toBe('Vectorizando...');
    });
  });

  describe('WHEN an upload status is provided', () => {
    it('THEN a success message is painted green', () => {
      fixture.componentRef.setInput('uploadStatus', '¡Planeación indexada con éxito!');

      fixture.detectChanges();

      expect(statusColor()).toBe(SUCCESS_COLOR);
    });

    it('THEN an error message is painted red', () => {
      fixture.componentRef.setInput('uploadStatus', 'Error: formato inválido');

      fixture.detectChanges();

      expect(statusColor()).toBe(ERROR_COLOR);
    });

    it('THEN any other message is painted neutral', () => {
      fixture.componentRef.setInput('uploadStatus', 'Vectorizando planeación...');

      fixture.detectChanges();

      expect(statusColor()).toBe(NEUTRAL_COLOR);
    });

    it('THEN no status block is rendered when the status is null', () => {
      fixture.componentRef.setInput('uploadStatus', null);

      fixture.detectChanges();

      expect(statusColor()).toBeNull();
    });
  });
});
