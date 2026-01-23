package de.guntram.mcmod.easiercrafting.recipebook;

import de.guntram.mcmod.easiercrafting.EasierCrafting;
import de.guntram.mcmod.easiercrafting.InventoryAccessor;
import de.guntram.mcmod.easiercrafting.extendedScreen.ExtendedGuiInventory;
import de.guntram.mcmod.easiercrafting.modConfig.ModConfig;
import de.guntram.mcmod.easiercrafting.recipe.RecipeTreeSet;
import de.guntram.mcmod.easiercrafting.recipe.RepairCraftingRecipeDisplay;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.client.recipebook.RecipeBookType;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.recipe.book.RecipeBookCategories;
import net.minecraft.recipe.display.ShapedCraftingRecipeDisplay;
import net.minecraft.recipe.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.*;

import static de.guntram.mcmod.easiercrafting.EasierCrafting.SPECIAL_CAT;

public class CraftingRecipeBook extends AbstractRecipeBook {

    public CraftingRecipeBook(HandledScreen<? extends ScreenHandler> craftScreen, int firstCraftSlotNo, int gridsize, int resultSlot, int firstInventorySlot, SlotDisplay craftingBlock) {
        super(craftScreen, firstCraftSlotNo, gridsize, resultSlot, firstInventorySlot, craftingBlock);
    }


    @Override
    public boolean updateRecipes() {
        assert MinecraftClient.getInstance().player != null;

        int hashID=0;
        for (RecipeDisplayEntry entry : craftableRecipes) {
            hashID^=entry.id().index();
        }
        updateAvailableStacks();

        craftableRecipes.clear();
        allRecipes.clear();

        for (RecipeResultCollection result : MinecraftClient.getInstance().player.getRecipeBook().getResultsForCategory(RecipeBookType.CRAFTING)){
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

            if (recipeCraftable(entry)) {
                craftableRecipes.add(entry);
//                System.out.println("can craft: " + entry.display().result().getFirst(worldContext).getName().getString()+", cat: "+getCat(entry).getPath());
            }
        }

        if (ModConfig.get().allowGeneratedRecipes) {
            addUnusualRecipe();
            allRecipes.removeIf(entry ->
                entry.category() == RecipeBookCategories.CRAFTING_MISC && entry.display().result().getFirst(worldContext).getItem()==Items.FIREWORK_ROCKET
            );
            craftableRecipes.removeIf(entry ->
                    entry.category() == RecipeBookCategories.CRAFTING_MISC && entry.display().result().getFirst(worldContext).getItem()==Items.FIREWORK_ROCKET
            );
        }

        craftableCategories.clear();
        for (RecipeDisplayEntry entry : craftableRecipes) {
            String category;
            if (!ModConfig.get().categorizeRecipes) {
                // dont categorize recipes
                category = I18n.translate("easiercrafting.category.possible");
            } else if (Objects.requireNonNull(getCat(entry)).getNamespace().startsWith(EasierCrafting.MODID)) {
                // generated recipe
                if (entry.display() instanceof RepairCraftingRecipeDisplay){
                    category = "Repair";
                } else {
                    category = I18n.translate("easiercrafting.category.special");
                }
            } else {
                category = getTranslatedItemGroup(entry);
            }
            craftableCategories.computeIfAbsent(category, k -> new RecipeTreeSet()).add(entry);
        }
        recalcListSize();

        for (RecipeDisplayEntry entry : craftableRecipes) {
            hashID^=entry.id().index();
        }

        return hashID==0;
    }

    @Override
    protected void drawRecipeGridOverlay(DrawContext context) {
        // get info
        int recipeWidth;
        List<SlotDisplay> ingredients;
        int maxCraftableStacks=1;
        Slot resultSlot = screenHandler.getSlot(resultSlotNo);
        switch (underMouse.display()) {
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
                // repair formular: durability = min(Item A uses + Item B uses + floor(Max uses / 20), Max uses) (from wiki)
                // damage = max(max-((max-damage a)+(max-damage b)+floor(Max/20)),0)
                // damage = max(damage a + damage b - floor(Max/20) - max , 0)
                // so best and easiest approach would be adding items with min durability
                drawHoloItem(context, resultSlot, repairDisplay.result().getFirst(worldContext));
                drawHoloItem(context, screenHandler.getSlot(firstCraftSlotNo), repairDisplay.ingredients().getFirst().getFirst(worldContext));
                drawHoloItem(context, screenHandler.getSlot(firstCraftSlotNo+1), repairDisplay.ingredients().get(1).getFirst(worldContext));

                return;
            }
            case null, default -> {
                return;
            }
        }

        // render result
        ItemStack resultStack = underMouse.display().result().getFirst(worldContext).copy();
        if (!(underMouse.display() instanceof RepairCraftingRecipeDisplay)) {
            resultStack.setCount(maxCraftableStacks*resultStack.getCount());
        }

        boolean canCraft = canCraft(underMouse);
        drawHoloItem(context, resultSlot, resultStack);
        if (!canCraft) context.fill(resultSlot.x-2,resultSlot.y-2,resultSlot.x+itemSize+2,resultSlot.y+itemSize+2,0x60FF0000);

        Object2IntOpenHashMap<Item> tempMap = avaliableItemMap.clone();

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < recipeWidth; x++) {
                if (y*recipeWidth+x >= ingredients.size()) return;
                SlotDisplay ingredient = ingredients.get(y*recipeWidth+x);
                if (ingredient.getFirst(worldContext).isEmpty()) continue;
                boolean found = false;
                for (ItemStack stack : ingredient.getStacks(worldContext)){
                    // cant craft
                    if (tempMap.getInt(stack.getItem())<=0) continue;
                    tempMap.addTo(stack.getItem(),-1);
                    renderCraftingIngredient(context,stack,screenHandler.getSlot(firstCraftSlotNo + y*gridSize+x));
                    found = true;
                    break;
                }
                if (!found) renderIngredient(context, ingredient.getStacks(worldContext), screenHandler.getSlot(firstCraftSlotNo + y*gridSize+x));
            }
        }
    }

    public void renderCraftingIngredient(DrawContext context, ItemStack stack, Slot slot) {
        if (stack.isEmpty()) return;
        drawHoloItem(context,slot,stack);
    }

    @Override
    protected void onRecipeClicked(RecipeDisplayEntry entry, int mouseButton) {
        List<Ingredient> recipeInput = entry.craftingRequirements().orElse(Collections.emptyList());
        if (recipeInput.isEmpty()) return;

        int maxCraftableStacks = 1;
        int rowadjust = 0;
        int recipeWidth;
        List<SlotDisplay> ingredients;

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
        boolean repairOccupied = false;
        for (int i = 0; i < ingredients.size(); i++) {
            int remaining = maxCraftableStacks;
            SlotDisplay ingredient = ingredients.get(i);
            if (ingredient.getStacks(worldContext).isEmpty()) continue;

            for (int slot = firstInventorySlotNo; remaining > 0 && slot < 36 + firstInventorySlotNo; slot++) {
                ItemStack slotcontent = screenHandler.getSlot(slot).getStack();
                // yes awful nested if statements... find a better logic for me then... but bare in mind mine works :3
                if (entry.display() instanceof RepairCraftingRecipeDisplay recipe){
                    if (slotcontent.getItem()!=recipe.result().getFirst(worldContext).getItem()) continue;
                    if (slotcontent.getDamage()>=recipe.ingredients().get(1).getFirst(worldContext).getDamage()){
                        // not as damage as most damaged
                        if (slotcontent.getDamage()<recipe.ingredients().getFirst().getFirst(worldContext).getDamage()){
                            if (repairOccupied) continue;
                            repairOccupied = true;
                        }
                    } else continue;
                }
                else if (!canActAsIngredient(ingredient, slotcontent)) continue;
                transfer(slot, i + firstCraftSlotNo + rowadjust, remaining);
                ItemStack inCraftSlot = screenHandler.getSlot(i + firstCraftSlotNo + rowadjust).getStack();
                remaining = maxCraftableStacks - inCraftSlot.getCount();
                if (!inCraftSlot.getRecipeRemainder().isEmpty()) {
                    removal[i] = true;
                }

            }
            if (recipeWidth > 0 && (i + 1) % recipeWidth == 0) {
                rowadjust += gridSize - recipeWidth;
            }
        }

        // actually craft item: hold control or right click to not instantly craft, hold q to drop
        if (mouseButton == 0 && !Screen.hasControlDown()) {
            if (isHoldingButton(GLFW.GLFW_KEY_Q)){
                if (Screen.hasShiftDown()) {
                    // icl but lazy method works well...
                    int resultCount = entry.display().result().getFirst(worldContext).getCount();
//                    int maxCount = entry.display().result().getFirst(worldContext).getMaxCount();
//                    int increment = 0;
                    LOGGER.info("bulk craft: "+maxCraftableStacks + " stacks of "+resultCount);
                    for (int i = 0; i < maxCraftableStacks; i++) {
//                        increment += resultCount;
//                        LOGGER.info(increment);
//                        if (increment >= maxCount) {
//                            LOGGER.info("try to throw away");
//                            increment = 0;
//                            slotClick(firstInventorySlotNo, 0, SlotActionType.PICKUP);
//                            slotClick(firstInventorySlotNo, 1, SlotActionType.THROW);
//                        }
//                        slotClick(resultSlotNo, 0, SlotActionType.PICKUP);
                        slotClick(resultSlotNo, 1, SlotActionType.THROW);
                    }
//                    LOGGER.info("try to throw away");
//                    slotClick(firstInventorySlotNo, 0, SlotActionType.PICKUP);
//                    slotClick(firstInventorySlotNo, 1, SlotActionType.THROW);
                } else {
                    slotClick(resultSlotNo, 0, SlotActionType.THROW);
                }
            } else {
                slotClick(resultSlotNo, 1, SlotActionType.QUICK_MOVE);
            }
            queueUpdateRecipe();

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

    private boolean recipeCraftable(RecipeDisplayEntry entry){
        Object2IntOpenHashMap<Item> tempMap = avaliableItemMap.clone();
        if (entry.craftingRequirements().isEmpty()) return false;
        List<Ingredient> ingredients= entry.craftingRequirements().get();

        for (Ingredient ingredient : ingredients) {
            if (ingredient.toDisplay().getFirst(worldContext).isEmpty()) continue;
            boolean canCraft = false;
            for (ItemStack stack : ingredient.toDisplay().getStacks(worldContext)) {
                // cant craft
                if (tempMap.getInt(stack.getItem()) <= 0) continue;
                canCraft = true;
                tempMap.addTo(stack.getItem(), -1);
                break;
            }
            if (!canCraft) return false;
        }

        return true;
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

            Item ingredient = set.getKey();
            ItemStack resultStack = ingredient.getDefaultStack();
            ItemStack ingredientA = ingredient.getDefaultStack();
            ItemStack ingredientB = ingredient.getDefaultStack();

            int damageA = 0;
            int damageB = 0;
            // find 2 slot with max damage
            for (int slot = firstInventorySlotNo; slot < 36 + firstInventorySlotNo; slot++) {
                ItemStack slotContent = screenHandler.getSlot(slot).getStack();
                if (slotContent.getItem()!=ingredient||slotContent.hasEnchantments()) continue;
                if (slotContent.getDamage() > damageA) {
                    damageB = damageA;
                    damageA = slotContent.getDamage();
                } else if (slotContent.getDamage() > damageB) {
                    damageB = slotContent.getDamage();
                }
            }

            ingredientA.setDamage(damageA);
            ingredientB.setDamage(damageB);
            resultStack.setDamage(Integer.max(damageA + damageB - resultStack.getMaxDamage()/20 - resultStack.getMaxDamage() , 0));


            // add the repairRecipe
            RepairCraftingRecipeDisplay display = new RepairCraftingRecipeDisplay(
                    List.of(new SlotDisplay.StackSlotDisplay(ingredientA),new SlotDisplay.StackSlotDisplay(ingredientB)),
                    new SlotDisplay.StackSlotDisplay(resultStack),
                    craftingBlock
            );

            RecipeDisplayEntry entry = new RecipeDisplayEntry(
                    // use hash code of the item
                    new NetworkRecipeId(set.getKey().hashCode()+resultStack.getDamage()),
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
