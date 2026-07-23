package com.happy.simipkit.util;

/**
 * Utility for escaping strings to be safely embedded inside JavaScript
 * string literals within {@code <script>} blocks in JSP/HTML.
 * Prevents XSS from user-controlled data (e.g. client names containing
 * quotes, backslashes, or {@code </script>} sequences).
 */
public class JsStringUtil {

    /**
     * Escapes a string for safe embedding inside a JS string literal
     * (single-quoted or double-quoted) within a &lt;script&gt; block.
     *
     * Escapes: backslash, single quote, double quote, newline, carriage return,
     * tab, and the sequence "&lt;/script&gt;" to prevent tag breakout.
     *
     * @param input the raw string from database or user input
     * @return escaped string safe for JS string literal context, or empty string if input is null
     */
    public static String escape(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length() + 16);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '\'': sb.append("\\'");  break;
                case '"':  sb.append("\\\""); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                case '/':
                    // Prevent </script> breakout
                    if (i >= 1 && input.charAt(i - 1) == '<') {
                        sb.append("\\/");
                    } else {
                        sb.append(c);
                    }
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
}
