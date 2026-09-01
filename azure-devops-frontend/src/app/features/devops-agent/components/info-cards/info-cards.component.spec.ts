import { ComponentFixture, TestBed } from '@angular/core/testing';
import { InfoCardsComponent } from './info-cards.component';

/**
 * Caracterización FASE 01.
 *
 * D-21 — Las etiquetas de tecnología están hardcodeadas dentro del componente. Esta prueba las
 * congela tal cual para que la FASE 03 pueda externalizarlas sin cambiar lo que ve el usuario.
 */
describe('GIVEN InfoCardsComponent', () => {
  const CURRENT_TAGS = ['Spring AI', 'WebFlux', 'MCP', 'Azure DevOps', 'OpenAI'];

  let fixture: ComponentFixture<InfoCardsComponent>;

  const textOf = (): string => fixture.nativeElement.textContent as string;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [InfoCardsComponent] }).compileComponents();
    fixture = TestBed.createComponent(InfoCardsComponent);
    fixture.detectChanges();
  });

  describe('WHEN the component renders', () => {
    it('THEN shows the three protocol phases', () => {
      expect(textOf()).toContain('Fase 1:');
      expect(textOf()).toContain('Fase 2:');
      expect(textOf()).toContain('Fase 3:');
    });

    it('THEN shows exactly the current hardcoded technology tags (D-21, externalizar en fase 03)', () => {
      const rendered: string[] = Array.from(fixture.nativeElement.querySelectorAll('span')).map(
        (element) => (element as HTMLElement).textContent?.trim() ?? '',
      );

      expect(rendered).toEqual(CURRENT_TAGS);
    });
  });
});
