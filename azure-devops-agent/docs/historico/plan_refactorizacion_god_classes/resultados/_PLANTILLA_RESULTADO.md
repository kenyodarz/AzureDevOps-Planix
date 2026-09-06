# PLANTILLA — Registro de resultado de fase

> Copia esta plantilla para generar cada `RESULTADO-FASE-XX.md`.
> Se escribe **al terminar** la fase, con datos reales de la ejecución, nunca con estimaciones.
> Su propósito es que cualquiera pueda auditar qué se hizo sin releer el diff completo.

---

```markdown
# RESULTADO — FASE XX: <Nombre>

> **Ejecutada:** AAAA-MM-DD · **Estado final:** 🟢 COMPLETADA / 🔴 ABORTADA
> **Commit:** `<hash>` — `tipo(scope): descripcion`
> **Instrucciones:** `docs/fases/FASE-XX-<nombre>.md`

## 1. Objetivo de la fase
<Una o dos frases. Qué deuda se ataca.>

## 2. Qué se construyó
<Árbol de archivos creados y modificados, agrupados por capa.>

## 3. Decisiones aplicadas
<Por cada DP-XX aplicada: resolución y cómo se materializó en el código.>

## 4. Métricas obtenidas
| Métrica | Antes | Después |
|---|---:|---:|
<Datos REALES medidos, no estimados.>

## 5. Comandos ejecutados
<Comandos literales y su resultado.>

## 6. Desviaciones respecto a las instrucciones
| Desviación | Justificación |
|---|---|
<Toda diferencia entre lo planificado y lo hecho, con su razón. Si no hubo, indicarlo.>

## 7. Checklist de calidad
<Tabla con los criterios de `spring-rules.md` §7 y su estado real.>

## 8. Hallazgos
<Defectos, deudas o sorpresas encontradas. Cada uno con la fase en que se atenderá.>

## 9. Estado al cerrar
- **Siguiente fase generada:** `FASE-XX+1-<nombre>.md`
- **Decisiones abiertas:** <lista>
- **Pendiente de ratificación:** <lista o «ninguno»>
```

