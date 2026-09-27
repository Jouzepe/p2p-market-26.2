package dev.jouzepe.p2pmarket.client;

import dev.jouzepe.p2pmarket.social.TradeOrderView;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class TradeScreen extends Screen {
    private static final int MAX_ROWS_PER_PAGE = 10;
    private static final int AUTO_REFRESH_TICKS = 100; // ~5 seconds, broadcast handles instant updates.

    private TradeTab selectedTab;
    private int page = 0;
    private int refreshTicks = 0;
    private int rowsPerPage = 6;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int contentX;
    private int contentY;
    private int contentRight;
    private int contentBottom;
    private int controlsY;

    private Button sellButton;
    private Button buyButton;
    private Button myOrdersButton;
    private final List<Button> orderButtons = new ArrayList<>();
    private Button prevButton;
    private Button nextButton;
    private Button refreshButton;
    private Button createButton;
    private EditBox itemBox;
    private EditBox amountBox;
    private EditBox priceBox;

    private List<TradeOrderView> visibleOrders = List.of();
    private String localNotice = "";

    public TradeScreen() {
        this(TradeTab.SELL);
    }

    public TradeScreen(TradeTab selectedTab) {
        super(TradeI18n.component("p2p_market.screen.trade"));
        this.selectedTab = selectedTab == null ? TradeTab.SELL : selectedTab;
    }

    @Override
    protected void init() {
        // This screen can be reopened as the same object after OrderDetailsScreen.
        // Do not keep stale row button references from the previous init.
        this.orderButtons.clear();
        this.refreshTicks = 0;

        this.panelWidth = Math.max(420, Math.min(760, this.width - 32));
        this.panelHeight = Math.max(330, Math.min(470, this.height - 40));
        this.panelX = (this.width - panelWidth) / 2;
        this.panelY = (this.height - panelHeight) / 2;
        this.contentX = panelX + 14;
        this.contentY = panelY + 54;
        this.contentRight = panelX + panelWidth - 14;
        this.contentBottom = panelY + panelHeight - 14;

        this.addRenderableWidget(new TradePanelWidget(panelX, panelY, panelWidth, panelHeight, () -> selectedTab));

        int tabWidth = 112;
        int gap = 7;
        int totalTabs = tabWidth * 3 + gap * 2;
        int tabX = panelX + (panelWidth - totalTabs) / 2;
        int tabY = panelY + 14;

        this.sellButton = Button.builder(TradeI18n.component("p2p_market.tab.sell"), button -> selectTab(TradeTab.SELL))
                .bounds(tabX, tabY, tabWidth, 20).build();
        this.buyButton = Button.builder(TradeI18n.component("p2p_market.tab.buy"), button -> selectTab(TradeTab.BUY))
                .bounds(tabX + tabWidth + gap, tabY, tabWidth, 20).build();
        this.myOrdersButton = Button.builder(TradeI18n.component("p2p_market.tab.my_orders"), button -> selectTab(TradeTab.MY_ORDERS))
                .bounds(tabX + (tabWidth + gap) * 2, tabY, tabWidth, 20).build();

        this.addRenderableWidget(sellButton);
        this.addRenderableWidget(buyButton);
        this.addRenderableWidget(myOrdersButton);

        int rowsTop = contentY + 50;
        int rowX = contentX + 12;
        int rowWidth = contentRight - contentX - 24;

        // Stretch the order list down toward the paging controls. On the normal large
        // terminal this gives nine visible orders instead of six, while still scaling
        // down safely on smaller GUI sizes.
        this.controlsY = contentBottom - 76;
        int availableRowsHeight = Math.max(0, controlsY - rowsTop - 5);
        this.rowsPerPage = Math.max(4, Math.min(MAX_ROWS_PER_PAGE, availableRowsHeight / 29));
        for (int i = 0; i < rowsPerPage; i++) {
            final int slot = i;
            Button row = Button.builder(Component.literal(""), button -> openRow(slot))
                    .bounds(rowX, rowsTop + i * 29, rowWidth, 24)
                    .build();
            this.orderButtons.add(row);
            this.addRenderableWidget(row);
        }

        this.prevButton = Button.builder(TradeI18n.component("p2p_market.button.previous"), button -> {
            if (page > 0) page--;
            updateContent();
        }).bounds(contentX + 12, controlsY, 58, 20).build();
        this.nextButton = Button.builder(TradeI18n.component("p2p_market.button.next"), button -> {
            page++;
            updateContent();
        }).bounds(contentX + 74, controlsY, 58, 20).build();
        this.refreshButton = Button.builder(TradeI18n.component("p2p_market.button.refresh"), button -> {
            localNotice = TradeI18n.text("p2p_market.notice.refreshing");
            TradeClientActions.requestSnapshot();
        }).bounds(contentRight - 88, controlsY, 76, 20).build();
        this.addRenderableWidget(prevButton);
        this.addRenderableWidget(nextButton);
        this.addRenderableWidget(refreshButton);

        int formY = contentBottom - 33;
        this.itemBox = new EditBox(this.font, contentX + 12, formY, Math.max(135, rowWidth - 330), 20,
                TradeI18n.component("p2p_market.field.item_name"));
        this.itemBox.setMaxLength(16);
        this.itemBox.setValue("");

        int itemRight = this.itemBox.getX() + this.itemBox.getWidth();
        this.amountBox = new EditBox(this.font, itemRight + 6, formY, 72, 20,
                TradeI18n.component("p2p_market.field.amount"));
        this.amountBox.setMaxLength(6);
        this.amountBox.setValue("64");

        this.priceBox = new EditBox(this.font, itemRight + 84, formY, 90, 20,
                TradeI18n.component("p2p_market.field.price"));
        this.priceBox.setMaxLength(10);
        this.priceBox.setValue("10");

        this.createButton = Button.builder(TradeI18n.component("p2p_market.button.create_order"), button -> createOrder())
                .bounds(contentRight - 122, formY, 110, 20)
                .build();

        this.addRenderableWidget(itemBox);
        this.addRenderableWidget(amountBox);
        this.addRenderableWidget(priceBox);
        this.addRenderableWidget(createButton);

        updateTabButtons();
        updateContent();
        TradeClientActions.requestSnapshot();
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

    private void selectTab(TradeTab tab) {
        this.selectedTab = tab;
        this.page = 0;
        this.localNotice = "";
        updateTabButtons();
        updateContent();
    }

    private static int myOrdersStatusPriority(String status) {
        if ("RESERVED".equals(status)) return 0;
        if ("AVAILABLE".equals(status)) return 1;
        return 2;
    }

    private void updateTabButtons() {
        if (sellButton == null) return;
        sellButton.active = selectedTab != TradeTab.SELL;
        buyButton.active = selectedTab != TradeTab.BUY;
        myOrdersButton.active = selectedTab != TradeTab.MY_ORDERS;
    }

    private void updateContent() {
        String me = ClientMarketState.playerUuid();
        List<TradeOrderView> source = ClientMarketState.orders();
        List<TradeOrderView> filtered = new ArrayList<>();
        for (TradeOrderView order : source) {
            if (order == null) continue;
            switch (selectedTab) {
                case SELL -> {
                    if ("SELL".equals(order.type) && !"COMPLETED".equals(order.status)) filtered.add(order);
                }
                case BUY -> {
                    if ("BUY".equals(order.type) && !"COMPLETED".equals(order.status)) filtered.add(order);
                }
                case MY_ORDERS -> {
                    boolean mine = me.equals(order.ownerUuid);
                    boolean reservedByMe = me.equals(order.reservedByUuid);
                    if (mine || reservedByMe) filtered.add(order);
                }
            }
        }
        if (selectedTab == TradeTab.MY_ORDERS) {
            // Keep the existing newest-first order inside each group, but always
            // show reserved orders first, then available orders, then closed ones.
            filtered.sort((left, right) -> Integer.compare(
                    myOrdersStatusPriority(left.status),
                    myOrdersStatusPriority(right.status)
            ));
        }

        this.visibleOrders = filtered;

        int maxPage = visibleOrders.isEmpty() ? 0 : (visibleOrders.size() - 1) / rowsPerPage;
        if (page > maxPage) page = maxPage;
        if (page < 0) page = 0;

        for (int i = 0; i < orderButtons.size(); i++) {
            int index = page * rowsPerPage + i;
            Button row = orderButtons.get(i);
            if (index < visibleOrders.size()) {
                TradeOrderView order = visibleOrders.get(index);
                row.setMessage(Component.literal(TradeFormat.rowLabel(order)));
                row.visible = true;
                row.active = true;
            } else {
                row.setMessage(Component.literal(""));
                row.visible = false;
                row.active = false;
            }
        }

        prevButton.active = page > 0;
        nextButton.active = page < maxPage;

        boolean createVisible = selectedTab == TradeTab.SELL || selectedTab == TradeTab.BUY;
        itemBox.visible = createVisible;
        amountBox.visible = createVisible;
        priceBox.visible = createVisible;
        createButton.visible = createVisible;
        createButton.setMessage(TradeI18n.component(
                selectedTab == TradeTab.SELL ? "p2p_market.button.list_sale" : "p2p_market.button.create_request"
        ));
    }

    private void openRow(int slot) {
        int index = page * rowsPerPage + slot;
        if (index < 0 || index >= visibleOrders.size()) return;
        TradeOrderView order = visibleOrders.get(index);
        this.minecraft.gui.setScreen(new OrderDetailsScreen(this, order.id));
    }

    private void createOrder() {
        if (selectedTab == TradeTab.MY_ORDERS) return;
        String itemName = itemBox.getValue().trim();
        try {
            int amount = Integer.parseInt(amountBox.getValue().trim());
            long price = Long.parseLong(priceBox.getValue().trim());
            if (itemName.isBlank() || itemName.length() > 16) throw new IllegalArgumentException();
            if (amount < 1 || price < 1) throw new IllegalArgumentException();
            String type = selectedTab == TradeTab.SELL ? "SELL" : "BUY";
            TradeClientActions.create(type, itemName, amount, price);
            localNotice = TradeI18n.text("p2p_market.notice.order_sent");
        } catch (Exception e) {
            localNotice = TradeI18n.text("p2p_market.notice.invalid_order");
        }
    }

    public void onMarketUpdated() {
        this.localNotice = TradeI18n.text("p2p_market.notice.market_updated");
        this.refreshTicks = 0;
        updateContent();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        if (contentX == 0) return;

        int count = visibleOrders.size();
        int pageCount = Math.max(1, (count + rowsPerPage - 1) / rowsPerPage);
        String pageText = TradeI18n.text("p2p_market.orders.page", count, page + 1, pageCount);
        // Mirror the left heading margin: keep Orders/Page anchored to the right edge
        // with the same 12 px inset used by the list content on the left.
        int metaX = contentRight - 12 - this.font.width(pageText);
        graphics.text(this.font, pageText, metaX, contentY + 13, 0xFF8E96A3, false);

        int formY = contentBottom - 33;
        if (selectedTab != TradeTab.MY_ORDERS) {
            graphics.text(this.font, TradeI18n.text("p2p_market.label.item_16"), itemBox.getX(), formY - 11, 0xFF9FA6B2, false);
            graphics.text(this.font, TradeI18n.text("p2p_market.label.amount"), amountBox.getX(), formY - 11, 0xFF9FA6B2, false);
            graphics.text(this.font, TradeI18n.text("p2p_market.label.price_each"), priceBox.getX(), formY - 11, 0xFF9FA6B2, false);
        } else {
            graphics.text(this.font, TradeI18n.text("p2p_market.my_orders.hint"),
                    contentX + 12, formY + 4, 0xFF8E96A3, false);
        }

        if (!localNotice.isBlank()) {
            // Align notice vertically with the Next button, immediately to its right.
            graphics.text(this.font, localNotice,
                    nextButton.getX() + nextButton.getWidth() + 10,
                    controlsY + 6,
                    0xFF8E96A3,
                    false);
        }
    }

    public TradeTab selectedTab() {
        return selectedTab;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(null);
    }
}
