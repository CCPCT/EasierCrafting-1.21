package de.guntram.mcmod.easiercrafting.recipe;

import de.guntram.mcmod.easiercrafting.*;
import de.guntram.mcmod.easiercrafting.extendedScreen.ExtendedGuiInventory;
import de.guntram.mcmod.easiercrafting.modConfig.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.client.recipebook.RecipeBookType;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.item.*;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.recipe.display.*;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.*;

public class CraftingRecipeBook extends AbstractRecipeBook<RecipeDisplayEntry> {

    public CraftingRecipeBook(HandledScreen<? extends ScreenHandler> craftScreen, int firstCraftSlotNo, int gridsize, int resultSlot, int firstInventorySlot) {
        super(craftScreen, firstCraftSlotNo, gridsize, resultSlot, firstInventorySlot);
    }


    @Override
    public boolean updateRecipes() {
        ScreenHandler inventory = screen.getScreenHandler();
        Set<RecipeDisplayEntry> before = new HashSet<>(craftableRecipes);
        updateAvailableStacks();
        refreshRecipeVar();
        ItemGroups.updateDisplayContext(player.networkHandler.getEnabledFeatures(), true, player.getWorld().getRegistryManager());

        Set<RecipeDisplayEntry> recipeEntries = craftableRecipes;

        if (ModConfig.getAllowGeneratedRecipes()) {
            recipeEntries.addAll(InventoryRecipeScanner.findUnusualRecipes(inventory, firstInventorySlotNo));
        }

        craftableCategories.clear();
        for (RecipeDisplayEntry entry : recipeEntries) {
            ItemStack result = entry.display().result().getFirst(worldContext);
            Item item = result.getItem();
            if (item == Items.AIR) continue;

            ItemGroup tab = null;
            for (ItemGroup group : ItemGroups.getGroups()) {
                if (!group.isSpecial() && group.contains(result)) {
                    tab = group;
                    break;
                }
            }

            String category;
            if (!ModConfig.getCategorizeRecipes()) {
                category = I18n.translate("easiercrafting.category.possible");
            } else if (getCat(entry).getNamespace().startsWith(EasierCrafting.MODID + ":")) {
                category = I18n.translate("easiercrafting.category.special");
            } else if (tab == null) {
                category = getCat(entry).toTranslationKey();
            } else {
                category = I18n.translate(tab.getDisplayName().getString());
            }

            craftableCategories.computeIfAbsent(category, k -> new RecipeTreeSet<>(this::recipeDisplayName)).add(entry);
        }
        recalcListSize();

        return before.equals(recipeEntries);
    }

    @Override
    protected Set<RecipeDisplayEntry> getRecipesForSearch() {
        return craftableRecipes;
    }

    @Override
    protected int drawSetOfRecipes(DrawContext context, RecipeTreeSet<?> treeSet, TextRenderer fontRenderer, int xpos, int ypos, int mouseX, int mouseY) {
        if (treeSet == null || treeSet.isEmpty()) return ypos;
        for (Object generalRecipe : treeSet) {
            if (generalRecipe instanceof RecipeDisplayEntry recipe) {
                if (ypos >= minYtoDraw) {
                    renderSingleRecipeOutput(context, fontRenderer, recipe.display().result().getFirst(worldContext), xOffset + xpos, ypos - itemLift);
                    if (mouseX >= xpos + xOffset && mouseX <= xpos + xOffset + itemSize - 1
                            && mouseY >= ypos - itemLift && mouseY <= ypos - itemLift + itemSize - 1) {
                        underMouse = recipe;
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
    protected List<ItemStack> getCraftingResult(RecipeDisplayEntry recipe) {
        return recipe.display().result().getStacks(worldContext);
    }

    @Override
    protected void refreshRecipeVar() {

        craftableRecipes.clear();

        for (RecipeResultCollection result : MinecraftClient.getInstance().player.getRecipeBook().getResultsForCategory(RecipeBookType.CRAFTING)){
            //System.out.println("found collection: "+result.getAllRecipes().getFirst().getStacks(EMPTY_CONTEXT).getFirst().getName());

            for (RecipeDisplayEntry entry : result.getAllRecipes()) {
                // craftable entries
                if (screen instanceof ExtendedGuiInventory) {
                    // in player inventory... filter out big recipes
                    if (entry.display() instanceof ShapedCraftingRecipeDisplay recipe) {
                        if (recipe.width()==3 || recipe.height()==3){
                            continue;
                        }
                    }
                    if (entry.display() instanceof ShapelessCraftingRecipeDisplay recipe) {
                        if (recipe.ingredients().size()>4){
                            continue;
                        }
                    }
                }
                if (getMaxCraftable(entry)>0) {
                    craftableRecipes.add(entry);
                    //System.out.println("can craft: " + entry.display().result().getFirst(getWorldContext()).getName().getString()+", cat: "+getCat(entry).getPath());
                }
            }
        }
    }

    @Override
    public String recipeDisplayName(RecipeDisplayEntry recipe) {
        return recipe.display().result().getFirst(worldContext).getName().getString();
    }

    @Override
    protected void drawRecipeGridOverlay(DrawContext context, TextRenderer fontRenderer, int height, int mouseX, int mouseY) {
        if (underMouse.display() instanceof ShapedCraftingRecipeDisplay shaped) {
            List<SlotDisplay> ingredients = shaped.ingredients();
            for (int x = 0; x < shaped.width(); x++) {
                for (int y = 0; y < shaped.height(); y++) {
                    SlotDisplay ingredient = ingredients.get(x + y * shaped.width());
                    if (ingredient.getFirst(worldContext).isEmpty()) continue;
                    renderIngredient(context, fontRenderer, ingredient, itemSize * x, height + itemSize + itemSize * y);
                }
            }
        } else if (underMouse.display() instanceof ShapelessCraftingRecipeDisplay recipeDisplay) {
            if (underMouse.craftingRequirements().isPresent()) {
                int x = 0;
                for (SlotDisplay ingredient : recipeDisplay.ingredients()) {
                    renderIngredient(context, fontRenderer, ingredient, itemSize * x, height + itemSize);
                    x++;
                }
            }
        }
    }

    @Override
    protected void onRecipeClicked(RecipeDisplayEntry entry, int mouseButton) {
        if (!(entry.display() instanceof ShapedCraftingRecipeDisplay) && !(entry.display() instanceof ShapelessCraftingRecipeDisplay)) {
            return;
        }

        List<Ingredient> recipeInput = entry.craftingRequirements().orElse(Collections.emptyList());
        if (recipeInput.isEmpty()) return;

        int maxCraftableStacks = 1;
        int rowadjust = 0;
        int recipeWidth;
        List<SlotDisplay> ingredients;

        if (entry.display() instanceof ShapedCraftingRecipeDisplay shaped) {
            recipeWidth = shaped.width();
            ingredients = shaped.ingredients();
            if (Screen.hasShiftDown()) maxCraftableStacks = getMaxCraftable(ingredients);
        } else if (entry.display() instanceof ShapelessCraftingRecipeDisplay shapeless) {
            ingredients = shapeless.ingredients();
            recipeWidth = ingredients.size() <= 4 ? 2 : 3;
            if (Screen.hasShiftDown()) maxCraftableStacks = getMaxCraftable(ingredients);
        } else {
            return;
        }

        if (recipeWidth > gridSize) return;

        boolean[] removal = new boolean[ingredients.size()];

        for (int i = 0; i < ingredients.size(); i++) {
            int remaining = maxCraftableStacks;
            SlotDisplay ingredient = ingredients.get(i);
            if (ingredient.getStacks(worldContext).isEmpty()) continue;

            for (int slot = firstInventorySlotNo; remaining > 0 && slot < 36 + firstInventorySlotNo; slot++) {
                ItemStack slotcontent = screen.getScreenHandler().getSlot(slot).getStack();
                if (canActAsIngredient(ingredient, slotcontent)) {
                    transfer(slot, i + firstCraftSlotNo + rowadjust, remaining);
                    ItemStack inCraftSlot = screen.getScreenHandler().getSlot(i + firstCraftSlotNo + rowadjust).getStack();
                    remaining = maxCraftableStacks - inCraftSlot.getCount();
                    if (!inCraftSlot.getRecipeRemainder().isEmpty()) {
                        removal[i] = true;
                    }
                }
            }
            if (recipeWidth > 0 && (i + 1) % recipeWidth == 0) {
                rowadjust += gridSize - recipeWidth;
            }
        }

        if (mouseButton == 0 && !Screen.hasControlDown()) {
            slotClick(resultSlotNo, mouseButton, SlotActionType.QUICK_MOVE);
            updateRecipesIn(ModConfig.getAutoUpdateRecipeTimer() * 50);

            rowadjust = 0;
            for (int i = 0; i < removal.length; i++) {
                if (removal[i]) {
                    slotClick(firstCraftSlotNo + i + rowadjust, 0, SlotActionType.QUICK_MOVE);
                }
                if (recipeWidth > 0 && (i + 1) % recipeWidth == 0) {
                    rowadjust += gridSize - recipeWidth;
                }
            }
        }
    }

    private boolean canActAsIngredient(SlotDisplay ingredient, ItemStack inventoryItem) {
        if (inventoryItem.isEmpty()) return false;
        List<ItemStack> validStacks = ingredient.getStacks(worldContext);
        return validStacks.stream().anyMatch(validStack -> validStack.getItem() == inventoryItem.getItem());
    }

    public void transfer(int from, int to, int amount) {
        Slot fromSlot = screen.getScreenHandler().getSlot(from);
        ItemStack fromContent = fromSlot.getStack();

        if (amount >= fromSlot.getStack().getCount()) {
            slotClick(from, 0, SlotActionType.PICKUP);
            slotClick(to, 0, SlotActionType.PICKUP);
            return;
        }

        int transfer;
        while (amount >= (transfer = ((fromSlot.getStack().getCount() + 1) / 2))) {
            slotClick(from, 1, SlotActionType.PICKUP);
            slotClick(to, 0, SlotActionType.PICKUP);
            amount -= transfer;
        }

        if (amount > 0) {
            int prevCount = fromContent.getCount();
            slotClick(from, 0, SlotActionType.PICKUP);
            for (int i = 0; i < amount; i++)
                slotClick(to, 1, SlotActionType.PICKUP);
            if (prevCount > amount)
                slotClick(from, 0, SlotActionType.PICKUP);
        }
    }

    private int getMaxCraftable(List<SlotDisplay> ingredients){
        int maxCraftableStacks = 64;
        Map<Item, Integer> ingredientMap = new HashMap<>();
        for (SlotDisplay ingredient : ingredients){
            List<ItemStack> chosenList = getCraftableStacks(ingredient);
            if (chosenList.isEmpty()) continue;
            Item chosenItem = chosenList.getFirst().getItem();

            // If chosenItem exists, add 1 to the current value.
            // If it doesn't exist, set the value to 1.
            ingredientMap.merge(chosenItem, 1, Integer::sum);
        }

        Map<Item, Integer> itemMap = avaliableItemMap;
        for (Map.Entry<Item, Integer> ingredientSet : ingredientMap.entrySet()) {
            maxCraftableStacks = Math.min(Math.min(maxCraftableStacks,itemMap.get(ingredientSet.getKey())/ingredientSet.getValue()),ingredientSet.getKey().getMaxCount());
        }
        return maxCraftableStacks;
    }

    private static int getMaxCraftable(RecipeDisplayEntry recipe) {
        List<Ingredient> requirements = recipe.craftingRequirements().get();
        if (requirements.isEmpty() || avaliableItemMap.isEmpty()) return 0;

        // 1. Create a simulated inventory map
        Map<Item, Integer> tempItemMap = new HashMap<>(avaliableItemMap);
        int craftCount = 0;

        // 2. Keep trying to craft until we hit a "false"
        while (true) {
            for (Ingredient ingredient : requirements) {
                if (ingredient.isEmpty()) continue;

                boolean found = false;
                // Try to find one item to satisfy this specific ingredient slot
                for (Map.Entry<Item, Integer> entry : tempItemMap.entrySet()) {
                    Item item = entry.getKey();
                    int count = entry.getValue();

                    if (count > 0 && ingredient.test(item.getDefaultStack())) {
                        tempItemMap.put(item, count - 1); // Consume 1
                        found = true;
                        break;
                    }
                }

                // If a SINGLE ingredient in the recipe can't be filled, we stop everything
                if (!found) return craftCount;
            }

            // If we finished the 'requirements' loop, it means 1 full craft succeeded
            craftCount++;

            // Safety break to prevent infinite loops (Optional)
            if (craftCount > 999) break;
        }

        return craftCount;
    }
}
