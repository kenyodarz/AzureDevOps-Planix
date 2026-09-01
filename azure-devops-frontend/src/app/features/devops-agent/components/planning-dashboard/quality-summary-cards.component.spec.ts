import { ComponentFixture, TestBed } from '@angular/core/testing';
import { QualitySummaryCardsComponent } from './quality-summary-cards.component';
import { DashboardMetrics } from '../../models/devops-agent.model';

describe('GIVEN QualitySummaryCardsComponent', () => {
  let fixture: ComponentFixture<QualitySummaryCardsComponent>;
  let component: QualitySummaryCardsComponent;

  const sampleMetrics: DashboardMetrics = {
    completedPoints: 20,
    totalPoints: 30,
    completedPercentage: 66,
    avgQualityScore: 85,
    undocumentedCount: 2,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QualitySummaryCardsComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(QualitySummaryCardsComponent);
    component = fixture.componentInstance;
  });

  describe('WHEN metrics are provided', () => {
    it('THEN renders the 4 summary cards with correct information', () => {
      component.metrics = sampleMetrics;
      component.largeStoriesCount = 3;
      fixture.detectChanges();

      const text = fixture.nativeElement.textContent;
      expect(text).toContain('20 / 30 SP');
      expect(text).toContain('66 %');
      expect(text).toContain('85%');
      expect(text).toContain('Buena (Suficiente)');
      expect(text).toContain('2 HUs');
      expect(text).toContain('3 HUs');
    });
  });

  describe('WHEN metrics are null or undefined', () => {
    it('THEN does not render cards', () => {
      component.metrics = null;
      fixture.detectChanges();

      expect(fixture.nativeElement.children.length).toBe(0);
    });
  });
});
