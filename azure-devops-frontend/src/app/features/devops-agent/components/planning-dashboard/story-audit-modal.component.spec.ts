import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StoryAuditModalComponent } from './story-audit-modal.component';
import { DashboardStoryItem } from '../../models/devops-agent.model';

describe('GIVEN StoryAuditModalComponent', () => {
  let fixture: ComponentFixture<StoryAuditModalComponent>;
  let component: StoryAuditModalComponent;

  const sampleStory: DashboardStoryItem = {
    id: 'HU-123',
    title: 'Autenticación Biométrica',
    points: 5,
    state: 'Active',
    hasAcceptanceCriteria: true,
    hasDoD: false,
    qualityScore: 70,
    linkedTasksCount: 1,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StoryAuditModalComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(StoryAuditModalComponent);
    component = fixture.componentInstance;
  });

  describe('WHEN visible is true and a story is passed', () => {
    it('THEN renders the modal header and details', () => {
      component.visible = true;
      component.story = sampleStory;
      fixture.detectChanges();

      const text = fixture.nativeElement.textContent;
      expect(text).toContain('HU-123');
      expect(text).toContain('Autenticación Biométrica');
    });

    it('THEN emits visibleChange event with false when close button is clicked', () => {
      component.visible = true;
      component.story = sampleStory;
      fixture.detectChanges();

      let visibleEmitted: boolean | null = null;
      component.visibleChange.subscribe((val) => (visibleEmitted = val));

      const closeBtn: HTMLButtonElement = fixture.nativeElement.querySelector('button');
      closeBtn.click();

      expect(visibleEmitted).toBe(false);
    });

    it('THEN emits runAudit event when audit button is clicked', () => {
      component.visible = true;
      component.story = sampleStory;
      fixture.detectChanges();

      let requestedStoryId: string | null = null;
      component.runAudit.subscribe((id) => (requestedStoryId = id));

      const auditBtn: HTMLButtonElement =
        fixture.nativeElement.querySelector('button.bg-gradient-to-r');
      auditBtn.click();

      expect(requestedStoryId).toBe('HU-123');
    });
  });

  describe('WHEN visible is false', () => {
    it('THEN does not render modal content', () => {
      component.visible = false;
      component.story = sampleStory;
      fixture.detectChanges();

      expect(fixture.nativeElement.children.length).toBe(0);
    });
  });
});
