# PLANTILLA OBLIGATORIA DE RESULTADO — Proyecto Janus (`janus-web`)

> Copia este archivo para generar cada `RESULTADO-FASE-XX.md` al cerrar una fase.
> Su ausencia deja la fase **incompleta**.

---

```markdown
# RESULTADO — FASE XX: <Nombre de la fase>

> **Ejecutada:** AAAA-MM-DD · **Estado final:** 🟢 COMPLETADA / 🔴 BLOQUEADA
> **Instrucciones:** `docs/fases/fase-XX.md`
> **Archivos modificados:** <N archivos>

---

## 1. Objetivo de la fase
<Párrafo conciso.>

## 2. Qué se construyó / modificó
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
| # | Métrica | Antes | Después | Meta |
|---:|---|---:|---:|---:|

## 5. Comandos ejecutados y resultados
```bash
# comandos exactos y salida resumida
```

## 6. Hallazgos no previstos
| ID | Hallazgo | Acción tomada |
|---|---|---|

## 7. Checklist de calidad
- [x] Standalone components
- [x] Control Flow moderno
- [x] Cero fugas de memoria
- [x] Cero `any`
- [x] Cero llamadas HTTP fuera de servicios
- [x] Web Components Caribe activos
- [x] `npm run build` OK
- [x] `npm test` OK

## 8. Desviaciones respecto a las instrucciones
| Desviación | Justificación |
|---|---|

## 9. Estado al cerrar
- Siguiente fase generada: `docs/fases/fase-XX+1.md`
- `docs/plan/ESTADO.md` actualizado: sí / no
```

