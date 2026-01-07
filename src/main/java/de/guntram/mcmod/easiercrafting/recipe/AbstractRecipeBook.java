package de.guntram.mcmod.easiercrafting.recipe;

import de.guntram.mcmod.easiercrafting.*;
import de.guntram.mcmod.easiercrafting.modConfig.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.recipe.display.CuttingRecipeDisplay;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.recipe.display.SlotDisplayContexts;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.context.ContextParameterMap;
import net.minecraft.util.context.ContextType;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public abstract class AbstractRecipeBook<T> {

    protected final Logger LOGGER;
    static Identifier ARROWS;
    static Map<Item,Integer> avaliableItemMap;
    ContextParameterMap worldContext;
    static final ContextParameterMap EMPTY_CONTEXT = new ContextParameterMap.Builder().build(new ContextType.Builder().build());

    // Protected fields for subclasses
    public final HandledScreen<? extends ScreenHandler> screen;
    protected final int firstCraftSlotNo;
    protected final int gridSize;
    protected final int resultSlotNo;
    public final int firstInventorySlotNo;

    public final Set<T> craftableRecipes = new HashSet<>();
    public final TreeMap<String, RecipeTreeSet<T>> craftableCategories = new TreeMap<>();
    public T underMouse;

    protected final MinecraftClient client;
    protected final ClientPlayerEntity player;

    // Layout
    protected final int itemSize = 20;
    protected final int itemLift = 5;
    protected int listSize;
    protected int itemsPerRow;
    protected int xOffset;
    protected int mouseScroll;
    protected int minYtoDraw = 0;
    protected int textBoxSize;
    protected int containerLeft;
    protected int containerTop;

    // Search & Updates
    protected long recipeUpdateTime = 0;
    protected long recipeFadeTime = 0;
    public TextFieldWidget pattern;
    public RecipeTreeSet<T> patternMatchingRecipes;
    public int patternListSize;

    /**
     * Factory method to create the correct RecipeBook instance.
     */

    protected AbstractRecipeBook(HandledScreen<? extends ScreenHandler> craftScreen, int firstCraftSlotNo, int gridsize, int resultSlot, int firstInventorySlot) {
        this.screen = craftScreen;
        this.firstCraftSlotNo = firstCraftSlotNo;
        this.gridSize = gridsize;
        this.resultSlotNo = resultSlot;
        this.firstInventorySlotNo = firstInventorySlot;
        this.pattern = null;
        this.underMouse = null;
        this.client = MinecraftClient.getInstance();
        this.player = client.player;
        assert MinecraftClient.getInstance().world != null;
        this.worldContext = SlotDisplayContexts.createParameters(MinecraftClient.getInstance().world);
        this.LOGGER = LogManager.getLogger(craftScreen.getScreenHandler());

        if (ARROWS == null) {
            ARROWS = Identifier.of(EasierCrafting.MODID, "textures/arrows.png");
        }
    }

    // --- Abstract Methods to be implemented by subclasses ---

    /**
     * Called to populate craftableCategories. Return if not updated anything/ remain unchanged
     */
    public abstract boolean updateRecipes();

    /**
     * Called when a recipe in the list is clicked.
     */
    protected abstract void onRecipeClicked(T entry, int mouseButton);

    /**
     * Called to draw the overlay (e.g. 3x3 grid) when hovering over a recipe.
     */
    protected abstract void drawRecipeGridOverlay(DrawContext context, TextRenderer fontRenderer, int height, int mouseX, int mouseY);

    /**
     * Returns the list of recipes to search through for the search bar.
     */
    protected abstract Set<T> getRecipesForSearch();

    // draw outputs... and set undermouse
    protected abstract int drawSetOfRecipes(DrawContext context, RecipeTreeSet<?> treeSet, TextRenderer fontRenderer, int xpos, int ypos, int mouseX, int mouseY);

    // return result of recipe
    protected abstract List<ItemStack> getCraftingResult(T recipe);

    protected abstract void refreshRecipeVar();

    public abstract String recipeDisplayName(T recipe);

    // --- Common Logic ---

    public void afterInitGui() {
        final int distanceFromGui = 25;
        this.containerLeft = (screen.width - 176) / 2;
        this.containerTop = (screen.height - 166) / 2;

        int tempItemsPerRow = 8;
        int tempXOffset = -itemSize * tempItemsPerRow - distanceFromGui;
        if (tempXOffset + containerLeft < 0) {
            tempItemsPerRow = (containerLeft - distanceFromGui) / itemSize;
            tempXOffset = -itemSize * tempItemsPerRow - distanceFromGui;
        }
        textBoxSize = -tempXOffset - 15;
        if (ModConfig.getShowGuiRight())
            tempXOffset = 176 + distanceFromGui;
        if (tempItemsPerRow < 2) {
            LOGGER.warn("forcing tempItemsPerRow to 2 when it's " + tempItemsPerRow);
            tempItemsPerRow = 2;
        }
        this.itemsPerRow = tempItemsPerRow;
        this.xOffset = tempXOffset;
        updatePatternMatch();
        mouseScroll = 0;
        updateRecipes();
    }

    public void drawAllRecipe(DrawContext context, TextRenderer fontRenderer, int left, int height, int mouseX, int mouseY) {
        if (pattern == null) {
            pattern = new TextFieldWidget(fontRenderer, xOffset, 0, textBoxSize, 20, Text.literal(""));
            if (ModConfig.getAutoFocusSearch()) {
                pattern.setFocused(true);
            }
        }

        // Update logic
        if (recipeUpdateTime != 0 && System.currentTimeMillis() > recipeUpdateTime) {
            recipeUpdateTime = 0;
            if (!updateRecipes()){
                LOGGER.info("Update recipe");
                mouseScroll=0;
                if (ModConfig.getFadeoutTime() > 0) {
                    recipeFadeTime = System.currentTimeMillis() + ModConfig.getFadeoutTime() * 50L;
                }
            } else {
                LOGGER.info("Didnt update recipe");
            }
        }

        if (recipeFadeTime > 0) {
            if (System.currentTimeMillis() < recipeFadeTime) {
                underMouse = null;
                return;
            } else {
                recipeFadeTime = 0;
            }
        }

        int ypos = 0;
        int neededHeight = patternListSize + listSize + itemSize; // + search box

        if (neededHeight > height) {
            ypos -= (neededHeight - height) / 2;
            if (ypos < -containerTop) {
                ypos = -containerTop + itemSize;
            } else {
                mouseScroll = 0;
            }
        } else {
            mouseScroll = 0;
        }

        underMouse = null;

        // Draw Search
        pattern.setY(ypos);
        pattern.renderWidget(context, 0, 0, 0f);
        ypos += itemSize * 3 / 2;
        minYtoDraw = ypos;
        ypos -= mouseScroll * itemSize;

        // Draw Search Results
        ypos = drawSetOfRecipes(context, patternMatchingRecipes, fontRenderer, 0, ypos, mouseX, mouseY);

        // Draw Categories
        for (String category : craftableCategories.keySet()) {
            if (ypos >= minYtoDraw) {
                context.drawText(fontRenderer, category, xOffset, ypos, 0xFFFFFF00, true);
            }
            ypos += itemSize;
            ypos = drawSetOfRecipes(context, craftableCategories.get(category), fontRenderer, 0, ypos, mouseX, mouseY);
        }

        // Draw crafting ingredient Overlay (Tooltip/Grid)
        if (underMouse != null) {
            String displayName = recipeDisplayName(underMouse);
            context.drawText(fontRenderer, displayName, 0, height + 3, 0xFFFFFF00, true);
            drawRecipeGridOverlay(context, fontRenderer, height, mouseX, mouseY);
        }
    }


    public static void updateAvailableStacks() {
        PlayerEntity player = MinecraftClient.getInstance().player;
        avaliableItemMap = new HashMap<>();
        if (player==null) return;
        // Iterate through slots (usually 0-35 for player inventory)
        for (ItemStack itemStack : player.getInventory().getMainStacks()) {
            if (itemStack.isEmpty()) continue;
            avaliableItemMap.merge(itemStack.getItem(), itemStack.getCount(), Integer::sum);
        }
    }

    public void renderSingleRecipeOutput(DrawContext context, TextRenderer fontRenderer, ItemStack items, int x, int y) {
        context.drawItem(items, x, y);
        context.drawStackOverlay(fontRenderer, items, x, y);
    }

    public void renderIngredient(DrawContext context, TextRenderer fontRenderer, SlotDisplay ingredient, int x, int y) {
        assert client.world != null;
        List<ItemStack> stacks = getCraftableStacks(ingredient);
        if (stacks.isEmpty()) return;

        int toRender = 0;
        if (stacks.size() > 1)
            toRender = (int) ((System.currentTimeMillis() / 333) % stacks.size());
        context.drawItem(stacks.get(toRender), x, y);
    }

    public void updateRecipesIn(int ms) {
        recipeUpdateTime = System.currentTimeMillis() + ms;
    }

    public void recalcListSize() {
        listSize = craftableCategories.size();
        for (RecipeTreeSet tree : craftableCategories.values())
            listSize += ((tree.size() + (itemsPerRow - 1)) / itemsPerRow);
        listSize *= itemSize;
    }

    public void updatePatternMatch() {
        patternListSize = 0;
        patternMatchingRecipes = new RecipeTreeSet<>(this::recipeDisplayName);
        String patternText = (pattern == null) ? "" : pattern.getText();

        if (patternText.isEmpty()) return;

        Set<T> recipes = getRecipesForSearch();

        try {
            Pattern regex = Pattern.compile(patternText, Pattern.CASE_INSENSITIVE);
            for (T entry : recipes) {
                List<ItemStack> results = getCraftingResult(entry);
                if (results.isEmpty() || results.getFirst().isEmpty()) continue;
                if (regex.matcher(results.getFirst().getName().getString()).find()) {
                    patternMatchingRecipes.add(entry);
                }
            }
        } catch (PatternSyntaxException ex) {
            // Ignore
        }
        recalcPatternMatchSize();
    }

    public void recalcPatternMatchSize() {
        patternListSize = ((patternMatchingRecipes.size() + (itemsPerRow - 1)) / itemsPerRow) * itemSize;
        mouseScroll = 0;
    }

    public void scrollBy(int ticks) {
        int maxScrollPos = ((listSize + patternListSize - screen.height + itemSize) / itemSize) + 3;
        mouseScroll = MathHelper.clamp(mouseScroll - ticks, 0, maxScrollPos);
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton, int guiLeft, int guiTop) {
        if (pattern != null) {
            pattern.setFocused(pattern.mouseClicked(mouseX - guiLeft, mouseY - guiTop, mouseButton));
        }

        // Scroll bar area click
        if (mouseY > 0 && mouseY < 20 && mouseX > xOffset + containerLeft && mouseX < xOffset + containerLeft + textBoxSize) {
            if (mouseX < xOffset + containerLeft + 20) scrollBy(-1);
            else if (mouseX > xOffset + containerLeft + textBoxSize - 20) scrollBy(1);
            return;
        }

        if (underMouse == null) return;

        // Ensure grid is empty (common check, though subclasses might override behavior)
        for (int craftslot = 0; craftslot < gridSize * gridSize; craftslot++) {
            ItemStack stack = screen.getScreenHandler().getSlot(craftslot + firstCraftSlotNo).getStack();
            if (stack != null && !stack.isEmpty()) return;
        }

        onRecipeClicked(underMouse, mouseButton);
    }

    public boolean keyPressed(int code, int scancode, int modifiers) {
        if (pattern == null) return false;
        if (code == GLFW.GLFW_KEY_ENTER || code == GLFW.GLFW_KEY_KP_ENTER || code == GLFW.GLFW_KEY_ESCAPE) {
            updatePatternMatch();
            pattern.setFocused(false);
            return true;
        } else if (pattern.isFocused()) {
            pattern.keyPressed(code, scancode, modifiers);
            updatePatternMatch();
            return true;
        }
        return false;
    }

    public boolean charTyped(char codepoint, int modifiers) {
        if (pattern != null && pattern.isFocused())
            return pattern.charTyped(codepoint, modifiers);
        return false;
    }

    // --- Helper Methods for Subclasses ---

    protected void slotClick(int slot, int mouseButton, SlotActionType clickType) {
        ((SlotClickAccepter) screen).slotClick(slot, mouseButton, clickType);
    }

    // other getters
    static Set<Item> getAvailableItemSet() {
        return avaliableItemMap.keySet();
    }
    static Map<Item,Integer> getAvailableItemMap(){
        return avaliableItemMap;
    }
    protected List<ItemStack> getCraftableStacks(SlotDisplay ingredient){
        return ingredient.getStacks(worldContext).stream()
                .filter(stack -> getAvailableItemSet().contains(stack.getItem()))
                .toList();
    }
    public static Identifier getCat(RecipeDisplayEntry entry){
        return MinecraftClient.getInstance().world.getRegistryManager().getOptional(RegistryKeys.RECIPE_BOOK_CATEGORY).get().getId(entry.category());
    }
}

