package dev.jouzepe.p2pmarket.social;

import java.util.ArrayList;
import java.util.List;

public final class TradeSnapshotData {
    public long serverTime;
    public long reservationMillis;
    public List<TradeOrderView> orders = new ArrayList<>();

    public TradeSnapshotData() {
    }
}
