package de.guntram.mcmod.easiercrafting.recipebook;

import de.guntram.mcmod.easiercrafting.modConfig.ModConfig;
import de.guntram.mcmod.easiercrafting.recipe.RecipeTreeSet;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.StonecutterRecipeDisplay;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class StonecutterRecipeBook extends AbstractRecipeBook {

    public StonecutterRecipeBook(AbstractContainerScreen<? extends AbstractContainerMenu> craftScreen) {
        super(craftScreen, 0, 1, 1, 2,  getSlotDisplay(Items.STONECUTTER));
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
        for (RecipeCollection collection : recipeBook.getCollection(RecipeBookCategories.STONECUTTER)){
            allRecipes.addAll(collection.getRecipes());
        }

        // add craftable recipes
        for (RecipeDisplayEntry entry : allRecipes){
            if (entry.display() instanceof StonecutterRecipeDisplay recipeDisplay){
                for (ItemStack slotDisplay : recipeDisplay.input().resolveForStacks(worldContext)){
                    if (avaliableItemMap.containsKey(slotDisplay.getItem())){
                        craftableRecipes.add(entry);
                        craftableCategories.computeIfAbsent(ModConfig.get().categorizeRecipes ?
                                getIngredients(entry).getFirst().getItem().getDescriptionId() :
                                I18n.get("easiercrafting.category.possible"),
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
        MultiPlayerGameMode interactionManager = client.gameMode;
        if (!(screenHandler instanceof StonecutterMenu container)||!(entry.display() instanceof StonecutterRecipeDisplay recipe)) {
            return;
        }

        // no item -> return
        for (ItemStack ingredient : getIngredients(entry)) {
            if (!avaliableItemMap.containsKey(ingredient.getItem()))return;
        }
        // move item to crafting slot
        search:
        for (int slot = firstInventorySlotNo; slot < 36 + firstInventorySlotNo; slot++) {
            ItemStack slotContent = container.getSlot(slot).getItem();
            for (ItemStack ingredientStack : recipe.input().resolveForStacks(worldContext)) {
                if (ingredientStack.getItem().equals(slotContent.getItem())){
                    if (isHoldingButton(GLFW.GLFW_KEY_LEFT_SHIFT)) {
                        slotClick(slot, 0, ContainerInput.PICKUP);
                        slotClick(slot, 0, ContainerInput.PICKUP_ALL);
                        slotClick(firstCraftSlotNo, 0, ContainerInput.PICKUP);
                        slotClick(slot, 0, ContainerInput.PICKUP);
                    } else {
                        slotClick(slot, 0, ContainerInput.PICKUP);
                        slotClick(firstCraftSlotNo, 1, ContainerInput.PICKUP);
                        slotClick(slot, 0, ContainerInput.PICKUP);
                    }
                    break search;
                }
            }
        }

        // click the recipe button (select the recipe)
        List<SelectableRecipe.SingleInputEntry<StonecutterRecipe>> available = container.getVisibleRecipes().entries();
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
            interactionManager.handleInventoryButtonClick(container.containerId, buttonIndex);

            // 4. Take the result from the output slot (slot 1) to complete the craft
            if (isHoldingButton(GLFW.GLFW_KEY_LEFT_CONTROL)) return;
            slotClick(1, 0, isHoldingButton(GLFW.GLFW_KEY_Q) ? ContainerInput.THROW : ContainerInput.QUICK_MOVE);
        }
    }

    @Override
    protected void drawRecipeGridOverlay(GuiGraphicsExtractor context) {
        if (!(underMouse.display() instanceof StonecutterRecipeDisplay recipe)) return;
        ItemStack result = recipe.result().resolveForFirstStack(worldContext).copy();
        boolean canCraft = canCraft(underMouse);
        if (canCraft){
            int i;
            Item item = null;
            if (isHoldingButton(GLFW.GLFW_KEY_LEFT_SHIFT)){
                for (i=0; i<recipe.input().resolveForStacks(worldContext).size(); i++){
                    item = recipe.input().resolveForStacks(worldContext).get(i).getItem();
                    if (avaliableItemMap.containsKey(item)){
                        result.setCount(Math.min(avaliableItemMap.getInt(item)*result.getCount(),item.getDefaultMaxStackSize()));
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
        List<ItemStack> craftable = recipe.input().resolveForStacks(worldContext).stream()
                .filter(stack -> getAvailableItemSet().contains(stack.getItem()))
                .toList();

        if (craftable.isEmpty()){
            return recipe.input().resolveForStacks(worldContext);
        } else {
            return craftable;
        }
    }

}
