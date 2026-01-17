package de.guntram.mcmod.easiercrafting.recipebook;

import de.guntram.mcmod.easiercrafting.InventoryAccessor;
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
import net.minecraft.recipe.display.FurnaceRecipeDisplay;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.List;

public class FurnaceRecipeBook extends AbstractRecipeBook {
    public static Item lastFuelUsed;
    protected final int FUEL_SLOT = 1;

    public FurnaceRecipeBook(HandledScreen<? extends ScreenHandler> craftScreen, SlotDisplay craftingBlock) {
        super(craftScreen, 0, 1, 2, 3, craftingBlock);
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

        // add all and craftable recipes
        for (RecipeResultCollection collection : recipeBook.getOrderedResults()) {
            for (RecipeDisplayEntry entry : collection.getAllRecipes()) {
                if (!(entry.display() instanceof FurnaceRecipeDisplay recipeDisplay)) continue;
                // its furnace recipe
                if (recipeDisplay.craftingStation().getFirst(worldContext).getItem()!=craftingBlock.getFirst(worldContext).getItem()) continue;
                allRecipes.add(entry);
                for (ItemStack slotDisplay : recipeDisplay.ingredient().getStacks(worldContext)) {
                    if (avaliableItemMap.containsKey(slotDisplay.getItem())) {
                        craftableRecipes.add(entry);
                        craftableCategories.computeIfAbsent(ModConfig.get().categorizeRecipes ?
                                getTranslatedItemGroup(entry) :
                                I18n.translate("easiercrafting.category.possible"),
                                k -> new RecipeTreeSet()).add(entry);
                        break;
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
        if (!(screenHandler instanceof FurnaceScreenHandler container && entry.display() instanceof FurnaceRecipeDisplay recipe)) return;
        List<ItemStack> inventory = ((InventoryAccessor)player.getInventory()).getCompatMain();
        ItemStack fuelStack = container.slots.get(FUEL_SLOT).getStack();

        // retrieve smelt items
        if (container.slots.get(resultSlotNo).hasStack() && !Screen.hasControlDown()) {
            slotClick(resultSlotNo,0,SlotActionType.QUICK_MOVE);
        }

        // replenish fuel if possible, if fuel slot is empty let player decide what fuel to use
        if (ModConfig.get().refillFuel) if (fuelStack.getItem().equals(Items.BUCKET)){
            // just used lava as fuel...
            slotClick(FUEL_SLOT,0,SlotActionType.QUICK_MOVE);

            for (int slot = 0; slot < 36; slot++){
                ItemStack itemStack = inventory.get(slot);
                if (itemStack.getItem().equals(Items.LAVA_BUCKET)){
                    LOGGER.info("try to refill lava");
                    slotClick(slot,0,SlotActionType.QUICK_MOVE);
                    lastFuelUsed = Items.LAVA_BUCKET;
                    break;
                }
            }
        } else if (!fuelStack.isEmpty()) {
            // refill fuel
            if (avaliableItemMap.containsKey(fuelStack.getItem())){
                lastFuelUsed = fuelStack.getItem();
                LOGGER.info("try to refill fuel: {}", fuelStack.getName().getString());
                slotClick(FUEL_SLOT,0,SlotActionType.PICKUP);
                slotClick(FUEL_SLOT,0,SlotActionType.PICKUP_ALL);
                slotClick(FUEL_SLOT,0,SlotActionType.PICKUP);
            }
        } else if (lastFuelUsed!=null){
            // refill fuel by last used as empty
            LOGGER.info("try to refill memory: {}",lastFuelUsed.getTranslationKey());
            for (int slot = firstInventorySlotNo; slot < 36+firstInventorySlotNo; slot++){
                ItemStack itemStack = container.slots.get(slot).getStack();
                if (itemStack.getItem().equals(lastFuelUsed)){
                    LOGGER.info("refilling memory: {}",lastFuelUsed.getTranslationKey());
                    slotClick(slot,0,SlotActionType.PICKUP);
                    slotClick(slot,0,SlotActionType.PICKUP_ALL);
                    slotClick(FUEL_SLOT,0,SlotActionType.PICKUP);
                    break;
                }
            }
        }

        // move items onto craft spot
        search:
        for (int slot = firstInventorySlotNo; slot < 36 + firstInventorySlotNo; slot++) {
            ItemStack slotContent = container.getSlot(slot).getStack();
            for (ItemStack ingredientStack : recipe.ingredient().getStacks(worldContext)) {
                if (ingredientStack.getItem().equals(slotContent.getItem())){
                    // remove item if not match recipe
                    if (ingredientStack.getItem() != container.slots.get(firstCraftSlotNo).getStack().getItem()){
                        slotClick(firstCraftSlotNo,0,SlotActionType.QUICK_MOVE);
                    }
                    // move item up
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
    }

    // override as want to stack more items onto instead of take out everytime
    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton, int guiLeft, int guiTop) {
        if (pattern != null) {
            boolean clickedPattern = pattern.mouseClicked(mouseX - guiLeft, mouseY - guiTop, 0);
            pattern.setFocused(clickedPattern);
            if (clickedPattern) {
                if (mouseButton == 1) {
                    pattern.setText("");
                    updatePatternMatch();
                }
                return;
            }
        }

        // Scroll bar area click
        if (mouseY > 0 && mouseY < 20 && mouseX > xOffset + containerLeft && mouseX < xOffset + containerLeft + textBoxSize) {
            if (mouseX < xOffset + containerLeft + 20) scrollBy(-1);
            else if (mouseX > xOffset + containerLeft + textBoxSize - 20) scrollBy(1);
            return;
        }

        if (underMouse == null) return;

        // dont craft uncraftable items
        if (!craftableRecipes.contains(underMouse)) return;

        // skip check -> implement check in onRecipeClicked
        onRecipeClicked(underMouse, mouseButton);
        queueUpdateRecipe();
    }

    @Override
    protected void drawRecipeGridOverlay(DrawContext context) {
        if (!(underMouse.display() instanceof FurnaceRecipeDisplay recipe)) return;
        ItemStack result = recipe.result().getFirst(worldContext).copy();
        boolean canCraft = canCraft(underMouse);
        if (canCraft){
            int i;
            Item item = null;
            if (hasShiftDown()){
                for (i=0; i<recipe.ingredient().getStacks(worldContext).size(); i++){
                    item = recipe.ingredient().getStacks(worldContext).get(i).getItem();
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
        if (!(entry.display() instanceof FurnaceRecipeDisplay recipe)) return null;
        List<ItemStack> craftable = recipe.ingredient().getStacks(worldContext).stream()
                .filter(stack -> getAvailableItemSet().contains(stack.getItem()))
                .toList();

        if (craftable.isEmpty()){
            return recipe.ingredient().getStacks(worldContext);
        } else {
            return craftable;
        }
    }

}
