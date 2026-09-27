package dev.jouzepe.p2pmarket.social;

public final class TradeOrderView {
    public long id;
    public String type;
    public String ownerUuid;
    public String ownerName;
    public String itemName;
    public int amount;
    public long pricePerItem;
    public long totalPrice;
    public long createdAt;
    public String status;
    public String reservedByUuid;
    public String reservedByName;
    public long reservedUntil;

    public TradeOrderView() {
    }

    public static TradeOrderView from(SocialOrder order) {
        TradeOrderView view = new TradeOrderView();
        view.id = order.id;
        view.type = order.type.name();
        view.ownerUuid = order.ownerUuid;
        view.ownerName = order.ownerName;
        view.itemName = order.itemName == null ? "" : order.itemName;
        view.amount = order.amount;
        view.pricePerItem = order.pricePerItem;
        view.totalPrice = Math.multiplyExact((long) order.amount, order.pricePerItem);
        view.createdAt = order.createdAt;
        view.status = order.status.name();
        view.reservedByUuid = order.reservedByUuid;
        view.reservedByName = order.reservedByName;
        view.reservedUntil = order.reservedUntil;
        return view;
    }
}
