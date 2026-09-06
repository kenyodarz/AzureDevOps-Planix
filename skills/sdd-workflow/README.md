# Spec-Driven Development (SDD) Workflow — Agent Skill

[![skills.sh compatible](https://img.shields.io/badge/skills.sh-compatible-blue.svg)](https://www.skills.sh)
[![Node.js Version](https://img.shields.io/badge/node-%3E%3D18.0.0-green.svg)](https://nodejs.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Habilidad de agente (Agent Skill) universal y agnóstica para el marco de trabajo **Spec-Driven Development (SDD)**. Diseñada para operar con asistentes de IA en IDEs y terminales (Antigravity, Claude Code, Cursor, Copilot CLI, Windsurf).

---

## 1. Instalación y Adición

### A través de `skills.sh`
```bash
npx skills add sdd-workflow
```

### O instalación global vía npm / Artifactory interno
```bash
npm install -g sdd-workflow
```

---

## 2. Uso Rápido (CLI de Scaffolding)

Inicializa la estructura documental SDD en cualquier proyecto con un solo comando:

```bash
# En el directorio raíz del proyecto:
npx sdd-workflow init --name "migracion-arquitectura-reactiva"

# O en un módulo específico:
npx sdd-workflow init --name "modulo-pagos" --target "./backend"
```

Esto generará automáticamente la jerarquía estandarizada:
```
docs/
├── README.md                            <- Protocolo de continuidad del proyecto
├── plan/
│   ├── PLAN_MAESTRO.md                  <- Contrato arquitectónico y mapa de fases
│   ├── DECISIONES_PENDIENTES.md         <- Registro de No-Asunción (DP-XX)
│   └── ESTADO.md                        <- Tablero vivo de avance (ÚNICA FUENTE DE VERDAD)
├── fases/
│   └── FASE-01-inicializacion.md        <- Especificación técnica de la primera fase
├── prompts/
│   └── PROMPT-FASE-01.md                <- Prompt autosuficiente para instruir al agente
└── resultados/
    └── .gitkeep                         <- Evidencias y mediciones de cierre
```

Para verificar el estado actual del plan en la terminal:
```bash
npx sdd-workflow status
```

---

## 3. Comportamiento que Inyecta la Skill al Agente de IA

Al cargarse en el contexto del agente a través de `SKILL.md`, la IA adopta de inmediato las siguientes restricciones de ingeniería senior:

1. **Cero Código sin Fase Previa:** No modifica código si no existe un documento `fases/FASE-XX.md` que detalle el cambio.
2. **Fases Atómicas:** Descompone los planes en pasos de 2 a 4 archivos como máximo para nunca saturar la ventana de contexto.
3. **Política Estricta de No-Asunción:** Si hay incertidumbre técnica o funcional, la registra en `DECISIONES_PENDIENTES.md` como `🔴 ABIERTA` y **se detiene para consultar al usuario**.
4. **Regla del Doble Artefacto:** Al cerrar una fase, genera el resultado medido (`RESULTADO-FASE-XX.md`), actualiza el tablero y **genera automáticamente la especificación y el prompt de la fase siguiente**.
5. **Retroalimentación Adaptativa:** Permite que los hallazgos de una fase reajusten el Plan Maestro y las fases subsiguientes.

---

## 4. Proceso de Verificación y Auditoría para Publicación

Para certificar y publicar esta Skill en el catálogo corporativo o registro de paquetes:

- [x] **Compatibilidad Node.js:** Ejecución sin dependencias externas pesadas (`node bin/cli.js --help` exit code 0).
- [x] **Agnosticismo Tecnológico:** Plantillas 100% libres de acoplamientos a proyectos o stacks específicos.
- [x] **Formato Estándar `SKILL.md`:** Frontmatter YAML con `name` y `description` normalizados según la especificación de `skills.sh`.
- [x] **Pruebas de Scaffolding:** Inicialización limpia de directorios, reemplazo dinámico de variables (`today`, `featureName`).
- [ ] **Aprobación de Arquitectura:** Validación con el equipo de plataforma o arquitectura para inclusión en el catálogo interno.
