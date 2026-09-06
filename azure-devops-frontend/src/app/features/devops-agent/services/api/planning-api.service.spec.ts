import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PlanningApiService } from './planning-api.service';
import {
  IngestPayload,
  Initiative,
  PlanningChunk,
  ProgramPlanRequestDTO,
  ProgramPlanResponseDTO,
  SpecDocumentDTO,
  SpecListDTO,
} from '../../models/devops-agent.model';

describe('GIVEN PlanningApiService', () => {
  let service: PlanningApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [PlanningApiService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PlanningApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('WHEN uploadPlanning is called', () => {
    it('THEN performs a POST request to /api/planning/ingest', () => {
      const payload: IngestPayload = {
        initiativeId: 'guardian-q3',
        title: 'Guardián Q3',
        markdownContent: '# Planeación',
      };

      service.uploadPlanning(payload).subscribe((res) => {
        expect(res).toEqual({ message: 'OK' });
      });

      const req = httpMock.expectOne('/api/planning/ingest');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(payload);
      req.flush({ message: 'OK' });
    });
  });

  describe('WHEN getInitiatives is called', () => {
    it('THEN performs a GET request to /api/planning/initiatives', () => {
      const mockList: Initiative[] = [
        { initiative_id: 'i1', initiative_title: 'Title 1', cell: 'c1' },
      ];

      service.getInitiatives().subscribe((res) => {
        expect(res).toEqual(mockList);
      });

      const req = httpMock.expectOne('/api/planning/initiatives');
      expect(req.request.method).toBe('GET');
      req.flush(mockList);
    });
  });

  describe('WHEN deleteInitiative is called', () => {
    it('THEN performs a DELETE request to /api/planning/initiatives/:id', () => {
      service.deleteInitiative('i1').subscribe();

      const req = httpMock.expectOne('/api/planning/initiatives/i1');
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('WHEN updateInitiativeCell is called', () => {
    it('THEN performs a PUT request to /api/planning/initiatives/:id/cell', () => {
      service.updateInitiativeCell('i1', 'new-cell').subscribe();

      const req = httpMock.expectOne('/api/planning/initiatives/i1/cell');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ cell: 'new-cell' });
      req.flush(null);
    });
  });

  describe('WHEN getInitiativeChunks is called', () => {
    it('THEN performs a GET request to /api/planning/initiatives/:id/chunks', () => {
      const mockChunks: PlanningChunk[] = [
        {
          id: 'chk-1',
          initiativeId: 'i1',
          sectionName: 'Scope',
          content: 'Content chunk',
          metadata: {},
        },
      ];

      service.getInitiativeChunks('i1').subscribe((chunks) => {
        expect(chunks).toEqual(mockChunks);
      });

      const req = httpMock.expectOne('/api/planning/initiatives/i1/chunks');
      expect(req.request.method).toBe('GET');
      req.flush(mockChunks);
    });
  });

  describe('WHEN triggerProgramPlanning is called', () => {
    it('THEN performs a POST request to /api/planning/program', () => {
      const request: ProgramPlanRequestDTO = {
        quarter: 'Q3',
        sprintCount: 6,
        maxCapacityPerSprint: 45,
        targetFronts: ['Canales', 'Core'],
        objectives: 'Objetivos Q3',
      };

      const mockResponse: ProgramPlanResponseDTO = {
        summary: 'Plan encolado',
        quarter: 'Q3',
        sprintCount: 6,
        maxCapacityPerSprint: 45,
        targetFronts: ['Canales', 'Core'],
        contextId: 'ctx-123',
        task: {
          id: 'task-q3',
          contextId: 'ctx-123',
          status: { state: 'submitted' },
        },
      };

      service.triggerProgramPlanning(request).subscribe((res) => {
        expect(res).toEqual(mockResponse);
        expect(res.task.id).toBe('task-q3');
      });

      const req = httpMock.expectOne('/api/planning/program');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(mockResponse);
    });
  });

  describe('WHEN getAvailableSpecs is called', () => {
    it('THEN performs a GET request to /api/planning/specs', () => {
      const mockSpecs: SpecListDTO = {
        specs: ['ideas_planning_Q3.md', 'frente_canales.md'],
        total: 2,
      };

      service.getAvailableSpecs().subscribe((res) => {
        expect(res).toEqual(mockSpecs);
        expect(res.total).toBe(2);
      });

      const req = httpMock.expectOne('/api/planning/specs');
      expect(req.request.method).toBe('GET');
      req.flush(mockSpecs);
    });
  });

  describe('WHEN getSpecDocument is called', () => {
    it('THEN performs a GET request to /api/planning/specs/:name with URI encoding', () => {
      const mockDoc: SpecDocumentDTO = {
        name: 'ideas_planning_Q3.md',
        content: '# Ideas Q3\nContenido',
        path: 'specs/ideas_planning_Q3.md',
      };

      service.getSpecDocument('ideas_planning_Q3.md').subscribe((res) => {
        expect(res).toEqual(mockDoc);
        expect(res.content).toContain('# Ideas Q3');
      });

      const req = httpMock.expectOne('/api/planning/specs/ideas_planning_Q3.md');
      expect(req.request.method).toBe('GET');
      req.flush(mockDoc);
    });
  });
});
