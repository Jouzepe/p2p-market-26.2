package dev.jouzepe.p2pmarket.client;

import dev.jouzepe.p2pmarket.social.TradeOrderView;

public final class TradeFormat {
    private TradeFormat() {
    }

    public static String itemName(String itemName) {
        if (itemName == null || itemName.isBlank()) return TradeI18n.text("p2p_market.item.unnamed");
        return itemName;
    }

    public static String status(String status) {
        return switch (status) {
            case "AVAILABLE" -> TradeI18n.text("p2p_market.status.available");
            case "RESERVED" -> TradeI18n.text("p2p_market.status.reserved_short");
            case "COMPLETED" -> TradeI18n.text("p2p_market.status.completed");
            case "CANCELLED" -> TradeI18n.text("p2p_market.status.cancelled");
            default -> status;
        };
    }

    public static String rowLabel(TradeOrderView order) {
        return TradeI18n.text(
                "p2p_market.order.row",
                order.id,
                itemName(order.itemName),
                order.amount,
                order.pricePerItem,
                order.ownerName,
                status(order.status)
        );
    }
}
