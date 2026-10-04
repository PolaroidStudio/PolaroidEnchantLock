package me.juancayc.polaroidenchantlock.domain;

import java.util.Map;

/**
 * Substitutes {@code %token%} placeholders in a template.
 *
 * <p><b>Single pass, on purpose.</b> Chained {@code String.replace} calls re-scan text that earlier
 * replacements produced, so a value that happens to contain {@code %name%} would itself be
 * expanded by a later step. Here the template is walked once and a substituted value is appended
 * to the output and never looked at again: a value can only ever be text.
 */
public final class Placeholders {

    private Placeholders() {
    }

    /**
     * @param values token name (without the percent signs) to replacement; a token with no entry is
     *               left in the output untouched, percent signs included
     */
    public static String apply(String template, Map<String, String> values) {
        if (template == null) {
            return "";
        }
        if (values.isEmpty() || template.indexOf('%') < 0) {
            return template;
        }
        StringBuilder out = new StringBuilder(template.length() + 32);
        int cursor = 0;
        while (cursor < template.length()) {
            int open = template.indexOf('%', cursor);
            if (open < 0) {
                break;
            }
            int close = template.indexOf('%', open + 1);
            if (close < 0) {
                break;
            }
            String value = values.get(template.substring(open + 1, close));
            if (value == null) {
                // Not a known token: keep the first '%' and rescan from the second, which may
                // itself open a real token ("100% of %goal%").
                out.append(template, cursor, close);
                cursor = close;
            } else {
                out.append(template, cursor, open).append(value);
                cursor = close + 1;
            }
        }
        return out.append(template, cursor, template.length()).toString();
    }
}
