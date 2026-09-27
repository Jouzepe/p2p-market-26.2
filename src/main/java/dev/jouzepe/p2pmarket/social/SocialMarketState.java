package dev.jouzepe.p2pmarket.social;

import java.util.ArrayList;
import java.util.List;

public final class SocialMarketState {
    public long nextId = 1L;
    public List<SocialOrder> orders = new ArrayList<>();

    public void normalize() {
        if (orders == null) orders = new ArrayList<>();
        long maxId = 0L;
        for (SocialOrder order : orders) {
            if (order == null) continue;
            maxId = Math.max(maxId, order.id);
            if (order.status == null) order.status = SocialOrderStatus.AVAILABLE;

            // v8 -> v9 migration: old itemId becomes a display name.
            if ((order.itemName == null || order.itemName.isBlank()) && order.itemId != null && !order.itemId.isBlank()) {
                order.itemName = order.itemId;
            }
        }
        if (nextId <= maxId) nextId = maxId + 1L;
        if (nextId < 1L) nextId = 1L;
    }
}
