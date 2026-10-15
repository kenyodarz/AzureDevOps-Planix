#!/usr/bin/env bash
# ==============================================================================
# Valida los mensajes de un rango de commits y, opcionalmente, el titulo de un
# Pull Request. Pensado para ejecutarse en CI (GitHub Actions).
#
# Uso:
#   tools/commit-lint/validate-range.sh <rango-git>
#   tools/commit-lint/validate-range.sh origin/main..HEAD
#   PR_TITLE="feat(x): agregar y" tools/commit-lint/validate-range.sh origin/main..HEAD
#
# Variables de entorno opcionales:
#   PR_TITLE  Titulo del Pull Request. Critico cuando se usa squash merge,
#             porque el mensaje del commit final se construye a partir de el.
# ==============================================================================

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./commit-rules.sh
. "$SCRIPT_DIR/commit-rules.sh"

RANGE="${1:-}"
FAILED=0
TOTAL=0
SKIPPED=0

echo "=========================================================="
echo "VALIDACION DE MENSAJES DE COMMIT (COMMIT_MESSAGE_CONVENTION.md)"
echo "=========================================================="

# --- Titulo del Pull Request -------------------------------------------------
if [ -n "${PR_TITLE:-}" ]; then
  echo ""
  echo "Titulo del Pull Request:"
  if validate_commit_header "$PR_TITLE"; then
    warn_header_length "$PR_TITLE"
    echo "  OK: $PR_TITLE"
  else
    echo "  FALLA: $PR_TITLE"
    echo ""
    echo "  El titulo del PR debe cumplir la convencion porque con squash merge"
    echo "  se convierte en el mensaje del commit que lee semantic-release."
    FAILED=1
  fi
fi

# --- Commits del rango -------------------------------------------------------
if [ -z "$RANGE" ]; then
  echo ""
  echo "No se indico rango de commits; solo se valido el titulo del PR."
else
  echo ""
  echo "Rango evaluado: $RANGE"
  echo "----------------------------------------------------------"

  while read -r sha; do
    [ -z "$sha" ] && continue
    TOTAL=$((TOTAL + 1))
    msg="$(git log -1 --pretty=format:%s "$sha")"

    if [[ "$msg" =~ $SKIP_REGEX ]]; then
      SKIPPED=$((SKIPPED + 1))
      echo "  OMITIDO  $msg"
      continue
    fi

    if validate_commit_header "$msg"; then
      warn_header_length "$msg"
      echo "  OK       $msg"
    else
      echo "  FALLA    [$sha] $msg"
      echo "----------------------------------------------------------"
      FAILED=1
    fi
  done < <(git rev-list "$RANGE")

  echo "----------------------------------------------------------"
  echo "Commits analizados: $TOTAL (omitidos: $SKIPPED)"
fi

# --- Resultado ---------------------------------------------------------------
echo ""
if [ "$FAILED" -ne 0 ]; then
  echo "=========================================================="
  echo "ERROR: uno o mas mensajes NO cumplen el estandar."
  echo ""
  echo "Como corregirlo:"
  echo "  1. git rebase -i \$(git merge-base HEAD origin/master)"
  echo "  2. marca 'reword' en los commits senalados y corrige el texto"
  echo "  3. git push --force-with-lease"
  echo ""
  echo "Si el que falla es el titulo del PR, editalo desde la interfaz."
  echo "=========================================================="
  exit 1
fi

echo "Todos los mensajes cumplen el estandar."
exit 0

