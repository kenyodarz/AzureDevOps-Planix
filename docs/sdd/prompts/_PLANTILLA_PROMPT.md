# PLANTILLA DE PROMPT PARA EJECUCIÓN DE FASES — Proyecto Janus

> Utiliza esta estructura para alimentar al agente de IA al iniciar cada fase del plan de migración.

---

```text
Actúa como un desarrollador experto en Angular 22, arquitectura limpia y Sistema de Diseño Caribe / Web Components Bancolombia.

Vamos a ejecutar la FASE XX de la migración del proyecto Janus:
1. Lee obligatoriamente:
   - docs/plan/ESTADO.md
   - docs/plan/DECISIONES_PENDIENTES.md
   - docs/fases/fase-XX.md
   - rules/angular-rules.md
   - COMMIT_RULES.md

2. Ejecuta todas las tareas técnicas T-01...T-NN especificadas en docs/fases/fase-XX.md sin desviaciones.
3. Asegúrate de cumplir con todas las restricciones de arquitectura (Zoneless, Signals, Control Flow, Web Components Caribe <bc-*> y cero PrimeNG).
4. Ejecuta y valida:
   - npm run build
   - npm test
5. Genera el entregable de trazabilidad:
   - docs/resultados/RESULTADO-FASE-XX.md (siguiendo docs/resultados/_PLANTILLA_RESULTADO.md)
   - Actualiza docs/plan/ESTADO.md con el nuevo estado y métricas.
   - Genera docs/fases/fase-XX+1.md para la siguiente fase.
6. NO realices commits automáticos sin antes presentar un resumen claro de los cambios y solicitar confirmación.
```

