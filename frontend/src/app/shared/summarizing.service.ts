import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface DocumentSummary {
  fileName: string;
  fileType: string;
  pages: number | null;
  characters: number;
  sections: number;
  summary: string;
  sectionSummaries: string[];
}

@Injectable({
  providedIn: 'root'
})
export class SummarizingService {
  private apiUrl = 'http://localhost:8081/api/summarize';
  private documentsUrl = 'http://localhost:8081/api/documents';

  constructor(private http: HttpClient) { }

  summarizeText(text: string): Observable<string> { // <-- Retourne directement un string
    return this.http.post(
      this.apiUrl,
      text, // <-- Envoie le texte brut (le backend attend juste un String)
      {
        headers: new HttpHeaders({
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        }),
        responseType: 'text' // <-- Spécifie qu'on attend un texte brut
      }
    );
  }

  /** Uploads a PDF, DOCX or TXT file and returns its summary. */
  summarizeDocument(file: File): Observable<DocumentSummary> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<DocumentSummary>(`${this.documentsUrl}/summarize`, form);
  }

  /** Turns a summary into a Word document. */
  exportDocx(result: DocumentSummary): Observable<Blob> {
    return this.http.post(
      `${this.documentsUrl}/export`,
      {
        fileName: result.fileName,
        pages: result.pages,
        characters: result.characters,
        summary: result.summary,
        sectionSummaries: result.sectionSummaries
      },
      { responseType: 'blob' }
    );
  }
}
