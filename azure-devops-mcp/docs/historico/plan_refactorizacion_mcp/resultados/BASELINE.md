# BASELINE — `azure-devops-mcp`

> **Fase:** [`docs/fases/fase-01.md`](../fases/fase-01.md) · **Plan:** [
`docs/plan/plan-maestro.md`](../plan/plan-maestro.md)
> **Fecha de medición:** 2026-08-30 · **Comando:** `.\gradlew.bat build --no-daemon`
> **Resultado del build:** 🟢 **VERDE** — 33 pruebas, **0 fallos**, 0 errores, 0 omitidas
>
> Este documento contiene **cifras medidas, no estimadas**. Es la referencia contra la que se
> comparará el cierre de cada fase. **No contiene ningún valor de token ni secreto** (DP-02).

---

## 1. Estado del build

| Dato             | Valor                             |
|------------------|-----------------------------------|
| Resultado        | **BUILD SUCCESSFUL** en 39 s      |
| Pruebas totales  | **33**                            |
| Fallos / errores | **0 / 0**                         |
| Gradle           | 9.6.1                             |
| Tareas           | 31 (16 ejecutadas, 15 up-to-date) |

> **No hay rojo heredado.** La excepción del §8.4 del plan maestro no se consume: todas las fases
> siguientes arrancan y deben cerrar en verde.

---

## 2. Pruebas por módulo y clase

| Módulo                                         | Clase de prueba                |  Tests | Fallos |
|------------------------------------------------|--------------------------------|-------:|-------:|
| `applications/app-service`                     | `ArchitectureTest`             |      6 |      0 |
| `applications/app-service`                     | `McpSecurityConfigTest`        |  **1** |      0 |
| `applications/app-service`                     | `UseCasesConfigTest`           |  **1** |      0 |
| `domain/model`                                 | — **no existe `src/test`**     |  **0** |      — |
| `domain/usecase`                               | `GetTeamIterationsUseCaseTest` |      9 |      0 |
| `infrastructure/driven-adapters/rest-consumer` | `RestConsumerTest`             |      5 |      0 |
| `infrastructure/entry-points/mcp-server`       | `AzureDevOpsToolsTest`         |      7 |      0 |
| `infrastructure/entry-points/mcp-server`       | `McpAuditAspectTest`           |      2 |      0 |
| `infrastructure/entry-points/mcp-server`       | `HealthToolTest`               |      2 |      0 |
| **TOTAL**                                      |                                | **33** |  **0** |

### Lecturas relevantes

- **`domain/model` no tiene carpeta `src/test`**: **21 clases de dominio con cero pruebas**. No es
  que la cobertura sea baja: es que **el módulo no participa en la medición**. Se registra como
  deuda nueva **D-26**.
- `McpSecurityConfigTest` y `UseCasesConfigTest` tienen **una prueba cada uno**. Ambos son
  precisamente los que deben verificar D-01 a D-05. Sospecha a confirmar en los pasos 13-14 de la
  Fase 01.
- `AzureDevOpsToolsTest` tiene **7 pruebas para 267 líneas** que incluyen el único flujo compuesto
  del sistema.

---

## 3. Cobertura de líneas (JaCoCo)

| Módulo                                         | Cubiertas / Totales |                       % |
|------------------------------------------------|--------------------:|------------------------:|
| `domain/model`                                 |     **sin reporte** | **0 %** *(sin pruebas)* |
| `domain/usecase`                               |             15 / 22 |              **68,2 %** |
| `infrastructure/entry-points/mcp-server`       |            91 / 129 |              **70,5 %** |
| `infrastructure/driven-adapters/rest-consumer` |            70 / 137 |              **51,1 %** |
| `applications/app-service`                     |            18 / 106 |              **17,0 %** |

**Objetivo del plan (`spring-rules.md` §5):** `domain/model` + `domain/usecase` **≥ 90 %**.
**Distancia real:** `domain/usecase` está a 21,8 puntos; `domain/model` **no ha empezado**.

### Mutación (Pitest)

| Dato                                 | Valor             |
|--------------------------------------|-------------------|
| Cobertura de línea de clases mutadas | 18/103 (17 %)     |
| Mutaciones generadas / eliminadas    | 44 / **5 (11 %)** |
| Mutaciones **sin cobertura**         | **39**            |
| *Test strength*                      | 100 %             |

> El 100 % de *test strength* con un 11 % de mutaciones eliminadas es engañoso: significa que las
> pocas líneas que se prueban se prueban bien, y que **39 de 44 mutaciones ni siquiera se
ejecutan**.

---

## 4. ArchUnit — verificado, y con un hallazgo nuevo

### 4.1 La violación existe

Extraído del log de `ArchitectureTest`:

```
ADVERTENCIA: ARCHITECTURE_RULE_VIOLATED: This will cause a build error in future.
java.lang.AssertionError: Architecture Violation [Priority: MEDIUM] -
Rule 'Rule_2.2: Domain classes should not be named with technology suffixes' was violated (1 times)
```

✅ **D-17 confirmada**: `Rule_2.2` se viola **una vez**, tal y como anticipaba la deuda **D-39**
heredada del plan del BFF. La clase responsable es `WorkItemsBatchRequest` (termina en `Request`
dentro de `domain/model`).

### 4.2 El hallazgo nuevo: el informe para Sonar sale vacío

```
applications/app-service/build/issues.json                     → {"issues":[],"rules":[]}
build/issues.json                                              → {"issues":[],"rules":[]}
domain/model/build/issues.json                                 → {"issues":[],"rules":[]}
domain/usecase/build/issues.json                               → {"issues":[],"rules":[]}
infrastructure/driven-adapters/rest-consumer/build/issues.json → {"issues":[],"rules":[]}
infrastructure/entry-points/mcp-server/build/issues.json       → {"issues":[],"rules":[]}
```

**Los seis ficheros están vacíos, pero la violación existe.** El `ArchitectureTest` la detecta, la
registra en el log como *warning*… y **la pierde antes de escribirla**. `checkWithWarning` solo
añade
la incidencia si `files.get(location.getClassName())` resuelve el fichero; cuando no resuelve, la
violación se descarta **en silencio**.

Consecuencia: **SonarQube nunca ha visto ninguna violación de arquitectura de este repositorio**, y
no porque no las haya. Se registra como deuda nueva **D-25**.

> Esto agrava D-17: la regla no solo está en *warning* en vez de *error* (previsto para la Fase 08),
> sino que **tampoco llega al panel de calidad**. Hoy nada, en ninguna parte, hace visible la
> violación salvo leer el log del test.

---

## 5. Veredictos de las dudas abiertas de la Fase 01

### D-06 — Cortacircuitos: **CONFIRMADA**

`grep` de `testGet` / `testPost` en todo el repositorio (excluyendo `build/` y `build-cache/`):

```
application.yaml:53: testGet:
application.yaml:62: testPost:
HealthToolTest.java:30: void testGetServerInfo()      ← falso positivo, es un nombre de método
```

**Las dos únicas instancias configuradas no las usa nadie.** Los siete `@CircuitBreaker` reales
—`getWorkItem`, `createWorkItem`, `updateWorkItem`, `queryByWiql`, `getWorkItemsBatch`,
`getTeamFieldValues`, `getTeamIterations`— corren con la configuración **por defecto** de
Resilience4j. Los umbrales cuidadosamente escritos en `application.yaml` (líneas 53-71) **no aplican
a nada**.

### D-21 — Cobertura de los gateways de rutas: **CONFIRMADA**

`RestConsumerTest` tiene exactamente cinco pruebas:

```
validateGetWorkItem · validateCreateWorkItem · validateUpdateWorkItem
validateQueryByWiql · validateGetWorkItemsBatch
```

**Ninguna cubre `getTeamFieldValues` ni `getTeamIterations`**: las dos consultas que sostienen la
resolución de `AreaPath` e `IterationPath`, es decir, justo las que arreglaron el fallo del año.
Coinciden con el 48,9 % no cubierto del módulo. ✅ Deuda **D-36** del plan del BFF, confirmada.

### D-05 — Doble wiring: **DESCARTADA como bloqueante, degradada a 🟡**

`UseCasesConfigWiringTest` levanta el contexto con los siete gateways como dobles y comprueba el
número de definiciones de bean por tipo. Resultado: **cada caso de uso resuelve a exactamente un
bean** y el contexto arranca sin conflictos.

El `@ComponentScan` con `includeFilters` por expresión regular **no aporta ninguna definición
observable**: o no registra nada, o lo que registra queda íntegramente sustituido por el `@Bean`
homónimo. En cualquiera de los dos casos **su efecto neto es cero**.

> D-05 deja de ser un riesgo de arranque y pasa a ser **código muerto que induce a error**: quien
> lea
> `UseCasesConfig` creerá que hay dos mecanismos compitiendo. Se retira en la **Fase 08**.

### Hallazgo colateral: **`UseCasesConfigTest` miente** (deuda nueva **D-27**)

Al leerlo para el paso 13 se encontró que da verde por **dos** motivos independientes, y ninguno
tiene que ver con `UseCasesConfig`:

```java
// 1. Registra su PROPIO bean, que ya satisface la única aserción por sí solo
@Bean public MyUseCase myUseCase() { return new MyUseCase(); }   // nombre → "myUseCase"
   ...
if (beanName.endsWith("UseCase")) { useCaseBeanFound = true; }   // ← lo encuentra siempre

// 2. Y si el contexto NO arranca, aprueba igualmente
} catch (UnsatisfiedDependencyException e) {
    assertTrue(true, "Unsatisfied dependencies are expected...");
}
```

Como no registra ningún gateway, el contexto **no puede** arrancar, así que el camino que toma es
el `catch`: **la aserción de la línea 25 probablemente no se ha ejecutado nunca**. Es el mismo
antipatrón que el plan del BFF registró como su D-17. Se deja intacto (la Fase 01 no toca nada) y su
retirada queda para la **Fase 08**.

### D-17 — `Rule_2.2`: **CONFIRMADA** (§4.1), con el agravante de **D-25** (§4.2)

---

## 6. Deudas nuevas detectadas en la medición

| ID       | Deuda                                                                                                                                                                                                              | Severidad |
|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------|
| **D-25** | **El informe de ArchUnit para Sonar sale vacío pese a haber una violación real.** `checkWithWarning` descarta en silencio toda incidencia cuyo fichero no resuelva. SonarQube **nunca** ha visto estas violaciones | 🟠 Alta   |
| **D-26** | **`domain/model` no tiene `src/test`**: 21 clases de dominio con **cero** pruebas. El módulo ni siquiera aparece en los informes de cobertura                                                                      | 🟠 Alta   |

---

## 7. Tabla de referencia para el cierre de fases

| Métrica                                       | **Baseline 2026-08-30** |    **Cierre Fase 01** |     Objetivo |
|-----------------------------------------------|------------------------:|----------------------:|-------------:|
| Pruebas totales                               |                  **33** |            **48** ▲15 |        ≥ 120 |
| Pruebas fallando                              |                   **0** |              **0** ✅ |            0 |
| Cobertura `domain/model`                      |   **0 %** *(sin tests)* | **0 %** *(sin tests)* |       ≥ 90 % |
| Cobertura `domain/usecase`                    |              **68,2 %** |          **68,2 %** = |       ≥ 90 % |
| Cobertura `rest-consumer`                     |              **51,1 %** |      **71,5 %** ▲20,4 |       ≥ 80 % |
| Cobertura `mcp-server`                        |              **70,5 %** |       **71,3 %** ▲0,8 |       ≥ 80 % |
| Cobertura `app-service`                       |              **17,0 %** |       **23,6 %** ▲6,6 |       ≥ 60 % |
| Violaciones ArchUnit reales                   |        **1** (Rule_2.2) |               **1** = |            0 |
| Violaciones ArchUnit **exportadas a Sonar**   |                   **0** |                 **0** | **= reales** |
| Cortacircuitos con configuración efectiva     |               **0 / 7** |             **0 / 7** |        7 / 7 |
| Módulos sin carpeta de pruebas                |                   **1** |                 **1** |            0 |
| Ficheros productivos (`src/main`) modificados |                     n/a |              **0** ✅ |            — |

> **Sobre las cifras de la Fase 01.** `rest-consumer` sube 20,4 puntos porque las seis pruebas de
> T-03 cubren los dos gateways que estaban a cero y sus dos ramas de error. `mcp-server` apenas se
> mueve (+0,8) y es **lo esperado**: las ocho pruebas de `WiqlCharacterizationTest` recorren
> exactamente el mismo código que ya recorría `AzureDevOpsToolsTest` — lo que aportan no es
> cobertura, es **precisión**: fijan la sentencia completa donde antes solo se comprobaban
> fragmentos. `domain/usecase` no se mueve porque esta fase no lo tocaba, y `domain/model` sigue a
> cero porque **el módulo no tiene carpeta de pruebas** (D-26), que es material de la Fase 07.

### 7.1 Sentencia WIQL congelada (T-02)

Plantilla fijada carácter a carácter en
`mcp-server/src/test/java/co/com/bancolombia/mcp/tools/WiqlCharacterizationTest.java`:

```
SELECT [System.Id] FROM workitems WHERE [System.TeamProject] = @project AND [System.IterationPath] = '%s' AND [System.AreaPath] = '%s' AND [System.WorkItemType] IN (%s) ORDER BY [System.Id]
```

Ejemplo real congelado (camino feliz, tipos por defecto):

```
SELECT [System.Id] FROM workitems WHERE [System.TeamProject] = @project AND [System.IterationPath] = 'Vicepresidencia Servicios de Tecnología\2025\Sprint 247' AND [System.AreaPath] = 'Vicepresidencia Servicios de Tecnología\EQU1096 - EXODIA' AND [System.WorkItemType] IN ('Historia de Usuario','Habilitador') ORDER BY [System.Id]
```

**Ocho ramas congeladas:** camino feliz · tipos en blanco · traducción `User Story` · tipos ya
entrecomillados · célula como ruta con barras dobles · repliegue del `AreaPath` · repliegue del
`IterationPath` **con** año del calendario · repliegue **sin** año.

---

## 8. Estado de la Fase 01

- [x] T-01 — Baseline medido
- [x] T-02 — `WiqlCharacterizationTest`, 8 casos, sentencia congelada
- [x] T-03 — 6 pruebas nuevas en `RestConsumerTest`; **D-21 saldada**
- [x] T-04 — `UseCasesConfigWiringTest`; **veredicto de D-05** (§5)
- [x] T-05 — Veredicto de D-06
- [x] T-06 — [`CONTRATO-MCP.md`](CONTRATO-MCP.md)


