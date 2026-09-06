import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  API_ENDPOINTS,
  initiativeCellUrl,
  initiativeChunksUrl,
  initiativeUrl,
  specUrl,
} from '../../../../core';
import {
  IngestPayload,
  IngestResponse,
  Initiative,
  PlanningChunk,
  ProgramPlanRequestDTO,
  ProgramPlanResponseDTO,
  SpecDocumentDTO,
  SpecListDTO,
} from '../../models/devops-agent.model';

@Injectable({
  providedIn: 'root',
})
export class PlanningApiService {
  private readonly http = inject(HttpClient);

  uploadPlanning(payload: IngestPayload): Observable<IngestResponse> {
    return this.http.post<IngestResponse>(API_ENDPOINTS.PLANNING_INGEST, payload);
  }

  getInitiatives(): Observable<Initiative[]> {
    return this.http.get<Initiative[]>(API_ENDPOINTS.PLANNING_INITIATIVES);
  }

  deleteInitiative(id: string): Observable<void> {
    return this.http.delete<void>(initiativeUrl(id));
  }

  updateInitiativeCell(id: string, cell: string): Observable<void> {
    return this.http.put<void>(initiativeCellUrl(id), { cell });
  }

  getInitiativeChunks(id: string): Observable<PlanningChunk[]> {
    return this.http.get<PlanningChunk[]>(initiativeChunksUrl(id));
  }

  triggerProgramPlanning(request: ProgramPlanRequestDTO): Observable<ProgramPlanResponseDTO> {
    return this.http.post<ProgramPlanResponseDTO>(API_ENDPOINTS.PLANNING_PROGRAM, request);
  }

  getAvailableSpecs(): Observable<SpecListDTO> {
    return this.http.get<SpecListDTO>(API_ENDPOINTS.PLANNING_SPECS);
  }

  getSpecDocument(name: string): Observable<SpecDocumentDTO> {
    return this.http.get<SpecDocumentDTO>(specUrl(name));
  }
}

