# PLANTILLA OBLIGATORIA: TABLERO DE ESTADO

> Copia este archivo para generar cada `ESTADO.md`.  
> **Propósito:** Tablero vivo y **ÚNICA FUENTE DE VERDAD** del progreso del plan.  
> **Lineamiento:** Diseñado como un dashboard de lectura en 1 minuto. Debe actualizarse al cerrar cada fase antes de hacer commit.

---

```markdown
# Tablero de Estado — <Título del Plan / Módulo>

> **ÚNICA FUENTE DE VERDAD DEL PROGRESO.**  
> Punto de entrada obligatorio al iniciar cualquier sesión de desarrollo o antes de ejecutar código.  
> 
> **Última actualización:** AAAA-MM-DD · **Fase activa:** <Fase XX — Nombre>

---

## 1. Tablero de Fases

| # | Fase | Archivo de Especificación | Resultado / Cierre | Estado | Fecha de Cierre |
| :---: | :--- | :--- | :--- | :---: | :---: |
| **01** | <Nombre Fase 01> | `fases/FASE-01-nombre.md` | `resultados/RESULTADO-FASE-01.md` | 🟡 **PENDIENTE** | — |
| **02** | <Nombre Fase 02> | `fases/FASE-02-nombre.md` | `resultados/RESULTADO-FASE-02.md` | ⚪ NO GENERADA | — |
| **03** | <Nombre Fase 03> | `fases/FASE-03-nombre.md` | `resultados/RESULTADO-FASE-03.md` | ⚪ NO GENERADA | — |

**Leyenda:** ⚪ NO GENERADA · 🟡 PENDIENTE · 🔵 EN_CURSO · 🟢 COMPLETADA · 🔴 BLOQUEADA

---

## 2. Siguiente Acción Concreta

> 🎯 **<Fase Activa> Lista para Ejecución:**  
> <Instrucción concisa en una sola frase de qué archivo abrir y qué comando o tarea ejecutar inmediatamente.>

---

## 3. Decisiones Abiertas que Bloquean

| ID | Bloquea | Estado | Resumen |
| :---: | :---: | :---: | :--- |
| **DP-XX-01** | Fase XX | 🔴 **ABIERTA** / 🟢 **RESUELTA** | <Descripción corta de la decisión> |

---

## 4. Métricas Vivas del Proyecto

| Métrica | Baseline Inicial | Meta del Plan | Estado Actual |
| :--- | :---:| :---:| :---: |
| **Tests Unitarios Pasando** | X | 100% | 🟢 X / X |
| **Cobertura de Código** | X% | $\ge 90\%$ | 🟢 X% |
| **Violaciones de Arquitectura** | 0 | 0 | 🟢 0 |
| **Estado del Build** | OK | OK | 🟢 Limpio |

---

## 5. Bitácora del Plan

| Fecha | Evento |
| :---: | :--- |
| **AAAA-MM-DD** | <Descripción del hito o fase completada.> |
```
