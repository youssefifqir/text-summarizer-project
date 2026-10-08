import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DocumentSummary, SummarizingService } from '../../shared/summarizing.service';
import { HttpErrorResponse } from '@angular/common/http';

type Mode = 'document' | 'text';

const MAX_FILE_BYTES = 10 * 1024 * 1024;
const ALLOWED_EXTENSIONS = ['pdf', 'docx', 'txt'];
const PLACEHOLDER = 'Your concise, AI-generated summary will materialize here';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent {
  mode: Mode = 'document';
  inputText: string = '';
  selectedFile: File | null = null;
  dragOver: boolean = false;
  summaryOutput: string = PLACEHOLDER;
  documentResult: DocumentSummary | null = null;
  errorMessage: string = '';
  isLoading: boolean = false;
  isSummaryAvailable: boolean = false;

  constructor(private summarizingService: SummarizingService) {}

  setMode(mode: Mode): void {
    if (this.isLoading) {
      return;
    }
    this.mode = mode;
    this.resetResult();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.setFile(input.files[0]);
    }
    input.value = '';
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragOver = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.dragOver = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver = false;
    const file = event.dataTransfer?.files?.[0];
    if (file) {
      this.setFile(file);
    }
  }

  clearFile(): void {
    this.selectedFile = null;
    this.resetResult();
  }

  generateSummary(): void {
    if (this.mode === 'document') {
      this.summarizeDocument();
    } else {
      this.summarizeText();
    }
  }

  downloadDocx(): void {
    if (!this.documentResult) {
      return;
    }
    const result = this.documentResult;
    this.summarizingService.exportDocx(result).subscribe({
      next: (blob: Blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = result.fileName.replace(/\.[^.]+$/, '') + '-summary.docx';
        link.click();
        URL.revokeObjectURL(url);
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage = 'Could not create the Word document. Please try again.';
        console.error('Export error:', err);
      }
    });
  }

  copyToClipboard(): void {
    if (this.summaryOutput.trim()) {
      navigator.clipboard.writeText(this.summaryOutput).then(() => {
        const copyBtn = document.getElementById('copy-btn');
        copyBtn?.classList.add('copied');
        setTimeout(() => copyBtn?.classList.remove('copied'), 1000);
      }).catch(err => console.error('Copy failed:', err));
    }
  }

  formatSize(bytes: number): string {
    return bytes < 1024 * 1024
      ? Math.max(1, Math.round(bytes / 1024)) + ' KB'
      : (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }

  private setFile(file: File): void {
    this.resetResult();
    const extension = file.name.split('.').pop()?.toLowerCase() ?? '';
    if (!ALLOWED_EXTENSIONS.includes(extension)) {
      this.selectedFile = null;
      this.errorMessage = 'Unsupported file type. Upload a PDF, DOCX or TXT file.';
      return;
    }
    if (file.size > MAX_FILE_BYTES) {
      this.selectedFile = null;
      this.errorMessage = 'This file is too large. The limit is 10 MB.';
      return;
    }
    this.selectedFile = file;
  }

  private summarizeDocument(): void {
    if (!this.selectedFile) {
      this.errorMessage = 'Choose a PDF, DOCX or TXT file first.';
      return;
    }

    this.startLoading();
    this.summarizingService.summarizeDocument(this.selectedFile).subscribe({
      next: (result: DocumentSummary) => {
        this.documentResult = result;
        this.summaryOutput = result.summary;
        this.isLoading = false;
        this.isSummaryAvailable = true;
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading = false;
        this.errorMessage = this.messageFrom(err);
        console.error('Error details:', err);
      }
    });
  }

  private summarizeText(): void {
    if (this.inputText.trim() === '') {
      this.errorMessage = 'Please enter some text to summarize.';
      return;
    }

    this.startLoading();
    this.summarizingService.summarizeText(this.inputText).subscribe({
      next: (response: string) => { // <-- On récupère directement le string
        this.summaryOutput = response;
        this.isLoading = false;
        this.isSummaryAvailable = true;
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading = false;
        this.errorMessage = typeof err.error === 'string' && err.error
          ? err.error
          : 'Summarization failed. Please try again.';
        console.error('Error details:', err);
      }
    });
  }

  private startLoading(): void {
    this.resetResult();
    this.isLoading = true;
  }

  private resetResult(): void {
    this.errorMessage = '';
    this.documentResult = null;
    this.isSummaryAvailable = false;
    this.summaryOutput = PLACEHOLDER;
  }

  private messageFrom(err: HttpErrorResponse): string {
    if (err.status === 0) {
      return 'The server is not reachable. Check that the backend is running.';
    }
    return err.error?.message ?? 'Summarization failed. Please try again.';
  }
}
