# Guía del Agente para Frontend (AGENTS.md)

Este repositorio contiene el **Frontend** de la aplicación, implementado exclusivamente en **Angular (v21+)** y estilizado con la combinación de **PrimeNG** y **Tailwind CSS**. 

Cualquier agente de IA, Copilot o desarrollador humano que trabaje en esta interfaz de usuario **debe** cumplir de forma obligatoria y sin excepciones con las reglas de estilo, estilo arquitectónico y calidad definidas en este documento y en su referencia correspondiente (`rules/angular-rules.md`).

---

## 1. Estructura del Proyecto Frontend y Rol

El código de la aplicación de usuario interactiva se compone de:
- **Estructura por Feature**: El código reside en `src/app/` y se organiza en módulos de dominio/característica dentro de `features/` (ej. `devops-agent`).
- **Arquitectura de Responsabilidades Desacopladas**:
  - `pages/`: Smart Components que actúan como puntos de entrada de ruta y controlan el flujo de datos.
  - `components/`: Dumb Components (presentacionales) reutilizables, que solo reciben inputs y emiten outputs de manera pura.
  - `services/`: Servicios de API (`{feature}-api.service.ts`) para transferir datos con el backend y servicios de estado `{feature}-state.service.ts` para managing de estado reactivo.
  - `models/`: Interfaces estrictas de TypeScript (`{feature}.model.ts`) para mantener un tipado seguro en toda la UI.

---

## 2. Componentes Standalone y Control Flow Moderno

- **Standalone como Estándar**: Todos los nuevos componentes, pipes y directivas de Angular deben declararse estrictamente como Standalone (`standalone: true`).
- **Control Flow Moderno de Angular**: No se deben utilizar las directivas antiguas `*ngIf`, `*ngFor` o `*ngSwitch`. Es obligatorio el uso exclusivo de la sintaxis moderna con bloques `@if`, `@for` y `@switch` para optimizar el renderizado y legibilidad del código.

```typescript
@Component({
  selector: 'app-dynamic-list',
  standalone: true,
  imports: [CommonModule, ButtonModule],
  template: `
    @if (loading()) {
      <div class="spinner">Cargando...</div>
    } @else {
      <ul>
        @for (item of items(); track item.id) {
          <li>{{ item.name }}</li>
        } @empty {
          <p>No hay elementos disponibles.</p>
        }
      </ul>
    }
  `
})
```

---

## 3. Manejo de Estado Reactivo y Prevención de Fugas de Memoria

- **Signals para Estado de IU Local**: Utilizar de manera preferente `signal()`, `computed()`, y `effect()` para datos reactivos de renderizado en el component (visibilidad de modales, filtros temporales, datos simples no propagados).
- **RxJS en Servicios de Estado**: En los servicios `{feature}-state.service.ts`, los flujos de estado correspondientes a features se gestionan internamente con `BehaviorSubject` privados y se exponen públicamente como observables públicos empleando el método `.asObservable()`.
- **Prevención de fugas de memoria (Memory Leaks)**:
  - Usar preferentemente el `AsyncPipe` (`| async`) en las plantillas HTML para suscripciones automáticas.
  - Al suscribirse manualmente en componentes TypeScript, usar obligatoriamente **`takeUntilDestroyed()`** del inyector o inyectar `DestroyRef` para desvincular suscripciones de flujos infinitos al destruirse el componente. Queda estrictamente prohibido mantener suscripciones manuales abiertas.

---

## 4. UI con PrimeNG, Tailwind y Accesibilidad Estricta (A11Y)

- **Layout y Maquetación**: Utilizar clases semánticas de Tailwind CSS para posicionamiento y espaciado estructurado (`flex`, `grid`, `gap`, `p-`, `m-`). Evitar el uso de directivas `style` en línea no dinámicas.
- **Accesibilidad Obligatoria**: Todo botón interactivo que contenga únicamente un icono (es decir, sin texto literal / label) **debe declarar explícitamente un atributo `aria-label` descriptivo** para lectores de pantalla.

```html
<!-- CORRECTO -->
<p-button icon="pi pi-trash" aria-label="Eliminar elemento del listado" (onClick)="deleteItem()"></p-button>

<!-- INCORRECTO -->
<p-button icon="pi pi-trash" (onClick)="deleteItem()"></p-button>
```

---

## 5. TypeScript Estricto y Calidad

- **Sin uso de `any`**: El tipo `any` está prohibido sin excepciones. Si un payload no está tipado, se usa `unknown` junto con Guards de tipo o se define adecuadamente la interfaz de contrato.
- **Safe Navigation**: Utilizar de manera proactiva los operadores seguros de navegación de TypeScript (`?.`) y nullish coalescing (`??`) para evitar de forma preventiva errores en tiempo de ejecución.
- **Tipado Explícito**: El tipado explícito de retornos en firmas públicas de métodos de componentes y servicios es requerido para mantener un código comprensible.

---

## 6. Pruebas Unitarias con Vitest

- **GIVEN / WHEN / THEN**: Los archivos de spec (`*.spec.ts`) deben usar bloques descriptivos de Vitest estructurando los casos de prueba lógicamente bajo la convención GIVEN/WHEN/THEN.
- **Mocks Limpios**: Usar `provideHttpClientTesting()` para simular peticiones e inyectores Mock de servicios cuando sea pertinente.

---

## 7. Política de No-Asunción (Transversal)

**Nunca asumas información visual o de copy que no esté explícita**. Si en el diseño, historia o especificación existe alguna ambigüedad en:
- Textos de labels, botones, modales o placeholders.
- Flujos de navegación interactivos de vuelta.
- Iconos de PrimeIcons a implementar.
- Límites de caracteres y validaciones de formulario.

El agente **se detendrá de inmediato y preguntará** al usuario antes de proceder.

---

## 8. Checklist de Entrega para Angular

Al finalizar o revisar un desarrollo en Angular, valida que:
- [ ] Todos los componentes creados son standalone (`standalone: true`).
- [ ] No se utilizan directivas antiguas como `*ngIf` o `*ngFor` (usar `@if`, `@for`).
- [ ] El estado del componente se maneja reactivamente con Signals u Observables seguros.
- [ ] No existen fugas de memoria (memory leaks); todas las suscripciones manuales se cierran con `takeUntilDestroyed()` o `AsyncPipe`.
- [ ] Todos los botones que contienen únicamente iconos tienen configurado su `aria-label`.
- [ ] No se utiliza `any` en ninguna declaración de variable o parámetro.
- [ ] Todas las llamadas a backend HTTP están centralizadas en servicios de API, nunca directamente en el componente.
- [ ] Se implementaron pruebas unitarias con Vitest que cubren happy paths y flujos lógicos aplicando GIVEN/WHEN/THEN.
- [ ] No se inventaron copys de interfaz o rutas sin la debida confirmación.

