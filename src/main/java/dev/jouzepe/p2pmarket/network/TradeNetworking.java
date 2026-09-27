package dev.jouzepe.p2pmarket.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.jouzepe.p2pmarket.P2PMarketMod;
import dev.jouzepe.p2pmarket.social.SocialMarketService;
import dev.jouzepe.p2pmarket.social.SocialOrderType;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class TradeNetworking {
    private static final Gson GSON = new GsonBuilder().create();

    private TradeNetworking() {
    }

    public static void registerCommon() {
        PayloadTypeRegistry.serverboundPlay().register(TradeActionPayload.TYPE, TradeActionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TradeSnapshotPayload.TYPE, TradeSnapshotPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(TradeActionPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            try {
                boolean marketChanged = handle(player, payload.json());
                if (marketChanged) {
                    broadcastSnapshot();
                } else {
                    sendSnapshot(player);
                }
            } catch (Exception e) {
                P2PMarketMod.LOGGER.warn("Trade action failed for {}: {}", player.getName().getString(), e.getMessage());
                player.sendSystemMessage(Component.literal("[P2P Market] " + readableMessage(e)));
                sendSnapshot(player);
            }
        });
    }

    public static void sendSnapshot(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, TradeSnapshotPayload.TYPE)) return;
        SocialMarketService service = P2PMarketMod.socialService();
        String json = GSON.toJson(service.snapshot());
        ServerPlayNetworking.send(player, new TradeSnapshotPayload(json));
    }

    public static void broadcastSnapshot() {
        SocialMarketService service = P2PMarketMod.socialService();
        String json = GSON.toJson(service.snapshot());
        for (ServerPlayer player : service.onlinePlayers()) {
            if (ServerPlayNetworking.canSend(player, TradeSnapshotPayload.TYPE)) {
                ServerPlayNetworking.send(player, new TradeSnapshotPayload(json));
            }
        }
    }

    private static boolean handle(ServerPlayer player, String json) throws Exception {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        String action = requiredString(root, "action");
        SocialMarketService service = P2PMarketMod.socialService();

        switch (action) {
            case "snapshot" -> {
                return false;
            }
            case "create" -> {
                SocialOrderType type = SocialOrderType.valueOf(requiredString(root, "type"));
                String itemName = requiredString(root, "itemName");
                int amount = root.get("amount").getAsInt();
                long price = root.get("price").getAsLong();
                var order = service.create(player, type, itemName, amount, price);
                player.sendSystemMessage(Component.literal("[P2P Market] Создан ордер #" + order.id + "."));
                return true;
            }
            case "reserve" -> {
                service.reserve(player, requiredLong(root, "orderId"));
                return true;
            }
            case "release" -> {
                service.releaseReservation(player, requiredLong(root, "orderId"));
                return true;
            }
            case "complete" -> {
                service.complete(player, requiredLong(root, "orderId"));
                return true;
            }
            case "cancel" -> {
                service.cancelOrder(player, requiredLong(root, "orderId"));
                return true;
            }
            default -> throw new IllegalArgumentException("Неизвестное действие терминала: " + action);
        }
    }

    private static String requiredString(JsonObject root, String key) {
        if (!root.has(key) || root.get(key).isJsonNull()) throw new IllegalArgumentException("Отсутствует поле: " + key);
        return root.get(key).getAsString();
    }

    private static long requiredLong(JsonObject root, String key) {
        if (!root.has(key) || root.get(key).isJsonNull()) throw new IllegalArgumentException("Отсутствует поле: " + key);
        return root.get(key).getAsLong();
    }

    private static String readableMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) return "Не удалось выполнить действие.";
        return message;
    }
}
