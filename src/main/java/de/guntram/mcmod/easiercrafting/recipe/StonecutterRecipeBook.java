package de.guntram.mcmod.easiercrafting.recipe;

import de.guntram.mcmod.easiercrafting.modConfig.ModConfig;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.recipe.StonecuttingRecipe;
import net.minecraft.recipe.display.CuttingRecipeDisplay;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.StonecutterScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StonecutterRecipeBook extends AbstractRecipeBook<CuttingRecipeDisplay.GroupEntry<StonecuttingRecipe>> {

    public StonecutterRecipeBook(HandledScreen<? extends ScreenHandler> craftScreen, int firstCraftSlotNo, int gridsize, int resultSlot, int firstInventorySlot) {
        super(craftScreen, firstCraftSlotNo, gridsize, resultSlot, firstInventorySlot);
    }


    @Override
    public boolean updateRecipes() {
        RecipeHandler.updateAvailableStacks();
        Map<CuttingRecipeDisplay.GroupEntry<StonecuttingRecipe>, Integer> before = new HashMap<>(RecipeHandler.getCraftableStoneCuttingRecipes());
        RecipeHandler.updateRecipes(client.currentScreen.getClass());

        craftableCategories.clear();
        Map<CuttingRecipeDisplay.GroupEntry<StonecuttingRecipe>, Integer> entries = RecipeHandler.getCraftableStoneCuttingRecipes();


        for (Map.Entry<CuttingRecipeDisplay.GroupEntry<StonecuttingRecipe>, Integer> entry : entries.entrySet()) {
            String category = "Stonecutting";
            // Get the list of valid input items for this recipe
            for (Map.Entry<Item, Integer> map : RecipeHandler.getAvaliableItemMap().entrySet()) {
                Item item = map.getKey();
                if (entry.getKey().input().test(item.getDefaultStack())) {
                    category = item.getName().getString();
                }
            }

            craftableCategories.computeIfAbsent(category, k -> new RecipeTreeSet<>()).add(entry.getKey());
        }

        recalcListSize();
        return before.equals(entries);
    }


    @Override
    protected void onRecipeClicked(CuttingRecipeDisplay.GroupEntry<StonecuttingRecipe> entry, int mouseButton) {
        // todo lock in ;-; and rewrite
        var interactionManager = client.interactionManager;
        ScreenHandler screenHandler = screen.getScreenHandler();
        if (!(screenHandler instanceof StonecutterScreenHandler container)||!(underMouse instanceof CuttingRecipeDisplay.GroupEntry<?> recipe)) return;

        LOGGER.info("try to craft");
        // no item -> return
        if (!RecipeHandler.getAvailableItems().contains(recipe.input().toDisplay().getFirst(worldContext).getItem())) return;

        ItemStack resultStack = recipe.recipe().optionDisplay().getFirst(worldContext);

        // move item to crafting slot
        for (int slot = firstInventorySlotNo; slot < 36 + firstInventorySlotNo; slot++) {
            ItemStack slotcontent = container.getSlot(slot).getStack();
            if (recipe.input().test(slotcontent)) {
                if (Screen.hasShiftDown()) {
                    slotClick(slot, 0, SlotActionType.QUICK_MOVE);
                } else {
                    slotClick(slot, 0, SlotActionType.PICKUP);
                    slotClick(0, 1, SlotActionType.PICKUP);
                    slotClick(slot, 0, SlotActionType.PICKUP);

                }
                break;
            }
        }

        List<CuttingRecipeDisplay.GroupEntry<StonecuttingRecipe>> available = container.getAvailableRecipes().entries();
        int buttonIndex = -1;

        for (int i = 0; i < available.size(); i++) {
            // Compare the recipe entries directly.
            if (available.get(i).recipe().equals(entry.recipe())) {
                buttonIndex = i;
                break;
            }
        }

        if (buttonIndex != -1 && interactionManager != null) {
            // 3. Select the recipe by clicking the button with the index
            interactionManager.clickButton(container.syncId, buttonIndex);

            // 4. Take the result from the output slot (slot 1) to complete the craft
            interactionManager.clickSlot(container.syncId, 1, 0, SlotActionType.QUICK_MOVE, player);
            updateRecipesIn(ModConfig.getAutoUpdateRecipeTimer() * 50);
        }

    }


    @Override
    protected Set<RecipeDisplayEntry> getRecipesForSearch() {
        return RecipeHandler.getCraftableRecipeEntries();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected int drawRecipeOutputs(DrawContext context, RecipeTreeSet<?> treeSet, TextRenderer fontRenderer, int xpos, int ypos, int mouseX, int mouseY) {
        if (treeSet == null || treeSet.isEmpty()) return ypos;
        for (Object generalRecipe : treeSet) {
            if (generalRecipe instanceof CuttingRecipeDisplay.GroupEntry<?> recipe) {
                if (ypos >= minYtoDraw) {
                    renderSingleRecipeOutput(context, fontRenderer, recipe.recipe().optionDisplay().getFirst(worldContext), xOffset + xpos, ypos - itemLift);
                    if (mouseX >= xpos + xOffset && mouseX <= xpos + xOffset + itemSize - 1
                            && mouseY >= ypos - itemLift && mouseY <= ypos - itemLift + itemSize - 1) {
                        // its save to cast :3
                        underMouse = (CuttingRecipeDisplay.GroupEntry<StonecuttingRecipe>) recipe;

                    }
                }
                xpos += itemSize;
                if (xpos >= itemSize * itemsPerRow) {
                    ypos += itemSize;
                    xpos = 0;
                }
            }
        }
        if (xpos != 0) ypos += itemSize;
        return ypos;
    }

    @Override
    protected void drawRecipeOverlay(DrawContext context, TextRenderer fontRenderer, int height, int mouseX, int mouseY) {
        if (underMouse instanceof CuttingRecipeDisplay.GroupEntry<?> recipe) {
            // assume only exist 1 ingredient? idk
            renderIngredient(context, fontRenderer, recipe.input().toDisplay(), 0, height + itemSize);
        }
    }
}
