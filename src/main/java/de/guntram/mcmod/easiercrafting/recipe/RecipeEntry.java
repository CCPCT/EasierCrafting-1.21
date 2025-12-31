//package de.guntram.mcmod.easiercrafting.recipe;
//
//import net.minecraft.item.ItemStack;
//import net.minecraft.recipe.Ingredient;
//import net.minecraft.recipe.Recipe;
//import net.minecraft.recipe.display.RecipeDisplay;
//import net.minecraft.recipe.display.SlotDisplay;
//import net.minecraft.util.Identifier;
//import net.minecraft.util.context.ContextParameterMap;
//import net.minecraft.util.context.ContextType;
//
//import java.util.Collections;
//import java.util.List;
//
//public class RecipeEntry {
//    private final Identifier id;
//    private final Recipe<?> recipe;
//    private final ItemStack result;
//    private final List<Ingredient> ingredients;
//
//    // Use this for standard recipes found in the RecipeManager
//    public RecipeEntry(Identifier id, Recipe<?> recipe) {
//        this.id = id;
//        this.recipe = recipe;
//        this.result = resolveResult();
//        this.ingredients = resolveIngedrients();
//    }
//    public RecipeEntry(Identifier id, Recipe<?> recipe, ItemStack result) {
//        this.id = id;
//        this.recipe = recipe;
//        this.result = result;
//        this.ingredients = resolveIngedrients();
//    }
//
//    private ItemStack resolveResult() {
//        List<RecipeDisplay> displays = recipe.getDisplays();
//
//        if (!displays.isEmpty()) {
//            RecipeDisplay display = displays.get(0);
//            if (display.result() instanceof SlotDisplay.StackSlotDisplay(ItemStack stack)) {
//                return stack;
//            }
//        }
//        return ItemStack.EMPTY;
//    }
//
//    private List<Ingredient> resolveIngedrients() {
//        return recipe.getIngredientPlacement().getIngredients();
//    }
//
//    public Identifier getId() { return id; }
//    public Recipe<?> getRecipe() { return recipe; }
//    public ItemStack getResult() { return result; }
//
//    public String getDisplayName() {
//        return result.isEmpty() ? "Unknown" : result.getName().getString();
//    }
//}