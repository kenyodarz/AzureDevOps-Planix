# PLANTILLA OBLIGATORIA DE FASE

> Copia este archivo para generar cada `FASE-XX-<nombre>.md`.
> **La estructura de tres bloques —CONTEXTO, INSTRUCCIONES, ORDEN DE EJECUCIÓN— es obligatoria y
> no puede alterarse.** Un MD de fase debe ser autosuficiente: quien lo lea sin ninguna memoria
> previa debe poder ejecutarlo de principio a fin.

---

```markdown
# FASE XX — <Nombre de la fase>

> **Estado:** PENDIENTE · **Depende de:** FASE XX-1 · **Riesgo:** Bajo/Medio/Alto
> **Commit al cerrar:** `tipo(scope): descripcion en espanol`

---

## 1. CONTEXTO

### 1.1 De dónde venimos
<Qué dejó lista la fase anterior. Si es la primera fase, describir el estado inicial del repo.>

### 1.2 Problema que resuelve esta fase
<Deuda técnica concreta (IDs D-XX del plan maestro) con archivo y línea exactos.>

### 1.3 Estado esperado al terminar
<Qué existirá que hoy no existe. Debe ser verificable objetivamente.>

### 1.4 Archivos involucrados
| Ruta | Acción |
|---|---|
| `ruta/al/archivo.java` | CREAR / MODIFICAR / LEER |

### 1.5 Reglas aplicables
<Referencias literales a `rules/spring-rules.md` que gobiernan esta fase.>

### 1.6 Decisiones pendientes que bloquean
| ID | Estado | Acción si sigue ABIERTA |
|---|---|---|
| DP-XX | 🔴 ABIERTA | DETENERSE y preguntar al usuario antes de continuar |

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer
<Instrucciones numeradas, imperativas y sin ambigüedad. Incluir firmas de clases y métodos
exactas cuando aplique. Nunca dejar un nombre a criterio del ejecutor.>

### 2.2 Qué NO hacer
<Prohibiciones explícitas: qué archivos no tocar, qué comportamientos no cambiar.>

### 2.3 Criterios de aceptación
- [ ] <Criterio verificable 1>
- [ ] <Criterio verificable 2>

### 2.4 Checklist de calidad (obligatoria, de `spring-rules.md` §7)
- [ ] `domain/model` sin dependencias de Spring, persistencia ni serialización
- [ ] `domain/usecase` sin anotaciones de Spring
- [ ] Sin secretos hardcodeados (S2068)
- [ ] `Optional<T>` en retornos opcionales (S2259)
- [ ] Complejidad cognitiva ≤ 15
- [ ] Llaves `{}` en todo control de flujo (S1117)
- [ ] Sin números mágicos (S109)
- [ ] Tests GIVEN/WHEN/THEN con Mockito
- [ ] `./gradlew build` verde

---

## 3. ORDEN DE EJECUCIÓN

| Paso | Acción | Resultado esperado |
|---:|---|---|
| 1 | Leer `docs/plan/ESTADO.md` y `docs/plan/DECISIONES_PENDIENTES.md` | Confirmar que la fase no está bloqueada |
| 2 | ... | ... |
| N-4 | Ejecutar `./gradlew build` | BUILD SUCCESSFUL |
| N-3 | **Escribir `docs/resultados/RESULTADO-FASE-XX.md`** con `_PLANTILLA_RESULTADO.md` y métricas reales | Trazabilidad de la ejecución |
| N-2 | Actualizar `docs/plan/ESTADO.md`: fase 🟢 COMPLETADA + métricas + bitácora | Tablero al día |
| N-1 | **Generar `docs/fases/FASE-XX+1-<nombre>.md`** con esta misma plantilla | Checkpoint de continuidad creado |
| N | Commit: `tipo(scope): descripcion` | Cambios versionados |

> ⚠️ **Los pasos N-3 y N-1 son innegociables.** Sin el registro de resultado no hay trazabilidad, y
> sin el MD de la siguiente fase el trabajo no es reanudable tras una desconexión. En cualquiera de
> los dos casos la fase se considera **incompleta**.

### 3.1 Contenido mínimo del MD de la siguiente fase
<Semilla concreta: objetivo, archivos y deudas que debe atacar la fase siguiente, para que el
ejecutor pueda redactarla sin releer el plan maestro completo.>
```

