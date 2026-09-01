import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { NotificationService } from '../services/notification.service';

/** Mensaje usado cuando la respuesta de error no trae ningún detalle legible. */
export const DEFAULT_HTTP_ERROR_MESSAGE = 'Error desconocido';

/**
 * Extrae un mensaje legible de un `HttpErrorResponse` respetando **exactamente** la precedencia
 * que ya usa el código productivo (`error.error || error.message || <por defecto>`, ver
 * `devops-agent-state.service.ts:167` y `:271`).
 *
 * El cuerpo de error puede llegar como texto plano, como objeto JSON o como `ProgressEvent`, por
 * eso se trata como `unknown` con comprobaciones explícitas en lugar de `any` (§5).
 */
export function extractHttpErrorMessage(error: HttpErrorResponse): string {
  const body: unknown = error.error;

  if (typeof body === 'string' && body.length > 0) {
    return body;
  }

  if (body !== null && typeof body === 'object') {
    const message = (body as { message?: unknown }).message;
    if (typeof message === 'string' && message.length > 0) {
      return message;
    }
  }

  if (typeof error.message === 'string' && error.message.length > 0) {
    return error.message;
  }

  return DEFAULT_HTTP_ERROR_MESSAGE;
}

/**
 * Interceptor funcional que normaliza los errores HTTP y los publica en el canal único de
 * notificaciones.
 *
 * ⚠️ **Vuelve a lanzar el error con `throwError`.** Es innegociable: los `subscribe({ error })`
 * existentes son los que producen el mensaje «❌ Error de red…» del chat y el texto de
 * `dashboardError`, ambos congelados por las pruebas de caracterización de la Fase 01. Si el
 * interceptor se tragase el error, esas pruebas fallarían y el usuario dejaría de ver el fallo.
 */
export const httpErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const notifications = inject(NotificationService);

  return next(req).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse) {
        notifications.error(extractHttpErrorMessage(error));
      }
      return throwError(() => error);
    }),
  );
};
