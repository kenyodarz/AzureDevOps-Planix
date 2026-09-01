import { DashboardStoryItem } from '../models/devops-agent.model';

/**
 * Constantes literales y umbrales de calidad del Tablero de Calidad (DP-02 / R-2).
 * Se conservan intactos según la decisión pendiente DP-02.
 */
export const LABEL_EXCELLENT_MIN = 90;
export const LABEL_GOOD_MIN = 70;
export const LABEL_REGULAR_MIN = 50;

export const COLOR_GREEN_MIN = 80;
export const COLOR_AMBER_MIN = 50;

export const LARGE_STORY_POINTS = 13;

export const LABEL_EXCELLENT = 'Excelente';
export const LABEL_GOOD = 'Buena (Suficiente)';
export const LABEL_REGULAR = 'Regular';
export const LABEL_POOR = 'Deficiente (Requiere Refinar)';

export const CLASS_GREEN = 'bg-[#10b981]';
export const CLASS_AMBER = 'bg-[#f59e0b]';
export const CLASS_RED = 'bg-[#ef4444]';

export interface StoryFilters {
  member?: string;
  state?: string;
  quality?: string;
}

/**
 * Retorna la etiqueta de calidad correspondiente a un puntaje (90/70/50).
 */
export function getQualityLabel(score: number): string {
  if (score >= LABEL_EXCELLENT_MIN) return LABEL_EXCELLENT;
  if (score >= LABEL_GOOD_MIN) return LABEL_GOOD;
  if (score >= LABEL_REGULAR_MIN) return LABEL_REGULAR;
  return LABEL_POOR;
}

/**
 * Retorna la clase de fondo CSS correspondiente al color del semáforo (80/50).
 */
export function getQualityBgClass(score: number): string {
  if (score >= COLOR_GREEN_MIN) return CLASS_GREEN;
  if (score >= COLOR_AMBER_MIN) return CLASS_AMBER;
  return CLASS_RED;
}

/**
 * Retorna la clase CSS correspondiente al estado de la historia en Azure DevOps.
 */
export function getStateClass(state: string): string {
  const s = state ? state.toLowerCase() : '';
  if (s === 'done' || s === 'closed') {
    return 'bg-[rgba(16,185,129,0.1)] text-[#10b981] border border-[rgba(16,185,129,0.2)]';
  }
  if (s === 'committed' || s === 'active') {
    return 'bg-[rgba(37,99,235,0.1)] text-[#2563eb] border border-[rgba(37,99,235,0.2)]';
  }
  if (s === 'approved') {
    return 'bg-[rgba(242,201,76,0.1)] text-[#f2c94c] border border-[rgba(242,201,76,0.2)]';
  }
  return 'bg-[rgba(255,255,255,0.05)] text-[#9ca3af] border border-[rgba(255,255,255,0.08)]';
}

/**
 * Cuenta las historias complejas / grandes (>= 13 SP).
 */
export function getLargeStoriesCount(items?: DashboardStoryItem[] | null): number {
  return items?.filter((item) => item.points >= LARGE_STORY_POINTS).length ?? 0;
}

/**
 * Filtra los ítems de historias según los criterios de miembro, estado y calidad (DP-02).
 */
export function filterStories(
  items: DashboardStoryItem[] | undefined | null,
  filters: StoryFilters,
): DashboardStoryItem[] {
  const { member, state, quality } = filters;
  return (
    items?.filter((item) => {
      if (member && item.assignedMember !== member) {
        return false;
      }
      if (state && item.state !== state) {
        return false;
      }
      if (quality) {
        if (quality === 'critical' && item.qualityScore >= COLOR_AMBER_MIN) {
          return false;
        }
        if (
          quality === 'regular' &&
          (item.qualityScore < COLOR_AMBER_MIN || item.qualityScore >= COLOR_GREEN_MIN)
        ) {
          return false;
        }
        if (quality === 'good' && item.qualityScore < COLOR_GREEN_MIN) {
          return false;
        }
      }
      return true;
    }) ?? []
  );
}

/**
 * Obtiene la lista única de estados presentes en las historias.
 */
export function getUniqueStates(items?: DashboardStoryItem[] | null): string[] {
  const states = items?.map((item) => item.state).filter((state): state is string => !!state) ?? [];
  return Array.from(new Set(states));
}

/**
 * Obtiene la lista única de integrantes asignados en las historias.
 */
export function getUniqueMembers(items?: DashboardStoryItem[] | null): string[] {
  const members =
    items?.map((item) => item.assignedMember).filter((member): member is string => !!member) ?? [];
  return Array.from(new Set(members));
}
