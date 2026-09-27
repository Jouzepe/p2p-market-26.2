package dev.jouzepe.p2pmarket.social;

import dev.jouzepe.p2pmarket.P2PMarketMod;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class SocialMarketService {
    public static final long RESERVATION_MILLIS = 10L * 60L * 1000L;
    private static final int MAX_ACTIVE_ORDERS_PER_PLAYER = 50;
    private static final int MAX_ITEM_NAME_LENGTH = 16;

    private final MinecraftServer server;
    private final SocialMarketStorage storage;
    private final SocialMarketState state;

    private SocialMarketService(MinecraftServer server, SocialMarketStorage storage, SocialMarketState state) {
        this.server = server;
        this.storage = storage;
        this.state = state;
    }

    public static SocialMarketService open(MinecraftServer server) {
        Path dataDir = server.getWorldPath(LevelResource.ROOT).resolve("data");
        Path file = dataDir.resolve("p2p_market.json");
        Path legacyFile = dataDir.resolve("perfomarket_social.json");
        SocialMarketStorage storage = new SocialMarketStorage(file);
        SocialMarketState state;
        try {
            if (!Files.exists(file) && Files.exists(legacyFile)) {
                state = new SocialMarketStorage(legacyFile).load();
                storage.save(state);
                P2PMarketMod.LOGGER.info("Migrated legacy market data from {} to {}", legacyFile, file);
            } else {
                state = storage.load();
            }
        } catch (Exception e) {
            P2PMarketMod.LOGGER.error("Failed to load social market state {}, starting empty", file, e);
            state = new SocialMarketState();
        }
        SocialMarketService service = new SocialMarketService(server, storage, state);
        service.expireReservations(System.currentTimeMillis());
        P2PMarketMod.LOGGER.info("P2P Market social market loaded: {} orders, file={}", state.orders.size(), file);
        return service;
    }

    public synchronized SocialOrder create(ServerPlayer player, SocialOrderType type, String itemName, int amount, long pricePerItem) throws IOException {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(type, "type");
        String normalizedItemName = validateItemName(itemName);
        if (amount < 1 || amount > 100_000) throw new IllegalArgumentException("Количество должно быть от 1 до 100000.");
        if (pricePerItem < 1 || pricePerItem > 1_000_000_000L) throw new IllegalArgumentException("Цена должна быть от 1 до 1000000000.");
        Math.multiplyExact((long) amount, pricePerItem);

        expireReservations(System.currentTimeMillis());
        long activeOwned = state.orders.stream()
                .filter(Objects::nonNull)
                .filter(order -> player.getUUID().toString().equals(order.ownerUuid))
                .filter(order -> order.status == SocialOrderStatus.AVAILABLE || order.status == SocialOrderStatus.RESERVED)
                .count();
        if (activeOwned >= MAX_ACTIVE_ORDERS_PER_PLAYER) {
            throw new IllegalStateException("У вас слишком много активных ордеров.");
        }

        SocialOrder order = new SocialOrder();
        order.id = state.nextId++;
        order.type = type;
        order.ownerUuid = player.getUUID().toString();
        order.ownerName = player.getName().getString();
        order.itemName = normalizedItemName;
        order.amount = amount;
        order.pricePerItem = pricePerItem;
        order.createdAt = System.currentTimeMillis();
        order.status = SocialOrderStatus.AVAILABLE;
        state.orders.add(order);
        save();
        return order;
    }

    public synchronized SocialOrder reserve(ServerPlayer player, long orderId) throws IOException {
        long now = System.currentTimeMillis();
        expireReservations(now);
        SocialOrder order = requireOrder(orderId);
        if (order.status != SocialOrderStatus.AVAILABLE) {
            throw new IllegalStateException("Этот ордер уже недоступен.");
        }
        if (player.getUUID().toString().equals(order.ownerUuid)) {
            throw new IllegalStateException("Нельзя резервировать собственный ордер.");
        }
        order.status = SocialOrderStatus.RESERVED;
        order.reservedByUuid = player.getUUID().toString();
        order.reservedByName = player.getName().getString();
        order.reservedUntil = now + RESERVATION_MILLIS;
        save();

        player.sendSystemMessage(Component.literal("[P2P Market] Ордер #" + order.id + " зарезервирован на 10 минут. Свяжитесь с " + order.ownerName + " через /tell " + order.ownerName + "."));
        ServerPlayer owner = findOnline(order.ownerUuid);
        if (owner != null) {
            owner.sendSystemMessage(Component.literal("[P2P Market] " + player.getName().getString() + " зарезервировал ваш ордер #" + order.id + ". Свяжитесь через /tell " + player.getName().getString() + "."));
        }
        return order;
    }

    public synchronized SocialOrder releaseReservation(ServerPlayer player, long orderId) throws IOException {
        long now = System.currentTimeMillis();
        expireReservations(now);
        SocialOrder order = requireOrder(orderId);
        if (order.status != SocialOrderStatus.RESERVED) {
            throw new IllegalStateException("Этот ордер не зарезервирован.");
        }
        String playerUuid = player.getUUID().toString();
        boolean owner = playerUuid.equals(order.ownerUuid);
        boolean reserver = playerUuid.equals(order.reservedByUuid);
        if (!owner && !reserver) throw new IllegalStateException("Вы не можете снять этот резерв.");

        String previousReserver = order.reservedByName;
        order.clearReservation();
        save();
        player.sendSystemMessage(Component.literal("[P2P Market] Резерв ордера #" + order.id + " снят."));
        if (owner && previousReserver != null) {
            ServerPlayer reservedPlayer = findOnlineByName(previousReserver);
            if (reservedPlayer != null && reservedPlayer != player) {
                reservedPlayer.sendSystemMessage(Component.literal("[P2P Market] Владелец снял резерв с ордера #" + order.id + "."));
            }
        }
        return order;
    }

    public synchronized SocialOrder complete(ServerPlayer player, long orderId) throws IOException {
        long now = System.currentTimeMillis();
        expireReservations(now);
        SocialOrder order = requireOrder(orderId);
        if (!player.getUUID().toString().equals(order.ownerUuid)) {
            throw new IllegalStateException("Завершить сделку может только владелец ордера.");
        }
        if (order.status != SocialOrderStatus.RESERVED) {
            throw new IllegalStateException("Перед завершением сделки ордер должен быть зарезервирован.");
        }
        String reserverName = order.reservedByName;
        String reserverUuid = order.reservedByUuid;
        order.status = SocialOrderStatus.COMPLETED;
        order.completedAt = now;
        order.reservedUntil = 0L;
        save();

        player.sendSystemMessage(Component.literal("[P2P Market] Ордер #" + order.id + " отмечен как завершённый."));
        ServerPlayer reserver = findOnline(reserverUuid);
        if (reserver != null) {
            reserver.sendSystemMessage(Component.literal("[P2P Market] " + order.ownerName + " завершил сделку по ордеру #" + order.id + "."));
        } else if (reserverName != null) {
            P2PMarketMod.LOGGER.info("Completed order #{} with offline reserver {}", order.id, reserverName);
        }
        return order;
    }

    public synchronized SocialOrder cancelOrder(ServerPlayer player, long orderId) throws IOException {
        expireReservations(System.currentTimeMillis());
        SocialOrder order = requireOrder(orderId);
        if (!player.getUUID().toString().equals(order.ownerUuid)) {
            throw new IllegalStateException("Можно отменить только собственный ордер.");
        }
        if (order.status == SocialOrderStatus.COMPLETED || order.status == SocialOrderStatus.CANCELLED) {
            throw new IllegalStateException("Этот ордер уже закрыт.");
        }
        String reserverUuid = order.reservedByUuid;
        order.status = SocialOrderStatus.CANCELLED;
        order.reservedUntil = 0L;
        save();
        player.sendSystemMessage(Component.literal("[P2P Market] Ордер #" + order.id + " отменён."));
        ServerPlayer reserver = findOnline(reserverUuid);
        if (reserver != null) {
            reserver.sendSystemMessage(Component.literal("[P2P Market] Ордер #" + order.id + " был отменён игроком " + order.ownerName + "."));
        }
        return order;
    }

    public synchronized TradeSnapshotData snapshot() {
        expireReservations(System.currentTimeMillis());
        TradeSnapshotData snapshot = new TradeSnapshotData();
        snapshot.serverTime = System.currentTimeMillis();
        snapshot.reservationMillis = RESERVATION_MILLIS;
        List<SocialOrder> sorted = new ArrayList<>(state.orders);
        sorted.removeIf(Objects::isNull);
        sorted.sort(Comparator.comparingLong((SocialOrder order) -> order.createdAt).reversed());
        for (SocialOrder order : sorted) {
            if (order.status == SocialOrderStatus.CANCELLED) continue;
            snapshot.orders.add(TradeOrderView.from(order));
            if (snapshot.orders.size() >= 100) break;
        }
        return snapshot;
    }

    public synchronized List<ServerPlayer> onlinePlayers() {
        return List.copyOf(server.getPlayerList().getPlayers());
    }

    public synchronized void saveQuietly() {
        try {
            save();
        } catch (Exception e) {
            P2PMarketMod.LOGGER.error("Failed to save P2P Market social market", e);
        }
    }

    public synchronized void save() throws IOException {
        storage.save(state);
    }

    private boolean expireReservations(long now) {
        boolean changed = false;
        for (SocialOrder order : state.orders) {
            if (order != null && order.reservationExpired(now)) {
                order.clearReservation();
                changed = true;
            }
        }
        if (changed) saveQuietly();
        return changed;
    }

    private SocialOrder requireOrder(long orderId) {
        return state.orders.stream()
                .filter(Objects::nonNull)
                .filter(order -> order.id == orderId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ордер #" + orderId + " не существует."));
    }

    private static String validateItemName(String raw) {
        if (raw == null) throw new IllegalArgumentException("Введите название товара.");
        String value = raw.trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Введите название товара.");
        if (value.length() > MAX_ITEM_NAME_LENGTH) throw new IllegalArgumentException("Название товара: максимум 16 символов.");
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                throw new IllegalArgumentException("Название товара содержит недопустимые символы.");
            }
        }
        return value;
    }

    private ServerPlayer findOnline(String uuid) {
        if (uuid == null) return null;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (uuid.equals(player.getUUID().toString())) return player;
        }
        return null;
    }

    private ServerPlayer findOnlineByName(String name) {
        if (name == null) return null;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (name.equalsIgnoreCase(player.getName().getString())) return player;
        }
        return null;
    }
}
