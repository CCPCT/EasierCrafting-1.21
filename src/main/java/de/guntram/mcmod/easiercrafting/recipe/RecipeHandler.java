package de.guntram.mcmod.easiercrafting.recipe;

import de.guntram.mcmod.easiercrafting.ExtendedGuiCrafting;
import de.guntram.mcmod.easiercrafting.ExtendedGuiInventory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.FurnaceScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.client.recipebook.RecipeBookType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.recipe.display.ShapedCraftingRecipeDisplay;
import net.minecraft.recipe.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.recipe.display.SlotDisplayContexts;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.context.ContextParameterMap;
import net.minecraft.util.context.ContextType;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecipeHandler {
    private static List<RecipeResultCollection> resultCollections;
    private static List<RecipeDisplayEntry> craftableRecipeEntries = new ArrayList<>(); //only craftable
    final static ContextParameterMap EMPTY_CONTEXT = new ContextParameterMap.Builder().build(new ContextType.Builder().build());

    private static final Map<Class<? extends Screen>, RecipeBookType> screenClassToRecipeBookType = Map.of(
            InventoryScreen.class, RecipeBookType.CRAFTING,
            CraftingScreen.class, RecipeBookType.CRAFTING,
            FurnaceScreen.class, RecipeBookType.FURNACE,
            ExtendedGuiCrafting.class, RecipeBookType.CRAFTING,
            ExtendedGuiInventory.class, RecipeBookType.CRAFTING
    );

    private static List<ItemStack> getAvailableItems() {
        PlayerEntity player = MinecraftClient.getInstance().player;
        List<ItemStack> items = new ArrayList<>();
        if (player==null) return items;
        // Iterate through slots (usually 0-35 for player inventory)
        for (ItemStack itemStack : player.getInventory().main) {
            if (itemStack.isEmpty()) continue;
            items.add(itemStack);
        }
        return items;
    }

    private static boolean canCraft(RecipeDisplayEntry recipe, List<ItemStack> inventory) {
        // 1. Get the list of ingredients required
        List<Ingredient> ingredients = recipe.craftingRequirements().get();
        if (ingredients.isEmpty()) return false;

        // 2. Create a temporary copy of the inventory to "consume" items from
        List<ItemStack> workingInv = new ArrayList<>(inventory.stream().map(ItemStack::copy).toList());

        for (Ingredient ingredient : ingredients) {
            if (ingredient.isEmpty()) continue;

            boolean found = false;
            for (int i = 0; i < workingInv.size(); i++) {
                ItemStack stack = workingInv.get(i);
                if (ingredient.test(stack)) {
                    stack.decrement(1); // "Use" one item
                    if (stack.isEmpty()) workingInv.remove(i);
                    found = true;
                    break;
                }
            }
            if (!found) return false; // Missing an ingredient
        }
        return true;
    }

    public static void updateRecipes(Class<? extends Screen> screen){
        if (screen==null) return;
        assert MinecraftClient.getInstance().player != null;
        resultCollections = MinecraftClient.getInstance().player.getRecipeBook().getResultsForCategory(screenClassToRecipeBookType.get(screen));
        craftableRecipeEntries.clear();
        // all recipe collection
        System.out.println("all collection count: "+resultCollections.size());

        List<ItemStack> availableItems = getAvailableItems();

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
                if (canCraft(entry,availableItems)) {
                    craftableRecipeEntries.add(entry);
                    //System.out.println("can craft: " + entry.display().result().getFirst(getEmptyContext()).getName().getString()+", cat: "+getCat(entry).getPath());
                }
            }
        }
    }

    public static ItemStack getResult(RecipeResultCollection recipe){
        World world = MinecraftClient.getInstance().world;
        assert world != null;
        return recipe.getAllRecipes().getFirst().display().result().getFirst(SlotDisplayContexts.createParameters(world));
    }

    public static Identifier getCat(RecipeDisplayEntry entry){
        return MinecraftClient.getInstance().world.getRegistryManager().getOptional(RegistryKeys.RECIPE_BOOK_CATEGORY).get().getId(entry.category());
    }

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
}
