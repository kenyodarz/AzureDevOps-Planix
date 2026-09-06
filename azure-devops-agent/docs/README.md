# Protocolo de Continuidad SDD — Harness de Agentes (Planning & Story Engine)

> **Módulo:** `azure-devops-agent`  
> **Metodología:** Spec-Driven Development (SDD) con retroalimentación continua.  
> **Última actualización:** 2026-09-05

---

## 1. Principios Operativos Fundamentales

1. **Contexto Pequeño y Focalizado:** Cada fase técnica es un documento individual y autosuficiente.
   No se debe cargar todo el historial para ejecutar una tarea.
2. **Generación Encadenada de Fases (La Regla del Doble Artefacto):**  
   Al concluir y verificar una fase con pruebas unitarias reales:
    * Se redacta el resultado en `resultados/RESULTADO-FASE-XX.md`.
    * **Se genera inmediatamente el archivo de la siguiente fase:** `fases/FASE-XX+1-<nombre>.md`.
    * Se actualiza `plan/ESTADO.md`.
3. **Ajuste Adaptativo ante Hallazgos:**  
   Si durante la ejecución de una fase surgen descubrimientos técnicos, impedimentos o desviaciones
   no previstas, **la fase tiene la potestad de ajustar el Plan Maestro y la especificación de la
   siguiente fase**. El plan es una guía viva, no una camisa de fuerza.
4. **Política de No-Asunción:** Si aparece una duda de arquitectura o alcance que bloquee la fase,
   se registra en `plan/DECISIONES_PENDIENTES.md` como `🔴 ABIERTA` y el agente **se detiene y
   pregunta al usuario**.

---

## 2. Estructura de Documentación del Agente

```
azure-devops-agent/docs/
├── README.md                            <- Este protocolo
├── historico/                           <- Planes pasados ya completados
│   └── plan_refactorizacion_god_classes/
├── plan/
│   ├── PLAN_MAESTRO.md                  <- Contrato arquitectónico y desglose de 9 fases
│   ├── DECISIONES_PENDIENTES.md         <- Registro de decisiones técnicas (DP-PL-XX)
│   └── ESTADO.md                        <- Tablero vivo de avance (ÚNICA FUENTE DE VERDAD)
├── fases/
│   └── FASE-01-modelos-y-puerto-spec-storage.md <- Fase activa lista para ejecutar
└── resultados/
    └── .gitkeep                         <- Resultados medidos de fases completadas
```

---

## 3. Protocolo de Ejecución Paso a Paso

1.
Abrir [ESTADO.md](file:///c:/Users/minaj/Work/GitHub/Labs/AzureDevOps/azure-devops-agent/docs/plan/ESTADO.md)
y leer la fase activa.
2.
Abrir [DECISIONES_PENDIENTES.md](file:///c:/Users/minaj/Work/GitHub/Labs/AzureDevOps/azure-devops-agent/docs/plan/DECISIONES_PENDIENTES.md)
y verificar que no existan decisiones bloqueantes en `🔴 ABIERTA`.
3. Abrir la fase activa en `fases/FASE-XX-*.md` y seguir sus instrucciones al pie de la letra.
4. Ejecutar la suite de pruebas unitarias (`./gradlew test`).
5. **Cerrar la fase:**
    * Crear `resultados/RESULTADO-FASE-XX.md` con mediciones reales.
    * Si hubo hallazgos, actualizar `plan/PLAN_MAESTRO.md` o la siguiente fase.
    * Actualizar `plan/ESTADO.md` a `🟢 COMPLETADA`.
    * Generar `fases/FASE-XX+1-*.md` para la próxima sesión.
    * Realizar commit siguiendo `tipo(scope): descripción`.
