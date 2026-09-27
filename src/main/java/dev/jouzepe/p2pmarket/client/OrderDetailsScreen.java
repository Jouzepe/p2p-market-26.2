package dev.jouzepe.p2pmarket.client;

import dev.jouzepe.p2pmarket.social.TradeOrderView;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

public final class OrderDetailsScreen extends Screen {
    private static final int AUTO_REFRESH_TICKS = 100;

    private final TradeScreen marketParent;
    private final long orderId;
    private int refreshTicks;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    private Button reserveButton;
    private Button releaseButton;
    private Button completeButton;
    private Button cancelButton;
    private Button backButton;
    private Button refreshButton;

    public OrderDetailsScreen(TradeScreen marketParent, long orderId) {
        super(TradeI18n.component("p2p_market.details.title"));
        this.marketParent = marketParent;
        this.orderId = orderId;
    }

    @Override
    protected void init() {
        refreshTicks = 0;
        panelWidth = Math.max(360, Math.min(560, this.width - 48));
        panelHeight = Math.max(260, Math.min(340, this.height - 60));
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;

        int buttonY = panelY + panelHeight - 43;
        this.backButton = Button.builder(TradeI18n.component("p2p_market.button.back"), button -> this.minecraft.gui.setScreen(marketParent))
                .bounds(panelX + 18, buttonY, 72, 20).build();
        this.refreshButton = Button.builder(TradeI18n.component("p2p_market.button.refresh"), button -> TradeClientActions.requestSnapshot())
                .bounds(panelX + 96, buttonY, 76, 20).build();
        this.reserveButton = Button.builder(TradeI18n.component("p2p_market.button.reserve"), button ->
                        this.minecraft.gui.setScreen(new ConfirmReservationScreen(this, orderId)))
                .bounds(panelX + panelWidth - 142, buttonY, 124, 20).build();
        this.releaseButton = Button.builder(TradeI18n.component("p2p_market.button.release"), button -> TradeClientActions.release(orderId))
                .bounds(panelX + panelWidth - 272, buttonY, 124, 20).build();
        this.completeButton = Button.builder(TradeI18n.component("p2p_market.button.complete"), button -> TradeClientActions.complete(orderId))
                .bounds(panelX + panelWidth - 272, buttonY, 124, 20).build();
        this.cancelButton = Button.builder(TradeI18n.component("p2p_market.button.cancel_order"), button -> TradeClientActions.cancel(orderId))
                .bounds(panelX + panelWidth - 118, buttonY, 100, 20).build();

        this.addRenderableWidget(backButton);
        this.addRenderableWidget(refreshButton);
        this.addRenderableWidget(reserveButton);
        this.addRenderableWidget(releaseButton);
        this.addRenderableWidget(completeButton);
        this.addRenderableWidget(cancelButton);

        updateActions();
    }

    @Override
    public void tick() {
        super.tick();
        refreshTicks++;
        if (refreshTicks >= AUTO_REFRESH_TICKS) {
            refreshTicks = 0;
            TradeClientActions.requestSnapshot();
        }
    }

    private void updateActions() {
        TradeOrderView order = ClientMarketState.find(orderId);
        String me = ClientMarketState.playerUuid();
        boolean exists = order != null;
        boolean owner = exists && me.equals(order.ownerUuid);
        boolean reserver = exists && me.equals(order.reservedByUuid);
        boolean available = exists && "AVAILABLE".equals(order.status);
        boolean reserved = exists && "RESERVED".equals(order.status);
        boolean closed = exists && ("COMPLETED".equals(order.status) || "CANCELLED".equals(order.status));

        reserveButton.visible = exists && available && !owner;
        reserveButton.active = reserveButton.visible;

        releaseButton.visible = exists && reserved && (owner || reserver);
        releaseButton.active = releaseButton.visible;

        completeButton.visible = exists && reserved && owner;
        completeButton.active = completeButton.visible;

        cancelButton.visible = exists && owner && !closed;
        cancelButton.active = cancelButton.visible;

        // Keep action buttons clear of Back/Refresh. Cancel Order is intentionally
        // narrower so the full owner action row fits even in Russian.
        final int rightMargin = 18;
        final int gap = 6;
        final int actionWidth = 124;
        final int cancelWidth = 100;
        int cursor = panelX + panelWidth - rightMargin;

        if (completeButton.visible) {
            cursor -= actionWidth;
            completeButton.setX(cursor);
            cursor -= gap;
        }
        if (releaseButton.visible) {
            cursor -= actionWidth;
            releaseButton.setX(cursor);
            cursor -= gap;
        }
        if (reserveButton.visible) {
            cursor -= actionWidth;
            reserveButton.setX(cursor);
            cursor -= gap;
        }
        if (cancelButton.visible) {
            cursor -= cancelWidth;
            cancelButton.setX(cursor);
        }
    }

    public void onMarketUpdated() {
        refreshTicks = 0;
        TradeOrderView order = ClientMarketState.find(orderId);
        if (order == null) {
            marketParent.onMarketUpdated();
            this.minecraft.gui.setScreen(marketParent);
            return;
        }
        updateActions();
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
        graphics.horizontalLine(panelX + 5, right - 5, panelY + 43, 0xFF5C626C);

        TradeOrderView order = ClientMarketState.find(orderId);
        graphics.text(this.font, TradeI18n.text("p2p_market.details.title"), panelX + 16, panelY + 13, 0xFFFFFFFF, true);
        graphics.text(this.font, TradeI18n.text("p2p_market.details.subtitle"), panelX + 16, panelY + 27, 0xFFB8BDC7, false);

        if (order == null) {
            graphics.text(this.font, TradeI18n.text("p2p_market.details.unavailable"), panelX + 22, panelY + 70, 0xFFFFB0B0, false);
            super.extractRenderState(graphics, mouseX, mouseY, delta);
            return;
        }

        int x = panelX + 22;
        int y = panelY + 67;
        String type = "SELL".equals(order.type)
                ? TradeI18n.text("p2p_market.type.sell")
                : TradeI18n.text("p2p_market.type.buy");
        graphics.text(this.font, TradeI18n.text("p2p_market.details.order_header", order.id, type), x, y, 0xFFFFFFFF, true);
        graphics.text(this.font, TradeI18n.text("p2p_market.details.item", TradeFormat.itemName(order.itemName)), x, y + 24, 0xFFD7DBE2, false);
        graphics.text(this.font, TradeI18n.text("p2p_market.details.amount", order.amount), x, y + 43, 0xFFD7DBE2, false);
        graphics.text(this.font, TradeI18n.text("p2p_market.details.price_each", order.pricePerItem), x, y + 62, 0xFFD7DBE2, false);
        graphics.text(this.font, TradeI18n.text("p2p_market.details.total", order.totalPrice), x, y + 81, 0xFFD7DBE2, false);
        graphics.text(this.font, TradeI18n.text("p2p_market.details.player", order.ownerName), x, y + 100, 0xFFD7DBE2, false);
        graphics.text(this.font, TradeI18n.text("p2p_market.details.status", prettyStatus(order.status)), x, y + 119, statusColor(order.status), false);

        if ("RESERVED".equals(order.status)) {
            String me = ClientMarketState.playerUuid();
            if (me.equals(order.ownerUuid) && order.reservedByName != null) {
                graphics.text(this.font, TradeI18n.text("p2p_market.details.reserved_by", order.reservedByName), x, y + 142, 0xFFFFD27A, false);
                graphics.text(this.font, TradeI18n.text("p2p_market.details.contact", order.reservedByName), x, y + 161, 0xFFA7ADB8, false);
            } else if (me.equals(order.reservedByUuid)) {
                graphics.text(this.font, TradeI18n.text("p2p_market.details.you_reserved"), x, y + 142, 0xFFFFD27A, false);
                graphics.text(this.font, TradeI18n.text("p2p_market.details.contact_owner", order.ownerName), x, y + 161, 0xFFA7ADB8, false);
            } else {
                graphics.text(this.font, TradeI18n.text("p2p_market.details.reserved_other"), x, y + 142, 0xFFA7ADB8, false);
            }
        } else if ("AVAILABLE".equals(order.status) && !ClientMarketState.playerUuid().equals(order.ownerUuid)) {
            graphics.text(this.font, TradeI18n.text("p2p_market.details.reserve_hint"), x, y + 142, 0xFFA7ADB8, false);
            graphics.text(this.font, TradeI18n.text("p2p_market.details.no_auto_transfer"), x, y + 161, 0xFFA7ADB8, false);
        }
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private static String prettyStatus(String status) {
        return switch (status) {
            case "AVAILABLE" -> TradeI18n.text("p2p_market.status.available");
            case "RESERVED" -> TradeI18n.text("p2p_market.status.reserved");
            case "COMPLETED" -> TradeI18n.text("p2p_market.status.completed");
            case "CANCELLED" -> TradeI18n.text("p2p_market.status.cancelled");
            default -> status;
        };
    }

    private static int statusColor(String status) {
        return switch (status) {
            case "AVAILABLE" -> 0xFF9CE69C;
            case "RESERVED" -> 0xFFFFD27A;
            case "COMPLETED" -> 0xFF8EBFFF;
            default -> 0xFFC8CCD3;
        };
    }

    public TradeScreen marketParent() {
        return marketParent;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(marketParent);
    }
}
