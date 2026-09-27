package dev.jouzepe.p2pmarket.client;

public enum TradeTab {
    SELL("p2p_market.tab.sell", "p2p_market.heading.sell", "p2p_market.description.sell"),
    BUY("p2p_market.tab.buy", "p2p_market.heading.buy", "p2p_market.description.buy"),
    MY_ORDERS("p2p_market.tab.my_orders", "p2p_market.heading.my_orders", "p2p_market.description.my_orders");

    private final String labelKey;
    private final String headingKey;
    private final String descriptionKey;

    TradeTab(String labelKey, String headingKey, String descriptionKey) {
        this.labelKey = labelKey;
        this.headingKey = headingKey;
        this.descriptionKey = descriptionKey;
    }

    public String label() {
        return TradeI18n.text(labelKey);
    }

    public String heading() {
        return TradeI18n.text(headingKey);
    }

    public String description() {
        return TradeI18n.text(descriptionKey);
    }
}
