---
name: sdd-workflow
description: Universal Spec-Driven Development (SDD) workflow for AI coding agents. Enforces state continuity, atomic phases (max 2-4 files), strict no-assumption policy, measured results verification, and automatic phase chaining. Use when planning or executing complex multi-step features, refactoring, migrations, or architectural changes across any technology stack.
---

# Spec-Driven Development (SDD) Agent Skill

Esta habilidad instruye al agente para operar como un ingeniero de software senior bajo la metodología **Spec-Driven Development (SDD)**, garantizando que el trabajo complejo se ejecute en pasos atómicos, sin saturar la ventana de contexto y con continuidad garantizada entre sesiones.

---

## 1. Cuándo Activar esta Skill

El agente **DEBE** activar este flujo cuando el usuario solicite:
* Diseñar un plan de implementación o refactorización para una iniciativa mediana o grande.
* Ejecutar una fase de un plan existente en cualquier módulo del repositorio.
* Retomar un trabajo interrumpido o continuar una sesión previa.
* Resolver tareas complejas que abarquen múltiples capas o componentes.

---

## 2. Los 6 Artefactos Obligatorios

En todo módulo o proyecto bajo SDD, la carpeta `docs/` contiene:
1. `plan/PLAN_MAESTRO.md`: Contrato de arquitectura, diagnóstico con IDs (`D-XX`) y mapa de fases atómicas (01 a 0N).
2. `plan/DECISIONES_PENDIENTES.md`: Registro de No-Asunción (`DP-XX`) con semáforo (`🔴 ABIERTA`, `🟢 RESUELTA`, `⚪ INFORMATIVA`).
3. `plan/ESTADO.md`: **Única fuente de verdad del progreso**: dashboard de lectura en 1 minuto con la fase activa y métricas vivas.
4. `fases/FASE-XX-<nombre>.md`: Especificación técnica ejecutable de la fase (Contexto, Instrucciones paso a paso, Checklist de calidad).
5. `prompts/PROMPT-FASE-XX.md`: Prompt autosuficiente para instruir la ejecución de la fase.
6. `resultados/RESULTADO-FASE-XX.md`: Cierre de fase con **métricas reales medidas** (no estimadas).

---

## 3. Comportamiento y Reglas No Negociables del Agente

### Regla 1: Cero Código sin Fase Previa
Queda estrictamente prohibido modificar archivos de código fuente si el cambio no está especificado explícitamente en un documento de fase (`fases/FASE-XX.md`).

### Regla 2: Atomicidad Quirúrgica (Regla de los 2-4 Archivos)
Una fase nunca debe abarcar más de 4 archivos de lógica ni tardar más de una sesión en validarse. Si un requerimiento es amplio, el agente **debe dividirlo en fases atómicas secuenciales**.

### Regla 3: Política Estricta de No-Asunción
Si durante la planificación o ejecución surge una duda sobre contratos, arquitectura, tecnologías o reglas de negocio:
* El agente **NO DEBE asumir silenciosamente**.
* Debe registrar la decisión en `DECISIONES_PENDIENTES.md` como `🔴 ABIERTA`.
* El agente **DEBE DETENERSE y preguntar al usuario**.

### Regla 4: Gobernanza y Reglas de Arquitectura (`rules/`)
Antes de redactar una fase o escribir código, el agente **DEBE consultar la carpeta `rules/`** del proyecto (ej: `spring-rules.md`, `angular-rules.md`):
* La fase debe heredar las restricciones del stack (separación de capas, inmutabilidad, Sonar, testing).
* La checklist de calidad de la fase debe validar explícitamente que no se violen las reglas arquitectónicas.
* Queda estrictamente prohibido introducir patrones, anotaciones o dependencias que contradigan los archivos en `rules/`.

### Regla 5: Convención Estricta de Commits y Semantic Release
Al concluir una fase y proponer el commit, el agente **DEBE seguir estrictamente `COMMIT_MESSAGE_CONVENTION.md`**:
* Estructura: `COMMIT_TYPE(SCOPE): DESCRIPTION` (o con `!` para Breaking Changes: `COMMIT_TYPE(SCOPE)!: DESCRIPTION`).
* **`COMMIT_TYPE`** en minúsculas: `feat`, `fix`, `perf`, `security`, `docs`, `style`, `refactor`, `test`, `build`, `ci`, `chore`, `deprecated`, `removed`.
* **`SCOPE`** estrictamente en **`snake_case`** (ej: `auth_service`, `user_module`, `agent_engine`).
* **`DESCRIPTION`** obligatoriamente en **español**, iniciando en minúscula y **sin punto final** (PROHIBIDO usar verbos en inglés como `add...`, `fix...`, `update...`).

### Regla 6: La Regla del Doble Artefacto de Cierre
Una fase no se considera cerrada solo por compilar el código. Al finalizar con éxito todas las pruebas unitarias:
1. Redactar `resultados/RESULTADO-FASE-XX.md` con métricas reales.
2. Si hubo hallazgos, **ajustar el Plan Maestro o la siguiente fase**. El plan es adaptativo.
3. Actualizar `plan/ESTADO.md` marcando la fase como `🟢 COMPLETADA`.
4. **Generar inmediatamente la especificación de la siguiente fase (`fases/FASE-XX+1-*.md`) y su prompt (`prompts/PROMPT-FASE-XX+1.md`)**.
5. Solicitar confirmación del commit aplicando la Regla 5.

---

## 4. Guía de Ejecución Rápida para el Agente

```
                  ┌─────────────────────────────────────┐
                  │ 1. Leer docs/plan/ESTADO.md         │
                  │    (Identificar Fase Activa)        │
                  └──────────────────┬──────────────────┘
                                     │
                                     ▼
                  ┌─────────────────────────────────────┐
                  │ 2. Verificar DECISIONES_PENDIENTES  │
                  │    ¿Hay alguna 🔴 ABIERTA?          │
                  └──────┬───────────────────────┬──────┘
                         │ SÍ                    │ NO
                         ▼                       ▼
            ┌─────────────────────────┐  ┌──────────────────────────────┐
            │ DETENERSE Y PREGUNTAR   │  │ 3. Consultar rules/          │
            │ AL USUARIO              │  │    y leer FASE-XX / PROMPT-XX│
            └─────────────────────────┘  └──────────────┬───────────────┘
                                                        │
                                                        ▼
                                         ┌──────────────────────────────┐
                                         │ 4. Ejecutar & Validar Tests  │
                                         │    (100% verde, cov >= 90%)  │
                                         └──────────────┬───────────────┘
                                                        │
                                                        ▼
                                         ┌──────────────────────────────┐
                                         │ 5. Doble Artefacto de Cierre:│
                                         │    - RESULTADO-FASE-XX.md    │
                                         │    - Actualizar ESTADO.md    │
                                         │    - FASE-XX+1 & PROMPT-XX+1 │
                                         │    - Proponer Commit SemVer  │
                                         └──────────────────────────────┘
```
