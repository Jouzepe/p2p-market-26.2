package dev.jouzepe.p2pmarket.social;

public final class SocialOrder {
    public long id;
    public SocialOrderType type;
    public String ownerUuid;
    public String ownerName;

    // Human-readable free-form item name (1..16 chars). No Minecraft item registry binding.
    public String itemName;

    // Legacy v8 field kept only so existing legacy market files can migrate automatically.
    public String itemId;

    public int amount;
    public long pricePerItem;
    public long createdAt;
    public SocialOrderStatus status = SocialOrderStatus.AVAILABLE;
    public String reservedByUuid;
    public String reservedByName;
    public long reservedUntil;
    public long completedAt;

    public SocialOrder() {
    }

    public boolean reservationExpired(long now) {
        return status == SocialOrderStatus.RESERVED && reservedUntil > 0L && reservedUntil <= now;
    }

    public void clearReservation() {
        status = SocialOrderStatus.AVAILABLE;
        reservedByUuid = null;
        reservedByName = null;
        reservedUntil = 0L;
    }
}
