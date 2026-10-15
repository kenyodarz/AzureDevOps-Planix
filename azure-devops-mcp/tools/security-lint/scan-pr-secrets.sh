#!/usr/bin/env bash
# ==============================================================================
# Security Lint: Escaneo de Archivos Prohibidos y Secretos en el Delta del PR
#
# Uso:
#   bash tools/security-lint/scan-pr-secrets.sh [BASE_REF]
#
# Ejemplos:
#   bash tools/security-lint/scan-pr-secrets.sh origin/trunk...HEAD
#   bash tools/security-lint/scan-pr-secrets.sh HEAD~1
# ==============================================================================

set -uo pipefail

# 1. Determinar el rango de comparación
TARGET_REF="${1:-}"

if [ -z "$TARGET_REF" ]; then
  if [ -n "${GITHUB_BASE_REF:-}" ]; then
    TARGET_REF="origin/${GITHUB_BASE_REF}...HEAD"
  elif git rev-parse --verify origin/trunk >/dev/null 2>&1; then
    TARGET_REF="origin/trunk...HEAD"
  elif git rev-parse --verify origin/main >/dev/null 2>&1; then
    TARGET_REF="origin/main...HEAD"
  elif git rev-parse --verify HEAD~1 >/dev/null 2>&1; then
    TARGET_REF="HEAD~1...HEAD"
  else
    TARGET_REF="HEAD"
  fi
fi

echo "=================================================================="
echo " 🛡️  Iniciando Escaneo de Seguridad y Credenciales en PR"
echo " Rango objetivo: $TARGET_REF"
echo "=================================================================="

ERRORS=0

# ==============================================================================
# 2. AUDITORÍA: Archivos Prohibidos
# ==============================================================================
echo "1️⃣  Verificando que no se incluyan archivos sensibles o prohibidos..."

PROHIBITED_PATTERN='(^|/)(\.env|\.env\..*|.*\.p12|.*\.jks|.*\.keystore|.*\.pem|.*\.key|.*\.log|.*\.dump)$'

# Obtener lista de archivos añadidos o modificados en el rango
CHANGED_FILES=$(git --no-pager diff --name-only --diff-filter=ACMR "$TARGET_REF" 2>/dev/null || true)

PROHIBITED_FOUND=$(echo "$CHANGED_FILES" | grep -iE "$PROHIBITED_PATTERN" || true)

if [ -n "$PROHIBITED_FOUND" ]; then
  echo ""
  echo "❌ ERROR: Se detectaron archivos prohibidos en los cambios del PR:"
  echo "------------------------------------------------------------------"
  echo "$PROHIBITED_FOUND" | while IFS= read -r f; do echo "  - $f"; done
  echo "------------------------------------------------------------------"
  echo "Por motivos de seguridad institucional en Bancolombia, estos archivos"
  echo "nunca deben ingresar a los repositorios de GitHub."
  echo "Acción requerida: Remuévelos de tu rama y agrégalos a .gitignore."
  echo ""
  ERRORS=$((ERRORS + 1))
else
  echo "  ✅ Sin archivos prohibidos detectados."
fi

# ==============================================================================
# 3. AUDITORÍA: Secretos y Credenciales Quemadas en el Diff
# ==============================================================================
echo "2️⃣  Escaneando código añadido en busca de credenciales en texto plano..."

# Extraer el diff del rango excluyendo documentación y bloqueos
DIFF_OUTPUT=$(git --no-pager diff -U0 "$TARGET_REF" -- . \
  ':(exclude)*.md' \
  ':(exclude)*.txt' \
  ':(exclude)package-lock.json' \
  ':(exclude)*.lock' \
  ':(exclude)*.svg' \
  ':(exclude)*.html' \
  ':(exclude)*.json' 2>/dev/null || true)

SECRETS_FOUND=0

while IFS= read -r line; do
  # Limpiar retorno de carro (\r) de Windows
  line=$(echo "$line" | tr -d '\r')

  # Solo analizar líneas añadidas
  [[ "$line" =~ ^\+ ]] || continue
  [[ "$line" =~ ^\+\+\+ ]] && continue

  # Ignorar comentarios (#, //, /*, *)
  if echo "$line" | grep -qE "^\+\s*(#|//|/\*|\*)"; then
    continue
  fi

  # Ignorar si usa sintaxis segura de variable de entorno ${...}
  if echo "$line" | grep -q '\${'; then
    continue
  fi

  # Detectar credenciales asignadas o tokens conocidos
  if echo "$line" | grep -iE "(password|client_secret|client-secret|secret_key|api_key|apikey)\s*[:=]" >/dev/null 2>&1 || \
     echo "$line" | grep -E "(sk-[a-zA-Z0-9_\-]{15,}|AKIA[0-9A-Z]{16}|bearer\s+[a-zA-Z0-9_\-\.]{25,})" >/dev/null 2>&1; then
    echo "  ⚠️  LÍNEA SOSPECHOSA: $line"
    SECRETS_FOUND=$((SECRETS_FOUND + 1))
  fi
done <<< "$DIFF_OUTPUT"

if [ $SECRETS_FOUND -gt 0 ]; then
  echo ""
  echo "❌ ERROR: Se detectaron $SECRETS_FOUND posibles credenciales quemadas en el código del PR."
  echo "Acción requerida:"
  echo "  Reemplaza los valores en texto plano por variables de entorno."
  echo "  Ejemplo seguro: client-secret: \${ENTRA_ID_CLIENT_SECRET:demo-secret}"
  echo ""
  ERRORS=$((ERRORS + 1))
else
  echo "  ✅ Sin credenciales quemadas detectadas en el diff."
fi

# ==============================================================================
# 4. Resultado Final
# ==============================================================================
echo "=================================================================="
if [ $ERRORS -gt 0 ]; then
  echo " ❌ AUDITORÍA FALLIDA: Se encontraron $ERRORS problemas de seguridad."
  echo " Corrige los hallazgos en tu rama local antes de integrar a trunk."
  echo "=================================================================="
  exit 1
else
  echo " ✅ AUDITORÍA EXITOSA: Código seguro y listo para Pull Request."
  echo "=================================================================="
  exit 0
fi
