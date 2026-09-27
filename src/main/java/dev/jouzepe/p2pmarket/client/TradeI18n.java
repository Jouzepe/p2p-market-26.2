package dev.jouzepe.p2pmarket.client;

import net.minecraft.network.chat.Component;

/**
 * Small client-side translation helper.
 * Only en_us and ru_ru are shipped by P2P Market. Minecraft falls back to
 * the default en_us translations for every other selected language.
 */
public final class TradeI18n {
    private TradeI18n() {
    }

    public static Component component(String key, Object... args) {
        return Component.translatable(key, args);
    }

    public static String text(String key, Object... args) {
        return component(key, args).getString();
    }
}
