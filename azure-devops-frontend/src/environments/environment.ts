/**
 * Configuración de entorno para compilaciones de PRODUCCIÓN.
 *
 * `apiBaseUrl` queda deliberadamente vacío: hoy todas las llamadas usan rutas relativas y el BFF
 * se sirve tras el mismo origen. Rellenarlo con una URL inventada cambiaría el comportamiento y
 * violaría la Política de No-Asunción (`rules/angular-rules.md` §7).
 */
export const environment = {
  production: true,
  apiBaseUrl: '',
} as const;
