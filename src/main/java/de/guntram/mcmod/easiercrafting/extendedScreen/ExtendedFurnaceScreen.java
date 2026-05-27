package de.guntram.mcmod.easiercrafting.extendedScreen;

import de.guntram.mcmod.easiercrafting.EasierCrafting;
import de.guntram.mcmod.easiercrafting.modConfig.ModConfig;
import de.guntram.mcmod.easiercrafting.recipebook.FurnaceRecipeBook;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.FurnaceMenu;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;

public class ExtendedFurnaceScreen extends FurnaceScreen {
    FurnaceRecipeBook recipeBook;
    Logger LOGGER = EasierCrafting.getGeneralLogger();

    public ExtendedFurnaceScreen(FurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void init() {
        super.init();
        if (!ModConfig.get().allowRecipeBook) {
            // just remove recipe book button
            this.children().removeIf(entry -> entry instanceof RecipeBookTabButton); //idk whats the widget called
        }
        this.recipeBook.screenYOffset = -super.topPos;
        this.recipeBook.afterInitGui();
    }

    public void setRecipeBook(FurnaceRecipeBook recipeBook) {
        this.recipeBook=recipeBook;
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        recipeBook.drawAllRecipe(graphics, this.width, this.height, mouseX-this.leftPos, mouseY-this.topPos);
    }



    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double xdelta, double ydelta) {
        recipeBook.scrollBy((int) ydelta);
        return super.mouseScrolled(mouseX, mouseY, xdelta, ydelta);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        super.mouseClicked(event, doubleClick);
        recipeBook.mouseClicked(event, doubleClick, leftPos, topPos);
        return true;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (event.isEscape())
            return super.keyPressed(event);
        else if (recipeBook.keyPressed(event))
            return true;
        else
            return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent event) {
        if (!recipeBook.charTyped(event))
            return super.charTyped(event);
        return true;
    }
}
