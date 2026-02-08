package de.guntram.mcmod.easiercrafting.recipebook;

import de.guntram.mcmod.easiercrafting.EasierCrafting;
import de.guntram.mcmod.easiercrafting.recipe.LoomRecipe;
import de.guntram.mcmod.easiercrafting.recipe.LoomRecipeDisplay;
import de.guntram.mcmod.easiercrafting.recipe.LoomRecipeHandler;
import de.guntram.mcmod.easiercrafting.recipe.RecipeTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import static de.guntram.mcmod.easiercrafting.EasierCrafting.SPECIAL_CAT;

public class LoomRecipeBook extends AbstractRecipeBook {

    public LoomRecipeBook(HandledScreen<? extends ScreenHandler> craftScreen) {
        super(craftScreen, 0, 1, 3, 4,  getSlotDisplayFromItem(Items.LOOM));
        LoomRecipeHandler.customRecipes.clear();
    }

    final static int DYE_SLOT = 1;
    final static int PATTERN_SLOT = 2;

    @Override
    public void updateRecipes() {
        updateAvailableStacks();

        craftableRecipes.clear();
        allRecipes.clear();

        for (LoomRecipe recipe : LoomRecipeHandler.LOADED_RECIPES) {
            allRecipes.add(recipeToEntry(recipe));
        }

        for (LoomRecipe recipe : LoomRecipeHandler.customRecipes) {
            allRecipes.add(recipeToEntry(recipe));
        }

        // add craftable recipes
        //allRecipes.stream().filter(this::canCraftScanned).forEach(craftableRecipes::add);
        allRecipes.forEach(craftableRecipes::add);
    }


    public static final Map<String, Item> SPECIAL_PATTERNS = Map.of(
            "minecraft:flower", Items.FLOWER_BANNER_PATTERN,
            "minecraft:creeper", Items.CREEPER_BANNER_PATTERN,
            "minecraft:skull", Items.SKULL_BANNER_PATTERN,
            "minecraft:mojang", Items.MOJANG_BANNER_PATTERN,
            "minecraft:globe", Items.GLOBE_BANNER_PATTERN,
            "minecraft:piglin", Items.PIGLIN_BANNER_PATTERN,
            "minecraft:flow", Items.FLOW_BANNER_PATTERN,
            "minecraft:guster", Items.GUSTER_BANNER_PATTERN,
            "minecraft:bricks", Items.FIELD_MASONED_BANNER_PATTERN,
            "minecraft:curly_border", Items.BORDURE_INDENTED_BANNER_PATTERN
    );

    public void saveRecipe(ItemStack banner, boolean permanent) {
        LoomRecipe.fromItemStack(banner, EasierCrafting.getIp()).ifPresent(recipe -> {

            // 1. Add to the in-memory list for the UI
            // (You can still build your RecipeDisplayEntry here)
            allRecipes.add(recipeToEntry(recipe));

            // 2. Save to file system
            if (permanent) {
                LoomRecipeHandler.saveRecipe(recipe);
            }
        });
    }

    public RecipeDisplayEntry recipeToEntry(LoomRecipe recipe) {
        // Standard lists for the RecipeDisplay
        List<SlotDisplay> dyeListDisplay = new ObjectArrayList<>();
        List<Ingredient> ingredients = new ObjectArrayList<>();
        List<String> patternList = new ObjectArrayList<>();

        // --- START SIMULATION ---
        // 1. Initialize the Base Banner
        Item baseItem = Registries.ITEM.get(Identifier.of(recipe.baseBanner()));
        ItemStack bannerStack = new ItemStack(baseItem);

        // 2. Setup the Builder for patterns
        // We need the registry to turn the String ID into a real Pattern Entry
        var registryManager = world.getRegistryManager();
        var patternRegistry = registryManager.getOrThrow(RegistryKeys.BANNER_PATTERN);
        BannerPatternsComponent.Builder patternBuilder = new BannerPatternsComponent.Builder();
        // --- END SIMULATION START ---

        ingredients.add(Ingredient.ofItem(baseItem));

        for (LoomRecipe.BannerStep step : recipe.steps()) {
            // Safe Color Conversion
            DyeColor color = DyeColor.valueOf(step.dye().toUpperCase());
            Item dyeItem = DyeItem.byColor(color);

            // --- SIMULATE LAYER ADDITION ---
            patternRegistry.getEntry(Identifier.of(step.pattern())).ifPresent(entry -> {
                patternBuilder.add(entry, color);
            });
            // -------------------------------

            dyeListDisplay.add(new SlotDisplay.StackSlotDisplay(dyeItem.getDefaultStack()));
            ingredients.add(Ingredient.ofItem(dyeItem));
            patternList.add(step.pattern());

            if (SPECIAL_PATTERNS.containsKey(step.pattern())) {
                ingredients.add(Ingredient.ofItem(SPECIAL_PATTERNS.get(step.pattern())));
            }
        }

        // --- APPLY SIMULATED DATA ---
        bannerStack.set(DataComponentTypes.BANNER_PATTERNS, patternBuilder.build());
        if (recipe.name() != null) {
            bannerStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(recipe.name()));
        }
        // ----------------------------

        LoomRecipeDisplay display = new LoomRecipeDisplay(
                dyeListDisplay,
                patternList,
                new SlotDisplay.StackSlotDisplay(bannerStack), // Now uses the simulated stack
                new SlotDisplay.ItemSlotDisplay(Items.LOOM)
        );

        return new RecipeDisplayEntry(
                new NetworkRecipeId(recipe.hashCode()),
                display,
                OptionalInt.empty(),
                SPECIAL_CAT,
                Optional.of(ingredients)
        );
    }

    @Override
    protected void onRecipeClicked(RecipeDisplayEntry entry, int mouseButton) {
        // todo
//        ClientPlayerInteractionManager interactionManager = client.interactionManager;
//        if (!(screenHandler instanceof LoomScreenHandler container)||!(entry.display() instanceof LoomRecipeDisplay recipe)) {
//            return;
//        }
//
//        // no item -> return
//        for (ItemStack ingredient : getIngredients(entry)) {
//            if (!avaliableItemMap.containsKey(ingredient.getItem()))return;
//        }
//        // move item to crafting slot
//        search:
//        for (int slot = firstInventorySlotNo; slot < 36 + firstInventorySlotNo; slot++) {
//            ItemStack slotContent = container.getSlot(slot).getStack();
//            for (ItemStack ingredientStack : recipe.input().getStacks(worldContext)) {
//                if (ingredientStack.getItem().equals(slotContent.getItem())){
//                    if (Screen.hasShiftDown()) {
//                        slotClick(slot, 0, SlotActionType.PICKUP);
//                        slotClick(slot, 0, SlotActionType.PICKUP_ALL);
//                        slotClick(firstCraftSlotNo, 0, SlotActionType.PICKUP);
//                        slotClick(slot, 0, SlotActionType.PICKUP);
//                    } else {
//                        slotClick(slot, 0, SlotActionType.PICKUP);
//                        slotClick(firstCraftSlotNo, 1, SlotActionType.PICKUP);
//                        slotClick(slot, 0, SlotActionType.PICKUP);
//                    }
//                    break search;
//                }
//            }
//        }
//
//        // click the recipe button (select the recipe)
//        List<CuttingRecipeDisplay.GroupEntry<StonecuttingRecipe>> available = container.getAvailableRecipes().entries();
//        int buttonIndex = -1;
//
//        for (int i = 0; i < available.size(); i++) {
//            // Compare the recipe entries directly.
//            if (available.get(i).recipe().optionDisplay().equals(recipe.result())) {
//                buttonIndex = i;
//                break;
//            }
//        }
//
//        if (buttonIndex != -1 && interactionManager != null) {
//            // 3. Select the recipe by clicking the button with the index
//            interactionManager.clickButton(container.syncId, buttonIndex);
//
//            // 4. Take the result from the output slot (slot 1) to complete the craft
//            if (Screen.hasControlDown()) return;
//            slotClick(1, 0, isHoldingButton(GLFW.GLFW_KEY_Q) ? SlotActionType.THROW : SlotActionType.QUICK_MOVE);
//        }
    }

    @Override
    protected void drawRecipeGridOverlay(DrawContext context) {
        if (!(underMouse.display() instanceof LoomRecipeDisplay recipe)) return;
        ItemStack result = recipe.result().getFirst(worldContext).copy();
        boolean canCraft = canCraft(underMouse);

        final int y = -itemSize-1;
        List<Ingredient> ingredients = underMouse.craftingRequirements().get();
        if (ingredients.isEmpty()) return;

        drawHoloItem(context,screenHandler.getSlot(0), ingredients.getFirst().toDisplay().getFirst(worldContext));

        for (int i = 1; i < ingredients.size(); i++) {
            int x = (i-1)*(itemSize + itemDisplaySpacing);
            ItemStack stack = ingredients.get(i).toDisplay().getFirst(worldContext);

            context.drawItem(stack, (i-1)*(itemSize + itemDisplaySpacing), y);
            context.drawStackOverlay(textRenderer,stack,x,y);
        }

        // draw result
        Slot resultSlot = screenHandler.getSlot(resultSlotNo);
        drawHoloItem(context,resultSlot,result);

        if (!canCraft) context.fill(resultSlot.x-2,resultSlot.y-2,resultSlot.x+itemSize+2,resultSlot.y+itemSize+2,0x60FF0000);

        //renderIngredient(context, getIngredients(underMouse), screenHandler.getSlot(firstCraftSlotNo));
    }

    @Override
    protected boolean refreshCategories() {
        craftableCategories.clear();
        int tempHash = 0;
        for (RecipeDisplayEntry entry : craftableRecipes) {
            if (!(entry.display() instanceof LoomRecipeDisplay recipe)) continue;
            craftableCategories.computeIfAbsent(DEFAULT_CAT,
                    k -> new RecipeTreeSet()).add(entry);
            tempHash^=entry.id().index();
        }
        boolean changed = tempHash==categoryHash;
        categoryHash=tempHash;
        recalcListSize();
        return changed;
    }

    @Override
    protected boolean canCraftScanned(RecipeDisplayEntry entry) {
        for (Ingredient i : entry.craftingRequirements().get()) {
            if (!avaliableItemMap.containsKey(i.toDisplay().getFirst(worldContext).getItem())) return false;
        }
        return true;
    }

    protected List<ItemStack> getIngredients(RecipeDisplayEntry entry) {
        List<ItemStack> all = entry.craftingRequirements().get().stream()
                .map(ingredient -> ingredient.toDisplay().getFirst(worldContext))
                .toList();

        List<ItemStack> craftable = all.stream().filter(stack -> getAvailableItemSet().contains(stack.getItem())).toList();


        if (craftable.isEmpty()){
            return all;
        } else {
            return craftable;
        }
    }

}
