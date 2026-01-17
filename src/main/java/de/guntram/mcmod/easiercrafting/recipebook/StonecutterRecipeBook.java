package de.guntram.mcmod.easiercrafting.recipebook;

import de.guntram.mcmod.easiercrafting.modConfig.ModConfig;
import de.guntram.mcmod.easiercrafting.recipe.RecipeTreeSet;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.recipe.StonecuttingRecipe;
import net.minecraft.recipe.book.RecipeBookCategories;
import net.minecraft.recipe.display.CuttingRecipeDisplay;
import net.minecraft.recipe.display.StonecutterRecipeDisplay;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.StonecutterScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.List;

public class StonecutterRecipeBook extends AbstractRecipeBook {

    public StonecutterRecipeBook(HandledScreen<? extends ScreenHandler> craftScreen) {
        super(craftScreen, 0, 1, 1, 2,  getSlotDisplayFromItem(Items.STONECUTTER));
    }


    @Override
    public boolean updateRecipes() {
        updateAvailableStacks();

        // process if craftable changed
        int hashID=0;
        for (RecipeDisplayEntry entry : craftableRecipes) {
            hashID^=entry.id().index();
        }

        craftableRecipes.clear();
        allRecipes.clear();
        craftableCategories.clear();

        // add all recipes
        for (RecipeResultCollection collection : recipeBook.getResultsForCategory(RecipeBookCategories.STONECUTTER)){
            allRecipes.addAll(collection.getAllRecipes());
        }

        // add craftable recipes
        for (RecipeDisplayEntry entry : allRecipes){
            if (entry.display() instanceof StonecutterRecipeDisplay recipeDisplay){
                for (ItemStack slotDisplay : recipeDisplay.input().getStacks(worldContext)){
                    if (avaliableItemMap.containsKey(slotDisplay.getItem())){
                        craftableRecipes.add(entry);
                        craftableCategories.computeIfAbsent(ModConfig.get().categorizeRecipes ?
                                getIngredients(entry).getFirst().getName().getString() :
                                I18n.translate("easiercrafting.category.possible"),
                                k -> new RecipeTreeSet()).add(entry);
                    }
                }

            }
        }


        recalcListSize();
        for (RecipeDisplayEntry entry : craftableRecipes) {
            hashID^=entry.id().index();
        }
        return hashID==0;
    }


    @Override
    protected void onRecipeClicked(RecipeDisplayEntry entry, int mouseButton) {
        var interactionManager = client.interactionManager;
        if (!(screenHandler instanceof StonecutterScreenHandler container)||!(entry.display() instanceof StonecutterRecipeDisplay recipe)) {
            return;
        }

        // no item -> return
        for (ItemStack ingredient : getIngredients(entry)) {
            if (!avaliableItemMap.containsKey(ingredient.getItem()))return;
        }
        // move item to crafting slot
        search:
        for (int slot = firstInventorySlotNo; slot < 36 + firstInventorySlotNo; slot++) {
            ItemStack slotContent = container.getSlot(slot).getStack();
            for (ItemStack ingredientStack : recipe.input().getStacks(worldContext)) {
                if (ingredientStack.getItem().equals(slotContent.getItem())){
                    if (Screen.hasShiftDown()) {
                        slotClick(slot, 0, SlotActionType.PICKUP);
                        slotClick(slot, 0, SlotActionType.PICKUP_ALL);
                        slotClick(firstCraftSlotNo, 0, SlotActionType.PICKUP);
                        slotClick(slot, 0, SlotActionType.PICKUP);
                    } else {
                        slotClick(slot, 0, SlotActionType.PICKUP);
                        slotClick(firstCraftSlotNo, 1, SlotActionType.PICKUP);
                        slotClick(slot, 0, SlotActionType.PICKUP);
                    }
                    break search;
                }
            }
        }

        // click the recipe button (select the recipe)
        List<CuttingRecipeDisplay.GroupEntry<StonecuttingRecipe>> available = container.getAvailableRecipes().entries();
        int buttonIndex = -1;

        for (int i = 0; i < available.size(); i++) {
            // Compare the recipe entries directly.
            if (available.get(i).recipe().optionDisplay().equals(recipe.result())) {
                buttonIndex = i;
                break;
            }
        }

        if (buttonIndex != -1 && interactionManager != null) {
            // 3. Select the recipe by clicking the button with the index
            interactionManager.clickButton(container.syncId, buttonIndex);

            // 4. Take the result from the output slot (slot 1) to complete the craft
            if (Screen.hasControlDown()) return;
            interactionManager.clickSlot(container.syncId, 1, 0, SlotActionType.QUICK_MOVE, player);
        }
    }

    @Override
    protected void drawRecipeGridOverlay(DrawContext context) {
        if (!(underMouse.display() instanceof StonecutterRecipeDisplay recipe)) return;
        ItemStack result = recipe.result().getFirst(worldContext).copy();
        boolean canCraft = canCraft(underMouse);
        if (canCraft){
            int i;
            Item item = null;
            if (Screen.hasShiftDown()){
                for (i=0; i<recipe.input().getStacks(worldContext).size(); i++){
                    item = recipe.input().getStacks(worldContext).get(i).getItem();
                    if (avaliableItemMap.containsKey(item)){
                        result.setCount(Math.min(avaliableItemMap.getInt(item)*result.getCount(),item.getMaxCount()));
                        break;
                    }
                }
            }
            assert item != null;

        }

        // draw result
        Slot resultSlot = screenHandler.getSlot(resultSlotNo);
        drawHoloItem(context,resultSlot,result);

        if (!canCraft) context.fill(resultSlot.x-2,resultSlot.y-2,resultSlot.x+itemSize+2,resultSlot.y+itemSize+2,0x60FF0000);

        renderIngredient(context, getIngredients(underMouse), screenHandler.getSlot(firstCraftSlotNo));
    }

    protected List<ItemStack> getIngredients(RecipeDisplayEntry entry) {
        if (!(entry.display() instanceof StonecutterRecipeDisplay recipe)) return null;
        List<ItemStack> craftable = recipe.input().getStacks(worldContext).stream()
                .filter(stack -> getAvailableItemSet().contains(stack.getItem()))
                .toList();

        if (craftable.isEmpty()){
            return recipe.input().getStacks(worldContext);
        } else {
            return craftable;
        }
    }

}
