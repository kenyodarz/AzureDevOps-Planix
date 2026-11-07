# Protocolo de Continuidad — Frontend de Program Planning

> **Módulo:** `azure-devops-frontend`  
> **Metodología:** Spec-Driven Development (SDD) para Agentes de IA (`../../../docs/README.md`)  
> **Especificación Base:** `../../docs/sdd/specs`

---

## 1. Regla de Oro para Agentes de IA

Si estás iniciando una nueva sesión o continuando el trabajo en este módulo:

1. **Lee primero `../../docs/sdd/plan`:** Es la **única fuente de verdad del progreso**. Te indicará la fase activa (`🟡 PENDIENTE` o `🔵 EN_CURSO`).
2. **Lee `../../docs/sdd/plan`:** Verifica que no existan decisiones bloqueantes en estado `🔴 ABIERTA`. Si las hay, **detente inmediatamente y consulta al usuario**.
3. **Lee la fase activa en `../../docs/sdd/fases` o su prompt en `../../docs/sdd/prompts`:** Contiene el contexto y las tareas quirúrgicas sin ambigüedad.
4. **Al terminar la fase:** Aplica la **Regla del Doble Artefacto**:
    - Genera el reporte de resultados en `../../docs/sdd/resultados`.
    - Actualiza `../../docs/sdd/plan`.
    - Genera la especificación de la siguiente fase (`../../docs/sdd/fases`) y su prompt (`../../docs/sdd/prompts`).

---

## 2. Mapa de Directorios

```text
azure-devops-frontend/docs/
├── README.md                                    <- Este documento
├── historico/                                   <- Planes completados anteriores
│   └── plan_desacoplamiento_frontend/           <- Fases 01 a 08 (cerrado 2026-09-01)
├── specs/                                       <- Especificaciones técnicas base
│   └── ESPECIFICACION-INTEGRACION-PLANNING-FRONTEND.md
├── plan/                                        <- Control del plan activo
│   ├── PLAN_MAESTRO.md
│   ├── DECISIONES_PENDIENTES.md
│   └── ESTADO.md
├── fases/                                       <- Fases atómicas ejecutables
│   └── FASE-01-contratos-api-modelos-dtos.md
├── prompts/                                     <- Prompts listos para detonar cada fase
│   └── PROMPT-FASE-01.md
└── resultados/                                  <- Evidencia con mediciones reales
```
