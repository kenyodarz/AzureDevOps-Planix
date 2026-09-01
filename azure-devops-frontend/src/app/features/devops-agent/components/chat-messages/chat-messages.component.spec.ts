import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ChatMessagesComponent } from './chat-messages.component';
import { Message } from '../../models/devops-agent.model';

/** Caracterización FASE 01 — congela el render de la conversación. */
describe('GIVEN ChatMessagesComponent', () => {
  const SCROLL_DELAY_MS = 50;

  let fixture: ComponentFixture<ChatMessagesComponent>;

  const userMessage = (text: string): Message => ({
    role: 'user',
    messageId: `u-${text}`,
    parts: [{ text }],
  });

  const agentMessage = (text: string): Message => ({
    role: 'agent',
    messageId: `a-${text}`,
    parts: [{ text }],
  });

  const bubbles = (): HTMLElement[] =>
    Array.from(fixture.nativeElement.querySelectorAll('.animate-fadeIn'));

  const typingIndicator = (): HTMLElement | null =>
    fixture.nativeElement.querySelector('.animate-bounce');

  beforeEach(async () => {
    vi.useFakeTimers();
    await TestBed.configureTestingModule({ imports: [ChatMessagesComponent] }).compileComponents();
    fixture = TestBed.createComponent(ChatMessagesComponent);
  });

  afterEach(() => {
    vi.clearAllTimers();
    vi.useRealTimers();
  });

  describe('WHEN a list of messages is provided', () => {
    it('THEN renders one bubble per message', () => {
      fixture.componentRef.setInput('messages', [
        agentMessage('hola'),
        userMessage('que tal'),
        agentMessage('bien'),
      ]);

      fixture.detectChanges();

      expect(bubbles()).toHaveLength(3);
    });

    it('THEN aligns user messages to the right and agent messages to the left', () => {
      fixture.componentRef.setInput('messages', [agentMessage('hola'), userMessage('que tal')]);

      fixture.detectChanges();

      const [first, second] = bubbles();
      expect(first.classList).toContain('justify-start');
      expect(second.classList).toContain('justify-end');
    });

    it('THEN renders the message text', () => {
      fixture.componentRef.setInput('messages', [agentMessage('contenido visible')]);

      fixture.detectChanges();

      expect(fixture.nativeElement.textContent).toContain('contenido visible');
    });
  });

  describe('WHEN messages is null', () => {
    it('THEN renders nothing without failing', () => {
      fixture.componentRef.setInput('messages', null);

      fixture.detectChanges();

      expect(bubbles()).toHaveLength(0);
    });
  });

  describe('WHEN loading is true', () => {
    it('THEN shows the typing indicator', () => {
      fixture.componentRef.setInput('messages', []);
      fixture.componentRef.setInput('loading', true);

      fixture.detectChanges();

      expect(typingIndicator()).not.toBeNull();
    });

    it('THEN hides the typing indicator once loading stops', () => {
      fixture.componentRef.setInput('messages', []);
      fixture.componentRef.setInput('loading', true);
      fixture.detectChanges();

      fixture.componentRef.setInput('loading', false);
      fixture.detectChanges();

      expect(typingIndicator()).toBeNull();
    });
  });

  describe('WHEN the message list changes', () => {
    // D-22 — El auto-scroll usa un `setTimeout` que nadie cancela en `ngOnDestroy`.
    // Esta prueba deja constancia; la FASE 08 lo corregirá.
    it('THEN schedules a deferred scroll that is never cancelled (D-22, corregir en fase 08)', () => {
      fixture.componentRef.setInput('messages', [agentMessage('hola')]);
      fixture.detectChanges();

      expect(vi.getTimerCount()).toBeGreaterThan(0);

      fixture.destroy();

      expect(vi.getTimerCount()).toBeGreaterThan(0);
      vi.advanceTimersByTime(SCROLL_DELAY_MS);
    });
  });
});
