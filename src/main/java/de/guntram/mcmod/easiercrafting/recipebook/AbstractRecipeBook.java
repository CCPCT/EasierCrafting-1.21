package de.guntram.mcmod.easiercrafting.recipebook;

import de.guntram.mcmod.easiercrafting.InventoryAccessor;
import de.guntram.mcmod.easiercrafting.SlotClickAccepter;
import de.guntram.mcmod.easiercrafting.modConfig.ModConfig;
import de.guntram.mcmod.easiercrafting.recipe.RecipeTreeSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.recipe.display.SlotDisplayContexts;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.context.ContextParameterMap;
import net.minecraft.util.context.ContextType;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public abstract class AbstractRecipeBook {

    protected final Logger LOGGER;
    static final Object2IntOpenHashMap<Item> avaliableItemMap = new Object2IntOpenHashMap<>(36);
    ContextParameterMap worldContext;
    public static final ContextParameterMap EMPTY_CONTEXT = new ContextParameterMap.Builder().build(new ContextType.Builder().build());

    // Protected fields for subclasses
    public final HandledScreen<? extends ScreenHandler> screen;
    protected final int firstCraftSlotNo;
    protected final int gridSize;
    protected final int resultSlotNo;
    protected final int firstInventorySlotNo;
    protected final SlotDisplay craftingBlock;
    protected ClientRecipeBook recipeBook;
    protected final TextRenderer textRenderer;


    public final ObjectArrayList<RecipeDisplayEntry> craftableRecipes = new ObjectArrayList<>();
    public final ObjectArrayList<RecipeDisplayEntry> allRecipes = new ObjectArrayList<>();
    public final TreeMap<String, RecipeTreeSet> craftableCategories = new TreeMap<>();
    public RecipeDisplayEntry underMouse;

    protected final MinecraftClient client;
    protected final ClientPlayerEntity player;
    protected final ClientWorld world;
    protected final ScreenHandler screenHandler;

    // Layout
    public static final int itemSize = 16;
    public static int itemDisplaySpacing = ModConfig.get().itemDisplaySpacing;
    public static int displayItemSize = itemSize+itemDisplaySpacing*2;
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
    public RecipeTreeSet patternMatchingRecipes;
    public int patternListSize;

    /**
     * Factory method to create the correct RecipeBook instance.
     */

    protected AbstractRecipeBook(HandledScreen<? extends ScreenHandler> craftScreen, int firstCraftSlotNo, int gridsize, int resultSlot, int firstInventorySlot, SlotDisplay craftingBlock) {
        this.client = MinecraftClient.getInstance();
        this.world = client.world;
        assert client.player != null;
        assert world != null;

        this.screen = craftScreen;
        this.textRenderer = client.textRenderer;
        this.firstCraftSlotNo = firstCraftSlotNo;
        this.gridSize = gridsize;
        this.resultSlotNo = resultSlot;
        this.firstInventorySlotNo = firstInventorySlot;
        this.pattern = new TextFieldWidget(textRenderer, 0, 0, 10, 20, Text.literal("")); // update width later
        this.underMouse = null;
        this.player = client.player;
        this.worldContext = SlotDisplayContexts.createParameters(world);
        this.LOGGER = LogManager.getLogger(craftScreen.getScreenHandler());
        this.craftingBlock = craftingBlock;
        this.recipeBook = player.getRecipeBook();
        this.screenHandler = screen.getScreenHandler();
        this.window = client.getWindow();
        itemDisplaySpacing = ModConfig.get().itemDisplaySpacing;
        displayItemSize = itemSize+itemDisplaySpacing*2;
    }

    // --- Abstract Methods to be implemented by subclasses ---

    /**
     * Called to update all avaliable and craftable recipes (the 2 sets). Return if not updated anything/ remain unchanged
     */
    public abstract boolean updateRecipes();

    /**
     * Called when a recipe in the list is clicked.
     */
    protected abstract void onRecipeClicked(RecipeDisplayEntry entry, int mouseButton);

    /**
     * Called to draw the overlay (e.g. 3x3 grid) when hovering over a recipe.
     */
    protected abstract void drawRecipeGridOverlay(DrawContext context);
    /**
     * Returns all craftable recipes.
     */

    // return all ingredient from SlotDisplayEntry
    //protected abstract List<ItemStack> getAllIngredient(SlotDisplay display);

    // draw outputs... and set undermouse
    protected int drawSetOfRecipes(DrawContext context, RecipeTreeSet treeSet, int xpos, int ypos, int screenBottom, int mouseX, int mouseY) {
        if (treeSet == null || treeSet.isEmpty()) return ypos;
        for (RecipeDisplayEntry recipe : treeSet) {
            if (ypos>screenBottom) return ypos;
            if (ypos >= minYtoDraw) {
                int x = xOffset + xpos;
                int y = ypos - itemLift;
                if (!canCraft(recipe)) {
                    // if cant craft draw red background on the result
                    context.fill(x-itemDisplaySpacing,y-itemDisplaySpacing,x+itemSize+itemDisplaySpacing,y+itemSize+itemDisplaySpacing,0x60FF0000);
                }

                renderSingleRecipeOutput(context, textRenderer, recipe.display().result().getFirst(worldContext), x, y);
                if (mouseX >= x &&
                        mouseX <= x + displayItemSize - 1 &&
                        mouseY >= y &&
                        mouseY <= y + displayItemSize - 1)
                {
                    underMouse = recipe;
                    // render background behind hovered item
                    context.fill(x-itemDisplaySpacing,y-itemDisplaySpacing,x+itemSize+itemDisplaySpacing,y+itemSize+itemDisplaySpacing,0x50E0E0E0);
                    // render recipe overlay
                    drawRecipeGridOverlay(context);
                }
            }
            xpos += displayItemSize;
            if (xpos >= displayItemSize * itemsPerRow) {
                ypos += displayItemSize;
                xpos = 0;
            }
        }
        if (xpos != 0) ypos += displayItemSize;
        return ypos;
    }

    // --- Common Logic ---

    public void afterInitGui() {
        final int distanceFromGui = 25;
        this.containerLeft = (screen.width - 176) / 2;
        this.containerTop = (screen.height - 166) / 2;

        int tempItemsPerRow = 9; // max item per row
        int tempXOffset = -displayItemSize * tempItemsPerRow - distanceFromGui;
        if (tempXOffset + containerLeft < 0) {
            tempItemsPerRow = (containerLeft - distanceFromGui) / displayItemSize;
            tempXOffset = -displayItemSize * tempItemsPerRow - distanceFromGui;
        }
        if (ModConfig.get().showGuiRight)
            tempXOffset = 176 + distanceFromGui;
        if (tempItemsPerRow < 2) {
            LOGGER.warn("forcing tempItemsPerRow to 2 when it's {}", tempItemsPerRow);
            tempItemsPerRow = 2;
        }
        this.itemsPerRow = tempItemsPerRow;
        this.xOffset = tempXOffset;
        updatePatternMatch();
        mouseScroll = 0;
        updateRecipes();

        pattern.setX(xOffset);
        textBoxSize=itemsPerRow*displayItemSize;
        pattern.setWidth(textBoxSize);

    }

    public void drawAllRecipe(DrawContext context, int left, int height, int mouseX, int mouseY) {
        if (pattern == null) {
            if (ModConfig.get().autoFocusSearch) {
                pattern.setFocused(true);
            }
        }

        // Update logic
        if (recipeUpdateTime != 0 && System.currentTimeMillis() > recipeUpdateTime) {
            recipeUpdateTime = 0;
            // call update recipe here
            if (!updateRecipes()){
                // before and after not same
                LOGGER.info("Update recipe");
                mouseScroll=0;
                if (ModConfig.get().fadeOutTime > 0) {
                    recipeFadeTime = System.currentTimeMillis() + ModConfig.get().fadeOutTime * 50L;
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
        int neededHeight = patternListSize + listSize + displayItemSize; // + search box

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

        // draw background
        int screenBottom = context.getScaledWindowHeight()-containerTop-5;
        if (ModConfig.get().recipeBackground){
            context.fill(pattern.getX()-5,pattern.getY()-5,pattern.getX()+textBoxSize+5,screenBottom ,0x50505050);
        }

        // Draw Search box
        pattern.setY(ypos);
        pattern.renderWidget(context, 0, 0, 0f);
        ypos += displayItemSize * 3 / 2;
        minYtoDraw = ypos;
        ypos -= mouseScroll * displayItemSize;

        // Draw Search Results
        ypos = drawSetOfRecipes(context, patternMatchingRecipes, 0, ypos, screenBottom-displayItemSize, mouseX, mouseY);

        // Draw Categories
        for (String category : craftableCategories.keySet()) {
            if (ypos>screenBottom-displayItemSize) return;
            if (ypos >= minYtoDraw) {
                context.drawText(textRenderer, category, xOffset, ypos, 0xFFFFFF00, true);
            }
            ypos += displayItemSize;
            ypos = drawSetOfRecipes(context, craftableCategories.get(category), 0,  ypos, screenBottom-displayItemSize, mouseX, mouseY);
        }
    }


    public void updateAvailableStacks() {
        avaliableItemMap.clear();
        if (player==null) return;
        // Iterate through slots (usually 0-35 for player inventory)
        for (ItemStack itemStack : ((InventoryAccessor) player.getInventory()).getCompatMain()) {
            if (itemStack.isEmpty()) continue;
            avaliableItemMap.merge(itemStack.getItem(), itemStack.getCount(), Integer::sum);
        }
        // populate creative item group so recipes can be grouped
        ItemGroups.updateDisplayContext(player.networkHandler.getEnabledFeatures(), true, world.getRegistryManager());
    }

    public void renderSingleRecipeOutput(DrawContext context, TextRenderer fontRenderer, ItemStack items, int x, int y) {
        context.drawItem(items, x, y);
        context.drawStackOverlay(fontRenderer, items, x, y);
    }

    public void renderIngredient(DrawContext context, List<ItemStack> stacks, Slot slot) {
        if (stacks.isEmpty()) return;
        int x = slot.x;
        int y = slot.y;
        if (!getAvailableItemSet().contains(stacks.getFirst().getItem())){
            // no recipe found
            context.fill(x,y,x+itemSize,y+itemSize,0x60FF0000);
        }

        int toRender = 0;
        if (stacks.size() > 1)
            toRender = (int) ((System.currentTimeMillis() / 333) % stacks.size());
        drawHoloItem(context,slot,stacks.get(toRender));
        context.drawStackOverlay(textRenderer,stacks.get(toRender),x,y);
    }

    public void recalcListSize() {
        listSize = craftableCategories.size();
        for (RecipeTreeSet tree : craftableCategories.values())
            listSize += ((tree.size() + (itemsPerRow - 1)) / itemsPerRow);
        listSize *= displayItemSize;
    }

    public void updatePatternMatch() {
        patternListSize = 0;
        patternMatchingRecipes = new RecipeTreeSet();
        String patternText = (pattern == null) ? "" : pattern.getText();

        if (patternText.isEmpty()) return;

        try {
            Pattern regex = Pattern.compile(patternText, Pattern.CASE_INSENSITIVE);
            for (RecipeDisplayEntry entry : (ModConfig.get().showAllRecipes ? allRecipes : craftableRecipes)) {
                List<ItemStack> results = getCraftingResult(entry);
                if (results.isEmpty() || results.getFirst().isEmpty()) continue;
                // if raw name or translated name match (support other languages)
                if (regex.matcher(results.getFirst().getItem().getTranslationKey()).find() || regex.matcher(results.getFirst().getName().getString()).find()) {
                    patternMatchingRecipes.add(entry);
                }
            }
        } catch (PatternSyntaxException ex) {
            // Ignore
        }
        recalcPatternMatchSize();
    }

    public void recalcPatternMatchSize() {
        patternListSize = ((patternMatchingRecipes.size() + (itemsPerRow - 1)) / itemsPerRow) * displayItemSize;
        mouseScroll = 0;
    }

    public void scrollBy(int ticks) {
        int maxScrollPos = ((listSize + patternListSize - screen.height + displayItemSize) / displayItemSize) + 3;
        mouseScroll = MathHelper.clamp(mouseScroll - ticks, 0, maxScrollPos);
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton, int guiLeft, int guiTop) {
        if (pattern != null) {
            boolean clickedPattern = pattern.mouseClicked(mouseX - guiLeft, mouseY - guiTop, 0);
            pattern.setFocused(clickedPattern);
            if (clickedPattern) {
                if (mouseButton == 1) {
                    pattern.setText("");
                    updatePatternMatch();
                }
                return;
            }
        }

        // Scroll bar area click
        if (mouseY > 0 && mouseY < 20 && mouseX > xOffset + containerLeft && mouseX < xOffset + containerLeft + textBoxSize) {
            LOGGER.info("Try to scroll...");
            if (mouseX < xOffset + containerLeft + 20) scrollBy(-1);
            else if (mouseX > xOffset + containerLeft + textBoxSize - 20) scrollBy(1);
            return;
        }

        if (underMouse == null) return;

        // dont craft uncraftable items
        if (!canCraft(underMouse)) return;

        // Ensure grid is empty (common check, though subclasses might override behavior)
        for (int craftslot = 0; craftslot < gridSize * gridSize; craftslot++) {
            ItemStack stack = screen.getScreenHandler().getSlot(craftslot + firstCraftSlotNo).getStack();
            if (stack != null && !stack.isEmpty()) {
                slotClick(craftslot+firstCraftSlotNo, 0, SlotActionType.QUICK_MOVE);
                if (!stack.isEmpty()) return; // can't move item away (inventory full or locked) stop crafting
            }
        }

        onRecipeClicked(underMouse, mouseButton);
        queueUpdateRecipe();
    }

    public boolean keyPressed(int code, int scancode, int modifiers) {
        if (pattern == null) return false;
        if (code == GLFW.GLFW_KEY_ENTER || code == GLFW.GLFW_KEY_KP_ENTER || code == GLFW.GLFW_KEY_ESCAPE) {
            pattern.setFocused(false);
            updatePatternMatch();
            return true;
        } else if (pattern.isFocused()) {
            pattern.keyPressed(code, scancode, modifiers);
            updatePatternMatch();
            return true;
        }
        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean charTyped(char codepoint, int modifiers) {
        if (pattern != null && pattern.isFocused()) {
            // TextFieldWidget.charTyped returns true if it added a character to the text
            if (pattern.charTyped(codepoint, modifiers)) {
                updatePatternMatch();
                return true;
            }
        }
        return false;
    }

    // --- Helper Methods for Subclasses ---

    protected void slotClick(int slot, int mouseButton, SlotActionType clickType) {
        ((SlotClickAccepter) screen).slotClick(slot, mouseButton, clickType);
    }

    protected void drawHoloItem(DrawContext context, Slot slot, ItemStack stack){
        int x = slot.x;
        int y = slot.y;
        context.drawItem(stack, x, y);
        // fill transparent colour of grid to make items look transparent
        context.fill(x, y, x+itemSize, y+itemSize, 0x808b8b8b);
        if (stack.getCount()>1){
            context.drawStackOverlay(textRenderer,stack,x,y);
        }
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
        assert MinecraftClient.getInstance().world != null;
        if (MinecraftClient.getInstance().world.getRegistryManager().getOptional(RegistryKeys.RECIPE_BOOK_CATEGORY).isEmpty()) return null;
        return MinecraftClient.getInstance().world.getRegistryManager().getOptional(RegistryKeys.RECIPE_BOOK_CATEGORY).get().getId(entry.category());
    }

    public String recipeDisplayName(RecipeDisplayEntry entry) {
        return getCraftingResult(entry).getFirst().getName().getString();
    }

    protected List<ItemStack> getCraftingResult(RecipeDisplayEntry recipe) {
        return recipe.getStacks(worldContext);
    }

    protected ItemStack getFirstCraftingResult(RecipeDisplayEntry recipe) {
        return recipe.display().result().getFirst(worldContext);
    }

    protected ItemGroup getItemGroup(RecipeDisplayEntry entry) {
        // get the group of result item
        Item resultItem = getFirstCraftingResult(entry).getItem();

        for (ItemGroup group : Registries.ITEM_GROUP) {
            // dont want to be generic "searched"
            if (group.getType() == ItemGroup.Type.SEARCH) continue;

            // We check the "display stacks" of the group to see if our item is there
            if (group.contains(resultItem.getDefaultStack())) {
                return group; // Found the Creative Tab!
            }
        }
        LOGGER.warn("Cant find group for: {}", resultItem.getTranslationKey());
        return ItemGroups.getDefaultTab();
    }

    protected String getTranslatedItemGroup(RecipeDisplayEntry entry){
        return I18n.translate(getItemGroup(entry).getDisplayName().getString());
    }

    protected void queueUpdateRecipe(){
        recipeUpdateTime = System.currentTimeMillis() + ModConfig.get().autoUpdateRecipeTimer * 50L;
    }

    public static SlotDisplay getSlotDisplayFromItem(Item item){
        return new SlotDisplay.StackSlotDisplay(new ItemStack(item));
    }

    protected boolean canCraft(RecipeDisplayEntry entry){
        return craftableRecipes.contains(entry);
    }

    public boolean hasShiftDown(){
        return InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_SHIFT);
    }

    public boolean hasControlDown(){
        return InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_CONTROL);
    }

}

