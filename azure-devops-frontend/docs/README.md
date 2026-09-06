# Protocolo de Continuidad — Frontend de Program Planning

> **Módulo:** `azure-devops-frontend`  
> **Metodología:** Spec-Driven Development (SDD) para Agentes de IA (`../../../docs/README.md`)  
> **Especificación Base:** `docs/specs/ESPECIFICACION-INTEGRACION-PLANNING-FRONTEND.md`

---

## 1. Regla de Oro para Agentes de IA

Si estás iniciando una nueva sesión o continuando el trabajo en este módulo:

1. **Lee primero `docs/plan/ESTADO.md`:** Es la **única fuente de verdad del progreso**. Te indicará la fase activa (`🟡 PENDIENTE` o `🔵 EN_CURSO`).
2. **Lee `docs/plan/DECISIONES_PENDIENTES.md`:** Verifica que no existan decisiones bloqueantes en estado `🔴 ABIERTA`. Si las hay, **detente inmediatamente y consulta al usuario**.
3. **Lee la fase activa en `docs/fases/` o su prompt en `docs/prompts/`:** Contiene el contexto y las tareas quirúrgicas sin ambigüedad.
4. **Al terminar la fase:** Aplica la **Regla del Doble Artefacto**:
   - Genera el reporte de resultados en `docs/resultados/RESULTADO-FASE-XX.md`.
   - Actualiza `docs/plan/ESTADO.md`.
   - Genera la especificación de la siguiente fase (`docs/fases/FASE-XX+1-*.md`) y su prompt (`docs/prompts/PROMPT-FASE-XX+1.md`).

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
