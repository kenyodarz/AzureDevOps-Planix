#!/usr/bin/env bash
# ==============================================================================
# FUENTE UNICA DE VERDAD para la validacion de mensajes de commit.
#
# Lo consumen:
#   - .githooks/commit-msg                     (validacion local, antes de crear el commit)
#   - .github/workflows/validate-commits.yml   (validacion en CI / GitHub Actions)
#
# No dupliques estas expresiones regulares en ningun otro lugar.
# Reglas completas: COMMIT_MESSAGE_CONVENTION.md
# ==============================================================================

# Tipos admitidos, alineados con COMMIT_RULES.md y .releaserc.json
COMMIT_TYPES="feat|fix|docs|style|refactor|perf|test|build|ci|removed|deprecated|security|chore"

# Formato: COMMIT_TYPE(scope_snake_case)!: descripcion
COMMIT_REGEX="^(${COMMIT_TYPES})\([a-z0-9_]+\)!?:[[:space:]]+.+$"

# Verbos en ingles prohibidos al inicio de la descripcion
ENGLISH_VERBS_REGEX="\):[[:space:]]+(add|adds|added|adding|update|updates|updated|updating|fix|fixes|fixed|fixing|implement|implements|implemented|create|creates|created|creating|refactor|refactors|refactored|remove|removes|removed|removing|change|changes|changed|improve|improves|improved|enhance|enhances|enhanced|introduce|introduces|introduced|support|supports|enable|enables|disable|disables|migrate|migrates|upgrade|upgrades|initialize|initializes|configure|configures|optimize|optimizes|scaffold|bump|allow|allows|ensure|ensures)([[:space:]]|$)"

# Longitud maxima recomendada del encabezado (solo advertencia)
MAX_HEADER_LENGTH=100

# Prefijos de commits automaticos que se omiten de la validacion
SKIP_REGEX="^(Merge|Revert|fixup!|squash!|Reverted|Merged)[[:space:]]"

# ------------------------------------------------------------------------------
# validate_commit_header <mensaje>
# Devuelve 0 si es valido, 1 si no. Imprime el detalle del error en stderr.
# ------------------------------------------------------------------------------
validate_commit_header() {
  local msg="$1"

  if [[ "$msg" =~ $SKIP_REGEX ]]; then
    return 0
  fi

  if [[ ! "$msg" =~ $COMMIT_REGEX ]]; then
    {
      echo "  Motivo:  no cumple el formato obligatorio"
      echo "  Formato: COMMIT_TYPE(scope_snake_case): descripcion en espanol"
      echo "  Tipos:   ${COMMIT_TYPES//|/, }"
      echo "  Scope:   obligatorio, snake_case, entre parentesis"
      echo "  Ejemplo: docs(github): agregar guia de configuracion"
    } >&2
    return 1
  fi

  if echo "$msg" | grep -Eiq "$ENGLISH_VERBS_REGEX"; then
    {
      echo "  Motivo:  la descripcion esta en ingles"
      echo "  Idioma:  espanol obligatorio, en minusculas"
      echo "  Verbos:  agregar, actualizar, corregir, implementar, crear, refactorizar, eliminar"
    } >&2
    return 1
  fi

  return 0
}

# ------------------------------------------------------------------------------
# warn_header_length <mensaje>
# Advierte si el encabezado supera la longitud recomendada. Nunca falla.
# ------------------------------------------------------------------------------
warn_header_length() {
  local msg="$1"
  if [ "${#msg}" -gt "$MAX_HEADER_LENGTH" ]; then
    echo "  AVISO: el encabezado tiene ${#msg} caracteres (recomendado <= ${MAX_HEADER_LENGTH})" >&2
  fi
}

