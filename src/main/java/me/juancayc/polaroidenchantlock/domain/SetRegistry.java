package me.juancayc.polaroidenchantlock.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Every locked set, and the one question the listeners ask on every anvil preview: is this
 * reference locked?
 *
 * <p>That question is a single {@code HashSet} lookup. A piece may sit in several sets, so the
 * lookup set is the union of all of them and is rebuilt whenever a set changes, never patched.
 *
 * <p>Reads never lock: the whole state is one immutable snapshot behind a {@code volatile}
 * field, swapped on write. Listeners on different region threads (Folia) therefore always see a
 * complete registry, old or new, and never a half-updated one.
 *
 * <p>Set names are unique ignoring case, so "Dragon" and "dragon" cannot coexist.
 */
public final class SetRegistry {

    private record Snapshot(Map<String, LockedSet> byKey, Set<String> locked) {

        static final Snapshot EMPTY = new Snapshot(Map.of(), Set.of());

        static Snapshot of(Map<String, LockedSet> byKey) {
            Set<String> locked = new HashSet<>();
            for (LockedSet set : byKey.values()) {
                locked.addAll(set.pieces());
            }
            return new Snapshot(byKey, locked);
        }
    }

    private volatile Snapshot snapshot = Snapshot.EMPTY;

    /** Whether any set holds this reference. {@code null} — an item no hook claims — never is. */
    public boolean isLocked(String reference) {
        return reference != null && snapshot.locked.contains(reference);
    }

    /** Whether nothing at all is locked, so a listener can skip resolving the item's id. */
    public boolean isEmpty() {
        return snapshot.locked.isEmpty();
    }

    public boolean contains(String name) {
        return snapshot.byKey.containsKey(key(name));
    }

    public Optional<LockedSet> find(String name) {
        return Optional.ofNullable(snapshot.byKey.get(key(name)));
    }

    /** Every set, in the order they were created. */
    public List<LockedSet> sets() {
        return List.copyOf(snapshot.byKey.values());
    }

    /** Replaces everything, as a reload does. A later set whose name repeats an earlier one is dropped. */
    public synchronized void replaceAll(Collection<LockedSet> sets) {
        Map<String, LockedSet> byKey = new LinkedHashMap<>();
        for (LockedSet set : sets) {
            byKey.putIfAbsent(key(set.name()), set);
        }
        snapshot = Snapshot.of(byKey);
    }

    /** Adds a new set. Returns false, changing nothing, when the name is taken. */
    public synchronized boolean add(LockedSet set) {
        if (contains(set.name())) {
            return false;
        }
        Map<String, LockedSet> byKey = new LinkedHashMap<>(snapshot.byKey);
        byKey.put(key(set.name()), set);
        snapshot = Snapshot.of(byKey);
        return true;
    }

    /**
     * Replaces the pieces of an existing set, keeping its position and the spelling of its name.
     * Returns false, changing nothing, when no set has that name.
     */
    public synchronized boolean update(String name, List<String> pieces) {
        LockedSet current = snapshot.byKey.get(key(name));
        if (current == null) {
            return false;
        }
        Map<String, LockedSet> byKey = new LinkedHashMap<>(snapshot.byKey);
        byKey.put(key(name), new LockedSet(current.name(), new ArrayList<>(pieces)));
        snapshot = Snapshot.of(byKey);
        return true;
    }

    /** Removes a set. Returns false when no set has that name. */
    public synchronized boolean remove(String name) {
        if (!contains(name)) {
            return false;
        }
        Map<String, LockedSet> byKey = new LinkedHashMap<>(snapshot.byKey);
        byKey.remove(key(name));
        snapshot = Snapshot.of(byKey);
        return true;
    }

    private static String key(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}
