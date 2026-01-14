package de.guntram.mcmod.easiercrafting.extendedScreen;

import de.guntram.mcmod.easiercrafting.SlotClickAccepter;
import de.guntram.mcmod.easiercrafting.modConfig.ModConfig;
import de.guntram.mcmod.easiercrafting.recipebook.CraftingRecipeBook;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

public class ExtendedGuiCrafting extends CraftingScreen implements SlotClickAccepter {

    private CraftingRecipeBook recipeBook;

    public ExtendedGuiCrafting(CraftingScreenHandler container, PlayerInventory lowerInv, Text title) {
        super(container, lowerInv, title);
    }
    
    @Override
    protected void init() {
        super.init();
        if (!ModConfig.get().allowRecipeBook) {
            // just remove recipe book button
            this.children().removeIf(entry -> entry instanceof RecipeBookWidget);
        }
        this.recipeBook.afterInitGui();
    }

    public void setRecipeBook(CraftingRecipeBook recipeBook) {
        this.recipeBook=recipeBook;
    }

    @Override
    protected void drawForeground(DrawContext context, final int mouseX, final int mouseY) {
        super.drawForeground(context, mouseX, mouseY);
        recipeBook.drawAllRecipe(context, textRenderer, backgroundWidth, backgroundHeight, mouseX-x, mouseY-y);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double xdelta, double ydelta) {
        recipeBook.scrollBy((int) ydelta);
        return super.mouseScrolled(mouseX, mouseY, xdelta, ydelta);
    }    
    
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        super.mouseClicked(click, doubled);
        recipeBook.mouseClicked(click, doubled, x, y);
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.isEscape())
            return super.keyPressed(input);
        else if (recipeBook.keyPressed(input))
            return true;
        else
            return super.keyPressed(input);
    }
    
    @Override
    public boolean charTyped(CharInput input) {
        if (!recipeBook.charTyped(input))
            return super.charTyped(input);
        return true;
    }

    @Override
    public void slotClick(int slot, int mouseButton, SlotActionType clickType) {
        this.onMouseClick(null, slot, mouseButton, clickType);
    }
}
