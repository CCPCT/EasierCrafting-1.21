package de.guntram.mcmod.easiercrafting.mixins;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MerchantScreen.class)
public interface MerchantScreenAccessor {
    @Accessor("indexStartOffset")
    int getIndexStartOffset();

    @Accessor("selectedIndex")
    int getSelectedIndex();

    @Accessor("DEPRECATED_TEXT")
    Text getdeprecatedText();

    @Accessor("offers")
    MerchantScreen.WidgetButtonPage[] getOffer();

    @Invoker("renderScrollbar")
    void renderScrollbar(DrawContext context, int x, int y, int mouseX, int mouseY, TradeOfferList offers);

    @Invoker("canScroll")
    boolean canScroll(int listSize);

    @Invoker("renderFirstBuyItem")
    void renderFirstBuyItem(DrawContext context, ItemStack adjustedFirstBuyItem, ItemStack originalFirstBuyItem, int x, int y);

    @Invoker("renderArrow")
    void renderArrow(DrawContext context, TradeOffer tradeOffer, int x, int y);

    @Invoker("drawLevelInfo")
    void drawLevelInfo(DrawContext context, int x, int y, TradeOffer tradeOffer);

    @Invoker("drawLevelInfo")
    void drawLevelInfo(DrawContext context, int x, int y, TradeOffer tradeOffer);
}
