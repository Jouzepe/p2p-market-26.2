package dev.jouzepe.p2pmarket.client;

import com.google.gson.JsonObject;
import dev.jouzepe.p2pmarket.network.TradeActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class TradeClientActions {
    private TradeClientActions() {
    }

    public static void requestSnapshot() {
        send(base("snapshot"));
    }

    public static void create(String type, String itemName, int amount, long price) {
        JsonObject root = base("create");
        root.addProperty("type", type);
        root.addProperty("itemName", itemName);
        root.addProperty("amount", amount);
        root.addProperty("price", price);
        send(root);
    }

    public static void reserve(long orderId) {
        sendOrderAction("reserve", orderId);
    }

    public static void release(long orderId) {
        sendOrderAction("release", orderId);
    }

    public static void complete(long orderId) {
        sendOrderAction("complete", orderId);
    }

    public static void cancel(long orderId) {
        sendOrderAction("cancel", orderId);
    }

    private static void sendOrderAction(String action, long orderId) {
        JsonObject root = base(action);
        root.addProperty("orderId", orderId);
        send(root);
    }

    private static JsonObject base(String action) {
        JsonObject root = new JsonObject();
        root.addProperty("action", action);
        return root;
    }

    private static void send(JsonObject root) {
        ClientPlayNetworking.send(new TradeActionPayload(root.toString()));
    }
}
