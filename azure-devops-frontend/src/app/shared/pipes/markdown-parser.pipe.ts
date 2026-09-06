import { inject, Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Pipe({
  name: 'markdownParser',
  standalone: true,
})
export class MarkdownParserPipe implements PipeTransform {
  private readonly sanitizer = inject(DomSanitizer);

  transform(value: string | undefined | null): SafeHtml {
    if (!value) {
      return '';
    }

    // 1. Extraer bloques de código (```...```) para aislarlos del parseo
    const codeBlocks: string[] = [];
    let html = value.replaceAll(/```([\s\S]*?)```/g, (match, code) => {
      const placeholder = `CODEBLOCKPLACEHOLDER${codeBlocks.length}`;
      codeBlocks.push(code.trim());
      return placeholder;
    });

    // 2. Escapar HTML del resto del texto para seguridad
    html = html.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;');

    // 3. Aplicar reglas de formato Markdown
    // Inline code
    html = html.replaceAll(/`([^`\n]+)`/g, '<code>$1</code>');

    // Bold
    html = html.replaceAll(
      /\*\*([^*]+)\*\*/g,
      '<strong style="font-weight: 700; color: #f3f4f6;">$1</strong>',
    );

    // Italics
    html = html.replaceAll(/\*([^*]+)\*/g, '<em>$1</em>');
    html = html.replaceAll(/_([^_]+)_/g, '<em>$1</em>');

    // Checkboxes [ ] and [x]
    html = html.replaceAll(
      '[ ]',
      '<input type="checkbox" disabled style="margin-right: 6px; transform: scale(1.1);">',
    );
    html = html.replaceAll(
      '[x]',
      '<input type="checkbox" checked disabled style="margin-right: 6px; transform: scale(1.1);">',
    );

    // Extraer y procesar tablas Markdown antes de saltos de línea
    const tableBlocks: string[] = [];
    html = this.parseAndExtractTables(html, tableBlocks);

    // Headings
    html = html.replaceAll(
      /^### (.*$)/gim,
      '<h4 style="margin:12px 0 6px 0; color:var(--primary); font-weight:600;">$1</h4>',
    );
    html = html.replaceAll(
      /^## (.*$)/gim,
      '<h3 style="margin:16px 0 8px 0; color:var(--primary); font-weight:600;">$1</h3>',
    );
    html = html.replaceAll(
      /^# (.*$)/gim,
      '<h2 style="margin:20px 0 10px 0; color:var(--primary); font-weight:700;">$1</h2>',
    );

    // Lists
    html = html.replaceAll(
      /^\s*[-*]\s+(.*$)/gim,
      '<li style="margin-left:15px; margin-bottom:4px;">$1</li>',
    );
    html = html.replaceAll('</li>\n<li>', '</li><li>');

    // Párrafos y Saltos de línea
    html = html.replaceAll('\n', '<br>');

    // 4. Reinsertar tablas
    for (let i = 0; i < tableBlocks.length; i++) {
      html = html.split(`TABLEBLOCKPLACEHOLDER${i}`).join(tableBlocks[i]);
    }

    // 5. Reinsertar bloques de código originales sin procesarles markdown
    for (let i = 0; i < codeBlocks.length; i++) {
      const escapedCode = codeBlocks[i]
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;');

      const placeholder = `CODEBLOCKPLACEHOLDER${i}`;
      const codeHtml = `<pre><code>${escapedCode}</code></pre>`;

      html = html.split(placeholder).join(codeHtml);
    }

    return this.sanitizer.bypassSecurityTrustHtml(html);
  }

  private parseAndExtractTables(text: string, tableBlocks: string[]): string {
    const lines = text.split('\n');
    const result: string[] = [];
    let i = 0;

    while (i < lines.length) {
      const current = lines[i].trim();
      const next = i + 1 < lines.length ? lines[i + 1].trim() : '';

      if (
        current.startsWith('|') &&
        current.endsWith('|') &&
        next.startsWith('|') &&
        next.endsWith('|')
      ) {
        const headerCells = current
          .slice(1, -1)
          .split('|')
          .map((c) => c.trim());
        const sepCells = next
          .slice(1, -1)
          .split('|')
          .map((c) => c.trim());
        const isSep =
          sepCells.length === headerCells.length && sepCells.every((c) => /^:?-+:?$/.test(c));

        if (isSep) {
          let tableHtml =
            '<div class="overflow-x-auto my-4 rounded-xl border border-slate-800 bg-slate-900/60 shadow-inner">' +
            '<table class="w-full text-left text-xs border-collapse text-slate-200">' +
            '<thead class="bg-slate-800/80 text-[#f2c94c] uppercase tracking-wider font-semibold border-b border-slate-700/80"><tr>';

          for (const header of headerCells) {
            tableHtml += `<th class="px-4 py-2.5 border-r border-slate-800 last:border-r-0">${header}</th>`;
          }
          tableHtml += '</tr></thead><tbody class="divide-y divide-slate-800/60">';

          i += 2;
          while (
            i < lines.length &&
            lines[i].trim().startsWith('|') &&
            lines[i].trim().endsWith('|')
          ) {
            const rowCells = lines[i]
              .trim()
              .slice(1, -1)
              .split('|')
              .map((c) => c.trim());
            tableHtml += '<tr class="hover:bg-slate-800/30 transition-colors">';
            for (const cell of rowCells) {
              tableHtml += `<td class="px-4 py-2.5 border-r border-slate-800/60 last:border-r-0 text-slate-300">${cell}</td>`;
            }
            tableHtml += '</tr>';
            i++;
          }

          tableHtml += '</tbody></table></div>';
          const placeholder = `TABLEBLOCKPLACEHOLDER${tableBlocks.length}`;
          tableBlocks.push(tableHtml);
          result.push(placeholder);
          continue;
        }
      }

      result.push(lines[i]);
      i++;
    }

    return result.join('\n');
  }
}
