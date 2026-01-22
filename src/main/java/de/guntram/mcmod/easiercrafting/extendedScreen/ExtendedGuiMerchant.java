package de.guntram.mcmod.easiercrafting.extendedScreen;

import de.guntram.mcmod.easiercrafting.mixins.MerchantScreenAccessor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

public class ExtendedGuiMerchant extends MerchantScreen {

    public ExtendedGuiMerchant(MerchantScreenHandler container, PlayerInventory lowerInv, Text title) {
        super(container, lowerInv, title);
    }

    @Override
    public void renderMain(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.renderMain(context, mouseX, mouseY, deltaTicks);
        TradeOfferList tradeOfferList = this.handler.getRecipes();
        if (!tradeOfferList.isEmpty()) {
            int i = (this.width - this.backgroundWidth) / 2;
            int j = (this.height - this.backgroundHeight) / 2;
            int k = j + 16 + 1;
            int l = i + 5 + 5;
            ((MerchantScreenAccessor)this).renderScrollbar(context, i, j, mouseX, mouseY, tradeOfferList);
            int m = 0;

            int maxVisibleEntries = 10;
            for(TradeOffer tradeOffer : tradeOfferList) {
                if (!((MerchantScreenAccessor)this).canScroll(tradeOfferList.size()) || m >= ((MerchantScreenAccessor)this).getIndexStartOffset() && m < maxVisibleEntries + ((MerchantScreenAccessor)this).getIndexStartOffset()) {

                    ItemStack itemStack = tradeOffer.getOriginalFirstBuyItem();
                    ItemStack itemStack2 = tradeOffer.getDisplayedFirstBuyItem();
                    ItemStack itemStack3 = tradeOffer.getDisplayedSecondBuyItem();
                    ItemStack itemStack4 = tradeOffer.getSellItem();

                    int n = k + 2;
                    ((MerchantScreenAccessor)this).renderFirstBuyItem(context, itemStack2, itemStack, l, n);

                    if (!itemStack3.isEmpty()) {
                        context.drawItemWithoutEntity(itemStack3, i + 5 + 35, n);
                        context.drawStackOverlay(this.textRenderer, itemStack3, i + 5 + 35, n);
                    }

                    ((MerchantScreenAccessor)this).renderArrow(context, tradeOffer, i, n);
                    context.drawItemWithoutEntity(itemStack4, i + 5 + 68, n);
                    context.drawStackOverlay(this.textRenderer, itemStack4, i + 5 + 68, n);

                    // 2. REDUCE THE HEIGHT: Changed from 20 to 16
                    // This makes the rows sit closer together
                    k += 16;
                    ++m;
                } else {
                    ++m;
                }
            }

            int o = ((MerchantScreenAccessor)this).getSelectedIndex();
            TradeOffer tradeOffer = (TradeOffer)tradeOfferList.get(o);
            if (((MerchantScreenHandler)this.handler).isLeveled()) {
                ((MerchantScreenAccessor)this).drawLevelInfo(context, i, j, tradeOffer);
            }

            if (tradeOffer.isDisabled() && this.isPointWithinBounds(186, 35, 22, 21, (double)mouseX, (double)mouseY) && ((MerchantScreenHandler)this.handler).canRefreshTrades()) {
                context.drawTooltip(this.textRenderer, ((MerchantScreenAccessor)this).getdeprecatedText(), mouseX, mouseY);
            }

            for(WidgetButtonPage widgetButtonPage : this.offers) {
                if (widgetButtonPage.isSelected()) {
                    widgetButtonPage.renderTooltip(context, mouseX, mouseY);
                }

                widgetButtonPage.visible = widgetButtonPage.index < ((MerchantScreenHandler)this.handler).getRecipes().size();
            }
        }

        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

}
