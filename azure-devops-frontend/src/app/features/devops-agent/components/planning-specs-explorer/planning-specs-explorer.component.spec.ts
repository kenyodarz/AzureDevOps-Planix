import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal, WritableSignal } from '@angular/core';
import { By } from '@angular/platform-browser';
import { PlanningSpecsExplorerComponent } from './planning-specs-explorer.component';
import { PlanningStateService } from '../../services/state/planning-state.service';
import { RefinementChatStateService } from '../../services/state/refinement-chat-state.service';
import { NotificationService } from '../../../../core';
import { SpecDocumentDTO } from '../../models/devops-agent.model';

class MockPlanningStateService {
  readonly availableSpecsSignal: WritableSignal<string[]> = signal<string[]>([]);
  readonly selectedSpecSignal: WritableSignal<SpecDocumentDTO | null> =
    signal<SpecDocumentDTO | null>(null);
  readonly loadingSpecsSignal: WritableSignal<boolean> = signal<boolean>(false);

  readonly loadAvailableSpecs = vi.fn((): void => undefined);
  readonly selectSpec = vi.fn((name: string): void => {
    this.selectedSpecSignal.set({
      name,
      path: `specs/${name}`,
      content: `# Contenido de ${name}`,
    });
  });
}

class MockRefinementChatStateService {
  readonly prefillPrompt = vi.fn((_prompt: string): void => undefined);
}

class MockNotificationService {
  readonly info = vi.fn();
  readonly success = vi.fn();
  readonly error = vi.fn();
}

describe('GIVEN PlanningSpecsExplorerComponent', () => {
  let component: PlanningSpecsExplorerComponent;
  let fixture: ComponentFixture<PlanningSpecsExplorerComponent>;
  let mockPlanningState: MockPlanningStateService;
  let mockRefinementChatState: MockRefinementChatStateService;
  let mockNotifications: MockNotificationService;

  beforeEach(async () => {
    mockPlanningState = new MockPlanningStateService();
    mockRefinementChatState = new MockRefinementChatStateService();
    mockNotifications = new MockNotificationService();

    await TestBed.configureTestingModule({
      imports: [PlanningSpecsExplorerComponent],
      providers: [
        { provide: PlanningStateService, useValue: mockPlanningState },
        { provide: RefinementChatStateService, useValue: mockRefinementChatState },
        { provide: NotificationService, useValue: mockNotifications },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PlanningSpecsExplorerComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('WHEN initialized', () => {
    it('THEN calls loadAvailableSpecs on planningState', () => {
      fixture.detectChanges();
      expect(mockPlanningState.loadAvailableSpecs).toHaveBeenCalledTimes(1);
    });

    it('THEN auto-selects master spec when available specs are loaded and none is selected', () => {
      mockPlanningState.availableSpecsSignal.set([
        'frente_canales.md',
        'ideas_planning_Q3.md',
        'frente_core.md',
      ]);
      fixture.detectChanges();

      expect(mockPlanningState.selectSpec).toHaveBeenCalledWith('ideas_planning_Q3.md');
    });

    it('THEN auto-selects the first spec when no master spec is present', () => {
      mockPlanningState.availableSpecsSignal.set(['frente_canales.md', 'frente_core.md']);
      fixture.detectChanges();

      expect(mockPlanningState.selectSpec).toHaveBeenCalledWith('frente_canales.md');
    });
  });

  describe('WHEN rendering specs list in the sidebar', () => {
    beforeEach(() => {
      mockPlanningState.availableSpecsSignal.set([
        'ideas_planning_Q3.md',
        'frente_canales.md',
        'frente_core.md',
      ]);
      fixture.detectChanges();
    });

    it('THEN renders all available specs with appropriate badges', () => {
      const buttons = fixture.debugElement.queryAll(By.css('aside button[type="button"]'));
      // 1 reload button + 3 spec item buttons = 4 buttons
      expect(buttons.length).toBeGreaterThanOrEqual(4);

      const text = fixture.nativeElement.textContent;
      expect(text).toContain('Roadmap Maestro (Q3)');
      expect(text).toContain('Frente Canales');
      expect(text).toContain('Frente Core');
    });

    it('THEN clicking a spec item calls selectSpec', () => {
      const specButtons = fixture.debugElement.queryAll(
        By.css('aside button[aria-label^="Seleccionar especificación"]'),
      );
      expect(specButtons.length).toBe(3);

      specButtons[1].nativeElement.click();
      expect(mockPlanningState.selectSpec).toHaveBeenCalledWith('frente_canales.md');
    });

    it('THEN clicking refresh specs button reloads catalog', () => {
      const reloadBtn = fixture.debugElement.query(
        By.css('button[aria-label="Recargar catálogo de especificaciones"]'),
      );
      reloadBtn.nativeElement.click();

      expect(mockPlanningState.loadAvailableSpecs).toHaveBeenCalledTimes(2);
    });
  });

  describe('WHEN searching/filtering specs', () => {
    beforeEach(() => {
      mockPlanningState.availableSpecsSignal.set([
        'ideas_planning_Q3.md',
        'frente_canales.md',
        'frente_core.md',
      ]);
      fixture.detectChanges();
    });

    it('THEN filters list according to filterText', () => {
      (component as unknown as { filterText: WritableSignal<string> }).filterText.set('canales');
      fixture.detectChanges();

      const specButtons = fixture.debugElement.queryAll(
        By.css('aside button[aria-label^="Seleccionar especificación"]'),
      );
      expect(specButtons.length).toBe(1);
      expect(specButtons[0].nativeElement.textContent).toContain('Frente Canales');
    });

    it('THEN shows empty state when filter matches nothing', () => {
      (component as unknown as { filterText: WritableSignal<string> }).filterText.set(
        'inexistente',
      );
      fixture.detectChanges();

      expect(fixture.nativeElement.textContent).toContain('No se encontraron especificaciones');
    });
  });

  describe('WHEN a front spec is selected (DP-FE-02)', () => {
    beforeEach(() => {
      mockPlanningState.selectedSpecSignal.set({
        name: 'frente_canales.md',
        path: 'specs/frente_canales.md',
        content: '# Frente Canales\n\n| Feature | SP |\n|---|---|\n| Login Biométrico | 8 SP |',
      });
      fixture.detectChanges();
    });

    it('THEN renders the document viewer header with Frente badge and title', () => {
      const header = fixture.debugElement.query(By.css('main header'));
      expect(header.nativeElement.textContent).toContain('Frente Canales');
      expect(header.nativeElement.textContent).toContain('Frente');
    });

    it('THEN displays the contextual button "Refinar HU en este Frente"', () => {
      const refineBtn = fixture.debugElement.query(
        By.css('button[aria-label="Refinar HU en este Frente"]'),
      );
      expect(refineBtn).toBeTruthy();
      expect(refineBtn.nativeElement.textContent).toContain('Refinar HU en este Frente');
    });

    it('THEN clicking "Refinar HU en este Frente" preloads prompt and emits refineRequested', () => {
      let emittedPrompt: string | undefined;
      component.refineRequested.subscribe((p) => (emittedPrompt = p));

      const refineBtn = fixture.debugElement.query(
        By.css('button[aria-label="Refinar HU en este Frente"]'),
      );
      refineBtn.nativeElement.click();

      expect(mockRefinementChatState.prefillPrompt).toHaveBeenCalledTimes(1);
      const calledPrompt = mockRefinementChatState.prefillPrompt.mock.calls[0][0];
      expect(calledPrompt).toContain('Frente Canales');
      expect(calledPrompt).toContain('frente_canales.md');
      expect(emittedPrompt).toBe(calledPrompt);
      expect(mockNotifications.info).toHaveBeenCalledTimes(1);
    });

    it('THEN renders Markdown content safely including styled tables', () => {
      const markdownArticle = fixture.debugElement.query(By.css('article .markdown-body'));
      expect(markdownArticle).toBeTruthy();
      expect(markdownArticle.nativeElement.innerHTML).toContain('<table');
      expect(markdownArticle.nativeElement.innerHTML).toContain('Login Biométrico');
    });
  });

  describe('WHEN a master spec is selected', () => {
    beforeEach(() => {
      mockPlanningState.selectedSpecSignal.set({
        name: 'ideas_planning_Q3.md',
        path: 'specs/ideas_planning_Q3.md',
        content: '# Roadmap Trimestral Q3',
      });
      fixture.detectChanges();
    });

    it('THEN renders Maestro badge and does NOT show "Refinar HU en este Frente"', () => {
      const header = fixture.debugElement.query(By.css('main header'));
      expect(header.nativeElement.textContent).toContain('Roadmap Maestro (Q3)');
      expect(header.nativeElement.textContent).toContain('Maestro');

      const refineBtn = fixture.debugElement.query(
        By.css('button[aria-label="Refinar HU en este Frente"]'),
      );
      expect(refineBtn).toBeNull();
    });
  });

  describe('WHEN copying and downloading markdown', () => {
    beforeEach(() => {
      mockPlanningState.selectedSpecSignal.set({
        name: 'frente_canales.md',
        path: 'specs/frente_canales.md',
        content: '# Frente Canales Contenido',
      });
      fixture.detectChanges();
    });

    it('THEN clicking copy markdown calls navigator.clipboard and notifies success', async () => {
      const writeTextSpy = vi.fn().mockResolvedValue(undefined);
      Object.assign(navigator, { clipboard: { writeText: writeTextSpy } });

      const copyBtn = fixture.debugElement.query(
        By.css('button[aria-label="Copiar contenido Markdown"]'),
      );
      copyBtn.nativeElement.click();

      expect(writeTextSpy).toHaveBeenCalledWith('# Frente Canales Contenido');
    });

    it('THEN clicking download markdown creates blob and triggers anchor download', () => {
      const clickSpy = vi.fn();
      const origCreateElement = document.createElement.bind(document);
      vi.spyOn(document, 'createElement').mockImplementation((tagName: string) => {
        const el = origCreateElement(tagName);
        if (tagName === 'a') {
          el.click = clickSpy;
        }
        return el;
      });

      const downloadBtn = fixture.debugElement.query(
        By.css('button[aria-label="Descargar archivo Markdown"]'),
      );
      downloadBtn.nativeElement.click();

      expect(clickSpy).toHaveBeenCalledTimes(1);
      expect(mockNotifications.success).toHaveBeenCalledWith(
        expect.stringContaining('frente_canales.md descargado'),
      );
    });
  });

  describe('WHEN helper methods are invoked directly', () => {
    it('THEN correctly handles formatSpecTitle for various naming schemes', () => {
      expect(component.formatSpecTitle('ideas_planning_q4.md')).toBe('Roadmap Maestro (Q4)');
      expect(component.formatSpecTitle('frente_seguridad.md')).toBe('Frente Seguridad');
      expect(component.formatSpecTitle('otro_documento.md')).toBe('otro_documento');
    });

    it('THEN getBadgeClass returns specific classes according to spec category', () => {
      expect(component.getBadgeClass('ideas_planning_q1.md')).toContain('text-[#f2c94c]');
      expect(component.getBadgeClass('frente_devops.md')).toContain('text-[#60a5fa]');
      expect(component.getBadgeClass('notas.md')).toContain('text-[#9ca3af]');
    });
  });

  describe('WHEN loadingSpecsSignal is true and no spec is selected', () => {
    it('THEN renders loading spinner in the document viewer area', () => {
      mockPlanningState.selectedSpecSignal.set(null);
      mockPlanningState.loadingSpecsSignal.set(true);
      fixture.detectChanges();

      expect(fixture.nativeElement.textContent).toContain(
        'Cargando contenido de la especificación...',
      );
    });
  });

  describe('WHEN no spec is selected and not loading', () => {
    it('THEN renders empty state guidance message', () => {
      mockPlanningState.selectedSpecSignal.set(null);
      mockPlanningState.loadingSpecsSignal.set(false);
      fixture.detectChanges();

      expect(fixture.nativeElement.textContent).toContain(
        'Selecciona una especificación documental',
      );
    });
  });
});
