import { computed, Injectable, signal } from '@angular/core';

/** Severidad de una notificación mostrada al usuario. */
export type NotificationSeverity = 'error' | 'warn' | 'info' | 'success';

/** Notificación emitida por cualquier capa de la aplicación. */
export interface AppNotification {
  readonly id: string;
  readonly severity: NotificationSeverity;
  readonly message: string;
  readonly timestamp: number;
}

/**
 * Canal único de notificaciones de la aplicación.
 *
 * En la Fase 02 el servicio **solo se registra y se alimenta desde el interceptor HTTP**. La
 * sustitución de los 10 `console.error` que hoy fallan en silencio (D-20) es responsabilidad de la
 * Fase 08: alterar ahora ese flujo cambiaría el comportamiento que congelan las 188 pruebas de
 * caracterización de la Fase 01.
 */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly notifications$ = signal<readonly AppNotification[]>([]);

  /** Historial de notificaciones emitidas, en orden de llegada. */
  public readonly notifications = this.notifications$.asReadonly();

  /** Última notificación emitida, o `null` si aún no hubo ninguna. */
  public readonly last = computed<AppNotification | null>(() => {
    const all = this.notifications$();
    return all.length > 0 ? all[all.length - 1] : null;
  });

  /** Publica una notificación de error. */
  public error(message: string): void {
    this.push('error', message);
  }

  /** Publica una notificación de advertencia. */
  public warn(message: string): void {
    this.push('warn', message);
  }

  /** Publica una notificación informativa. */
  public info(message: string): void {
    this.push('info', message);
  }

  /** Publica una notificación de éxito. */
  public success(message: string): void {
    this.push('success', message);
  }

  /** Vacía el historial de notificaciones. */
  public clear(): void {
    this.notifications$.set([]);
  }

  private push(severity: NotificationSeverity, message: string): void {
    const notification: AppNotification = {
      id: crypto.randomUUID(),
      severity,
      message,
      timestamp: Date.now(),
    };
    this.notifications$.update((current) => [...current, notification]);
  }
}
