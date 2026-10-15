#!/usr/bin/env bash
# ==============================================================================
# Activa los git hooks versionados del repositorio.
#
# Uso:
#   bash tools/setup-hooks.sh
#
# Los hooks viven en .githooks/ (versionados) en lugar de .git/hooks/ (local y
# no versionado), de modo que todo el equipo comparte las mismas validaciones.
# ==============================================================================

set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT"

git config core.hooksPath .githooks
chmod +x .githooks/* 2>/dev/null || true
chmod +x tools/commit-lint/*.sh 2>/dev/null || true

# Evita mensajes de commit con acentos corruptos en Windows.
git config i18n.commitEncoding utf-8
git config i18n.logOutputEncoding utf-8

echo "Hooks activados."
echo "  core.hooksPath      = $(git config --get core.hooksPath)"
echo "  i18n.commitEncoding = $(git config --get i18n.commitEncoding)"
echo ""
echo "Prueba rapida:"
echo "  echo 'docs: add something' > /tmp/m && bash .githooks/commit-msg /tmp/m"
echo "  (debe fallar)"

