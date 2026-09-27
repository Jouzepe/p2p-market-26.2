package dev.jouzepe.p2pmarket.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.jouzepe.p2pmarket.social.TradeOrderView;
import dev.jouzepe.p2pmarket.social.TradeSnapshotData;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public final class ClientMarketState {
    private static final Gson GSON = new GsonBuilder().create();
    private static TradeSnapshotData snapshot = new TradeSnapshotData();

    private ClientMarketState() {
    }

    public static synchronized void applyJson(String json) {
        TradeSnapshotData parsed = GSON.fromJson(json, TradeSnapshotData.class);
        if (parsed == null) parsed = new TradeSnapshotData();
        if (parsed.orders == null) parsed.orders = new ArrayList<>();
        snapshot = parsed;
    }

    public static synchronized TradeSnapshotData snapshot() {
        return snapshot;
    }

    public static synchronized List<TradeOrderView> orders() {
        return List.copyOf(snapshot.orders);
    }

    public static String playerUuid() {
        Minecraft client = Minecraft.getInstance();
        return client.player == null ? "" : client.player.getUUID().toString();
    }

    public static TradeOrderView find(long orderId) {
        for (TradeOrderView order : orders()) {
            if (order.id == orderId) return order;
        }
        return null;
    }
}
