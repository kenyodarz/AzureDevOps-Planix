import { TestBed } from '@angular/core/testing';
import { MarkdownParserPipe } from './markdown-parser.pipe';
import { DomSanitizer } from '@angular/platform-browser';

describe('GIVEN MarkdownParserPipe', () => {
  let pipe: MarkdownParserPipe;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        MarkdownParserPipe,
        {
          provide: DomSanitizer,
          useValue: {
            bypassSecurityTrustHtml: (val: string) => val
          }
        }
      ]
    });
    pipe = TestBed.inject(MarkdownParserPipe);
  });

  describe('WHEN transform is invoked with null or empty text', () => {
    it('THEN returns an empty string', () => {
      expect(pipe.transform(null)).toBe('');
      expect(pipe.transform('')).toBe('');
    });
  });

  describe('WHEN transform is invoked with simple markdown text', () => {
    it('THEN correctly converts markdown elements to HTML tags', () => {
      const result = pipe.transform('este es `codigo inline` mas texto').toString();
      expect(result).toContain('<code>codigo inline</code>');
    });

    it('THEN converts bold text to strong tags with custom styles', () => {
      const result = pipe.transform('este es **texto en negrita** mas texto').toString();
      expect(result).toContain('<strong style="font-weight: 700; color: #f3f4f6;">texto en negrita</strong>');
    });

    it('THEN converts italic text to em tags', () => {
      const result1 = pipe.transform('este es *texto en cursiva* mas texto').toString();
      expect(result1).toContain('<em>texto en cursiva</em>');

      const result2 = pipe.transform('este es _texto en cursiva_ mas texto').toString();
      expect(result2).toContain('<em>texto en cursiva</em>');
    });

    it('THEN converts headings with custom styles', () => {
      const heading3 = pipe.transform('### Mi Titulo').toString();
      expect(heading3).toContain('<h4 style="margin:12px 0 6px 0; color:var(--primary); font-weight:600;">Mi Titulo</h4>');
    });

    it('THEN converts lists with custom bullets', () => {
      const list = pipe.transform('- Elemento 1\n- Elemento 2').toString();
      expect(list).toContain('<li style="margin-left:15px; margin-bottom:4px;">Elemento 1</li>');
      expect(list).toContain('<li style="margin-left:15px; margin-bottom:4px;">Elemento 2</li>');
    });

    it('THEN converts checkbox lists', () => {
      const checkboxes = pipe.transform('[ ] Pendiente\n[x] Completado').toString();
      expect(checkboxes).toContain('<input type="checkbox" disabled style="margin-right: 6px; transform: scale(1.1);">');
      expect(checkboxes).toContain('<input type="checkbox" checked disabled style="margin-right: 6px; transform: scale(1.1);">');
    });
  });

  describe('WHEN transform is invoked with preformatted code blocks', () => {
    it('THEN isolates them from markdown processing and renders pre/code tags', () => {
      const markdown = '```\nconst a = 1;\nif (a < 2) { console.log(a); }\n```';
      const result = pipe.transform(markdown).toString();
      expect(result).toContain('<pre><code>const a = 1;\nif (a &lt; 2) { console.log(a); }</code></pre>');
    });
  });
});
