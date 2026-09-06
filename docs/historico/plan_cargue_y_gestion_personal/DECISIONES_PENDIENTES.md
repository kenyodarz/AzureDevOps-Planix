# DECISIONES PENDIENTES — Plan Maestro: Cargue y Gestión Organizacional

> **Regla que aplica:** `rules/angular-rules.md` §7 — Política de No-Asunción.
> **Actualizado:** 2026-09-03
>
> Si una decisión que bloquea la fase en curso sigue 🔴 **ABIERTA**, el ejecutor debe
> **detenerse y preguntar al usuario**. Está prohibido resolverla por criterio propio.

---

## Leyenda

| Símbolo | Significado |
|---|---|
| 🔴 **ABIERTA** | Requiere respuesta del usuario. Bloquea la fase indicada. |
| 🟢 **RESUELTA** | Respondida. Se registra la decisión y la fecha. |
| ⚪ **INFORMATIVA** | No bloquea, pero conviene confirmarla antes del cierre del plan. |

---

## DP-CG-01 — Estrategia de Fallback en Servicios HttpClient & Mocking

- **Estado:** 🟢 **RESUELTA (2026-09-03)** · **Bloqueaba:** Fase 01
- **Decisión adoptada:** Implementar los servicios utilizando `HttpClient` apuntando a las rutas de Spring Boot (`/api/...`), y dotar a cada servicio de un almacén reactivo en memoria con datos mock enriquecidos vía fallback (`catchError` o fallback signal) para garantizar desarrollo autónomo en frontend, pruebas unitarias y modo demo sin caídas.
- **Implementación:** `src/app/core/services/` y `src/app/core/models/`.

---

## DP-CG-02 — Mecanismo de Polling para Monitoreo de Spring Batch

- **Estado:** 🟢 **RESUELTA (2026-09-03)** · **Bloqueaba:** Fase 02
- **Decisión adoptada:** Utilizar un polling reactivo a `GET /api/cargue-personal/{id}/estado` cada 1.5 segundos con operadores RxJS (`timer`, `switchMap`, `takeWhile`) sincronizado con un `WritableSignal<EstadoCargue>` para renderizar la barra de progreso y métricas en tiempo real.
- **Implementación:** `CarguePersonalService` y `CarguePersonalComponent`.


