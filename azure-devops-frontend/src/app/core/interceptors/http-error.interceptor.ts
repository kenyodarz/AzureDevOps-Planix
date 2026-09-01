import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { NotificationService } from '../services/notification.service';

/** Mensaje usado cuando la respuesta de error no trae ningún detalle legible. */
export const DEFAULT_HTTP_ERROR_MESSAGE = 'Error desconocido';

function extractStringProperty(obj: unknown, prop: string): string | null {
  if (obj !== null && typeof obj === 'object') {
    const val = (obj as Record<string, unknown>)[prop];
    if (typeof val === 'string' && val.length > 0) {
      return val;
    }
  }
  return null;
}

function extractNestedErrorMessage(body: unknown): string | null {
  if (body === null || typeof body !== 'object') {
    return null;
  }
  const fromMessage = extractStringProperty(body, 'message');
  if (fromMessage) {
    return fromMessage;
  }
  const fromError = extractStringProperty(body, 'error');
  if (fromError) {
    return fromError;
  }
  const nestedError = (body as { error?: unknown }).error;
  const fromNested = extractStringProperty(nestedError, 'message');
  if (fromNested) {
    return fromNested;
  }
  return extractStringProperty(body, 'detail');
}

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

  const nested = extractNestedErrorMessage(body);
  if (nested) {
    return nested;
  }

  if (typeof error.message === 'string' && error.message.length > 0) {
    return error.message;
  }

  return DEFAULT_HTTP_ERROR_MESSAGE;
}

/**
 * Extrae un mensaje legible de cualquier tipo de error para evitar [object Object] (D-28).
 */
export function extractErrorMessage(
  error: unknown,
  defaultMsg: string = DEFAULT_HTTP_ERROR_MESSAGE,
): string {
  if (error instanceof HttpErrorResponse) {
    return extractHttpErrorMessage(error);
  }
  if (error instanceof Error && error.message.length > 0) {
    return error.message;
  }
  if (typeof error === 'string' && error.length > 0) {
    return error;
  }

  const nested = extractNestedErrorMessage(error);
  if (nested) {
    return nested;
  }

  return defaultMsg;
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
