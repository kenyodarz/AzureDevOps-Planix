import { ComponentFixture, TestBed } from '@angular/core/testing';
import { QualityStoriesTableComponent } from './quality-stories-table.component';
import { DashboardStoryItem } from '../../models/devops-agent.model';

describe('GIVEN QualityStoriesTableComponent', () => {
  let fixture: ComponentFixture<QualityStoriesTableComponent>;
  let component: QualityStoriesTableComponent;

  const sampleItems: DashboardStoryItem[] = [
    {
      id: 'STORY-101',
      title: 'Historia de Prueba',
      points: 8,
      state: 'Active',
      assignedMember: 'Dev One',
      hasAcceptanceCriteria: true,
      hasDoD: true,
      qualityScore: 85,
      linkedTasksCount: 2,
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QualityStoriesTableComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(QualityStoriesTableComponent);
    component = fixture.componentInstance;
  });

  describe('WHEN rendered with story items', () => {
    it('THEN displays the story information in the table', () => {
      component.items = sampleItems;
      component.filteredItems = sampleItems;
      fixture.detectChanges();

      const text = fixture.nativeElement.textContent;
      expect(text).toContain('STORY-101');
      expect(text).toContain('Historia de Prueba');
      expect(text).toContain('Dev One');
      expect(text).toContain('8 SP');
      expect(text).toContain('Active');
    });

    it('THEN emits refineStory event when refine button is clicked', () => {
      component.items = sampleItems;
      component.filteredItems = sampleItems;
      fixture.detectChanges();

      let emittedItem: DashboardStoryItem | null = null;
      component.refineStory.subscribe((item) => (emittedItem = item));

      const refineBtn: HTMLButtonElement =
        fixture.nativeElement.querySelector('button.bg-gradient-to-r');
      refineBtn.click();

      expect(emittedItem).toEqual(sampleItems[0]);
    });

    it('THEN emits toggleExpand event when chevron button is clicked', () => {
      component.items = sampleItems;
      component.filteredItems = sampleItems;
      fixture.detectChanges();

      let expandedId: string | null = null;
      component.toggleExpand.subscribe((id) => (expandedId = id));

      const toggleBtn: HTMLButtonElement = fixture.nativeElement.querySelector('tbody td button');
      toggleBtn.click();

      expect(expandedId).toBe('STORY-101');
    });

    it('THEN emits runDetailedAudit event when audit button is clicked in expanded row', () => {
      component.items = sampleItems;
      component.filteredItems = sampleItems;
      component.expandedItemIds = new Set(['STORY-101']);
      fixture.detectChanges();

      let auditedId: string | null = null;
      component.runDetailedAudit.subscribe((id) => (auditedId = id));

      const auditBtn: HTMLButtonElement = fixture.nativeElement.querySelectorAll('button')[2];
      auditBtn.click();

      expect(auditedId).toBe('STORY-101');
    });
  });
});
