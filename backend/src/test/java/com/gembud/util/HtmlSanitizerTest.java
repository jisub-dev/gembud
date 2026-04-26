package com.gembud.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HtmlSanitizerTest {

    private HtmlSanitizer htmlSanitizer;

    @BeforeEach
    void setUp() {
        htmlSanitizer = new HtmlSanitizer();
    }

    @Test
    @DisplayName("sanitize - null 입력은 null을 반환한다")
    void sanitize_null_returnsNull() {
        assertThat(htmlSanitizer.sanitize(null)).isNull();
    }

    @Test
    @DisplayName("sanitize - HTML 특수문자를 이스케이프한다")
    void sanitize_escapesHtmlCharacters() {
        String input = "<script>alert('xss')</script>";
        String result = htmlSanitizer.sanitize(input);

        assertThat(result).doesNotContain("<", ">", "'");
        assertThat(result).contains("&lt;", "&gt;", "&#x27;");
    }

    @Test
    @DisplayName("sanitize - & 문자를 &amp;로 이스케이프한다")
    void sanitize_escapesAmpersand() {
        assertThat(htmlSanitizer.sanitize("a & b")).isEqualTo("a &amp; b");
    }

    @Test
    @DisplayName("sanitize - 따옴표를 이스케이프한다")
    void sanitize_escapesQuotes() {
        assertThat(htmlSanitizer.sanitize("\"double\"")).isEqualTo("&quot;double&quot;");
    }

    @Test
    @DisplayName("sanitize - 슬래시를 이스케이프한다")
    void sanitize_escapesSlash() {
        assertThat(htmlSanitizer.sanitize("path/to")).isEqualTo("path&#x2F;to");
    }

    @Test
    @DisplayName("sanitize - 일반 텍스트는 변경하지 않는다")
    void sanitize_plainText_unchanged() {
        assertThat(htmlSanitizer.sanitize("hello world 123")).isEqualTo("hello world 123");
    }

    @Test
    @DisplayName("sanitizeAndLimit - null 입력은 null을 반환한다")
    void sanitizeAndLimit_null_returnsNull() {
        assertThat(htmlSanitizer.sanitizeAndLimit(null, 10)).isNull();
    }

    @Test
    @DisplayName("sanitizeAndLimit - maxLength 초과 시 잘라낸다")
    void sanitizeAndLimit_truncatesAtMaxLength() {
        String result = htmlSanitizer.sanitizeAndLimit("hello world", 5);
        assertThat(result).hasSize(5).isEqualTo("hello");
    }

    @Test
    @DisplayName("sanitizeAndLimit - maxLength 이하이면 전체 반환한다")
    void sanitizeAndLimit_withinLimit_returnsAll() {
        String result = htmlSanitizer.sanitizeAndLimit("hi", 100);
        assertThat(result).isEqualTo("hi");
    }

    @Test
    @DisplayName("sanitizeAndLimit - 이스케이프 후 길이 기준으로 자른다")
    void sanitizeAndLimit_appliesSanitizeBeforeTruncate() {
        // "<abc>" becomes "&lt;abc&gt;" (10 chars), limit=8 → "&lt;abc&g"
        String result = htmlSanitizer.sanitizeAndLimit("<abc>", 8);
        assertThat(result).hasSize(8).startsWith("&lt;");
    }
}
