import { Pipe, PipeTransform, inject } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Pipe({
  name: 'markdownParser',
  standalone: true
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
    html = html
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;');

    // 3. Aplicar reglas de formato Markdown
    // Inline code
    html = html.replaceAll(/`([^`\n]+)`/g, '<code>$1</code>');

    // Bold
    html = html.replaceAll(/\*\*([^*]+)\*\*/g, '<strong style="font-weight: 700; color: #f3f4f6;">$1</strong>');

    // Italics
    html = html.replaceAll(/\*([^*]+)\*/g, '<em>$1</em>');
    html = html.replaceAll(/_([^_]+)_/g, '<em>$1</em>');

    // Headings
    html = html.replaceAll(/^### (.*$)/gim, '<h4 style="margin:12px 0 6px 0; color:var(--primary); font-weight:600;">$1</h4>');
    html = html.replaceAll(/^## (.*$)/gim, '<h3 style="margin:16px 0 8px 0; color:var(--primary); font-weight:600;">$1</h3>');
    html = html.replaceAll(/^# (.*$)/gim, '<h2 style="margin:20px 0 10px 0; color:var(--primary); font-weight:700;">$1</h2>');

    // Lists
    html = html.replaceAll(/^\s*[-*]\s+(.*$)/gim, '<li style="margin-left:15px; margin-bottom:4px;">$1</li>');
    html = html.replaceAll('</li>\n<li>', '</li><li>');

    // Checkboxes [ ] and [x]
    html = html.replaceAll('[ ]', '<input type="checkbox" disabled style="margin-right: 6px; transform: scale(1.1);">');
    html = html.replaceAll('[x]', '<input type="checkbox" checked disabled style="margin-right: 6px; transform: scale(1.1);">');

    // Párrafos y Saltos de línea
    html = html.replaceAll('\n', '<br>');

    // 4. Reinsertar bloques de código originales sin procesarles markdown
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
}
