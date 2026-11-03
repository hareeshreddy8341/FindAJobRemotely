package com.findajobremotely.core;

import java.nio.charset.StandardCharsets;

/** Small, dependency-free helpers for sanitising user-supplied text. */
public final class TextClean {

    private TextClean() {
    }

    /** Single-line text: control characters become spaces, whitespace is collapsed, ends are trimmed. */
    public static String line(String s) {
        if (s == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            sb.append(Character.isISOControl(ch) ? ' ' : ch);
        }
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    /** Multi-line text: keeps newlines and tabs, drops every other control character. */
    public static String block(String s) {
        if (s == null) {
            return null;
        }
        String n = s.replace("\r\n", "\n").replace('\r', '\n');
        StringBuilder sb = new StringBuilder(n.length());
        for (int i = 0; i < n.length(); i++) {
            char ch = n.charAt(i);
            if (ch == '\n' || ch == '\t' || !Character.isISOControl(ch)) {
                sb.append(ch);
            }
        }
        return sb.toString().trim();
    }

    /** Escapes regex metacharacters so user input can be used as a literal inside a MongoDB $regex. */
    public static String escapeRegex(String s) {
        return s.replaceAll("([\\\\.^$|?*+()\\[\\]{}])", "\\\\$1");
    }

    /** Reduces an uploaded file name to a harmless display name. */
    public static String safeFileName(String name) {
        String base = name == null ? "" : name;
        int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        base = base.replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        if (base.length() > 100) {
            base = base.substring(base.length() - 100);
        }
        if (base.isEmpty() || base.equals(".pdf")) {
            return "resume.pdf";
        }
        if (!base.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) {
            base = base + ".pdf";
        }
        return base;
    }

    /** True when the bytes start with the PDF magic number. */
    public static boolean isPdf(byte[] data) {
        if (data == null || data.length < 5) {
            return false;
        }
        byte[] magic = "%PDF-".getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i < magic.length; i++) {
            if (data[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }
}
