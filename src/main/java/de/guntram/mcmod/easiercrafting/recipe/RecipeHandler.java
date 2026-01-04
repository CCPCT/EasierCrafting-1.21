package de.guntram.mcmod.easiercrafting.recipe;

import de.guntram.mcmod.easiercrafting.extendedScreen.ExtendedGuiCrafting;
import de.guntram.mcmod.easiercrafting.extendedScreen.ExtendedGuiInventory;
import de.guntram.mcmod.easiercrafting.extendedScreen.ExtendedGuiStonecutter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.client.recipebook.RecipeBookType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.recipe.StonecuttingRecipe;
import net.minecraft.recipe.display.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.context.ContextParameterMap;
import net.minecraft.util.context.ContextType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class RecipeHandler {
    private static final Logger LOGGER = LogManager.getLogger(RecipeHandler.class);

    private static List<RecipeResultCollection> resultCollections = new ArrayList<>();
    private static final Set<RecipeDisplayEntry> craftableRecipeEntries = new HashSet<>(); //only craftable
    private static List<CuttingRecipeDisplay.GroupEntry<StonecuttingRecipe>> stoneCuttingRecipesCollection;
    private static final Map<CuttingRecipeDisplay.GroupEntry<StonecuttingRecipe>,Integer> craftableStoneCuttingRecipes = new HashMap<>();
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

    public static int getMaxCraftable(List<SlotDisplay> ingredients){
        int maxCraftableStacks = 64;
        Map<Item, Integer> ingredientMap = new HashMap<>();
        for (SlotDisplay ingredient : ingredients){
            List<ItemStack> chosenList = RecipeHandler.getCraftableStacks(ingredient);
            if (chosenList.isEmpty()) continue;
            Item chosenItem = chosenList.getFirst().getItem();

            // If chosenItem exists, add 1 to the current value.
            // If it doesn't exist, set the value to 1.
            ingredientMap.merge(chosenItem, 1, Integer::sum);
        }

        Map<Item, Integer> itemMap = RecipeHandler.getAvaliableItemMap();
        for (Map.Entry<Item, Integer> ingredientSet : ingredientMap.entrySet()) {
            maxCraftableStacks = Math.min(Math.min(maxCraftableStacks,itemMap.get(ingredientSet.getKey())/ingredientSet.getValue()),ingredientSet.getKey().getMaxCount());
        }
        return maxCraftableStacks;
    }

    public static void updateRecipes(Class<? extends Screen> screen){
        if (screen==null) return;
        assert MinecraftClient.getInstance().player != null;

        // initialise var
        craftableRecipeEntries.clear();

        // special case for stonecutter (and prob other screens...)
        if (screen==ExtendedGuiStonecutter.class){
            updateStoneCutterRecipes();
            return;
        }
        RecipeBookType bookType = screenClassToRecipeBookType.get(screen);
        // unsupported inventory
        if (bookType==null) return;
        resultCollections = MinecraftClient.getInstance().player.getRecipeBook().getResultsForCategory(bookType);

        // all recipe collection

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

    public static void updateStoneCutterRecipes(){
        assert MinecraftClient.getInstance().world != null;
        stoneCuttingRecipesCollection = MinecraftClient.getInstance().world.getRecipeManager().getStonecutterRecipes().entries();
        craftableStoneCuttingRecipes.clear();
        for (CuttingRecipeDisplay.GroupEntry<StonecuttingRecipe> recipe : stoneCuttingRecipesCollection){
            if (recipe.input().isEmpty()) continue;
            for (Map.Entry<Item, Integer> avaliable : avaliableItemMap.entrySet()){
                if (recipe.input().test(avaliable.getKey().getDefaultStack())){
                    // tested success
                    craftableStoneCuttingRecipes.put(recipe,Math.min(avaliable.getKey().getMaxCount(),avaliable.getValue()));
                    break;
                }
            }
        }


        LOGGER.info("stonecutter recipe count: {}", stoneCuttingRecipesCollection.size());
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
    public static Set<RecipeDisplayEntry> getCraftableRecipeEntries() {
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
    public static List<CuttingRecipeDisplay.GroupEntry<StonecuttingRecipe>> getStoneCuttingRecipesCollection() {
        return stoneCuttingRecipesCollection;
    }
    public static Map<CuttingRecipeDisplay.GroupEntry<StonecuttingRecipe>,Integer> getCraftableStoneCuttingRecipes() {
        return craftableStoneCuttingRecipes;
    }


}
