# Documentación de Refactorización — `azure-devops-agent`

Esta carpeta contiene el plan de refactorización del agente y el mecanismo de **continuidad entre
sesiones** para que el trabajo pueda retomarse tras una caída, desconexión o pérdida de contexto.

## Estructura

```
docs/
├── README.md                    <- Este archivo (protocolo de continuidad)
├── plan/
│   ├── PLAN_MAESTRO.md          <- Plan completo por fases (visión global)
│   ├── DECISIONES_PENDIENTES.md <- Política de No-Asunción: dudas abiertas
│   └── ESTADO.md                <- Tablero de avance (ÚNICA fuente de verdad del progreso)
└── fases/
    ├── _PLANTILLA_FASE.md       <- Plantilla obligatoria para generar cada fase
    └── FASE-XX-<nombre>.md      <- Instrucciones ejecutables de cada fase
```

## Protocolo de continuidad (LEER SIEMPRE AL INICIAR SESIÓN)

Si eres un agente de IA retomando este trabajo, ejecuta estos pasos **en orden** antes de tocar
código:

1. Lee `docs/plan/ESTADO.md` y localiza la primera fase con estado `PENDIENTE` o `EN_CURSO`.
2. Lee `docs/plan/PLAN_MAESTRO.md` para entender el objetivo global y dónde encaja esa fase.
3. Lee `docs/plan/DECISIONES_PENDIENTES.md`. Si la fase depende de una decisión sin resolver,
   **detente y pregunta al usuario** (regla obligatoria de No-Asunción, `rules/spring-rules.md` §6).
4. Lee `docs/fases/FASE-XX-<nombre>.md` correspondiente y ejecútala **literalmente**.
5. Al terminar la fase:
   - Verifica la checklist de salida de esa fase (debe estar 100% marcada).
   - Actualiza `docs/plan/ESTADO.md` marcando la fase como `COMPLETADA` con fecha y notas.
   - **Genera el MD de la siguiente fase** en `docs/fases/` usando `_PLANTILLA_FASE.md`.
   - Haz commit siguiendo `COMMIT_RULES.md`.

## Regla de oro

**Nunca se ejecutan dos fases en la misma sesión sin haber generado el MD de la siguiente.**
El MD de la siguiente fase es el checkpoint que garantiza que el trabajo es reanudable.

## Reglas de obligado cumplimiento

Todo cambio debe respetar:

- `rules/spring-rules.md` — Clean Architecture Bancolombia, SOLID, SonarQube, testing.
- `.github/copilot-instructions.md` — seguridad, no secretos, política de No-Asunción.
- `COMMIT_RULES.md` — formato `tipo(scope): descripción` en español.

