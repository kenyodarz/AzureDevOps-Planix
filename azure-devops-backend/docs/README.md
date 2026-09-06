# Protocolo de Continuidad SDD — Integración de Program Planning en el BFF

> **Módulo:** `azure-devops-backend` (BFF)  
> **Metodología:** Spec-Driven Development (SDD) con retroalimentación continua  
> **Especificación Base:** `docs/specs/ESPECIFICACION-INTEGRACION-PLANNING-BFF.md`  
> **Última actualización:** 2026-09-06

---

## 1. Principios Operativos Fundamentales

1. **Contexto Pequeño y Focalizado:** Cada fase técnica es un documento individual y autosuficiente. No se debe cargar todo el historial para ejecutar una tarea.
2. **Generación Encadenada de Fases (La Regla del Doble Artefacto):**  
   Al concluir y verificar una fase con pruebas unitarias reales:
   - Se redacta el resultado en `resultados/RESULTADO-FASE-XX.md`.
   - **Se genera inmediatamente el archivo de la siguiente fase:** `fases/FASE-XX+1-<nombre>.md` y su prompt `prompts/PROMPT-FASE-XX+1.md`.
   - Se actualiza `plan/ESTADO.md`.
3. **Ajuste Adaptativo ante Hallazgos:**  
   Si durante la ejecución de una fase surgen descubrimientos técnicos, impedimentos o restricciones no previstas, la fase tiene la potestad de ajustar el Plan Maestro y la especificación de la siguiente fase. El plan es una guía viva adaptativa.
4. **Política de No-Asunción:** Si aparece una disyuntiva de arquitectura o alcance que bloquee la fase, se registra en `plan/DECISIONES_PENDIENTES.md` como `🔴 ABIERTA` y el desarrollador o agente **se detiene inmediatamente y consulta al usuario**.

---

## 2. Estructura de Documentación del BFF

```text
azure-devops-backend/docs/
├── README.md                            <- Este protocolo de continuidad
├── specs/                               <- Especificaciones técnicas funcionales
│   └── ESPECIFICACION-INTEGRACION-PLANNING-BFF.md
├── historico/                           <- Planes pasados ya completados
│   └── plan_refactorizacion_bff/
├── plan/
│   ├── PLAN_MAESTRO.md                  <- Contrato arquitectónico y desglose de fases
│   ├── DECISIONES_PENDIENTES.md         <- Registro de decisiones técnicas (DP-BFF-XX)
│   └── ESTADO.md                        <- Tablero vivo de avance (ÚNICA FUENTE DE VERDAD)
├── fases/
│   └── FASE-01-modelos-y-puerto-spec-storage.md <- Fase activa lista para ejecutar
├── prompts/
│   └── PROMPT-FASE-01.md                <- Prompt ejecutable para iniciar la fase activa
└── resultados/
    └── .gitkeep                         <- Resultados medidos de fases completadas
```

---

## 3. Protocolo de Ejecución Paso a Paso

1. Abrir [ESTADO.md](file:///c:/Users/minaj/Work/GitHub/Labs/AzureDevOps/azure-devops-backend/docs/plan/ESTADO.md) y leer la fase activa.
2. Abrir [DECISIONES_PENDIENTES.md](file:///c:/Users/minaj/Work/GitHub/Labs/AzureDevOps/azure-devops-backend/docs/plan/DECISIONES_PENDIENTES.md) y verificar que no existan decisiones bloqueantes en `🔴 ABIERTA`.
3. Abrir la fase activa en `fases/FASE-XX-*.md` o su prompt en `prompts/PROMPT-FASE-XX.md` y seguir sus instrucciones al pie de la letra.
4. Ejecutar la suite de pruebas unitarias (`./gradlew test`) y `./gradlew validateStructure`.
5. **Cerrar la fase:**
   - Crear `resultados/RESULTADO-FASE-XX.md` con mediciones reales.
   - Si hubo hallazgos, actualizar `plan/PLAN_MAESTRO.md` o la siguiente fase.
   - Actualizar `plan/ESTADO.md` marcando la fase como `🟢 COMPLETADA`.
   - Generar `fases/FASE-XX+1-*.md` y `prompts/PROMPT-FASE-XX+1.md` para la próxima sesión.
   - Solicitar confirmación de commit siguiendo `tipo(scope): descripción`.
