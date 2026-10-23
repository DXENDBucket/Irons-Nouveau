package dev.ironsnouveau.compat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Informational only; optional-mod detection stays in the loader-specific login handlers. */
public final class AnsCoexistenceNotice {
    private static final String SEEN = "irons_nouveau:ans_overlap_notice_v1";

    public static void onLogin(ServerPlayer player, boolean arsNSpellsLoaded) {
        if (!arsNSpellsLoaded) return;
        // Forge/NeoForge preserve PlayerPersisted across death. Player saves are world-local,
        // so reconnects, dimension changes and respawns cannot repeat this world's notice.
        var persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (persisted.getBoolean(SEEN)) return;
        player.sendSystemMessage(Component.translatable("irons_nouveau.compat.ans_overlap").withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.translatable("irons_nouveau.compat.ans_disable_glyphs").withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.translatable("irons_nouveau.compat.ans_server_setting").withStyle(ChatFormatting.GRAY));
        persisted.putBoolean(SEEN, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    private AnsCoexistenceNotice() {}
}
