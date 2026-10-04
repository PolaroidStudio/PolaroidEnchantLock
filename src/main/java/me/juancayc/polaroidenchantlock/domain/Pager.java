package me.juancayc.polaroidenchantlock.domain;

import java.util.List;

/** Splits a list into the pages a menu shows. There is always at least one page, even when empty. */
public final class Pager {

    private Pager() {
    }

    public static int pageCount(int items, int perPage) {
        return Math.max(1, (items + perPage - 1) / perPage);
    }

    /** The nearest valid page index, zero-based. */
    public static int clamp(int page, int items, int perPage) {
        return Math.max(0, Math.min(page, pageCount(items, perPage) - 1));
    }

    /** The entries of one page. {@code page} is clamped first, so a stale index never throws. */
    public static <T> List<T> slice(List<T> entries, int page, int perPage) {
        int from = clamp(page, entries.size(), perPage) * perPage;
        return entries.subList(Math.min(from, entries.size()), Math.min(from + perPage, entries.size()));
    }
}
