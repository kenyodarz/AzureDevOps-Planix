/**
 * Configuración de entorno para DESARROLLO.
 *
 * `apiBaseUrl` vacío: en desarrollo las rutas relativas las resuelve `proxy.conf.json`.
 */
export const environment = {
  production: false,
  apiBaseUrl: '',
} as const;
