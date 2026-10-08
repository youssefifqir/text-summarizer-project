package com.youssef.textsummarizerbackendms1.service.document;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Cuts a long text into pieces the summarization model can read, breaking on sentence
 * boundaries so that no piece starts or ends in the middle of a sentence.
 */
@Component
public class TextChunker {

    private static final int MIN_CHUNK_SIZE = 200;
    // A sentence ends at . ! ? followed by whitespace, or at a blank line (paragraph break).
    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[.!?])\\s+|\\n{2,}");
    private static final Pattern SINGLE_NEWLINE = Pattern.compile("(?<!\\n)\\n(?!\\n)");
    private static final Pattern SPACES = Pattern.compile("[ \\t\\u00a0]+");
    private static final Pattern MANY_NEWLINES = Pattern.compile("\\n{3,}");

    public List<String> chunk(String text, int maxChars) {
        if (maxChars < MIN_CHUNK_SIZE) {
            throw new IllegalArgumentException("Chunk size must be at least " + MIN_CHUNK_SIZE + " characters");
        }
        String normalized = normalize(text);
        if (normalized.isEmpty()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String raw : SENTENCE_BOUNDARY.split(normalized)) {
            String sentence = raw.strip();
            if (sentence.isEmpty()) {
                continue;
            }
            if (sentence.length() > maxChars) {
                flush(current, chunks);
                splitLongSentence(sentence, maxChars, chunks);
                continue;
            }
            if (current.length() + sentence.length() + 1 > maxChars) {
                flush(current, chunks);
            }
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(sentence);
        }
        flush(current, chunks);
        return chunks;
    }

    /** Joins the line wraps PDFs put inside sentences, keeps blank lines as paragraph breaks. */
    private static String normalize(String text) {
        String unified = text.replace("\r\n", "\n").replace('\r', '\n');
        unified = SINGLE_NEWLINE.matcher(unified).replaceAll(" ");
        unified = SPACES.matcher(unified).replaceAll(" ");
        unified = MANY_NEWLINES.matcher(unified).replaceAll("\n\n");
        return unified.strip();
    }

    private static void splitLongSentence(String sentence, int maxChars, List<String> chunks) {
        String remaining = sentence;
        while (remaining.length() > maxChars) {
            int cut = remaining.lastIndexOf(' ', maxChars);
            if (cut <= 0) {
                cut = maxChars;
            }
            chunks.add(remaining.substring(0, cut).strip());
            remaining = remaining.substring(cut).strip();
        }
        if (!remaining.isEmpty()) {
            chunks.add(remaining);
        }
    }

    private static void flush(StringBuilder current, List<String> chunks) {
        if (current.length() > 0) {
            chunks.add(current.toString());
            current.setLength(0);
        }
    }
}
