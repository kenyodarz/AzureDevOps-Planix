# CIERRE DEL PLAN — `azure-devops-mcp`

> **Fecha:** 2026-08-30 · **Estado:** 🟢 **PLAN CERRADO** · **Fases:** 8 de 8 completadas
> **Plan:** [`docs/plan/plan-maestro.md`](../plan/plan-maestro.md)
> **Build final:** 🟢 **VERDE** — **204 pruebas, 0 fallos**

---

## 1. De dónde se partía

El diagnóstico de la Fase 01 encontró un servidor MCP funcional con **la pirámide invertida**: toda
la lógica de valor vivía en el entry-point, el dominio hacía de contrato de cable en los dos
extremos, y los mecanismos de control informaban de éxito sin comprobarlo.

```
BASELINE                                     CIERRE

entry-point  ████████████ 267 líneas         entry-point  ████ 182 (protocolo MCP y nada más)
             (WIQL + rutas + normalización)
usecase      █ (7 delegantes vacíos)         usecase      ███████ (el flujo compuesto)
model        █ (anémico, mutable)            model        █████ (15 VOs con invariantes)
adapter      ████████ (7 gateways juntos)    adapter      ██ ██ ██ (uno por agregado)
```

## 2. Las cifras, de principio a fin

| Métrica                                          | Baseline |                                                              Cierre |
|--------------------------------------------------|---------:|--------------------------------------------------------------------:|
| Modos de seguridad declarados y probados         |        0 |                                                            ✅ **2** |
| Recompilaciones para endurecer la seguridad      |        1 |                                                            ✅ **0** |
| Tools con autorización declarada **y compilada** |   2 de 8 |                                                       ✅ **6 de 6** |
| Anotaciones de seguridad comentadas              |        3 |                                                            ✅ **0** |
| Reglas de negocio en `entry-points`              |        4 |                                                            ✅ **0** |
| Fronteras de contrato cerradas                   |   0 de 3 |                                                       ✅ **3 de 3** |
| Modelos de dominio cruzando el cable             |        5 |                                                            ✅ **0** |
| Clases de `domain/model` mutables                |        9 |                                                            ✅ **0** |
| Value Objects inmutables                         |        0 |                                                           ✅ **15** |
| Excepciones de dominio                           |        0 |                                                ✅ **3** *(+1 base)* |
| Errores técnicos crudos hacia el cliente         |    todos |                                                            ✅ **0** |
| Cortacircuitos configurados                      |   0 de 7 |                                                       ✅ **3 de 3** |
| Timeouts por operación                           |        0 |                                                            ✅ **4** |
| Puertos por agregado                             |    7 / 1 |                                                            ✅ **3** |
| Puertos huérfanos                                |        1 |                                                            ✅ **0** |
| Mecanismos de wiring por bean                    |        2 |                                                            ✅ **1** |
| Tools comentadas con código vivo                 |        1 |                                                            ✅ **0** |
| Capacidades MCP anunciadas sin implementar       |        2 |                                                            ✅ **0** |
| Pruebas que no pueden fallar                     |        1 |                                                            ✅ **0** |
| Pruebas totales                                  |   **33** |                                                          ✅ **204** |
| Cobertura `domain/model`                         |      0 % |                                                       ✅ **96,2 %** |
| Cobertura `domain/usecase`                       |   68,2 % |                                                       ✅ **97,8 %** |
| Cobertura `mcp-server`                           |   70,5 % |                                                       ✅ **84,3 %** |
| Cobertura `rest-consumer`                        |   51,1 % |                                                       ✅ **92,3 %** |
| Cobertura `app-service`                          |   17,0 % |                                     🔸 **55,7 %** *(objetivo 60 %)* |
| Mutaciones (Pitest)                              |     5/44 | ✅ **98 %** `model` · **97 %** `usecase` · **84 %** `rest-consumer` |

## 3. Las ocho decisiones del propietario

Ninguna fase resolvió por cuenta propia una decisión de negocio. **Seis de las ocho fases se
detuvieron a preguntar antes de escribir código**, y las seis veces fue lo correcto.

| ID        | Qué se decidió                                                                                                             |
|-----------|----------------------------------------------------------------------------------------------------------------------------|
| **DP-01** | La seguridad se queda laxa, pero pasa de **código a configuración**: `mcp.security.mode`, mismo binario, dos modos         |
| **DP-02** | El PAT vive fuera del repositorio; su ausencia **rompe el arranque** en `ENFORCED`. **B-01:** el formato no cambia         |
| **DP-03** | Se autoriza renombrar tipos de dominio; **el cable no se toca**. `WorkItemsBatchRequest` → `WorkItemBatchCriteria`         |
| **DP-04** | El repliegue **se conserva y se mide**; las reglas de tipos son **dominio**; el caso de uso recibe un objeto comando       |
| **DP-05** | Se reagrupan paquetes y puertos: **7 → 3**. **B-05:** nombres de cortacircuito definitivos                                 |
| **DP-06** | Contrato de errores público: `CODIGO: mensaje neutro`, **4 códigos**. El cuerpo de Azure DevOps **se registra y no viaja** |
| **DP-07** | Se cierra la **última frontera** (salida hacia MCP); el modelo pasa a `record`; invariantes **mínimas**                    |
| **DP-08** | Se retiran `queryByWiql` como tool y `HealthTool` entero; capacidades a `false`; **un solo mecanismo de wiring**           |

## 4. Lo que se aprendió por el camino

**1. Una prueba que no puede fallar es peor que no tener prueba.** Aparecieron dos:
`UseCasesConfigTest` (D-27) envolvía todo en un `catch` sobre un contexto que no podía arrancar, de
modo que su única aserción probablemente **no se ejecutó jamás**; y el `issues.json` de ArchUnit
(D-25) sale vacío haya o no violaciones. Las dos daban verde. Ninguna comprobaba nada.

**2. El orden importa tanto como el resultado.** En la Fase 07, escribir la prueba de
caracterización **antes** de tocar el modelo fue lo que permitió demostrar que el JSON no cambió. Al
revés, la prueba habría congelado lo que produjera el código nuevo y habría certificado como
correcto cualquier cambio silencioso.

**3. Las cifras heredadas hay que medirlas.** El plan arrastraba dos que eran falsas: «21 clases con
`@Setter`» —eran **9**— y «5 modelos serializados hacia MCP» —eran **4**—. Ninguna cambiaba lo que
había que hacer, pero las dos cambiaban el tamaño declarado del problema.

**4. Endurecer de más también rompe.** El javadoc de `TeamScope` dejó escrito que rechazar cadenas
en blanco convertiría un tablero vacío en un error. Ese precedente evitó que la Fase 07 reabriera
DP-04 sin querer, y por eso las tolerancias están hoy **fijadas por prueba**.

**5. Lo que parece limpieza a veces es contrato.** Poner las capacidades MCP a `false` parecía un
detalle cosmético; la especificación dice que declarar una capacidad es **una promesa de
responder**.
Casi se decide al revés por intuición.

## 5. Deuda que queda viva

| ID                                 | Estado             | Por qué                                                                                                                                                                                                                                                                                                                                             |
|------------------------------------|--------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **D-25**                           | 🟠 **Abierta**     | El `issues.json` para SonarQube sale vacío. Se diagnosticó la causa —el troceado de rutas de `Utils.findFiles()` no contempla el separador de Windows— pero **el fichero lo genera el plugin `cleanArchitecture`**, así que cualquier corrección se pierde en el siguiente build. **Requiere arreglo upstream** o una prueba de arquitectura propia |
| **ArchUnit como `error`**          | 🟠 **No aplicado** | El `checkWithWarning` vive en `ArchitectureTest.java`, también generado por el plugin. No existe propiedad documentada para cambiarlo; solo `arch.unit.skip`                                                                                                                                                                                        |
| **Cobertura `app-service`**        | 🔸 **55,7 %**      | Por debajo del objetivo del 60 %. Bajó al retirar `HealthTool` y los nueve `@Bean`, que **sí** estaban cubiertos                                                                                                                                                                                                                                    |
| **`AzureDevOpsTools`: 182 líneas** | 🔸 **Aceptada**    | Objetivo ≤ 120. **B-12 lo aceptó formalmente**: lo que exige `spring-rules.md` es **0 reglas de negocio**, y eso se cumple. Lo que queda es declaración de protocolo y javadoc                                                                                                                                                                      |

## 6. Estado de las 27 deudas

**Saldadas: 23** · **Vivas: 4** *(D-25 y las tres anotadas arriba)*

| Fase | Deudas saldadas                       |
|------|---------------------------------------|
| 01   | D-21 *(enunciado reescrito en la 08)* |
| 02   | D-01, D-02, D-03, D-04, D-22          |
| 03   | D-11, D-17                            |
| 04   | D-07, D-08, D-09, D-12, D-26          |
| 05   | D-10, D-14, D-15                      |
| 06   | D-06, D-13, D-18, D-24                |
| 07   | D-16                                  |
| 08   | D-05, D-19, D-20, D-23, D-27          |

## 7. Lo que el sistema garantiza hoy y no garantizaba al empezar

1. **Un `WorkItem` válido no puede dejar de serlo** a mitad de un flujo reactivo.
2. **Ningún modelo de dominio cruza ninguna de las tres fronteras** de contrato.
3. **Ningún error técnico de Azure DevOps llega crudo al cliente**: los cuatro códigos son públicos
   y
   estables, y el cuerpo original se registra sin propagarse.
4. **La sentencia WIQL está congelada carácter a carácter** en sus ocho ramas.
5. **El JSON de respuesta está congelado contra literales**, no contra sí mismo.
6. **Endurecer la seguridad no requiere recompilar**: una variable de entorno.
7. **El wiring tiene un solo mecanismo**, y hay una prueba que se pone roja si vuelve a haber dos.
8. **El repliegue de rutas sigue vivo, medido y documentado** — con sus tolerancias fijadas por
   prueba para que nadie las elimine por parecer más limpias.

