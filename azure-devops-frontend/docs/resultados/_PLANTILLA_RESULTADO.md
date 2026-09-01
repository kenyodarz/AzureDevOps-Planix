# PLANTILLA OBLIGATORIA DE RESULTADO — azure-devops-frontend

> Copia este archivo para generar cada `RESULTADO-FASE-XX.md` al cerrar una fase.
> Su ausencia deja la fase **incompleta**.

---

````markdown
# RESULTADO — FASE XX: <Nombre de la fase>

> **Ejecutada:** AAAA-MM-DD · **Estado final:** 🟢 COMPLETADA / 🔴 BLOQUEADA
> **Commit:** `<hash>` — `tipo(scope): descripcion`
> **Instrucciones:** `docs/fases/fase-XX.md`
> **Código productivo modificado:** <N líneas / cero líneas>

---

## 1. Objetivo de la fase
<Párrafo conciso.>

## 2. Qué se construyó
| Artefacto | Acción | Detalle |
|---|---|---|
| `src/app/...` | CREADO / MODIFICADO / ELIMINADO | ... |

### 2.1 Pruebas nuevas o reescritas
| Archivo | Casos antes | Casos después |
|---|---:|---:|

## 3. Decisiones aplicadas
| ID | Resolución | Efecto en esta fase |
|---|---|---|

## 4. Métricas obtenidas
| # | Métrica | Antes | Después | Objetivo |
|---:|---|---:|---:|---:|

## 5. Comandos ejecutados
```bash
# comandos exactos, con su salida resumida
```

## 6. Hallazgos no previstos
| ID | Hallazgo | Acción tomada |
|---|---|---|
| H-1 | ... | Nueva deuda D-XX registrada en el plan maestro §3 |

## 7. Checklist de calidad
<Réplica de §2.4 del MD de la fase, con todas las casillas marcadas y evidencia.>

## 8. Desviaciones respecto a las instrucciones
| Desviación | Justificación |
|---|---|

## 9. Estado al cerrar
- Siguiente fase generada: `docs/fases/fase-XX+1.md`
- Decisiones abiertas: DP-XX, DP-YY
- `docs/plan/ESTADO.md` actualizado: sí / no
````

