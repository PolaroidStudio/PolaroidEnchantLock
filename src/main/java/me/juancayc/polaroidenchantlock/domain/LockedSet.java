package me.juancayc.polaroidenchantlock.domain;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * A named group of locked pieces. The pieces are canonical {@link ItemReference} strings, in the
 * order the admin added them and without repeats.
 */
public record LockedSet(String name, List<String> pieces) {

    public LockedSet {
        pieces = List.copyOf(new LinkedHashSet<>(pieces));
    }

    public int size() {
        return pieces.size();
    }
}
