package dev.jouzepe.p2pmarket.client;

import dev.jouzepe.p2pmarket.social.TradeOrderView;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

public final class ConfirmReservationScreen extends Screen {
    private final OrderDetailsScreen parent;
    private final long orderId;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    public ConfirmReservationScreen(OrderDetailsScreen parent, long orderId) {
        super(TradeI18n.component("p2p_market.confirm.title"));
        this.parent = parent;
        this.orderId = orderId;
    }

    @Override
    protected void init() {
        panelWidth = Math.max(340, Math.min(500, this.width - 60));
        panelHeight = 210;
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;

        int y = panelY + panelHeight - 42;
        this.addRenderableWidget(Button.builder(TradeI18n.component("p2p_market.button.back"), button -> this.minecraft.gui.setScreen(parent))
                .bounds(panelX + 18, y, 90, 20).build());
        this.addRenderableWidget(Button.builder(TradeI18n.component("p2p_market.button.confirm"), button -> {
                    TradeClientActions.reserve(orderId);
                    this.minecraft.gui.setScreen(parent.marketParent());
                })
                .bounds(panelX + panelWidth - 108, y, 90, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int right = panelX + panelWidth;
        int bottom = panelY + panelHeight;
        graphics.fill(panelX + 5, panelY + 6, right + 5, bottom + 6, 0x88000000);
        graphics.fill(panelX, panelY, right, bottom, 0xFFE3E3E3);
        graphics.outline(panelX, panelY, panelWidth, panelHeight, 0xFF111111);
        graphics.fill(panelX + 4, panelY + 4, right - 4, bottom - 4, 0xFF111318);
        graphics.fill(panelX + 5, panelY + 5, right - 5, panelY + 43, 0xFF2B2F36);

        TradeOrderView order = ClientMarketState.find(orderId);
        graphics.text(this.font, TradeI18n.text("p2p_market.confirm.title"), panelX + 16, panelY + 14, 0xFFFFFFFF, true);
        if (order == null) {
            graphics.text(this.font, TradeI18n.text("p2p_market.confirm.unavailable"), panelX + 20, panelY + 69, 0xFFFFB0B0, false);
            super.extractRenderState(graphics, mouseX, mouseY, delta);
            return;
        }

        String actionKey = "SELL".equals(order.type)
                ? "p2p_market.confirm.action.buy_from"
                : "p2p_market.confirm.action.sell_to";
        graphics.text(this.font, TradeI18n.text("p2p_market.confirm.question", order.id, TradeI18n.text(actionKey), order.ownerName), panelX + 20, panelY + 68, 0xFFFFFFFF, false);
        graphics.text(this.font, TradeI18n.text("p2p_market.confirm.summary", TradeFormat.itemName(order.itemName), order.amount, order.totalPrice), panelX + 20, panelY + 91, 0xFFD7DBE2, false);
        graphics.text(this.font, TradeI18n.text("p2p_market.confirm.timeout"), panelX + 20, panelY + 118, 0xFFFFD27A, false);
        graphics.text(this.font, TradeI18n.text("p2p_market.confirm.manual_trade"), panelX + 20, panelY + 137, 0xFFA7ADB8, false);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }
}
