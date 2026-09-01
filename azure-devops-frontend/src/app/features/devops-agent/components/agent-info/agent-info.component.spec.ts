import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AgentInfoComponent } from './agent-info.component';
import { AgentCard } from '../../models/devops-agent.model';

/** Caracterización FASE 01 — congela el render actual del panel de información del agente. */
describe('GIVEN AgentInfoComponent', () => {
  let fixture: ComponentFixture<AgentInfoComponent>;

  const textOf = (): string => fixture.nativeElement.textContent as string;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [AgentInfoComponent] }).compileComponents();
    fixture = TestBed.createComponent(AgentInfoComponent);
  });

  describe('WHEN a card is provided', () => {
    it('THEN renders name, version and description', () => {
      const card: AgentCard = { name: 'Scrum Master', version: '1.2.3', description: 'Un agente' };
      fixture.componentRef.setInput('card', card);

      fixture.detectChanges();

      expect(textOf()).toContain('Scrum Master');
      expect(textOf()).toContain('1.2.3');
      expect(textOf()).toContain('Un agente');
    });
  });

  describe('WHEN the card is null', () => {
    it('THEN renders the loading placeholder without failing', () => {
      fixture.componentRef.setInput('card', null);

      fixture.detectChanges();

      expect(textOf()).toContain('Cargando...');
    });
  });
});
