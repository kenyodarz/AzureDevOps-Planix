import { describe, expect, it } from 'vitest';
import { buildAuditPrompt, buildRefinementPrompt } from './chat-prompts';

describe('GIVEN chat prompt builders in domain layer', () => {
  describe('WHEN buildAuditPrompt is called with a story ID', () => {
    it('THEN returns the exact audit command formatted with the ID', () => {
      // Arrange (GIVEN)
      const storyId = 'X-1';

      // Act (WHEN)
      const result = buildAuditPrompt(storyId);

      // Assert (THEN)
      expect(result).toBe('audita la calidad de (ID:X-1)');
    });
  });

  describe('WHEN buildRefinementPrompt is called with a story ID and title', () => {
    it('THEN returns the refinement prompt with title and ID interpolated in their positions', () => {
      // Arrange (GIVEN)
      const storyId = '123';
      const title = 'Carga masiva de aprobadores';

      // Act (WHEN)
      const result = buildRefinementPrompt(storyId, title);

      // Assert (THEN)
      expect(result).toBe(
        'Asistente, quiero que analicemos y refinemos la Historia de Usuario: "Carga masiva de aprobadores" (ID: 123). Ayúdame a revisar sus criterios de aceptación y calidad de documentación.',
      );
    });
  });
});
