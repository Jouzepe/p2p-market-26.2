package dev.jouzepe.p2pmarket.client;

import dev.jouzepe.p2pmarket.network.TradeSnapshotPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class P2PMarketClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(TradeSnapshotPayload.TYPE, (payload, context) -> {
            ClientMarketState.applyJson(payload.json());
            Minecraft client = context.client();
            Screen screen = client.gui.screen();
            if (screen instanceof TradeScreen tradeScreen) {
                tradeScreen.onMarketUpdated();
            } else if (screen instanceof OrderDetailsScreen detailsScreen) {
                detailsScreen.onMarketUpdated();
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) ->
                dispatcher.register(ClientCommands.literal("trade").executes(context -> {
                    Minecraft client = Minecraft.getInstance();
                    client.execute(() -> client.gui.setScreen(new TradeScreen()));
                    return 1;
                }))
        );
    }
}
