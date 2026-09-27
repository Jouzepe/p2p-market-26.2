package dev.jouzepe.p2pmarket.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public final class TradePanelWidget extends AbstractWidget {
    private final Supplier<TradeTab> selectedTab;

    public TradePanelWidget(int x, int y, int width, int height, Supplier<TradeTab> selectedTab) {
        super(x, y, width, height, TradeI18n.component("p2p_market.screen.trade"));
        this.selectedTab = selectedTab;
        this.active = false;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int right = x + this.width;
        int bottom = y + this.height;

        graphics.fill(x + 5, y + 6, right + 5, bottom + 6, 0x88000000);
        graphics.fill(x, y, right, bottom, 0xFFE3E3E3);
        graphics.outline(x, y, this.width, this.height, 0xFF111111);
        graphics.fill(x + 4, y + 4, right - 4, bottom - 4, 0xFF111318);

        // Compact top bar with fixed branding text. These two labels intentionally
        // stay in English for every Minecraft language.
        graphics.fill(x + 5, y + 5, right - 5, y + 43, 0xFF1B1E24);
        graphics.horizontalLine(x + 5, right - 5, y + 43, 0xFF08090B);
        if (this.width >= 620) {
            var font = Minecraft.getInstance().font;
            String leftBrand = "Player-to-Player";
            String rightBrand = "by jouzepe_";
            graphics.text(font, leftBrand, x + 14, y + 20, 0xFFB8BDC7, false);
            graphics.text(font, rightBrand, right - 14 - font.width(rightBrand), y + 20, 0xFFB8BDC7, false);
        }

        int contentX = x + 14;
        int contentY = y + 54;
        int contentRight = right - 14;
        int contentBottom = bottom - 14;
        graphics.fill(contentX, contentY, contentRight, contentBottom, 0xFF090B0F);
        graphics.outline(contentX, contentY, contentRight - contentX, contentBottom - contentY, 0xFF4C515B);

        TradeTab tab = selectedTab.get();
        graphics.text(Minecraft.getInstance().font, tab.heading(), contentX + 14, contentY + 12, 0xFFFFFFFF, true);
        graphics.text(Minecraft.getInstance().font, tab.description(), contentX + 14, contentY + 29, 0xFFA7ADB8, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
    }
}
