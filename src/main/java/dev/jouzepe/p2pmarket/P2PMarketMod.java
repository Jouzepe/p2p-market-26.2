package dev.jouzepe.p2pmarket;

import dev.jouzepe.p2pmarket.network.TradeNetworking;
import dev.jouzepe.p2pmarket.social.SocialMarketService;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class P2PMarketMod implements ModInitializer {
    public static final String MOD_ID = "p2p_market";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static volatile SocialMarketService socialService;

    @Override
    public void onInitialize() {
        TradeNetworking.registerCommon();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> socialService = SocialMarketService.open(server));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            SocialMarketService current = socialService;
            if (current != null) current.saveQuietly();
            socialService = null;
        });
        LOGGER.info("P2P Market initialized with player-to-player trade board");
    }

    public static SocialMarketService socialService() {
        SocialMarketService current = socialService;
        if (current == null) throw new IllegalStateException("Social market service is not ready");
        return current;
    }
}
