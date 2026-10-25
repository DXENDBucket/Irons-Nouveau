package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.enchanting_apparatus.ArmorUpgradeRecipe;
import dev.ironsnouveau.armor.ArmorFusion;
import dev.ironsnouveau.config.SpellLevelConfig;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("irons_nouveau_armor") @PrefixGameTestTemplate(false)
public final class ArmorFusionGameTests {
    @GameTest(template="empty") public static void nativeAnvilAndPolicies(GameTestHelper h) {
        ArmorFusionCases.anvil(h, ForgeSmokeTests.player(h));
    }
    @GameTest(template="empty") public static void nativeInfusionUpgradeAndSync(GameTestHelper h) {
        ArmorFusionCases.infusionAndUpgrade(h, ForgeSmokeTests.player(h),
                s -> new ArmorUpgradeRecipe(List.of(), 0, 1).getResult(List.of(), s, null),
                s -> {
                    var saved = ItemStack.of(s.save(new net.minecraft.nbt.CompoundTag()));
                    var buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
                    try { buf.writeItem(saved); return buf.readItem(); }
                    finally { buf.release(); }
                });
    }
    private record Stat(String attribute, double amount, int operation) {}
    private static List<Stat> stats(ItemStack s) {
        var result = new ArrayList<Stat>();
        s.getAttributeModifiers(ArmorFusion.slot(s)).forEach((a,m) -> result.add(new Stat(BuiltInRegistries.ATTRIBUTE.getKey(a).toString(), m.getAmount(), m.getOperation().ordinal())));
        return result;
    }
    @GameTest(template="empty") public static void attributesAndTierToughness(GameTestHelper h) {
        ArmorFusionCases.enabled(() -> {
            double old = SpellLevelConfig.ARS_ARMOR_TOUGHNESS_PER_TIER.get();
            try {
                SpellLevelConfig.ARS_ARMOR_TOUGHNESS_PER_TIER.set(0.0);
                for (var type : List.of(ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS)) {
                    var source = ArmorFusionCases.ars(type); var donor = ArmorFusionCases.iron(type);
                    var result = ArmorFusion.fuse(source, donor);
                    var expected = new ArrayList<>(stats(source));
                    stats(donor).stream().filter(s -> !s.attribute.equals("minecraft:generic.armor") && !s.attribute.equals("minecraft:generic.armor_toughness"))
                            .forEach(expected::add);
                    var actual = stats(result);
                    h.assertTrue(actual.size() == expected.size() && actual.containsAll(expected), "Only non-armor donor attributes are added: " + type);
                    SpellLevelConfig.ARS_ARMOR_TOUGHNESS_PER_TIER.set(1.0);
                    for (int tier=0; tier<3; tier++) {
                        var upgraded = tier == 0 ? result : new ArmorUpgradeRecipe(List.of(), 0, tier).getResult(List.of(), result.copy(), null);
                        double expectedBonus = tier+1;
                        var ids = new HashSet<String>();
                        upgraded.getAttributeModifiers(ArmorFusion.slot(upgraded)).forEach((a,m) -> {
                            h.assertTrue(ids.add(BuiltInRegistries.ATTRIBUTE.getKey(a)+"/"+m.getId()), "Modifier IDs do not collide");
                            if (m.getName().contains("ars_toughness/")) h.assertTrue(a == Attributes.ARMOR_TOUGHNESS && m.getAmount() == expectedBonus, "Live tier toughness");
                        });
                        h.assertTrue(ArmorFusion.toughness(upgraded) == expectedBonus, "Displayed tier controls bonus");
                    }
                    SpellLevelConfig.ARS_ARMOR_TOUGHNESS_PER_TIER.set(0.0);
                }
            } finally { SpellLevelConfig.ARS_ARMOR_TOUGHNESS_PER_TIER.set(old); }
        });
        h.succeed();
    }
}
