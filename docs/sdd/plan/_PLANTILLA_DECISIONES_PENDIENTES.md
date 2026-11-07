# PLANTILLA OBLIGATORIA: DECISIONES PENDIENTES

> Copia este archivo para generar cada `DECISIONES_PENDIENTES.md`.  
> **Propósito:** Registro estricto de la **Política de No-Asunción**. Si una decisión que bloquea la fase activa está abierta, el agente o desarrollador debe **detenerse y preguntar**.

---

```markdown
# DECISIONES PENDIENTES — <Título del Plan / Módulo>

> **Regla rectora:** Política de No-Asunción (`.github/copilot-instructions.md`).  
> **Última actualización:** AAAA-MM-DD  
> 
> Si una decisión que bloquea la fase activa se encuentra marcada como 🔴 **ABIERTA**, el desarrollador o agente de IA debe **detenerse inmediatamente y solicitar confirmación al usuario**. Queda estrictamente prohibido asumir o implementar por criterio propio una alternativa abierta.

---

## Leyenda de Estados

| Estado | Significado |
| :---: | :--- |
| 🔴 **ABIERTA** | Requiere pronunciamiento o confirmación del usuario. Bloquea la fase indicada. |
| 🟢 **RESUELTA** | Decisión confirmada y acordada. Registra la fecha, la justificación y el impacto en código. |
| ⚪ **INFORMATIVA** | Lineamiento acordado que no bloquea la ejecución de la fase en curso. |

---

## Registro de Decisiones

### DP-XX-01 — <Título Corto y Claro de la Decisión>
- **Estado:** 🔴 **ABIERTA** / 🟢 **RESUELTA (AAAA-MM-DD)** · **Bloqueaba:** Fase XX
- **Contexto y Problema:** <Por qué surge la disyuntiva, qué alternativas existen y qué implicación técnica tiene cada una.>
- **Alternativas Evaluadas:**
  1. *Opción A:* <Descripción, pros y contras.>
  2. *Opción B:* <Descripción, pros y contras.>
- **Decisión Adoptada:** <Texto claro de la alternativa elegida y su justificación técnica o de negocio.>
- **Impacto en Código:** <Módulos, clases, capas o configuraciones afectadas.>

---

### DP-XX-02 — <Título Corto y Claro de la Decisión>
- **Estado:** 🔴 **ABIERTA** / 🟢 **RESUELTA (AAAA-MM-DD)** · **Bloqueaba:** Fase XX
- **Contexto y Problema:** <Descripción.>
- **Decisión Adoptada:** <Texto.>
- **Impacto en Código:** <Archivos involucrados.>
```
