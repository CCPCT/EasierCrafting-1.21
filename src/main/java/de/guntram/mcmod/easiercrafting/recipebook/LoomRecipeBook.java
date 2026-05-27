package de.guntram.mcmod.easiercrafting.recipebook;

import de.guntram.mcmod.easiercrafting.EasierCrafting;
import de.guntram.mcmod.easiercrafting.recipe.LoomRecipe;
import de.guntram.mcmod.easiercrafting.recipe.LoomRecipeDisplay;
import de.guntram.mcmod.easiercrafting.recipe.LoomRecipeHandler;
import de.guntram.mcmod.easiercrafting.recipe.RecipeTreeSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

import java.util.*;

import static de.guntram.mcmod.easiercrafting.EasierCrafting.SPECIAL_CAT;

public class LoomRecipeBook extends AbstractRecipeBook {

    public LoomRecipeBook(AbstractContainerScreen<? extends AbstractContainerMenu> craftScreen) {
        super(craftScreen, 0, 1, 3, 4,  getSlotDisplay(Items.LOOM));
        LoomRecipeHandler.customRecipes.clear();
    }

    final static int DYE_SLOT = 1;
    final static int PATTERN_SLOT = 2;
    private static final Queue<LoomTask> actionQueue = new LinkedList<>();
    private static boolean updatable = true;

    @Override
    public void updateRecipes() {
        if (!updatable) return;
        updateAvailableStacks();

        craftableRecipes.clear();
        allRecipes.clear();

        for (LoomRecipe recipe : LoomRecipeHandler.LOADED_RECIPES) {
            allRecipes.add(recipeToEntry(recipe));
        }

        LoomRecipeHandler.onPasteButtonClicked();
        for (LoomRecipe recipe : LoomRecipeHandler.customRecipes) {
            allRecipes.add(recipeToEntry(recipe));
        }

        // add craftable recipes
        allRecipes.stream().filter(this::canCraftScanned).forEach(craftableRecipes::add);
        //allRecipes.forEach(craftableRecipes::add);
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
        Item baseItem = stringToItem(recipe.baseBanner());
        ItemStack bannerStack = new ItemStack(baseItem);

        // 2. Setup the Builder for patterns
        // We need the registry to turn the String ID into a real Pattern Entry
        var registryManager = world.registryAccess();
        var patternRegistry = registryManager.lookupOrThrow(net.minecraft.core.registries.Registries.BANNER_PATTERN);
        BannerPatternLayers.Builder patternBuilder = new BannerPatternLayers.Builder();
        // --- END SIMULATION START ---

        ingredients.add(Ingredient.of(baseItem));

        for (LoomRecipe.BannerStep step : recipe.steps()) {
            // Safe Color Conversion
            DyeColor color = DyeColor.valueOf(step.dye().toUpperCase());
            Item dyeItem = stringToItem(step.dye()+"_dye");

            // --- SIMULATE LAYER ADDITION ---
            patternRegistry.get(Identifier.parse(step.pattern())).ifPresent(entry -> {
                patternBuilder.add(entry, color);
            });
            // -------------------------------

            dyeListDisplay.add(getSlotDisplay(dyeItem));
            ingredients.add(Ingredient.of(dyeItem));
            patternList.add(step.pattern());

            if (SPECIAL_PATTERNS.containsKey(step.pattern())) {
                ingredients.add(Ingredient.of(SPECIAL_PATTERNS.get(step.pattern())));
            }
        }

        // --- APPLY SIMULATED DATA ---
        bannerStack.set(DataComponents.BANNER_PATTERNS, patternBuilder.build());
        if (recipe.name() != null) {
            bannerStack.set(DataComponents.CUSTOM_NAME, Component.literal(recipe.name()));
        }
        // ----------------------------

        LoomRecipeDisplay display = new LoomRecipeDisplay(
                dyeListDisplay,
                patternList,
                getSlotDisplay(bannerStack), // Now uses the simulated stack
                getSlotDisplay(Items.LOOM)
        );



        return new RecipeDisplayEntry(
                new RecipeDisplayId(recipe.hashCode()),
                display,
                OptionalInt.empty(),
                SPECIAL_CAT,
                Optional.of(ingredients)
        );
    }

    @Override
    protected void onRecipeClicked(RecipeDisplayEntry entry, int mouseButton) {
        // todo
        if (!(screenHandler instanceof LoomMenu container)||!(entry.display() instanceof LoomRecipeDisplay recipe)) {
            return;
        }

        // no item -> return
        if (!canCraft(entry)) return;

        if (entry.craftingRequirements().isEmpty()) return;
        var ingredient = entry.craftingRequirements().get();

        // move banner up
        int bannerSlot = findEmptyBanner(ingredient.getFirst().display().resolveForFirstStack(worldContext).getItem());
        if (bannerSlot==-1) return;
        LoomTask.click(bannerSlot,0, ContainerInput.PICKUP);
        LoomTask.click(FIRST_CRAFT_SLOT,1, ContainerInput.PICKUP);
        LoomTask.click(bannerSlot,0, ContainerInput.PICKUP);


        int ingredientIndex = 1;
        for (int i = 0; i < recipe.dye().size(); i++) {
            LOGGER.info("try craft loom, loop {}",i);
            //move dye
            int dyeSlot = findItem(recipe.dye().get(i).resolveForFirstStack(worldContext).getItem());
            LoomTask.click(dyeSlot,0,ContainerInput.PICKUP);
            LoomTask.click(DYE_SLOT,1,ContainerInput.PICKUP);
            LoomTask.click(dyeSlot,0,ContainerInput.PICKUP);

            LOGGER.info(recipe.pattern().get(i));
            LOGGER.info(ingredient.get(ingredientIndex).display().resolveForFirstStack(worldContext).getItem().asItem().getDescriptionId());
            Item pattern = SPECIAL_PATTERNS.get(recipe.pattern().get(i));
            if (pattern != null) {
                int patternSlot = findItem(pattern);
                LoomTask.click(patternSlot,0,ContainerInput.QUICK_MOVE);
            } else {
                LoomTask.button(Identifier.parse(recipe.pattern().get(i)));
                ingredientIndex--;
            }

            LoomTask.click(FIRST_RESULT_SLOT,0,ContainerInput.PICKUP);
            LoomTask.click(FIRST_CRAFT_SLOT,0,ContainerInput.PICKUP);

            if (pattern != null) {
                LoomTask.click(PATTERN_SLOT,0,ContainerInput.QUICK_MOVE);
            }

            ingredientIndex+=2;
        }
        LoomTask.click(FIRST_CRAFT_SLOT,0,ContainerInput.QUICK_MOVE);
        LoomTask.update();
    }

    public record LoomTask(int slot, int button, ContainerInput type, Identifier buttonID) {
        // Helper constructor for a standard slot click
        public static void click(int slot, int button, ContainerInput type) {
            actionQueue.add(new LoomTask(slot, button, type, null));
        }

        // Helper constructor for a button press
        public static void button(Identifier buttonId) {
            actionQueue.add(new LoomTask(0, 0, null, buttonId));
        }

        // update
        public static void update() {
            actionQueue.add(new LoomTask(0, 0, null, null));
        }
    }

    public static void onTick() {
        if (actionQueue.isEmpty()) {
            updatable = true;
            return;
        }
        LoomTask task = actionQueue.poll();
        if (task==null) return;
        if (task.type == null && task.buttonID == null) {
            EasierCrafting.updateRecipe();
            return;
        }
        updatable=false;


        LocalPlayer player = Minecraft.getInstance().player;
        if (player==null) return;
        MultiPlayerGameMode interactionManager = Minecraft.getInstance().gameMode;
        assert interactionManager != null;
        AbstractContainerMenu screenHandler = player.containerMenu;

        if (task.buttonID != null) {
            //button
            if (!(screenHandler instanceof LoomMenu container)) return;
            List<Holder<BannerPattern>> availablePatterns = container.getSelectablePatterns();
            for (int j = 0; j < availablePatterns.size(); j++) {
                if (availablePatterns.get(j).is(task.buttonID)) {
                    
                    interactionManager.handleInventoryButtonClick(screenHandler.containerId, j);
                    EasierCrafting.info(availablePatterns.get(j).getRegisteredName());
                    return;
                }
            }
        } else {
            // slot
            interactionManager.handleContainerInput(screenHandler.containerId, task.slot, task.button, task.type, player);
        }
    }

    @Override
    protected void drawRecipeGridOverlay(GuiGraphicsExtractor context) {
        if (!(underMouse.display() instanceof LoomRecipeDisplay recipe)) return;
        ItemStack result = recipe.result().resolveForFirstStack(worldContext).copy();
        boolean canCraft = canCraft(underMouse);

        final int y = -ITEM_SIZE -1;
        if (underMouse.craftingRequirements().isEmpty()) return;
        List<Ingredient> ingredients = underMouse.craftingRequirements().get();
        if (ingredients.isEmpty()) return;

        // banner
        ItemStack bannerIngredient = ingredients.getFirst().display().resolveForFirstStack(worldContext);
        drawHoloItem(context,screenHandler.getSlot(0), bannerIngredient);
        if (findEmptyBanner(bannerIngredient.getItem()) == -1) {
            context.fill(screenHandler.getSlot(0).x,
                    screenHandler.getSlot(0).y,
                    screenHandler.getSlot(0).x+ITEM_SIZE,
                    screenHandler.getSlot(0).y+ITEM_SIZE,
                    CANT_CRAFT_COLOUR
                    );
        }

        Object2IntOpenHashMap<Item> inventory = avaliableItemMap.clone();

        for (int i = 1; i < ingredients.size(); i++) {
            int x = (i-1)*(ITEM_SIZE + itemDisplaySpacing);
            ItemStack stack = ingredients.get(i).display().resolveForFirstStack(worldContext);

            context.item(stack, x, y);
            context.itemDecorations(textRenderer,stack,x,y);
            if (inventory.getInt(stack.getItem()) <= 0) {
                context.fill(x, y, x + ITEM_SIZE, y + ITEM_SIZE, CANT_CRAFT_COLOUR);
            } else {
                inventory.addTo(stack.getItem(),-1);
            }
        }

        // draw result
        Slot resultSlot = screenHandler.getSlot(FIRST_RESULT_SLOT);
        drawHoloItem(context,resultSlot,result);

        if (!canCraft) context.fill(resultSlot.x-2,resultSlot.y-2,resultSlot.x+ ITEM_SIZE +2,resultSlot.y+ ITEM_SIZE +2,0x60FF0000);

        //renderIngredient(context, getIngredients(underMouse), screenHandler.getSlot(firstCraftSlotNo));
    }

    @Override
    protected boolean refreshCategories() {
        craftableCategories.clear();
        int tempHash = 0;
        for (RecipeDisplayEntry entry : allRecipes) {
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
        if (entry.craftingRequirements().isEmpty()) return false;

        // check if banner is empty
        Item required = entry.craftingRequirements().get().getFirst().display().resolveForFirstStack(worldContext).getItem();
        if (findEmptyBanner(required)==-1) return false;

        var tempMap = avaliableItemMap.clone();
        for (Ingredient i : entry.craftingRequirements().get()) {
            Item ingItem = i.display().resolveForFirstStack(worldContext).getItem();
            if (tempMap.getInt(ingItem)<=0) {
                return false;
            }
            if (ingItem instanceof DyeItem) {
                tempMap.addTo(ingItem,-1);
            }
        }
        return true;
    }

    protected int findEmptyBanner(Item lookFor) {
        List<Slot> inventory = screen.getMenu().slots;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.get(i).getItem();
            if (stack.getItem() == lookFor) {
                if (stack.get(DataComponents.BANNER_PATTERNS) == null || stack.get(DataComponents.BANNER_PATTERNS).layers().isEmpty()) {
                    return i;
                }
            }
        }
        return -1;
    }

    protected int findItem(Item lookFor) {
        List<Slot> inventory = screen.getMenu().slots;
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i).getItem().getItem() == lookFor) {
                return i;
            }
        }

        return -1;
    }

    private Item stringToItem(String string) {
        var optional = BuiltInRegistries.ITEM.get(Identifier.withDefaultNamespace(string.replace("minecraft:","")));
        return optional.map(Holder.Reference::value).orElse(Items.AIR);
    }
}
