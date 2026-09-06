# Estándar Metodológico: Spec-Driven Development (SDD) para Agentes de IA

> **Propósito:** Marco de trabajo universal y agnóstico para planificar, ejecutar y mantener trazabilidad en proyectos de software desarrollados o asistidos por Agentes de Inteligencia Artificial.  
> **Aplicabilidad:** Universal (monorepos, microservicios, frontend, backend, librerías, scripts).

---

## 1. ¿Por qué Spec-Driven Development (SDD) con IA?

Los asistentes y agentes de código autónomos enfrentan tres problemas críticos cuando abordan tareas medianas o grandes:
1. **Saturación de ventana de contexto:** A medida que la conversación se alarga, el modelo pierde instrucciones tempranas, alucina contratos o repite errores.
2. **Dispersión horizontal (Scope Creep):** Intentar implementar demasiadas cosas a la vez produce código incompleto, clases a medio terminar y rotura de componentes adyacentes.
3. **Pérdida de continuidad entre sesiones:** Si la sesión se reinicia, se cae la conexión o entra un agente nuevo, no hay forma de saber con certeza qué se completó, qué falló y qué falta por hacer.

**SDD resuelve esto dividiendo el trabajo en fases atómicas autocontenidas**, donde el agente solo necesita leer el estado actual y la especificación de la fase activa para operar con máxima precisión y contexto mínimo.

---

## 2. Los 6 Artefactos Nucleares del Framework

En esta carpeta (`docs/`) residen las plantillas maestras reutilizables para cualquier iniciativa:

| Artefacto | Ubicación de Plantilla | Propósito y Contrato |
| :--- | :--- | :--- |
| **Plan Maestro** | `plan/_PLANTILLA_PLAN_MAESTRO.md` | Contrato global de arquitectura: objetivo, diagnóstico con IDs (D-XX), diagrama de flujo/componentes y mapa completo de fases atómicas (01 a 0N). |
| **Decisiones Pendientes** | `plan/_PLANTILLA_DECISIONES_PENDIENTES.md` | Instrumento de la **Política de No-Asunción**: registro de disyuntivas de diseño (DP-XX) categorizadas con semáforo (`🔴 ABIERTA`, `🟢 RESUELTA`, `⚪ INFORMATIVA`). |
| **Tablero de Estado** | `plan/_PLANTILLA_ESTADO.md` | **Única fuente de verdad del progreso**: dashboard de lectura en 1 minuto que indica la fase activa, la siguiente acción inmediata y las métricas vivas de calidad. |
| **Especificación de Fase** | `fases/_PLANTILLA_FASE.md` | Guía técnica ejecutable y autosuficiente para una fase individual (Contexto, Instrucciones paso a paso, Qué NO hacer, Criterios de aceptación). |
| **Prompt Ejecutable** | `prompts/_PLANTILLA_PROMPT.md` | Prompt autosuficiente estructurado para instruir a cualquier agente o sesión en la ejecución de la fase activa sin ambigüedades. |
| **Resultado de Fase** | `resultados/_PLANTILLA_RESULTADO.md` | Informe de cierre que registra **métricas reales medidas** (tests unitarios, cobertura, archivos modificados y hallazgos descubiertos). |

---

## 3. Estructura de Directorios Recomendada en Cada Proyecto

Cuando se aplica este estándar a un proyecto o módulo, la carpeta de documentación debe adoptar la siguiente jerarquía:

```
<ruta-del-modulo>/docs/
├── README.md                            <- Protocolo de continuidad del módulo
├── historico/                           <- Planes pasados completados y archivados
│   └── plan_<nombre_iniciativa_anterior>/
├── plan/                                <- Estrategia global y control
│   ├── PLAN_MAESTRO.md
│   ├── DECISIONES_PENDIENTES.md
│   └── ESTADO.md
├── fases/                               <- Fases atómicas ejecutables (ANTES)
│   └── FASE-01-<nombre>.md
├── prompts/                             <- Prompts listos para detonar cada fase
│   └── PROMPT-FASE-01.md
└── resultados/                          <- Evidencia con métricas reales (DESPUÉS)
    └── .gitkeep
```

---

## 4. Instructivo Operativo para Agentes de IA

Cualquier agente que interactúe con un repositorio bajo este estándar debe seguir estrictamente uno de los dos flujos operativos:

### Flujo A: Inicializar un Nuevo Plan de Trabajo

Cuando el usuario solicite planificar una nueva funcionalidad, refactorización o iniciativa:

1. **Gestión de Histórico:**  
   Si en la carpeta de documentación del módulo ya existen carpetas `plan/`, `fases/`, etc. de un trabajo completado anteriormente, **nunca las sobreescribas**. Muévelas completas a `docs/historico/plan_<nombre_descriptivo>/`.
2. **Dimensionamiento Atómico (Regla Anti-Monolito):**  
   * **Prohibido:** Crear planes de 2 o 3 macro-fases gigantes donde se mezcle diseño, backend, frontend y tests.
   * **Obligatorio:** Descomponer la iniciativa en **fases atómicas y quirúrgicas de 2 a 4 archivos como máximo**, diseñadas para ejecutarse y verificarse en una sesión de trabajo de corta duración.
3. **Instanciación Inicial:**  
   Copia y completa las plantillas maestras creando:
   * `docs/README.md`
   * `docs/plan/PLAN_MAESTRO.md` (con el mapa de fases 01 a 0N)
   * `docs/plan/DECISIONES_PENDIENTES.md` (con las decisiones iniciales resueltas o abiertas)
   * `docs/plan/ESTADO.md` (con la Fase 01 en `🟡 PENDIENTE` y las restantes en `⚪ NO GENERADA`)
   * `docs/fases/FASE-01-<nombre>.md` (especificación completa de la primera fase)
   * `docs/prompts/PROMPT-FASE-01.md` (prompt listo para detonar la ejecución)
4. **Pausa Obligatoria y Aprobación:**  
   Presenta el resumen del plan al usuario y **espera su visto bueno explícito antes de tocar cualquier archivo de código**.

---

### Flujo B: Ejecutar y Continuar un Plan Activo

Cuando se inicie una sesión de desarrollo para continuar el trabajo:

1. **Lectura en Cascada (Mínimo Contexto):**  
   * Lee `docs/plan/ESTADO.md` $\rightarrow$ Localiza la fase activa (`🟡 PENDIENTE` o `🔵 EN_CURSO`).
   * Lee `docs/plan/DECISIONES_PENDIENTES.md` $\rightarrow$ Verifica que no existan decisiones bloqueantes en estado `🔴 ABIERTA`. Si hay una decisión abierta que impida avanzar, **DETENTE y pregunta al usuario**.
   * Lee la especificación en `docs/fases/FASE-XX-*.md` o su prompt en `docs/prompts/PROMPT-FASE-XX.md`.
2. **Ejecución Técnica:**  
   Implementa los cambios respetando las instrucciones de la fase, la arquitectura del proyecto y las buenas prácticas de código limpio e inmutabilidad.
3. **Validación Automática:**  
   Ejecuta las pruebas unitarias, linters y suites de verificación técnica del proyecto. No se avanza si hay tests en rojo.
4. **Cierre de Fase — La Regla del Doble Artefacto:**  
   Una fase **NUNCA** se da por terminada únicamente modificando código. El agente debe ejecutar estos 5 pasos de cierre:
   * **1. Registrar el Resultado:** Crear `docs/resultados/RESULTADO-FASE-XX.md` con mediciones reales y verificables.
   * **2. Adaptabilidad ante Hallazgos:** Si durante la implementación se descubrieron restricciones o mejores enfoques, **ajustar el Plan Maestro o la siguiente fase**. El plan es adaptativo y evoluciona con la realidad técnica.
   * **3. Actualizar el Tablero:** Marcar en `docs/plan/ESTADO.md` la fase como `🟢 COMPLETADA` con su fecha y notas.
   * **4. Generar la Siguiente Fase y Prompt:** Redactar de inmediato `docs/fases/FASE-XX+1-*.md` y `docs/prompts/PROMPT-FASE-XX+1.md` para que la continuidad sea automática.
   * **5. Confirmar Commit:** Proponer el mensaje de commit siguiendo las convenciones del repositorio (`tipo(scope): descripción`) y solicitar confirmación.

---

## 5. Las 5 Reglas de Oro Inquebrantables

1. **Cero Código sin Especificación:** Todo cambio de código debe pertenecer a una fase documentada previamente en `fases/FASE-XX.md`.
2. **Atomicidad Quirúrgica:** Una fase no debe abarcar más de 4 archivos de lógica ni tardar más de una sesión en ser probada y cerrada. Si crece, se atomiza.
3. **Política Estricta de No-Asunción:** Si hay incertidumbre sobre un contrato, una regla de negocio o una decisión de diseño, el agente debe detenerse y consultar al usuario. Queda prohibido asumir silenciosamente.
4. **Doble Artefacto de Cierre:** El cierre de una fase genera el resultado medido de la fase actual y la especificación ejecutable de la siguiente.
5. **Mejora Continua y Adaptabilidad:** Los hallazgos y lecciones aprendidas de una fase tienen la facultad de refinar y optimizar las fases posteriores en el Plan Maestro.
