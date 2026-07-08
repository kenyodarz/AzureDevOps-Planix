import {Component, EventEmitter, Input, Output, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';

@Component({
  selector: 'app-planning-upload',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="bg-[rgba(255,255,255,0.02)] border border-[rgba(255,255,255,0.08)] rounded-xl p-4 flex flex-col gap-3">
      <h3 class="text-[0.9rem] text-[#f2c94c] font-semibold uppercase tracking-wider">Cargar Planeación</h3>
      <p class="text-[0.75rem] text-[#9ca3af] -mt-1 leading-normal">
        Indexa un archivo Markdown en la base de datos vectorial para consultas semánticas.
      </p>

      <div class="flex flex-col gap-2 mt-1">
        <input
          aria-label="ID Iniciativa"
          placeholder="ID Iniciativa (ej: guardian-q3)"
          class="bg-[rgba(255,255,255,0.05)] border border-[rgba(255,255,255,0.08)] rounded-md px-3 py-2 text-[0.8rem] text-[#f3f4f6] outline-none focus:border-[#f2c94c] w-full"
          type="text"
          [(ngModel)]="initiativeId"
        />

        <input
          aria-label="Título"
          placeholder="Título (ej: Guardián Q3)"
          class="bg-[rgba(255,255,255,0.05)] border border-[rgba(255,255,255,0.08)] rounded-md px-3 py-2 text-[0.8rem] text-[#f3f4f6] outline-none focus:border-[#f2c94c] w-full"
          type="text"
          [(ngModel)]="title"
        />

        <div class="flex gap-2 items-center w-full">
          <label
            for="planning-file"
            [class.border-[#f2c94c]]="selectedFileName()"
            class="flex-1 text-center bg-[rgba(255,255,255,0.05)] border border-dashed border-[rgba(255,255,255,0.08)] rounded-md p-2 text-[0.75rem] cursor-pointer hover:bg-[rgba(255,255,255,0.08)] transition-all overflow-hidden text-ellipsis whitespace-nowrap block"
          >
            {{ selectedFileName() ?? 'Seleccionar .md' }}
          </label>
          <input
            accept=".md"
            id="planning-file"
            class="hidden"
            type="file"
            (change)="onFileSelected($event)"
          />
        </div>

        <button
          [disabled]="uploading || !initiativeId || !title || !selectedFile"
          [style.opacity]="(uploading || !initiativeId || !title || !selectedFile) ? 0.5 : 1"
          class="bg-[#f2c94c] text-[#000] border-none rounded-md py-2 font-semibold text-[0.8rem] cursor-pointer hover:bg-[#e0b83b] transition-colors"
          (click)="onVectorize()"
        >
          {{ uploading ? 'Vectorizando...' : 'Vectorizar Contexto' }}
        </button>

        @if (uploadStatus) {
          <div
            [style.color]="getStatusColor()"
            class="text-[0.75rem] text-center mt-1 font-medium"
          >
            {{ uploadStatus }}
          </div>
        }
      </div>
    </div>
  `
})
export class PlanningUploadComponent {
  @Input() uploading = false;
  @Input() uploadStatus: string | null = null;
  @Output() upload = new EventEmitter<{ initiativeId: string; title: string; content: string }>();

  protected initiativeId = '';
  protected title = '';
  protected selectedFile: File | null = null;
  protected readonly selectedFileName = signal<string | null>(null);

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
      this.selectedFileName.set(this.selectedFile.name);
    } else {
      this.selectedFile = null;
      this.selectedFileName.set(null);
    }
  }

  protected onVectorize(): void {
    if (!this.selectedFile || !this.initiativeId || !this.title) {
      return;
    }

    this.selectedFile.text()
    .then((content) => {
      this.upload.emit({
        initiativeId: this.initiativeId.trim(),
        title: this.title.trim(),
        content
      });
      // Reset state on successful send request
      this.initiativeId = '';
      this.title = '';
      this.selectedFile = null;
      this.selectedFileName.set(null);
    })
    .catch((err) => {
      console.error('Error reading file:', err);
    });
  }

  protected getStatusColor(): string {
    if (!this.uploadStatus) {
      return '';
    }
    if (this.uploadStatus.includes('éxito')) {
      return '#10b981'; // Green
    }
    if (this.uploadStatus.includes('Error')) {
      return '#ef4444'; // Red
    }
    return '#9ca3af'; // Grey for Loading
  }
}
