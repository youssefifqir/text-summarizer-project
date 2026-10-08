# Text and Document Summarizer

Summarize pasted text, or upload a PDF, DOCX or TXT file and get a summary you can download as a Word document. The summaries come from the `facebook/bart-large-cnn` model on Hugging Face.

- **Backend:** Spring Boot 3 (Java 17), WebClient, Apache PDFBox and Apache POI. Port 8081.
- **Frontend:** Angular 19. Port 4200.

## How a document is summarized

BART reads about 1,000 tokens at a time, so a long document cannot be sent in one call.

1. The file is read into plain text (PDFBox for PDF, POI for DOCX).
2. The text is cut into sections of at most 3,000 characters, always at a sentence boundary.
3. Each section is summarized by the model.
4. The section summaries are summarized together, repeating while they are still too long for one call.
5. The result can be exported as a Word document with the overall summary and one block per section.

## API

| Method | Path | Body | Returns |
|---|---|---|---|
| POST | `/api/summarize` | raw text | summary as plain text |
| POST | `/api/documents/summarize` | multipart field `file` (PDF, DOCX or TXT, 10 MB max) | JSON: file name, pages, characters, sections, summary, section summaries |
| POST | `/api/documents/export` | JSON from the call above | `.docx` file |

Errors come back as JSON `{ "message": "..." }` with a matching status: 415 unsupported type, 422 unreadable or empty document, 413 too large, 502 model unreachable.

## Run it

You need a Hugging Face access token with the "Inference Providers" permission.

```bash
# backend
cd backend
export HF_API_TOKEN=hf_your_token      # Windows cmd: set HF_API_TOKEN=hf_your_token
mvn spring-boot:run

# frontend, in a second terminal
cd frontend
npm install
npx ng serve
```

Open http://localhost:4200. Run the backend tests with `mvn test` in `backend`.

## Limits

- Scanned PDFs are images and have no text to read, so they are rejected with a clear message. There is no OCR.
- `bart-large-cnn` was trained on English news articles, so it works best on English prose.
- Old `.doc` files are not supported. Save them as `.docx`.
