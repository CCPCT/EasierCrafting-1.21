package de.guntram.mcmod.easiercrafting.recipe;

import de.guntram.mcmod.easiercrafting.modConfig.ModConfig;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.StonecuttingRecipe;
import net.minecraft.recipe.display.CuttingRecipeDisplay;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.StonecutterScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.*;

public class StonecutterRecipeBook extends AbstractRecipeBook<CuttingRecipeDisplay.GroupEntry<StonecuttingRecipe>> {

    public StonecutterRecipeBook(HandledScreen<? extends ScreenHandler> craftScreen, int firstCraftSlotNo, int gridsize, int resultSlot, int firstInventorySlot, SlotDisplay craftingBlock) {
        super(craftScreen, firstCraftSlotNo, gridsize, resultSlot, firstInventorySlot,  craftingBlock);
    }


    @Override
    public boolean updateRecipes() {
        updateAvailableStacks();

        ObjectOpenHashSet<CuttingRecipeDisplay.GroupEntry<StonecuttingRecipe>> before = new ObjectOpenHashSet<>(craftableRecipes.size());
        before.addAll(craftableRecipes);

        assert MinecraftClient.getInstance().world != null;
        craftableRecipes.clear();
        allRecipes.clear();
        allRecipes.addAll(MinecraftClient.getInstance().world.getRecipeManager().getStonecutterRecipes().entries());
        for (CuttingRecipeDisplay.GroupEntry<StonecuttingRecipe> recipe : allRecipes){
            if (recipe.input().isEmpty()) continue;
            for (Map.Entry<Item, Integer> avaliable : avaliableItemMap.object2IntEntrySet()){
                if (recipe.input().test(avaliable.getKey().getDefaultStack())){
                    // tested success
                    craftableRecipes.add(recipe);
                    break;
                }
            }
        }

        craftableCategories.clear();
        ObjectOpenHashSet<CuttingRecipeDisplay.GroupEntry<StonecuttingRecipe>> entries = craftableRecipes;


        for (CuttingRecipeDisplay.GroupEntry<StonecuttingRecipe> entry : entries) {
            String category = "Stonecutting";
            // Get the list of valid input items for this recipe
            for (Map.Entry<Item, Integer> map : getAvailableItemMap().entrySet()) {
                Item item = map.getKey();
                if (entry.input().test(item.getDefaultStack())) {
                    category = item.getName().getString();
                }
            }

            craftableCategories.computeIfAbsent(category, k -> new RecipeTreeSet<>(this::recipeDisplayName)).add(entry);
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
        if (!getAvailableItemSet().contains(recipe.input().toDisplay().getFirst(worldContext).getItem())) return;

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
    @SuppressWarnings("unchecked")
    protected int drawSetOfRecipes(DrawContext context, RecipeTreeSet<?> treeSet, TextRenderer fontRenderer, int xpos, int ypos, int mouseX, int mouseY) {
        if (treeSet == null || treeSet.isEmpty()) return ypos;
        for (Object generalRecipe : treeSet) {
            if (generalRecipe instanceof CuttingRecipeDisplay.GroupEntry<?> recipe) {
                if (ypos >= minYtoDraw) {
                    renderSingleRecipeOutput(context, fontRenderer, recipe.recipe().optionDisplay().getFirst(worldContext), xOffset + xpos, ypos - itemLift);
                    if (mouseX >= xpos + xOffset && mouseX <= xpos + xOffset + itemSize - 1
                            && mouseY >= ypos - itemLift && mouseY <= ypos - itemLift + itemSize - 1) {
                        // its safe to cast :3
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
    protected List<ItemStack> getCraftingResult(CuttingRecipeDisplay.GroupEntry<StonecuttingRecipe> recipe) {
        return recipe.recipe().optionDisplay().getStacks(worldContext);
    }

    @Override
    public String recipeDisplayName(CuttingRecipeDisplay.GroupEntry<StonecuttingRecipe> recipe) {
        return recipe.recipe().optionDisplay().getFirst(worldContext).getName().getString();
    }

    @Override
    protected void drawRecipeGridOverlay(DrawContext context, TextRenderer fontRenderer, int height, int mouseX, int mouseY) {
        if (underMouse instanceof CuttingRecipeDisplay.GroupEntry<?> recipe) {
            // assume only exist 1 ingredient? idk
            renderIngredient(context, fontRenderer, recipe.input().toDisplay(), 0, height + itemSize);
        }
    }
}
