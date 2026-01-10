package de.guntram.mcmod.easiercrafting.recipebook;

import de.guntram.mcmod.easiercrafting.*;
import de.guntram.mcmod.easiercrafting.extendedScreen.ExtendedGuiInventory;
import de.guntram.mcmod.easiercrafting.modConfig.ModConfig;
import de.guntram.mcmod.easiercrafting.recipe.RecipeTreeSet;
import de.guntram.mcmod.easiercrafting.recipe.RepairCraftingRecipeDisplay;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.client.recipebook.RecipeBookType;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.item.*;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.recipe.display.*;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.*;

import static de.guntram.mcmod.easiercrafting.EasierCrafting.SPECIAL_CAT;

public class CraftingRecipeBook extends AbstractRecipeBook {

    public CraftingRecipeBook(HandledScreen<? extends ScreenHandler> craftScreen, int firstCraftSlotNo, int gridsize, int resultSlot, int firstInventorySlot, SlotDisplay craftingBlock) {
        super(craftScreen, firstCraftSlotNo, gridsize, resultSlot, firstInventorySlot, craftingBlock);
    }


    @Override
    public boolean updateRecipes() {
        assert MinecraftClient.getInstance().player != null;

        ObjectOpenHashSet<NetworkRecipeId> beforeID = new ObjectOpenHashSet<>(craftableRecipes.size());
        for (RecipeDisplayEntry entry : craftableRecipes) {
            beforeID.add(entry.id());
        }
        updateAvailableStacks();

        craftableRecipes.clear();
        allRecipes.clear();

        for (RecipeResultCollection result : MinecraftClient.getInstance().player.getRecipeBook().getResultsForCategory(RecipeBookType.CRAFTING)){
            //System.out.println("found collection: "+result.getAllRecipes().getFirst().getStacks(EMPTY_CONTEXT).getFirst().getName());
            allRecipes.addAll(result.getAllRecipes());
        }
        for (RecipeDisplayEntry entry : allRecipes) {
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
                //System.out.println("can craft: " + entry.display().result().getFirst(worldcontext).getName().getString()+", cat: "+getCat(entry).getPath());
            }
        }


        ItemGroups.updateDisplayContext(player.networkHandler.getEnabledFeatures(), true, world.getRegistryManager());

        if (ModConfig.get().allowGeneratedRecipes) {
            addUnusualRecipe();
        }

        craftableCategories.clear();
        for (RecipeDisplayEntry entry : craftableRecipes) {
            String category;
            if (!ModConfig.get().categorizeRecipes) {
                // dont categorize recipes
                category = I18n.translate("easiercrafting.category.possible");
            } else if (Objects.requireNonNull(getCat(entry)).getNamespace().startsWith(EasierCrafting.MODID)) {
                // generated recipe
                category = I18n.translate("easiercrafting.category.special");
            } else {
                category = getTranslatedItemGroup(entry);
            }

            craftableCategories.computeIfAbsent(category, k -> new RecipeTreeSet()).add(entry);
        }
        recalcListSize();

        ObjectOpenHashSet<NetworkRecipeId> afterID = new ObjectOpenHashSet<>(craftableRecipes.size());
        for (RecipeDisplayEntry entry : craftableRecipes) {
            afterID.add(entry.id());
        }

        return beforeID.equals(afterID);
    }

    @Override
    protected void drawRecipeGridOverlay(DrawContext context, TextRenderer fontRenderer, int height, int mouseX, int mouseY) {
        RecipeDisplay display = underMouse.display();
        if (display instanceof ShapedCraftingRecipeDisplay shaped) {
            List<SlotDisplay> ingredients = shaped.ingredients();
            for (int x = 0; x < shaped.width(); x++) {
                for (int y = 0; y < shaped.height(); y++) {
                    SlotDisplay ingredient = ingredients.get(x + y * shaped.width());
                    if (ingredient.getFirst(worldContext).isEmpty()) continue;
                    renderIngredient(context, fontRenderer, ingredient, itemSize * x, height + itemSize + itemSize * y);
                }
            }
        } else if (display instanceof ShapelessCraftingRecipeDisplay recipeDisplay) {
            if (underMouse.craftingRequirements().isPresent()) {
                int x = 0;
                for (SlotDisplay ingredient : recipeDisplay.ingredients()) {
                    renderIngredient(context, fontRenderer, ingredient, itemSize * x, height + itemSize);
                    x++;
                }
            }
        } else if (display instanceof RepairCraftingRecipeDisplay repairDisplay) {
            int x = 0;
            for (SlotDisplay ingredient : repairDisplay.ingredients()) {
                renderIngredient(context, fontRenderer, ingredient, itemSize * x, height + itemSize);
                x++;
            }
        }
    }

    @Override
    protected void onRecipeClicked(RecipeDisplayEntry entry, int mouseButton) {
        if (!craftableRecipes.contains(underMouse)) return;
        List<Ingredient> recipeInput = entry.craftingRequirements().orElse(Collections.emptyList());
        if (recipeInput.isEmpty()) return;

        int maxCraftableStacks = 1;
        int rowadjust = 0;
        int recipeWidth;
        List<SlotDisplay> ingredients;

        // todo make repairing
//        byte[] resultSlots = new byte[entry.craftingRequirements().get().size()];
//        boolean predefineIngredients = false;

        switch (entry.display()) {
            case ShapedCraftingRecipeDisplay shaped -> {
                recipeWidth = shaped.width();
                ingredients = shaped.ingredients();
                if (Screen.hasShiftDown()) maxCraftableStacks = getMaxCraftable(ingredients);
            }
            case ShapelessCraftingRecipeDisplay shapeless -> {
                ingredients = shapeless.ingredients();
                recipeWidth = ingredients.size() <= 4 ? 2 : 3;
                if (Screen.hasShiftDown()) maxCraftableStacks = getMaxCraftable(ingredients);
            }
            case RepairCraftingRecipeDisplay repairDisplay -> {
                ingredients = repairDisplay.ingredients();
                recipeWidth = 2;
            }
            case null, default -> {
                return;
            }
        }

        if (recipeWidth > gridSize) return;

        // remove leftovers e.g. bucket/ glass bottles
        boolean[] removal = new boolean[ingredients.size()];

        // move item on crafting grid
        for (int i = 0; i < ingredients.size(); i++) {
            int remaining = maxCraftableStacks;
            SlotDisplay ingredient = ingredients.get(i);
            if (ingredient.getStacks(worldContext).isEmpty()) continue;

            // todo make algorithm to make best repairing
//            if (predefineIngredients) {
//                transfer(resultSlots[i], i + firstCraftSlotNo + rowadjust, remaining);
//                ItemStack inCraftSlot = screen.getScreenHandler().getSlot(i + firstCraftSlotNo + rowadjust).getStack();
//                if (!inCraftSlot.getRecipeRemainder().isEmpty()) {
//                    removal[i] = true;
//                }
//                continue;
//            }

            for (int slot = firstInventorySlotNo; remaining > 0 && slot < 36 + firstInventorySlotNo; slot++) {
                ItemStack slotcontent = screenHandler.getSlot(slot).getStack();
                if (canActAsIngredient(ingredient, slotcontent)) {
                    transfer(slot, i + firstCraftSlotNo + rowadjust, remaining);
                    ItemStack inCraftSlot = screenHandler.getSlot(i + firstCraftSlotNo + rowadjust).getStack();
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

        // actually craft item
        if (mouseButton == 0 && !Screen.hasControlDown()) {
            slotClick(resultSlotNo, mouseButton, SlotActionType.QUICK_MOVE);
            updateRecipesIn(ModConfig.get().autoUpdateRecipeTimer * 50);

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
        Slot fromSlot = screenHandler.getSlot(from);
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

        for (Map.Entry<Item, Integer> ingredientSet : ingredientMap.entrySet()) {
            maxCraftableStacks = Math.min(Math.min(maxCraftableStacks, avaliableItemMap.getInt(ingredientSet.getKey())/ingredientSet.getValue()),ingredientSet.getKey().getMaxCount());
        }
        return maxCraftableStacks;
    }

    private static int getMaxCraftable(RecipeDisplayEntry recipe) {
        if (recipe.craftingRequirements().isEmpty() || avaliableItemMap.isEmpty()) return 0;
        List<Ingredient> requirements = recipe.craftingRequirements().get();

        // 1. Pre-filter and Cache: Map each requirement to a list of valid items in the inventory.
        // This avoids calling ingredient.test() thousands of times.
        Object2IntOpenHashMap<Item> ingredientMatches = new Object2IntOpenHashMap<>(10);
        for (Ingredient ingredient : requirements) {
            if (ingredient.isEmpty()) continue;

            Item validItems = Items.AIR;
            for (Item item : avaliableItemMap.keySet()) {
                if (ingredient.test(item.getDefaultStack())) {
                    validItems = item;
                    break;
                }
            }

            if (validItems.equals(Items.AIR)) return 0;
            ingredientMatches.addTo(validItems,1);

        }

        // 3. Crafting Simulation
        int maxCraftableStacks = 64;
        for (Map.Entry<Item, Integer> set : ingredientMatches.object2IntEntrySet()) {
            // min(var,avaliable/need,max stackable ingredient)
            maxCraftableStacks=Math.min(Math.min(maxCraftableStacks,avaliableItemMap.getInt(set.getKey())/set.getValue()),set.getKey().getMaxCount());
        }

        return maxCraftableStacks;
    }

    private void addUnusualRecipe(){
        addFireworkRecipe();
        addRepairRecipe();
    }

    private void addFireworkRecipe(){
        int gunPowerCount = avaliableItemMap.getInt(Items.GUNPOWDER);
        if (avaliableItemMap.containsKey(Items.PAPER) && gunPowerCount > 0) {
            // power one is already there
            for (int power = 1; power <= 3; power++) {
                if (gunPowerCount >= power) {
                    // 2. Prepare the Result Item
                    ItemStack resultItem = new ItemStack(Items.FIREWORK_ROCKET, 3);

                    // In 1.21, FireworksComponent is a record: (int flightDuration, List<FireworkExplosionComponent> explosions)
                    // We pass an empty list for explosions if there are none.
                    resultItem.set(DataComponentTypes.FIREWORKS, new FireworksComponent(power, List.of()));
                    resultItem.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Firework strength: " + power));

                    // 3. Prepare the Ingredients List for the Display
                    List<SlotDisplay> ingredientsDisplay = new ArrayList<>();
                    List<Ingredient> ingredients = new ArrayList<>();
                    ingredients.add(Ingredient.ofItem(Items.PAPER));
                    ingredientsDisplay.add(new SlotDisplay.StackSlotDisplay(new ItemStack(Items.PAPER)));
                    for (int k = 0; k < power; k++) {
                        ingredientsDisplay.add(new SlotDisplay.StackSlotDisplay(new ItemStack(Items.GUNPOWDER)));
                        ingredients.add(Ingredient.ofItem(Items.GUNPOWDER));
                    }



                    // 4. Create the Display logic
                    ShapelessCraftingRecipeDisplay display = new ShapelessCraftingRecipeDisplay(
                            ingredientsDisplay,
                            new SlotDisplay.StackSlotDisplay(resultItem),
                            craftingBlock
                    );

                    // 5. Add to your results list
                    RecipeDisplayEntry entry = new RecipeDisplayEntry(
                            new NetworkRecipeId(670 + power), // Use unique IDs to avoid UI glitches
                            display,
                            OptionalInt.empty(),
                            SPECIAL_CAT,
                            Optional.of(ingredients)
                    );

                    allRecipes.add(entry);
                    craftableRecipes.add(entry);
                }
            }
        }

    }

    private void addRepairRecipe(){
        Object2IntOpenHashMap<Item> item = new Object2IntOpenHashMap<>(16);
        for (ItemStack itemStack : ((InventoryAccessor) player.getInventory()).getCompatMain()){
            if (itemStack.isEmpty()|| !itemStack.isDamaged() || itemStack.hasEnchantments()) continue;
            // have breaking item
            item.addTo(itemStack.getItem(), 1);
        }

        for (Map.Entry<Item, Integer> set : item.object2IntEntrySet()) {
            if (set.getValue()<2) continue;

            // add the repairRecipe
            RepairCraftingRecipeDisplay display = new RepairCraftingRecipeDisplay(
                    Collections.nCopies(2,new SlotDisplay.StackSlotDisplay(new ItemStack(set.getKey()))),
                    new SlotDisplay.StackSlotDisplay(new ItemStack(set.getKey())),
                    craftingBlock
            );

            RecipeDisplayEntry entry = new RecipeDisplayEntry(
                    new NetworkRecipeId((int)System.currentTimeMillis()), // yes this will warp every abt 50 days... dont play for 50 days straight
                    display,
                    OptionalInt.empty(),
                    SPECIAL_CAT,
                    Optional.of(Collections.nCopies(2,Ingredient.ofItem(set.getKey())))
            );

            allRecipes.add(entry);
            craftableRecipes.add(entry);
        }

    }
}
