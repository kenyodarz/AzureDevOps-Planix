export interface ChatSuggestion {
  readonly label: string;
  readonly value: string;
}

export const CHAT_SUGGESTIONS: readonly ChatSuggestion[] = [
  {
    label: '💡 CRUD Aprobadores (Idea corta)',
    value:
      'AI Admin panel - Crear la funcion de carga en batch para poblar la tabla de aprovadores de MCP y todo el tema de CRUD',
  },
  { label: '✅ Aprobado', value: 'Aprobado' },
  { label: '🚀 Crear', value: 'Crear' },
] as const;
