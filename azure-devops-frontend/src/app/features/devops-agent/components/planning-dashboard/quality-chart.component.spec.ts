import { ComponentFixture, TestBed } from '@angular/core/testing';
import { QualityChartComponent } from './quality-chart.component';
import { DashboardStoryItem } from '../../models/devops-agent.model';

describe('GIVEN QualityChartComponent', () => {
  let fixture: ComponentFixture<QualityChartComponent>;
  let component: QualityChartComponent;

  const sampleItems: DashboardStoryItem[] = [
    {
      id: '1',
      title: 'Story 1',
      points: 3,
      state: 'Active',
      hasAcceptanceCriteria: true,
      hasDoD: true,
      qualityScore: 90,
      linkedTasksCount: 0,
    },
    {
      id: '2',
      title: 'Story 2',
      points: 5,
      state: 'Active',
      hasAcceptanceCriteria: true,
      hasDoD: true,
      qualityScore: 60,
      linkedTasksCount: 0,
    },
    {
      id: '3',
      title: 'Story 3',
      points: 8,
      state: 'Active',
      hasAcceptanceCriteria: false,
      hasDoD: false,
      qualityScore: 40,
      linkedTasksCount: 0,
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QualityChartComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(QualityChartComponent);
    component = fixture.componentInstance;
  });

  describe('WHEN rendered with metrics and story items', () => {
    it('THEN renders the distribution stats correctly', () => {
      component.metrics = {
        totalPoints: 16,
        completedPoints: 0,
        completedPercentage: 0,
        avgQualityScore: 63,
        undocumentedCount: 1,
      };
      component.items = sampleItems;
      fixture.detectChanges();

      const text = fixture.nativeElement.textContent;
      expect(text).toContain('Distribución de Calidad del Sprint');
      expect(text).toContain('3 Historias evaluadas');
      expect(text).toContain('Buenas');
      expect(text).toContain('Regulares (50-79%):');
      expect(text).toContain('Críticas (< 50%):');
    });
  });
});
