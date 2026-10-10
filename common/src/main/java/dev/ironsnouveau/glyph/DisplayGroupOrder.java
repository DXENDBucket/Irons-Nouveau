package dev.ironsnouveau.glyph;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/** Keep semantic categories contiguous even when addons share numeric sort indices. */
public final class DisplayGroupOrder {
    private DisplayGroupOrder() {}
    public static <T, K> Comparator<T> comparator(List<T> values, Comparator<T> original,
            Function<T, K> group, ToIntFunction<T> rank, ToIntFunction<K> categoryOrder, Predicate<T> deferred) {
        var ranks = new HashMap<K, Integer>();
        for (var value : values) ranks.merge(group.apply(value), rank.applyAsInt(value), Math::min);
        return Comparator.<T>comparingInt(value -> ranks.get(group.apply(value)))
                .thenComparingInt(value -> categoryOrder.applyAsInt(group.apply(value)))
                .thenComparingInt(value -> deferred.test(value) ? 1 : 0)
                .thenComparing(original);
    }
}
