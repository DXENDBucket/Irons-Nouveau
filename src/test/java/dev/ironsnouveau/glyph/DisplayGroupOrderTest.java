package dev.ironsnouveau.glyph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DisplayGroupOrderTest {
    record Glyph(String name, int category, int rank, boolean iron) {}
    @Test void addonSubformWithEffectRankCannotCaptureIronEffects() {
        var effect = new Glyph("A native effect", 1, 5, false);
        var addonEffect = new Glyph("B addon effect", 1, 5, false);
        var ironEffect = new Glyph("A iron effect", 1, 5, true);
        // NEG Reverse Direction implements IPropagator but inherits AbstractEffect's rank 5.
        var reverse = new Glyph("C reverse direction subform", 5, 5, false);
        var propagate = new Glyph("D propagate subform", 5, 8, false);
        var augment = new Glyph("Z native augment", 2, 3, false);
        var ironAugment = new Glyph("A iron augment", 2, 3, true);
        var values = new ArrayList<>(List.of(ironEffect, reverse, ironAugment, propagate, addonEffect, augment, effect));
        var original = Comparator.comparingInt(Glyph::rank).thenComparing(Glyph::name);
        values.sort(DisplayGroupOrder.comparator(values, original, Glyph::category, Glyph::rank, x -> x, Glyph::iron));
        assertEquals(List.of(augment, ironAugment, effect, addonEffect, ironEffect, reverse, propagate), values);
    }
    @Test void effectsRemainBeforeSameRankSubformWhenNamesOrSearchResultsChange() {
        var ironEffect = new Glyph("Z iron heal", 1, 5, true);
        var reverse = new Glyph("A reverse direction", 5, 5, false);
        var values = new ArrayList<>(List.of(reverse, ironEffect));
        values.sort(DisplayGroupOrder.comparator(values, Comparator.comparing(Glyph::name),
                Glyph::category, Glyph::rank, x -> x, Glyph::iron));
        assertEquals(List.of(ironEffect, reverse), values);
    }
}
