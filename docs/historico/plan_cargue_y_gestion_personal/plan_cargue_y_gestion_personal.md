# Plan Maestro: Módulo Frontend de Cargue y Gestión Organizacional

Este documento define la arquitectura de vistas, flujos de usuario, integración de servicios y lineamientos de diseño para la implementación del módulo frontend de **Cargue Masivo de Personal y Gestión de Estructura Organizacional** en la aplicación Janus (`janus-web`).

---

## 1. Lineamientos de Estilo y Librería de Componentes

1. **Uso Obligatorio de la Librería de Componentes Oficial**:
   - Todas las vistas, controles de formulario, tablas, diálogos modales, alertas y botones **deben implementarse utilizando exclusivamente la librería de componentes del proyecto** (Design System corporativo Caribe BDS / Web Components `<bc-*>`).
   - Queda estrictamente prohibido instalar librerías de UI externas de terceros (PrimeNG, Angular Material, Bootstrap, etc.).
2. **Estilos y Layout (CSS Nativo)**:
   - El layout de las páginas debe basarse en estándares nativos de la plataforma web: **CSS Grid** para estructuras bidimensionales y **Flexbox** para componentes unidimensionales.
   - Utilizar las Custom Properties (variables CSS y tokens de diseño) de colores, espaciados, tipografías y sombras provistas por el sistema de diseño del proyecto (`styles.scss`).
   - Responsive design fluido adaptado a resoluciones desktop corporativas y tablets.
3. **Arquitectura Angular 22**:
   - Componentes `standalone: true` con `ChangeDetectionStrategy.OnPush`.
   - Reactividad moderna basada en **Signals** (`signal`, `computed`, `effect`, `input`, `output`).
   - Nuevo flujo de control (`@if`, `@for`, `@switch`).
   - Integración de Web Components mediante directivas puente `ControlValueAccessor` ya existentes en `src/app/core/directives/forms/`.

---

## 2. Mapa de Navegación y Vistas del Módulo

El módulo se compone de **4 vistas principales**, accesibles desde el menú lateral (`SidebarComponent`) y configuradas con carga perezosa (*Lazy Loading*) en `app.routes.ts`:

```
[Gestión de Personal y Cargues]
 ├── 1. Centro de Cargue Masivo (Upload y Monitoreo Batch)      -> /cargue-personal
 ├── 2. Bandeja de Alertas y Novedades Organizacionales        -> /alertas-organizacionales
 ├── 3. Mantenimiento de Catálogos Organizacionales (Selects)   -> /catalogos-organizacionales
 └── 4. Directorio de Posiciones y Colaboradores               -> /posiciones-personal
```

---

## 3. Especificación Detallada de Vistas

### Vista 1: Centro de Cargue Masivo (`/cargue-personal`)
- **Cabecera de Página**: Título, subtítulo institucional y botón secundario *"Descargar Plantilla Excel"* (descarga la plantilla vacía con las 23 columnas preconfiguradas).
- **Formulario de Selección y Carga**:
  - Selector de Periodo: Mes (desplegable con meses del año) y Año (campo numérico).
  - Zona de Carga (Drag & Drop): Área interactiva para soltar o seleccionar archivo `.xlsx` (máx 50 MB) con validación en cliente y vista previa.
  - Botón de Acción Principal: *"Iniciar Cargue y Procesamiento"* (deshabilitado hasta seleccionar archivo).
- **Panel de Estado en Vivo (Reactivo & Polling)**:
  - Transición de estados: `CARGADO` -> `EN_PROCESO` (con animación de carga / barra indeterminada) -> `PROCESADO` (tarjeta de éxito) / `FALLIDO` (notificación de error y excepción técnica). Polling cada 1.5s a `GET /api/cargue-personal/{id}/estado`.
- **Métricas Consolidadas del Lote**: Tarjetas de resumen con Total Filas Leídas, Total Procesadas Exitosamente y Total Descartadas.
- **Historial de Lotes Anteriores**: Tabla con fecha, periodo, nombre de archivo, badge de estado y métricas.

### Vista 2: Bandeja de Alertas y Novedades (`/alertas-organizacionales`)
- **Pestañas y Filtros**:
  - Pestañas *"Pendientes"* (con contador numérico) y *"Resueltas"*.
  - Filtros por Tipo de Alerta: `DATOS_INCOMPLETOS_CELULA`, `DATOS_INCOMPLETOS_POSICION`, `NOVEDAD_RETIRO_O_TRASLADO`.
- **Lista / Tabla de Alertas**:
  - Severidad (`Informativa`, `Advertencia`, `Crítica`), Entidad (`Célula`, `Posición`, `Colaborador`), Código de referencia, Mensaje, Fecha.
  - Acciones: *"Completar Datos"* y *"Marcar como Resuelta"*.
- **Modal de Completitud de Datos de Célula**:
  - Despliega código/nombre de célula con aviso visual de datos provisionales (`es_dummy = true`).
  - Buscador para asignar **Product Owner** y **Líder Técnico** definitivos. Al guardar actualiza `es_dummy = false` y auto-resuelve la alerta.

### Vista 3: Mantenimiento de Catálogos Organizacionales (`/catalogos-organizacionales`)
- **Catálogos Gestionados (9 catálogos)**:
  1. `roles_talento`
  2. `niveles_talento`
  3. `proveedores`
  4. `entornos_funcionales`
  5. `evcs_funcionales`
  6. `unidades_organizativas`
  7. `gestiones_gasto`
  8. `segundos_niveles`
  9. `tipos_recurso`
- **Selector y Barra de Herramientas**: Pestañas/menú para cambiar de catálogo, buscador de texto y botón *"+ Nuevo Ítem"*.
- **Tabla y Control de Errores de Negocio**:
  - Listado con nombre, código y estado de asignación.
  - Eliminación (`DELETE /api/catalogos/{tipo}/{id}`): si el backend retorna `409 Conflict`, mostrar modal/alerta indicando que el registro está en uso por colaboradores activos.
- **Modal de Nuevo Ítem**: Formulario validado con campo de texto y acciones guardar/cancelar.

### Vista 4: Directorio de Posiciones y Colaboradores (`/posiciones-personal`)
- **Filtros Avanzados**: Búsqueda por cédula/nombre/código, filtro por célula, modalidad (`Presencial`, `Teletrabajo`, `Flexiwork`), proveedor/tipo de recurso (`Interno`, `Externo`) y switch *"Solo entidades con datos provisionales (Dummy)"*.
- **Tabla Maestra**: Columnas con cédula, colaborador, posición, célula, modalidad, rol/nivel, proveedor y badge de advertencia para registros provisionales (`[Pendiente Asignar Líderes]`).
- **Drawer / Panel de Detalle**: Al seleccionar una posición se abre panel lateral con titular actual, histórico de ocupantes y esquema de trabajo.

---

## 4. Contratos de API REST Backend

| Funcionalidad | Método | URL | Payload / Params | Respuesta Exitosa |
| :--- | :---: | :--- | :--- | :--- |
| **Carga de Archivo Excel** | `POST` | `/api/cargue-personal/upload?mes={mes}&anio={anio}` | `multipart/form-data` con campo `archivo` | `201 Created` (`CargueControl`) |
| **Consultar Estado de Lote** | `GET` | `/api/cargue-personal/{cargueId}/estado` | - | `200 OK` (`EstadoCargue`) |
| **Reprocesar Lote** | `POST` | `/api/cargue-personal/{cargueId}/procesar` | JSON opcional con filtros | `200 OK` (`ResultadoCargue`) |
| **Historial de Cargues** | `GET` | `/api/cargue-personal/historial` | - | `200 OK` (`CargueControl[]`) |
| **Listar Catálogo** | `GET` | `/api/catalogos/{tipo}` | `tipo` en path | `200 OK` (`ItemCatalogo[]`) |
| **Crear Ítem en Catálogo** | `POST` | `/api/catalogos/{tipo}` | `{"nombre": "NUEVO_VALOR"}` | `201 Created` (`ItemCatalogo`) |
| **Eliminar Ítem Huérfano** | `DELETE` | `/api/catalogos/{tipo}/{id}` | - | `204 No Content` (`409 Conflict` si en uso) |
| **Listar Alertas** | `GET` | `/api/alertas` | - | `200 OK` (`AlertaOrganizacional[]`) |
| **Resolver Alerta** | `PATCH` | `/api/alertas/{id}/resolver` | - | `200 OK` |
| **Completar Célula** | `POST` | `/api/alertas/completar-celula` | `{"celulaId", "poId", "tlId"}` | `200 OK` |
| **Listar Directorio Posiciones** | `GET` | `/api/posiciones-personal` | query params de filtro | `200 OK` (`PosicionDirectorio[]`) |

---

## 5. Estructura de Fases de Implementación

| Fase | Título | Alcance Principal | Entregable Clave |
|---|---|---|---|
| **01** | Modelos, Contratos & Servicios Core | Modelos TypeScript, Servicios API reactivos con Signals, Stubs y Mocks de prueba, Pruebas unitarias de servicios, Integración de rutas y menú | `src/app/core/models/`, `src/app/core/services/`, rutas y 100% tests verdes |
| **02** | Centro de Cargue Masivo (`/cargue-personal`) | Vista de upload drag & drop, polling reactivo a Spring Batch, panel de métricas, historial de lotes | Feature `cargue-personal` con tests unitarios |
| **03** | Bandeja de Alertas y Novedades (`/alertas-organizacionales`) | Pestañas pendientes/resueltas, filtros de severidad/tipo, resolución rápida, modal de completitud de células dummy | Feature `alertas-organizacionales` con tests unitarios |
| **04** | Mantenimiento de Catálogos (`/catalogos-organizacionales`) | Gestión de los 9 catálogos, tabla, creación rápida, eliminación con manejo de conflicto 409 | Feature `catalogos-organizacionales` con tests unitarios |
| **05** | Directorio de Posiciones y Colaboradores (`/posiciones-personal`) | Filtros multifaceta, tabla con badges dummy, drawer lateral de detalle e histórico | Feature `posiciones-personal` con tests unitarios |
| **06** | Calidad, Accesibilidad (WCAG 2.1 AA) & CI/CD | Auditoría A11Y, cobertura > 90% en Vitest, compilación limpia de producción | Suite de pruebas integradas y build validado |

