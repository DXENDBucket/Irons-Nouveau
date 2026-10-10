package dev.ironsnouveau.recipe;
import com.google.gson.JsonObject;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import java.util.stream.Stream;

public final class SpellScrollIngredient extends Ingredient {
    private final ResourceLocation spell;
    public SpellScrollIngredient(ResourceLocation spell) { super(Stream.empty()); this.spell = spell; }
    public static void register(IEventBus bus) {
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) ->
            event.enqueueWork(() -> CraftingHelper.register(dev.ironsnouveau.platform.Locations.id("irons_nouveau", "spell_scroll"), SERIALIZER)));
    }
    @Override public boolean test(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof Scroll)) return false;
        var container = ISpellContainer.get(stack);
        if (container == null) return false;
        var data = container.getSpellAtIndex(0);
        return data != null && data.getSpell() != SpellRegistry.none() && data.getLevel() > 0
                && data.getSpell().getSpellResource().equals(spell);
    }
    @Override public ItemStack[] getItems() {
        var nativeSpell = SpellRegistry.getSpell(spell);
        if (nativeSpell == SpellRegistry.none()) return new ItemStack[0];
        var example = new ItemStack(ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(nativeSpell, nativeSpell.getMinLevel(), example);
        return new ItemStack[]{example};
    }
    @Override public boolean isSimple() { return false; }
    @Override public boolean isEmpty() { return false; }
    @Override public JsonObject toJson() {
        var json = new JsonObject(); json.addProperty("type", "irons_nouveau:spell_scroll"); json.addProperty("spell", spell.toString()); return json;
    }
    @Override public IIngredientSerializer<? extends Ingredient> getSerializer() { return SERIALIZER; }
    private static final IIngredientSerializer<SpellScrollIngredient> SERIALIZER = new IIngredientSerializer<>() {
        public SpellScrollIngredient parse(FriendlyByteBuf buf) { return new SpellScrollIngredient(buf.readResourceLocation()); }
        public SpellScrollIngredient parse(JsonObject json) { return new SpellScrollIngredient(dev.ironsnouveau.platform.Locations.id(json.get("spell").getAsString())); }
        public void write(FriendlyByteBuf buf, SpellScrollIngredient value) { buf.writeResourceLocation(value.spell); }
    };
}
