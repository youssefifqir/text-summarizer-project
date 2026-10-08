package com.youssef.textsummarizerbackendms1.service.document;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TextChunkerTest {

    private final TextChunker chunker = new TextChunker();

    @Test
    void shortTextStaysInOneChunk() {
        List<String> chunks = chunker.chunk("First sentence. Second sentence.", 500);

        assertThat(chunks).containsExactly("First sentence. Second sentence.");
    }

    @Test
    void blankTextGivesNoChunks() {
        assertThat(chunker.chunk("  \n\n \t ", 500)).isEmpty();
    }

    @Test
    void chunksBreakOnSentenceBoundariesAndRespectTheLimit() {
        String sentence = "This is a sentence of about fifty characters long.";
        String text = String.join(" ", java.util.Collections.nCopies(20, sentence));

        List<String> chunks = chunker.chunk(text, 220);

        assertThat(chunks.size()).isGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.length()).isLessThanOrEqualTo(220);
            assertThat(chunk).startsWith("This").endsWith(".");
        });
    }

    @Test
    void noWordIsLostWhenSplitting() {
        String text = "Alpha beta gamma. Delta epsilon zeta. Eta theta iota. Kappa lambda mu. "
                .repeat(30);

        List<String> chunks = chunker.chunk(text, 200);

        assertThat(String.join(" ", chunks).split("\\s+")).containsExactly(text.strip().split("\\s+"));
    }

    @Test
    void lineWrapsInsideASentenceAreJoined() {
        List<String> chunks = chunker.chunk("The quarterly report shows\ngrowth in all\nregions.", 500);

        assertThat(chunks).containsExactly("The quarterly report shows growth in all regions.");
    }

    @Test
    void aSentenceLongerThanTheLimitIsSplitOnSpaces() {
        String longSentence = "word ".repeat(100).strip();

        List<String> chunks = chunker.chunk(longSentence, 200);

        assertThat(chunks.size()).isGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(200));
    }

    @Test
    void rejectsATinyLimit() {
        assertThatThrownBy(() -> chunker.chunk("text", 10)).isInstanceOf(IllegalArgumentException.class);
    }
}
