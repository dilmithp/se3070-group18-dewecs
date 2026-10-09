package com.group18.dewecs.service.impl;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A very small PDF writer for text and horizontal bars: A4 pages, Helvetica, automatic page breaks, a header line on
 * every page. It is enough for the printed report; it supports Latin-1 text only (other characters become "?").
 */
final class SimplePdf {

    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 50;
    private static final int LEADING = 14;
    private static final int WRAP = 92;

    private final String headerLine;
    private final String bannerLine;
    private final List<StringBuilder> pages = new ArrayList<>();
    private int y;

    SimplePdf(String headerLine, String bannerLine) {
        this.headerLine = headerLine;
        this.bannerLine = bannerLine;
        newPage();
    }

    void heading(String text) {
        ensure(LEADING * 2);
        y -= 6;
        line(text, "F2", 12);
        y -= 2;
    }

    void text(String text) {
        for (String wrapped : wrap(text)) {
            ensure(LEADING);
            line(wrapped, "F1", 10);
        }
    }

    void bold(String text) {
        for (String wrapped : wrap(text)) {
            ensure(LEADING);
            line(wrapped, "F2", 10);
        }
    }

    void gap() {
        y -= LEADING / 2;
    }

    /** A label, a bar whose length is {@code fraction} (0 to 1) of the full width, and a value text. */
    void bar(String label, double fraction, String valueText) {
        ensure(LEADING + 4);
        double clamped = Math.max(0, Math.min(1, fraction));
        StringBuilder page = current();
        page.append("BT /F1 9 Tf ").append(MARGIN).append(' ').append(y).append(" Td (").append(escape(trim(label, 28))).append(") Tj ET\n");
        int x = MARGIN + 170;
        int width = (int) Math.round(220 * clamped);
        page.append("0.31 0.36 0.84 rg ").append(x).append(' ').append(y - 1).append(' ').append(Math.max(width, 1)).append(" 8 re f 0 g\n");
        page.append("BT /F1 9 Tf ").append(x + 230).append(' ').append(y).append(" Td (").append(escape(valueText)).append(") Tj ET\n");
        y -= LEADING;
    }

    byte[] build() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        write(out, "%PDF-1.4\n");
        int pageCount = pages.size();
        // 1 catalog, 2 pages, 3 and 4 fonts, then for each page a page object and a content stream
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pageCount; i++) {
            kids.append(5 + i * 2).append(" 0 R ");
        }
        object(out, offsets, "<< /Type /Catalog /Pages 2 0 R >>");
        object(out, offsets, "<< /Type /Pages /Kids [" + kids + "] /Count " + pageCount + " >>");
        object(out, offsets, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        object(out, offsets, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");
        for (int i = 0; i < pageCount; i++) {
            int content = 6 + i * 2;
            object(out, offsets, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + PAGE_WIDTH + " " + PAGE_HEIGHT
                    + "] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + content + " 0 R >>");
            String stream = pages.get(i).toString() + footer(i + 1, pageCount);
            byte[] bytes = stream.getBytes(StandardCharsets.ISO_8859_1);
            offsets.add(out.size());
            write(out, (offsets.size()) + " 0 obj\n<< /Length " + bytes.length + " >>\nstream\n");
            out.write(bytes, 0, bytes.length);
            write(out, "\nendstream\nendobj\n");
        }
        int xref = out.size();
        write(out, "xref\n0 " + (offsets.size() + 1) + "\n0000000000 65535 f \n");
        for (int offset : offsets) {
            write(out, String.format(Locale.ROOT, "%010d 00000 n \n", offset));
        }
        write(out, "trailer\n<< /Size " + (offsets.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n");
        return out.toByteArray();
    }

    // ---------- internals ----------

    private void newPage() {
        StringBuilder page = new StringBuilder();
        pages.add(page);
        y = PAGE_HEIGHT - MARGIN;
        page.append("BT /F1 8 Tf 0.4 g ").append(MARGIN).append(' ').append(y).append(" Td (").append(escape(headerLine)).append(") Tj ET 0 g\n");
        y -= 12;
        if (bannerLine != null) {
            page.append("BT /F2 9 Tf 0.75 0.1 0.1 rg ").append(MARGIN).append(' ').append(y).append(" Td (").append(escape(bannerLine)).append(") Tj ET 0 g\n");
            y -= 14;
        }
        y -= 10;
    }

    private String footer(int number, int total) {
        return "BT /F1 8 Tf 0.4 g " + (PAGE_WIDTH - MARGIN - 60) + " 30 Td (Page " + number + " of " + total + ") Tj ET 0 g\n";
    }

    private StringBuilder current() {
        return pages.get(pages.size() - 1);
    }

    private void ensure(int needed) {
        if (y - needed < MARGIN + 20) {
            newPage();
        }
    }

    private void line(String text, String font, int size) {
        current().append("BT /").append(font).append(' ').append(size).append(" Tf ").append(MARGIN).append(' ')
                .append(y).append(" Td (").append(escape(text)).append(") Tj ET\n");
        y -= LEADING;
    }

    private List<String> wrap(String text) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : (text == null ? "" : text).split("\\R")) {
            String rest = paragraph;
            while (rest.length() > WRAP) {
                int cut = rest.lastIndexOf(' ', WRAP);
                if (cut <= 0) {
                    cut = WRAP;
                }
                lines.add(rest.substring(0, cut));
                rest = rest.substring(cut).stripLeading();
            }
            lines.add(rest);
        }
        return lines;
    }

    private String trim(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "...";
    }

    static String escape(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : (text == null ? "" : text).toCharArray()) {
            if (c == '\\' || c == '(' || c == ')') {
                sb.append('\\').append(c);
            } else if (c == '–' || c == '—') {
                sb.append('-');
            } else if (c == '’' || c == '‘') {
                sb.append('\'');
            } else if (c >= 32 && c < 256) {
                sb.append(c);
            } else {
                sb.append('?');
            }
        }
        return sb.toString();
    }

    private void object(ByteArrayOutputStream out, List<Integer> offsets, String body) {
        offsets.add(out.size());
        write(out, offsets.size() + " 0 obj\n" + body + "\nendobj\n");
    }

    private void write(ByteArrayOutputStream out, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.ISO_8859_1);
        out.write(bytes, 0, bytes.length);
    }
}
