# Guía Práctica: Cómo Ejecutar un Ciclo de Program Planning

> **Cuadrante Diátaxis: Guías Prácticas / How-To (Problem-Oriented)**  
> Esta guía explica los pasos para ejecutar una planeación trimestral asistida por IA mediante el
> wizard interactivo o la interfaz conversacional.

---

## 🎯 Objetivo

Generar un roadmap estructurado de historias de usuario (HU) e historias de acompañamiento HyMS (HA)
delimitadas normativamente (**DP-PL-02**), calculando la capacidad real del equipo y persistiendo el
artefacto maestro `ideas_planning_QX.md`.

---

## Método 1: Mediante el Wizard Gráfico (Frontend)

1. **Abrir el Wizard:**  
   Ingresa a `http://localhost:4200` y pulsa el botón **"Nuevo Plan de Trabajo"** o navega a la
   pestaña de Program Planning.
2. **Paso 1: Parámetros del Trimestre:**
    - Selecciona el Trimestre (ej. `Q3-2026`).
    - Define el número de Sprints (ej. `6`).
    - Asigna la velocidad estimada por sprint (ej. `34 puntos de historia`).
    - Ingresa el nombre del frente de trabajo (ej. `Canales`).
3. **Paso 2: Diagnóstico de Capacidad:**  
   El sistema consultará a través del BFF y el servidor MCP la velocidad histórica y feriados del
   equipo para validar si la meta es factible o presenta sobreasignación.
4. **Paso 3: Iniciativas e Historias:**  
   Pega el texto de la iniciativa o historias base.
5. **Paso 4: Generación y Supervisión:**  
   Pulsa **"Generar Plan"**. El flujo reactivo SSE mostrará el progreso de descomposición.
6. **Paso 5: Aprobación:**  
   Revisa las HUs (hasta QA) y las HAs (HyMS). Confirma para persistir el documento en
   `docs/sdd/specs/`.

---

## Método 2: Mediante Comando Conversacional A2A

Puedes invocar la planeación directamente desde el chat del agente o vía JSON-RPC:

### Sintaxis del Comando:

```text
/plan <Trimestre> <N° Sprints> <Velocidad sp> [<Frente>]
```

### Ejemplo de Petición HTTP (`POST /` a `:8080`):

```json
{
  "jsonrpc": "2.0",
  "id": "cmd-plan-01",
  "method": "message/send",
  "params": {
    "message": "/plan Q3-2026 6 sprints 34 sp [Canales] Implementación de módulo de pagos QR interoperable"
  }
}
```

### Resultado Esperado:

El agente responderá con el plan en formato Markdown, registrando el documento en
`docs/sdd/specs/ideas_planning_q3_2026.md` y emitiendo un estado `COMPLETED`.
