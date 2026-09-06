# Documentación de Refactorización — `azure-devops-agent`

Esta carpeta contiene el plan de refactorización del agente y el mecanismo de **continuidad entre
sesiones** para que el trabajo pueda retomarse tras una caída, desconexión o pérdida de contexto.

## Estructura

```
docs/
├── README.md                        <- Este archivo (protocolo de continuidad)
├── plan/
│   ├── PLAN_MAESTRO.md              <- Plan completo por fases (visión global)
│   ├── DECISIONES_PENDIENTES.md     <- Política de No-Asunción: dudas abiertas y resueltas
│   └── ESTADO.md                    <- Tablero de avance (ÚNICA fuente de verdad del progreso)
├── fases/
│   ├── _PLANTILLA_FASE.md           <- Plantilla obligatoria para generar cada fase
│   └── FASE-XX-<nombre>.md          <- Instrucciones ejecutables de cada fase (ANTES)
└── resultados/
    ├── _PLANTILLA_RESULTADO.md      <- Plantilla obligatoria del registro de resultado
    └── RESULTADO-FASE-XX.md         <- Qué ocurrió realmente al ejecutarla (DESPUÉS)
```

**`fases/` vs `resultados/`:** `fases/` responde a *«qué hay que hacer»* y se escribe antes.
`resultados/` responde a *«qué se hizo, con qué números y qué se desvió»* y se escribe después, con
datos reales medidos. Nunca se mezclan.

## Protocolo de continuidad (LEER SIEMPRE AL INICIAR SESIÓN)

Si eres un agente de IA retomando este trabajo, ejecuta estos pasos **en orden** antes de tocar
código:

1. Lee `docs/plan/ESTADO.md` y localiza la primera fase con estado `PENDIENTE` o `EN_CURSO`.
2. Lee `docs/plan/PLAN_MAESTRO.md` para entender el objetivo global y dónde encaja esa fase.
3. Lee el `docs/resultados/RESULTADO-FASE-XX.md` de la fase **anterior**: contiene las desviaciones
   reales y los hallazgos que condicionan la siguiente.
4. Lee `docs/plan/DECISIONES_PENDIENTES.md`. Si la fase depende de una decisión sin resolver,
   **detente y pregunta al usuario** (regla obligatoria de No-Asunción, `rules/spring-rules.md` §6).
5. Lee `docs/fases/FASE-XX-<nombre>.md` correspondiente y ejecútala **literalmente**.
6. Al terminar la fase:
   - Verifica la checklist de salida de esa fase (debe estar 100% marcada).
   - **Escribe `docs/resultados/RESULTADO-FASE-XX.md`** usando `_PLANTILLA_RESULTADO.md`, con
     métricas reales medidas, no estimadas.
   - Actualiza `docs/plan/ESTADO.md` marcando la fase como `COMPLETADA` con fecha y notas.
   - **Genera el MD de la siguiente fase** en `docs/fases/` usando `_PLANTILLA_FASE.md`.
   - Haz commit siguiendo `COMMIT_RULES.md`.

## Regla de oro

**Una fase no está cerrada hasta que existen sus dos artefactos:**
el `RESULTADO-FASE-XX.md` de la fase ejecutada y el `FASE-XX+1-<nombre>.md` de la siguiente.
El primero garantiza trazabilidad; el segundo, que el trabajo sea reanudable tras una desconexión.

## Reglas de obligado cumplimiento

Todo cambio debe respetar:

- `rules/spring-rules.md` — Clean Architecture Bancolombia, SOLID, SonarQube, testing.
- `.github/copilot-instructions.md` — seguridad, no secretos, política de No-Asunción.
- `COMMIT_RULES.md` — formato `tipo(scope): descripción` en español.

