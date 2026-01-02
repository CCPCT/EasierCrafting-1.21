package de.guntram.mcmod.easiercrafting.recipe;

import de.guntram.mcmod.easiercrafting.extendedScreen.ExtendedGuiCrafting;
import de.guntram.mcmod.easiercrafting.extendedScreen.ExtendedGuiInventory;
import de.guntram.mcmod.easiercrafting.extendedScreen.ExtendedGuiStonecutter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.FurnaceScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.client.recipebook.RecipeBookType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.recipe.display.ShapedCraftingRecipeDisplay;
import net.minecraft.recipe.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.recipe.display.SlotDisplayContexts;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.context.ContextParameterMap;
import net.minecraft.util.context.ContextType;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeHandler {
    private static List<RecipeResultCollection> resultCollections = new ArrayList<>();
    private static final List<RecipeDisplayEntry> craftableRecipeEntries = new ArrayList<>(); //only craftable
    final static ContextParameterMap EMPTY_CONTEXT = new ContextParameterMap.Builder().build(new ContextType.Builder().build());
    static List<Item> avaliableItems;
    static Map<Item, Integer> avaliableItemMap = new HashMap<>();

    private static final Map<Class<? extends Screen>, RecipeBookType> screenClassToRecipeBookType = Map.of(
            ExtendedGuiCrafting.class, RecipeBookType.CRAFTING,
            ExtendedGuiInventory.class, RecipeBookType.CRAFTING
    );

    public static void updateAvailableStacks() {
        PlayerEntity player = MinecraftClient.getInstance().player;
        avaliableItems = new ArrayList<>();
        avaliableItemMap = new HashMap<>();
        if (player==null) return;
        // Iterate through slots (usually 0-35 for player inventory)
        for (ItemStack itemStack : player.getInventory().getMainStacks()) {
            if (itemStack.isEmpty()) continue;
            avaliableItems.add(itemStack.getItem());
            avaliableItemMap.merge(itemStack.getItem(), itemStack.getCount(), Integer::sum);
        }
    }

    private static int getMaxCraftable(RecipeDisplayEntry recipe) {
        var requirements = recipe.craftingRequirements().get();
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


    public static void updateRecipes(Class<? extends Screen> screen){
        if (screen==null) return;
        assert MinecraftClient.getInstance().player != null;
        RecipeBookType bookType = screenClassToRecipeBookType.get(screen);
        // unsupported inventory
        if (bookType==null) return;
        resultCollections = MinecraftClient.getInstance().player.getRecipeBook().getResultsForCategory(bookType);
        craftableRecipeEntries.clear();
        // all recipe collection
        System.out.println("all collection count: "+resultCollections.size());

        //List<ItemStack> availableItems = getAvailableStacks();

        for (RecipeResultCollection result : resultCollections){
            //System.out.println("found collection: "+result.getAllRecipes().getFirst().getStacks(EMPTY_CONTEXT).getFirst().getName());

            for (RecipeDisplayEntry entry : result.getAllRecipes()) {
                // craftable entries
                if (screen == ExtendedGuiInventory.class) {
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
                    craftableRecipeEntries.add(entry);
                    //System.out.println("can craft: " + entry.display().result().getFirst(getWorldContext()).getName().getString()+", cat: "+getCat(entry).getPath());
                }
            }
        }
    }

    public static List<ItemStack> getCraftableStacks(SlotDisplay ingredient){
        return getAllIngredients(ingredient).stream()
                .filter(stack -> getAvailableItems().contains(stack.getItem()))
                .toList();
    }

    public static List<ItemStack> getAllIngredients(SlotDisplay ingredient){
        return ingredient.getStacks(getWorldContext());
    }

    public static Identifier getCat(RecipeDisplayEntry entry){
        //System.out.println(MinecraftClient.getInstance().world.getRegistryManager().getOptional(RegistryKeys.RECIPE_BOOK_CATEGORY).get().getId(entry.category()).getPath());
        return MinecraftClient.getInstance().world.getRegistryManager().getOptional(RegistryKeys.RECIPE_BOOK_CATEGORY).get().getId(entry.category());
    }

    // getters
    public static List<RecipeResultCollection> getRecipeCollections() {
        return resultCollections;
    }
    public static List<RecipeDisplayEntry> getCraftableRecipeEntries() {
        return craftableRecipeEntries;
    }
    public static ContextParameterMap getEmptyContext(){
        return EMPTY_CONTEXT;
    }
    public static RecipeBookType getRecipeBookTypeFromScreenClass(Class<? extends Screen> screenClass){
        return screenClassToRecipeBookType.get(screenClass);
    }
    public static ContextParameterMap getWorldContext(){
        assert MinecraftClient.getInstance().world != null;
        return SlotDisplayContexts.createParameters(MinecraftClient.getInstance().world);
    }
    public static List<Item> getAvailableItems() {
        return avaliableItems;
    }
    public static Map<Item,Integer> getAvaliableItemMap(){
        return avaliableItemMap;
    }

}
