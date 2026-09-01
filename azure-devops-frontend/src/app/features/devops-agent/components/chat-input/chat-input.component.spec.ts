import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ChatInputComponent } from './chat-input.component';

/**
 * Caracterización FASE 01 — congela el contrato de entrada del chat.
 *
 * D-21 — Las sugerencias rápidas están hardcodeadas dentro del componente. Esta prueba fija las
 * tres actuales para que la FASE 03 pueda externalizarlas sin cambiar lo que ve el usuario.
 */
describe('GIVEN ChatInputComponent', () => {
  const CURRENT_SUGGESTION_LABELS = ['💡 CRUD Aprobadores (Idea corta)', '✅ Aprobado', '🚀 Crear'];

  let fixture: ComponentFixture<ChatInputComponent>;
  let component: ChatInputComponent;
  let emitted: string[];

  const textarea = (): HTMLTextAreaElement =>
    fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;

  const sendButton = (): HTMLButtonElement =>
    Array.from(fixture.nativeElement.querySelectorAll('button')).find(
      (element) => (element as HTMLElement).textContent?.trim() === 'Enviar',
    ) as HTMLButtonElement;

  const suggestionButtons = (): HTMLButtonElement[] =>
    Array.from(fixture.nativeElement.querySelectorAll('button')).filter(
      (element) => (element as HTMLElement).textContent?.trim() !== 'Enviar',
    ) as HTMLButtonElement[];

  const typeText = (value: string): void => {
    const element = textarea();
    element.value = value;
    element.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  };

  beforeEach(async () => {
    vi.useFakeTimers();
    await TestBed.configureTestingModule({ imports: [ChatInputComponent] }).compileComponents();
    fixture = TestBed.createComponent(ChatInputComponent);
    component = fixture.componentInstance;
    emitted = [];
    component.sendMessage.subscribe((text: string) => emitted.push(text));
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.clearAllTimers();
    vi.useRealTimers();
  });

  describe('WHEN the component renders', () => {
    it('THEN shows exactly the current hardcoded suggestions (D-21, externalizar en fase 03)', () => {
      const labels = suggestionButtons().map((button) => button.textContent?.trim() ?? '');

      expect(labels).toEqual(CURRENT_SUGGESTION_LABELS);
    });

    it('THEN the textarea declares an accessible label', () => {
      expect(textarea().getAttribute('aria-label')).toBe('Responder o escribir idea');
    });

    it('THEN the send button starts disabled because the text is empty', () => {
      expect(sendButton().disabled).toBe(true);
    });
  });

  describe('WHEN the user submits text', () => {
    it('THEN emits the trimmed text', () => {
      typeText('  una idea  ');

      sendButton().click();
      fixture.detectChanges();

      expect(emitted).toEqual(['una idea']);
    });

    it('THEN clears its internal buffer so a second submit emits nothing', () => {
      typeText('una idea');

      sendButton().click();
      fixture.detectChanges();
      sendButton().click();
      fixture.detectChanges();

      expect(emitted).toEqual(['una idea']);
    });

    it('THEN pressing Enter without Shift submits the message', () => {
      typeText('otra idea');

      textarea().dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', shiftKey: false }));
      fixture.detectChanges();

      expect(emitted).toEqual(['otra idea']);
    });

    it('THEN pressing Shift+Enter does NOT submit', () => {
      typeText('multilinea');

      textarea().dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', shiftKey: true }));
      fixture.detectChanges();

      expect(emitted).toEqual([]);
    });

    it('THEN blank text is never emitted', () => {
      typeText('    ');

      textarea().dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', shiftKey: false }));
      fixture.detectChanges();

      expect(emitted).toEqual([]);
    });
  });

  describe('WHEN a suggestion is clicked', () => {
    it('THEN emits the suggestion value, not its label', () => {
      suggestionButtons()[1].click();
      fixture.detectChanges();

      expect(emitted).toEqual(['Aprobado']);
    });
  });

  describe('WHEN loading is true', () => {
    beforeEach(() => {
      fixture.componentRef.setInput('loading', true);
      fixture.detectChanges();
    });

    it('THEN every control is disabled', () => {
      expect(sendButton().disabled).toBe(true);
      suggestionButtons().forEach((button) => expect(button.disabled).toBe(true));
    });

    it('THEN typed text is not emitted', () => {
      typeText('bloqueado');

      textarea().dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', shiftKey: false }));
      fixture.detectChanges();

      expect(emitted).toEqual([]);
    });
  });
});
