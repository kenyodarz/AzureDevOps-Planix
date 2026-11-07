# PLANTILLA OBLIGATORIA DE FASE — Proyecto Janus (`janus-web`)

> Copia este archivo para generar cada `fase-XX.md`.
> **La estructura de tres bloques —CONTEXTO, INSTRUCCIONES, ORDEN DE EJECUCIÓN— es obligatoria y no
> puede alterarse.** Un MD de fase debe ser autosuficiente: quien lo lea sin memoria previa
> debe poder ejecutarlo de principio a fin sin releer el plan maestro.

---

```markdown
# FASE XX — <Nombre de la fase>

> **Estado:** ⚪ PENDIENTE · **Depende de:** FASE XX-1 · **Riesgo:** Nulo/Bajo/Medio/Alto
> **Commit al cerrar:** `tipo(scope): descripcion en espanol`
> **Siguiente fase:** `fase-XX+1.md`

---

## 1. CONTEXTO

### 1.1 De dónde venimos
<Qué dejó lista la fase anterior. Si es la primera fase, describir el estado inicial del repo.>

### 1.2 Problema que resuelve esta fase
<Deuda técnica concreta con archivo y componentes exactos.>

### 1.3 Estado esperado al terminar
<Qué existirá que hoy no existe. Debe ser verificable objetivamente.>

### 1.4 Archivos involucrados
| Ruta | Acción |
|---|---|
| `src/app/...` | CREAR / MODIFICAR / LEER / NO TOCAR |

### 1.5 Reglas aplicables
<Referencias a `rules/angular-rules.md` que gobiernan esta fase.>

### 1.6 Decisiones pendientes que bloquean
| ID | Estado | Acción si sigue ABIERTA |
|---|---|---|
| DP-XX | 🔴 ABIERTA | DETENERSE y preguntar al usuario antes de continuar |

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer
<Tareas numeradas T-01…T-NN, imperativas y sin ambigüedad. Incluir nombres de archivo, de clase y
firmas de métodos exactas.>

### 2.2 Qué NO hacer
<Prohibiciones explícitas: qué archivos no tocar, qué librerías no añadir (ej. PrimeNG), qué copys no
reescribir.>

### 2.3 Criterios de aceptación
- [ ] <Criterio verificable 1>
- [ ] <Criterio verificable 2>

### 2.4 Checklist de calidad (obligatoria, de `rules/angular-rules.md`)
- [ ] Todos los componentes creados o tocados son `standalone: true`
- [ ] Cero `*ngIf` / `*ngFor` / `*ngSwitch`: solo `@if` / `@for` / `@switch`
- [ ] Estado reactivo con Signals u Observables seguros
- [ ] Cero fugas de memoria: `AsyncPipe` o `takeUntilDestroyed()`
- [ ] Botones de solo icono con `aria-label`
- [ ] Cero `any` en variables, parámetros o retornos
- [ ] Cero llamadas HTTP fuera de la capa de servicios de API
- [ ] Tipado explícito en todos los métodos públicos
- [ ] Uso exclusivo de Web Components Caribe (`<bc-*>`) para controles de UI
- [ ] `npm run build` correcto
- [ ] `npm test` sin fallos

---

## 3. ORDEN DE EJECUCIÓN

| Paso | Acción | Resultado esperado |
|---:|---|---|
| P-01 | Leer `docs/plan/ESTADO.md` y `docs/plan/DECISIONES_PENDIENTES.md` | Confirmar que la fase no está bloqueada |
| P-02 | Ejecutar tareas técnicas T-01 a T-NN | Código implementado |
| P-N-3 | Ejecutar `npm run build` y `npm test` | Verde |
| P-N-2 | **Escribir `docs/resultados/RESULTADO-FASE-XX.md`** con `_PLANTILLA_RESULTADO.md` | Trazabilidad de la ejecución |
| P-N-1 | Actualizar `docs/plan/ESTADO.md`: fase 🟢 COMPLETADA + métricas | Tablero al día |
| P-N | **Generar `docs/fases/fase-XX+1.md`** con esta misma plantilla | Checkpoint de continuidad creado |
| P-N+1 | Notificar al usuario para autorización de commit o siguiente paso | Paso listo |

> ⚠️ **Los pasos P-N-2 y P-N son innegociables.** Sin el registro de resultado no hay trazabilidad,
> y sin el MD de la siguiente fase el trabajo no es reanudable tras una desconexión.
```

