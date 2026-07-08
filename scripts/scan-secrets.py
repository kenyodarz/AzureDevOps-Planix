#!/usr/bin/env python3
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
IGNORE_DIRS = {'.git', '.idea', '.vscode', '.gradle', 'build', 'node_modules', 'dist', 'coverage', 'out', 'target', 'build-cache'}
SKIP_MARKERS = ('${', 'placeholder', 'changeme', 'example', 'your-', 'dummy', 'fake', 'replace-me')

PATTERNS = [
    re.compile(r'AKIA[0-9A-Z]{16}'),
    re.compile(r'ghp_[A-Za-z0-9]{36}'),
    re.compile(r'github_pat_[A-Za-z0-9_]{20,}'),
    re.compile(r'AIza[0-9A-Za-z\-_]{35}'),
    re.compile(r'(?i)(api[_-]?key|access[_-]?token|client[_-]?secret)\s*[:=]\s*[\"\']?[A-Za-z0-9/+=._-]{8,}'),
]

files_to_scan = []
for path in ROOT.rglob('*'):
    if not path.is_file():
        continue
    if any(part in IGNORE_DIRS for part in path.parts):
        continue
    if path.name.startswith('.git'):
        continue
    files_to_scan.append(path)

hits = []
for path in files_to_scan:
    try:
        text = path.read_text(encoding='utf-8', errors='ignore')
    except Exception:
        continue
    if path.name == '.env.example':
        continue
    for pattern in PATTERNS:
        for line in text.splitlines():
            lower_line = line.lower()
            if any(marker in lower_line for marker in SKIP_MARKERS):
                continue
            if pattern.search(line):
                hits.append(str(path.relative_to(ROOT)))
                break
        if hits and hits[-1] == str(path.relative_to(ROOT)):
            break

if hits:
    print('Possible secrets or tokens detected:')
    for hit in hits:
        print(f' - {hit}')
    sys.exit(1)

print('No obvious secrets detected.')
