export const buildRefinementPrompt = (storyId: string, title: string): string =>
  `Asistente, quiero que analicemos y refinemos la Historia de Usuario: "${title}" (ID: ${storyId}). Ayúdame a revisar sus criterios de aceptación y calidad de documentación.`;

export const buildAuditPrompt = (id: string): string => `audita la calidad de (ID:${id})`;
