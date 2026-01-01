package de.guntram.mcmod.easiercrafting.recipe;

/*
this class is used to render ...
the recipe handler is moved to recipe.RecipeHandler
*/

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.guntram.mcmod.easiercrafting.*;
import de.guntram.mcmod.easiercrafting.extendedScreen.ExtendedGuiCrafting;
import de.guntram.mcmod.easiercrafting.extendedScreen.ExtendedGuiInventory;
import de.guntram.mcmod.easiercrafting.extendedScreen.ExtendedGuiStonecutter;
import de.guntram.mcmod.easiercrafting.modConfig.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.*;
import net.minecraft.recipe.*;
import net.minecraft.recipe.display.*;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.context.ContextParameterMap;
import net.minecraft.util.context.ContextType;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

public class RecipeBook {

    public static final Logger LOGGER = LogManager.getLogger(RecipeBook.class);

    public final HandledScreen screen;
    private final int firstCraftSlot;
    private final int gridSize;
    private final int resultSlotNo;
    public final int firstInventorySlotNo;
    public TreeMap<String, RecipeTreeSet> craftableCategories;
    public RecipeDisplayEntry underMouse;

    private final int itemSize = 20;
    private final int itemLift = 5;         // how many pixels to display items above where they would be normally

    private int listSize;
    private int itemsPerRow;                // # of items per row. Normally 8.
    private int xOffset;                    // offset of list to standard gui. itemSize*itemsPerRow+10.
    private int mouseScroll;
    private int minYtoDraw = 0;               // implements clipping top part of the item list
    private int textBoxSize;
    private long recipeUpdateTime;
    private final RecipeType wantedRecipeType;

    public TextFieldWidget pattern;
    public RecipeTreeSet patternMatchingRecipes;
    public int patternListSize;

    static Identifier ARROWS;
    private int containerLeft;
    private int containerTop;

    ClientPlayerEntity player;
    MinecraftClient client;

//    /**
//     * @param craftScreen        The container the recipe book is attached to - this
//     *                           can be a GuiCrafting or a GuiInventory container
//     * @param firstCraftSlot     The slot number of the first slot that is a craft
//     *                           slot in craftinv
//     * @param gridsize           2 (for inventory) or 3 (for workbench)
//     * @param resultSlot         the slot number of the craft result slot
//     * @param firstInventorySlot the slot number of the first inventory slot
//     */


    public RecipeBook(HandledScreen<? extends ScreenHandler> craftScreen, int firstCraftSlot, int gridsize, int resultSlot, int firstInventorySlot) {
        this.screen = craftScreen;
        this.firstCraftSlot = firstCraftSlot;
        this.gridSize = gridsize;
        this.resultSlotNo = resultSlot;
        this.firstInventorySlotNo = firstInventorySlot;
        this.pattern = null;
        this.underMouse = null;
        client = MinecraftClient.getInstance();
        player = client.player;


        if (screen instanceof ExtendedGuiStonecutter) {
            wantedRecipeType = RecipeType.STONECUTTING;
        } else if (screen instanceof ExtendedGuiCrafting || screen instanceof ExtendedGuiInventory) {
            wantedRecipeType = RecipeType.CRAFTING;
        } else {
            wantedRecipeType = null;
        }

        if (ARROWS == null) {
            ARROWS = Identifier.of(EasierCrafting.MODID, "textures/arrows.png");
        }
    }

    public void afterInitGui() {
        final int distanceFromGui = 25;

        this.containerLeft = (screen.width - 176 /*screen.containerWidth */) / 2;
        this.containerTop = (screen.height - 166 /* screen.containerHeight */) / 2;

        int tempItemsPerRow = 8;
        int tempXOffset = -itemSize * tempItemsPerRow - distanceFromGui;
        if (tempXOffset + containerLeft < 0) {
            tempItemsPerRow = (containerLeft - distanceFromGui) / itemSize;
            tempXOffset = -itemSize * tempItemsPerRow - distanceFromGui;
        }
        textBoxSize = -tempXOffset - 15;
        if (ModConfig.getShowGuiRight())
            tempXOffset = 176 /* screen.containerWidth */ + distanceFromGui;
        if (tempItemsPerRow < 2) {
            LOGGER.warn("forcing tempItemsPerRow to 2 when it's " + tempItemsPerRow);
            tempItemsPerRow = 2;
        }
        this.itemsPerRow = tempItemsPerRow;
        this.xOffset = tempXOffset;
        this.mouseScroll = 0;
//        System.out.println("left="+containerLeft+", items="+itemsPerRow+", offset="+tempXOffset+", textbox="+textBoxSize);
        updatePatternMatch();
        updateRecipes();
    }

    // left is the X position we want to draw at, Y is (normally) 0.
    // However, if our height is larger than the GUI container height,
    // adjust our Y position accordingly.
    public void drawRecipeList(DrawContext context, TextRenderer fontRenderer, int left, int height, int mouseX, int mouseY) {
        // We can't do this in the constructor as we don't yet know sizes from initGui.
        // Also, not in afterInitGui() because we don't know fontRender there.
        if (pattern == null) {
            pattern = new TextFieldWidget(fontRenderer, xOffset, 0, textBoxSize, 20, Text.literal(""));
            if (ModConfig.getAutoFocusSearch()) {
                // doh - in 1.15, changeFocus toggles the focus and ignores the parameter
                pattern.setFocused(true);
            }
        }

        boolean underMouseIsCraftable = true;

        if (recipeUpdateTime != 0 && System.currentTimeMillis() > recipeUpdateTime) {
            updateRecipes();
            recipeUpdateTime = 0;
        }

        if (recipeUpdateTime != 0 && System.currentTimeMillis() > recipeUpdateTime - ModConfig.getFadeoutTime()) {
            underMouse = null;
            return;
        }

        int xpos, ypos = 0;
        int neededHeight = patternListSize + listSize;
        neededHeight += itemSize;       // search box

        if (neededHeight > height) {
            ypos -= (neededHeight - height) / 2;
            //System.out.println("ypos is now "+ypos);
            if (ypos < -containerTop) {
                ypos = -containerTop;
//                client.getTextureManager().bindTexture(ARROWS);
//                context.drawTexture(ARROWS, xOffset, ypos, 0, 0, 20, 20);
//                context.drawTexture(ARROWS, xOffset + textBoxSize - 20, ypos, 20, 0, 20, 20);
                ypos += itemSize;
            } else {
                mouseScroll = 0;
            }
        } else {
            mouseScroll = 0;
        }
//        GuiLighting.enable();
//        GuiLighting.enableForItems();

        underMouse = null;

        // show searched item
        pattern.setY(ypos);
        pattern.renderWidget(context, 0, 0, 0f);    // <-- parameters neccessary but unused
        ypos += itemSize * 3 / 2;
        minYtoDraw = ypos;
        ypos -= mouseScroll * itemSize;
        ypos = drawRecipeOutputs(context,  patternMatchingRecipes, fontRenderer, 0, ypos, mouseX, mouseY);
        if (underMouse != null) {
            underMouseIsCraftable = false;
        }

        // actual draw
        // todo rewrite this
        if (craftableCategories != null) {
            for (String category : craftableCategories.keySet()) {
//            System.out.println(category+" at "+xOffset+"/"+ypos);
                if (ypos >= minYtoDraw) {
                    context.drawText(fontRenderer, category, xOffset, ypos, 0xFFFFFF00, true);
                }
                ypos += itemSize;
                ypos = drawRecipeOutputs(context, craftableCategories.get(category), fontRenderer, 0, ypos, mouseX, mouseY);
            }
        }



        if (underMouse != null) {
            // mouse on sth
            // update context once per frame

            String displayName = EasierCrafting.recipeDisplayName(underMouse);
            context.drawText(fontRenderer, displayName, 0, height + 3, 0xFFFFFF00, true);
            if (underMouse.display() instanceof ShapedCraftingRecipeDisplay shaped) {

                List<SlotDisplay> ingredients = shaped.ingredients();

                // fontRenderer.draw(stack, "sr", left-20, height, 0x202020);
                for (int x = 0; x < shaped.width(); x++) {
                    for (int y = 0; y < shaped.height(); y++) {
                        SlotDisplay ingredient = ingredients.get(x + y * shaped.width());
                        renderIngredient(context, fontRenderer,
                                ingredient,
                                itemSize * x, height + itemSize + itemSize * y);
                    }
                }

            } else if (underMouse.display() instanceof ShapelessCraftingRecipeDisplay recipeDisplay) {
                if (underMouse.craftingRequirements().isEmpty()){
                    underMouseIsCraftable = false;
                } else {
                    xpos = 0;
                    for (SlotDisplay ingredient : recipeDisplay.ingredients()) {
                        renderIngredient(context, fontRenderer, ingredient, itemSize * xpos, height + itemSize);
                        xpos++;
                    }
                }
            }
        }

        if (!underMouseIsCraftable) {
            // prevent action when clicking the output icon
            underMouse = null;
        }
    }

    public int drawRecipeOutputs(DrawContext context,
                                 RecipeTreeSet treeSet,
                                 TextRenderer fontRenderer,
                                 int xpos, int ypos,
                                 int mouseX, int mouseY) {
        if (treeSet==null || treeSet.isEmpty()) return ypos;
        for (RecipeDisplayEntry recipe : treeSet) {
            if (ypos >= minYtoDraw) {
                renderSingleRecipeOutput(context, fontRenderer, recipe.display().result().getFirst(RecipeHandler.getWorldContext()), xOffset + xpos, ypos - itemLift);
                if (mouseX >= xpos + xOffset && mouseX <= xpos + xOffset + itemSize - 1
                        && mouseY >= ypos - itemLift && mouseY <= ypos - itemLift + itemSize - 1) {
                    underMouse = recipe;
                }
            }
            xpos += itemSize;
            if (xpos >= itemSize * itemsPerRow) {
                ypos += itemSize;
                xpos = 0;
            }
        }
        if (xpos != 0)
            ypos += itemSize;
        return ypos;
    }

    public void renderSingleRecipeOutput(DrawContext context, TextRenderer fontRenderer,
                                         ItemStack items, int x, int y) {
        context.drawItem(items, x, y);
    }

    // updated function using slot display instead of ingredients
    public void renderIngredient(DrawContext context, TextRenderer fontRenderer, SlotDisplay ingredient, int x, int y) {
        assert client.world != null;
        List<ItemStack> stacks = RecipeHandler.getCraftableStacks(ingredient);

        if (stacks.isEmpty()) {
            System.err.println("No stacks can be used");
            return;
        }
        int toRender = 0;
        if (stacks.size() > 1)
            toRender = (int) ((System.currentTimeMillis() / 333) % stacks.size());
        context.drawItem(stacks.get(toRender), x, y);
    }



    public void updateRecipesIn(int ms) {
        recipeUpdateTime = System.currentTimeMillis() + ms;
    }

    public void updateRecipes() {
        ScreenHandler inventory = screen.getScreenHandler();
        // outdated
//        List<Recipe<?>> recipes = new ArrayList<>();
//
//        recipes.addAll(player.getRecipeBook().getResultsForCategory(RecipeBookType.CRAFTING).getFirst().);
// disabled for 1.19            recipes.addAll(LocalRecipeManager.getInstance().values());
        //todo update

        assert client.currentScreen != null;
//        System.out.println(client.currentScreen.getClass().getName());
//        System.out.println(RecipeHandler.getRecipeBookTypeFromScreenClass(client.currentScreen.getClass()).getClass().getName());
//        System.out.println(RecipeBookType.CRAFTING.getClass().getName());

        // initialise recipeHandler
        RecipeHandler.updateAvailableStacks();
        RecipeHandler.updateRecipes(client.currentScreen.getClass());
        List<RecipeDisplayEntry> recipeEntries = RecipeHandler.getCraftableRecipeEntries();

        if (wantedRecipeType == RecipeType.CRAFTING && ModConfig.getAllowGeneratedRecipes()) {
            recipeEntries.addAll(InventoryRecipeScanner.findUnusualRecipes(inventory, firstInventorySlotNo));

        }



        /* In 1.20 the creative tabs don't exist until we do this ... */
        ItemGroups.updateDisplayContext(player.networkHandler.getEnabledFeatures(), true, player.getWorld().getRegistryManager());


        craftableCategories = new TreeMap<>();
        for (RecipeDisplayEntry entry : recipeEntries) {
            //System.out.println("grid size is "+gridSize+", recipe needs "+recipe.getRecipeSize());
            // assume craftable in current inventory (todo add check)
            ItemStack result = entry.display().result().getFirst(RecipeHandler.getWorldContext());
            Item item = result.getItem();
            if (item == Items.AIR)
                continue;
            ItemGroup tab = null;
            for (ItemGroup group : ItemGroups.getGroups()) {
                if (!group.isSpecial()
                        && group.contains(result)) {
                    tab = group;
                    break;
                }
            }

            // sort catagory
            String category;
            if (!ModConfig.getCategorizeRecipes()) {
                // no cat
                category = I18n.translate("easiercrafting.category.possible");
            } else if (RecipeHandler.getCat(entry).getNamespace().startsWith(EasierCrafting.MODID + ":")) {
                // custom cat
                category = I18n.translate("easiercrafting.category.special");
            } else if (tab == null) {
                // have cat
                category = RecipeHandler.getCat(entry).toTranslationKey();
            } else {
                if (wantedRecipeType == RecipeType.STONECUTTING) {
                    Block block = Block.getBlockFromItem(item);
                    if (block instanceof StairsBlock) {
                        category = I18n.translate("easiercrafting.category.stairs");
                    } else if (block instanceof SlabBlock) {
                        category = I18n.translate("easiercrafting.category.slabs");
                    } else if (block instanceof WallBlock) {
                        category = I18n.translate("easiercrafting.category.walls");
                    } else {
                        category = I18n.translate("easiercrafting.category.blocks");
                    }
                } else {
                    category = I18n.translate(tab.getDisplayName().getString());
                }
            }
            RecipeTreeSet recipeTreeSet = craftableCategories.get(category);
            //System.out.println("cat size: "+craftableCategories.size());
            if (recipeTreeSet == null) {
                // if no other crafting for same cat make new tree set
                recipeTreeSet = new RecipeTreeSet();
                craftableCategories.put(category, recipeTreeSet);
            }
            //System.out.println("tree size: "+recipeTreeSet.size());
            LOGGER.log(Level.DEBUG, "adding " + result.getName().getString() + " in " + category);
            recipeTreeSet.add(entry);
        }
        recalcListSize();
    }

    public void recalcListSize() {
        listSize = craftableCategories.size();
        for (RecipeTreeSet tree : craftableCategories.values())
            listSize += ((tree.size() + (itemsPerRow - 1)) / itemsPerRow);
        listSize *= itemSize;
        mouseScroll = 0;
    }

    public String getPatternText() {
        if (pattern == null)
            return "";
        return pattern.getText();
    }

    public void updatePatternMatch() {
        patternListSize = 0;
        patternMatchingRecipes = new RecipeTreeSet(); // Ensure this supports RecipeDisplayEntry

        String patternText = getPatternText();
        if (patternText.isEmpty())
            return;

        // 2. 1.21.4 uses RecipeDisplayEntry for the client list
        List<RecipeDisplayEntry> recipes = RecipeHandler.getCraftableRecipeEntries();

        try {
            Pattern regex = Pattern.compile(patternText, Pattern.CASE_INSENSITIVE);

            for (RecipeDisplayEntry entry : recipes) {
                // 3. Workstation Check (Update your custom method to take RecipeDisplayEntry)
                // todo make check... assume same for now :3

                // 4. Get the result stack (The icon shown in the recipe book)
                // Recipes can have multiple potential results, we check the first one
                List<ItemStack> results = entry.display().result().getStacks(RecipeHandler.getWorldContext());
                if (results.isEmpty() || results.getFirst().isEmpty()) {
                    continue;
                }

                ItemStack result = results.getFirst();

                // 5. Match against the display name
                String displayName = result.getName().getString();
                if (!regex.matcher(displayName).find()) {
                    continue;
                }
                // found
                patternMatchingRecipes.add(entry);
            }
        } catch (PatternSyntaxException ex) {
            // Invalid regex, skip
        }

        recalcPatternMatchSize();
    }

    public void recalcPatternMatchSize() {
        patternListSize = ((patternMatchingRecipes.size() + (itemsPerRow - 1)) / itemsPerRow) * itemSize;
        mouseScroll = 0;
    }

//    private boolean recipeTypeMatchesWorkstation(RecipeDisplayEntry recipe) {
//        return wantedRecipeType == recipe.getType();
//    }

    static class Takefrom {
        Slot invitem;
        int amount;

        Takefrom(Slot i, int n) {
            invitem = i;
            amount = n;
        }
    }

    // recipe
//    private boolean canCraftRecipe(Recipe recipe, ScreenHandler inventory, int gridSize) {
//        if (recipe instanceof ShapelessRecipe shapelessRecipe) {
//            if (recipe.getIngredients().size() > gridSize * gridSize) {
//                // System.out.println("shapeless for "+recipe.getOutput().getTranslationKey()+" has "+recipe.getIngredients().size()+" items while gridSizs is "+gridSize);
//                return false;
//            }
//            return canCraftShapeless(shapelessRecipe, inventory);
//        } else if (recipe instanceof ShapedRecipe shapedRecipe) {
//            return canCraftShaped(shapedRecipe, inventory, gridSize);
//        } else if (recipe instanceof InventoryGeneratedRecipe || recipe instanceof RepairRecipe) {
//            return recipe.fits(gridSize, gridSize);
//        } else if (recipe instanceof CuttingRecipe cuttingRecipe) {
//            ItemStack stack = cuttingRecipe.getResult(null);
//            LOGGER.debug("output: " + stack.getItem().getName().getString());
//            for (Ingredient ing : cuttingRecipe.getIngredients()) {
//                ItemStack[] stacks = ing.getMatchingStacks();
//                if (stacks.length > 1) {
//                    LOGGER.debug(stacks.length + " possible inputs for " + stack.getItem().getName().getString());
//                    for (ItemStack stack2 : stacks) {
//                        LOGGER.debug("    " + stack2.getItem().getName().getString());
//                    }
//                }
//            }
//            return canCraftCutting(cuttingRecipe, inventory);
//        } else if (recipe instanceof BrewingRecipe brewingRecipe) {
//            return canBrew(brewingRecipe, inventory);
//        } else {
//            //System.out.println(recipe.getRecipeOutput().getDisplayName()+" is a "+recipe.getClass().getCanonicalName());
//        }
//        return false;
//    }

//    private boolean canCraftShapeless(ShapelessRecipe recipe, ScreenHandler inventory) {
//        DefaultedList<Ingredient> neededList = recipe.getIngredients();
//        return canCraft(recipe, neededList, inventory);
//    }
//
//    private boolean canCraftShaped(ShapedRecipe recipe, ScreenHandler inventory, int gridSize) {
//        if (!recipe.fits(gridSize, gridSize)) {
//            return false;
//        }
//        DefaultedList<Ingredient> neededList = recipe.getIngredients();
//        return canCraft(recipe, neededList, inventory);
//    }
//
//    private boolean canCraftCutting(CuttingRecipe recipe, ScreenHandler inventory) {
//        DefaultedList<Ingredient> neededList = recipe.getIngredients();
//        return canCraft(recipe, neededList, inventory);
//    }
//
//    private boolean canCraft(Recipe recipe, List<Ingredient> neededList, ScreenHandler inventory) {
//        ArrayList<Takefrom> source = new ArrayList<>(neededList.size());
//        for (Ingredient neededItem : neededList) {                                // iterate over needed items
//            ItemStack[] stacks = neededItem.getMatchingStacks();
//            if (stacks.length == 0)
//                continue;
//            int neededAmount = stacks[0].getCount();
//            // System.out.println("need "+neededAmount+" "+stacks[0].getDisplayName()+" for "+recipe.getRecipeOutput().getDisplayName());
//            if (recipe.getResult(null).getItem() == Items.DISPENSER) {
//                LOGGER.debug("look for dispenser item " + I18n.translate(stacks[0].getItem().getTranslationKey()));
//            }
//            for (int i = 0; i < 36; i++) {
//                Slot invitem = inventory.getSlot(i + firstInventorySlotNo);
//                ItemStack slotcontent = invitem.getStack();
//                if (canActAsIngredient(neededItem, slotcontent)) {
//                    if (recipe.getResult(null).getItem() == Items.DISPENSER) {
//                        LOGGER.debug("Item in inv slot " + i + ":" + I18n.translate(slotcontent.getItem().getTranslationKey()) + " works");
//                    }
//                    int providedAmount = slotcontent.getCount();                 // check how many items there are
//                    for (int j = 0; j < source.size(); j++)                         // subtract how many have been used on other slots
//                        if (source.get(j).invitem == invitem)
//                            providedAmount -= source.get(j).amount;
//                    if (providedAmount > neededAmount)                            // don't provide more than needed
//                        providedAmount = neededAmount;
//                    if (providedAmount > 0) {
//                        source.add(new Takefrom(invitem, providedAmount));      // and remember how much we can take from here
//                        neededAmount -= providedAmount;
//                    }
//                } else {
//                    if (recipe.getResult(null).getItem() == Items.DISPENSER) {
//                        LOGGER.debug("Item in inv slot " + i + ":" + I18n.translate(stacks[0].getItem().getTranslationKey()) + "doesn't work");
//                    }
//                }
//            }
//            if (neededAmount > 0) {                                               // we don't have enough of this item so we can't craft this
//                //System.out.println("can't craft "+recipe.getRecipeOutput().getDisplayName()+" because we don't have "+neededItem.getCount()+" "+neededItem.getDisplayName());
//                return false;
//            }
//        }
//        //System.out.println("enough stuff for "+recipe.getRecipeOutput().getDisplayName());
//        return true;
//    }

    // This is a bit more complicated, because we have to handle item recipes (potion -> splash -> lingering)
    // as well as potion recipes (Item stays the same, but the Potion NBT tag changes).
    // In both cases, we need the ingredient, which can be in the player inventory or the brewing stand.
    // In case of Potion recipes, we need the input potion type in either inventory or brewing stand. Brewing
    // stand is fine, we know exactly what's going to be crafted in this case. But if all inputs are in the
    // player inventory, and there's more than one usable item (for example, Potion water -> weakness, and the player
    // has water and splash water in his inventory; or potion -> splash, and the player has weakness and night vision
    // in his inventory), we might want to act somehow to prevent crafting the wrong input ...

//    private boolean canBrew(BrewingRecipe recipe, ScreenHandler inventory) {
//        List<Ingredient> inputs = recipe.getIngredients();
//        Item ingredient = inputs.get(1).getMatchingStacks()[0].getItem();
//        ItemStack inputPotionStack = inputs.get(0).getMatchingStacks()[0];
//        boolean haveIngredient = false;
//        boolean haveInputPotion = false;
//
///*
//        Level level=Level.DEBUG;
//        if (ingredient == Items.GUNPOWDER || ingredient == Items.NETHER_WART) {
//            level = Level.INFO;
//        }
//*/
//
///*        LOGGER.log(level, "Check for "+(recipe.isItemRecipe() ? "Item recipe " : "Potion recipe ")+
//                PotionUtil.getPotion(recipe.getOutput()).getName(recipe.getOutput().getItem().getName().getString()+" ")+
//                " from "+
//                PotionUtil.getPotion(inputPotionStack).getName(inputPotionStack.getItem().getName().getString()+" ")+
//                " and "+
//                ingredient.getName().getString()); */
//        // check if the brewing stand has a usable input potion
//        for (int i = 0; i < 3; i++) {
//            ItemStack inventoryItemStack = inventory.getSlot(i + firstCraftSlot).getStack();
//            if (recipe.isItemRecipe()) {
//                haveInputPotion |= (inventoryItemStack.getItem() == inputPotionStack.getItem());
//            } else {
//                haveInputPotion |= PotionUtil.getPotion(inventoryItemStack) == PotionUtil.getPotion(inputPotionStack);
//            }
//            if (haveInputPotion) {
//                break;
//            }
//        }
////        LOGGER.log(level, "  haveInputPotion in Brewing Stand is "+haveInputPotion);
//
//        // check if the brewing stand already has the ingredient
////        LOGGER.log(level, "  ing slot item is "+inventory.getSlot(3+firstCraftSlot).getStack().getItem());
////        LOGGER.log(level, "  ingredient is "+ingredient);
//        if (inventory.getSlot(3 + firstCraftSlot).getStack().getItem() == ingredient) {
//            haveIngredient = true;
//        }
////        LOGGER.log(level, "  haveIngredient in Brewing Stand is "+haveIngredient);
//
//        // check the player inventory
//        for (int i = 0; i < 36; i++) {
//            if (haveInputPotion && haveIngredient) {
//                break;
//            }
//            ItemStack inventoryItemStack = inventory.getSlot(i + firstInventorySlotNo).getStack();
//            if (inventoryItemStack.isEmpty())
//                continue;
//
//            if (recipe.isItemRecipe()) {
////                LOGGER.log(Level.TRACE, "item recipe compare "+inventoryItemStack.getItem()+" to "+inputPotionStack.getItem());
//                haveInputPotion |= (inventoryItemStack.getItem() == inputPotionStack.getItem());
//            } else {
////                LOGGER.log(Level.TRACE, "potion recipe compare "+PotionUtil.getPotion(inventoryItemStack).getName("")+" to "+PotionUtil.getPotion(inputPotionStack).getName(""));
//                haveInputPotion |= (PotionUtil.getPotion(inventoryItemStack) == PotionUtil.getPotion(inputPotionStack));
//            }
//            if (inventoryItemStack.getItem() == ingredient) {
//                haveIngredient = true;
//            }
//        }
////         LOGGER.log(level, MessageFormat.format("  after player inv; haveInputPotion = {0}, haveIngredient ={1}", haveInputPotion, haveIngredient));
//        return haveInputPotion && haveIngredient;
//    }

    static class InputCount {
        int count;
        int items;
    }

    public void scrollBy(int ticks) {
        System.out.println("Scrolling by " + ticks);
        int maxScrollPos = ((listSize + patternListSize - screen.height + itemSize) / itemSize);
        // Add 2 to maxScrollPos for search bar, arrows, and one for safety
        maxScrollPos += 3;
        mouseScroll = MathHelper.clamp(mouseScroll - ticks, 0, maxScrollPos);
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton, int guiLeft, int guiTop) {
        if (pattern != null) {
            pattern.setFocused(pattern.mouseClicked(mouseX - guiLeft, mouseY - guiTop, mouseButton));
        }

        if (mouseY > 0 && mouseY < 20 && mouseX > xOffset + containerLeft && mouseX < xOffset + containerLeft + textBoxSize) {
            if (mouseX < xOffset + containerLeft + 20)
                scrollBy(-1);
            else if (mouseX > xOffset + containerLeft + textBoxSize - 20)
                scrollBy(1);
            return;
        }

        // we assume the mouse is clicked where it was when we updated the screen last ...
        if (underMouse == null)
            return;
        // todo brewing
//        if (underMouse.getType() == BrewingRecipe.recipeType) {
//            // this is so different from other containers, we handle it now and return
//            fillBrewingStandSlots((BrewingRecipe) underMouse);
//            return;
//        }

//        do {
        // Do nothing if the grid isn't empty.
//            boolean empty = true;
        for (int craftslot = 0; craftslot < gridSize * gridSize; craftslot++) {
            ItemStack stack = screen.getScreenHandler().getSlot(craftslot + firstCraftSlot).getStack();
            if (stack != null && !stack.isEmpty()) {
                return;
//                    LOGGER.info("returning as slot "+craftslot+" has "+stack.getCount()+" of "+stack.getTranslationKey());
//                    empty = false;
            }
        }
//            if (!empty) return;

        // todo repair item
//        if (underMouse instanceof RepairRecipe repairRecipe) {
//            fillCraftSlotsWithBestRepair(repairRecipe);
//        } else {
//            fillCraftSlotsWithAnyMaterials(underMouse);
//        }
        // move stuffs to crafting grid
        fillCraftSlotsWithAnyMaterials(underMouse);

        // todo stonecutter
//        if (underMouse.display() instanceof StonecutterRecipeDisplay) {
//            var interactionManager = client.interactionManager;
//            var container = (StonecutterScreenHandler) screen.getScreenHandler();
//
//            List<CuttingRecipeDisplay.GroupEntry<StonecuttingRecipe>> groups = container.getAvailableRecipes().entries();
//
//            int globalIndex = 0;
//            int foundIndex = -1;
//
//            search:
//            for (CuttingRecipeDisplay.GroupEntry<StonecuttingRecipe> group : groups) {
//                for (StonecuttingRecipe recipe : group.recipe().recipe().get()) {
//
//                    // 1. Get the displays for this specific recipe
//                    List<RecipeDisplay> recipeDisplays = recipe.getDisplays();
//
//                    // 2. Check if any of these displays match the one under the mouse
//                    for (RecipeDisplay display : recipeDisplays) {
//                        if (display.equals(underMouse.display())) {
//                            foundIndex = globalIndex;
//                            break search;
//                        }
//                    }
//                    globalIndex++;
//                }
//            }
//
//            if (foundIndex >= 0 && interactionManager != null) {
//                container.onButtonClick(player, foundIndex);
//                interactionManager.clickButton(container.syncId, foundIndex);
//            }
//        }

//            LOGGER.info("Item in result slot is "+screen.getContainer().getSlot(resultSlotNo).getStack().getItem().getName().getString());

        // left click or holding control = dont instant craft
        if (mouseButton == 0 && !Screen.hasControlDown()) {
            slotClick(resultSlotNo, mouseButton, SlotActionType.QUICK_MOVE);     // which is really PICKUP ALL
            updateRecipesIn(ModConfig.getAutoUpdateRecipeTimer()*50);

            if (underMouse.display().result().getFirst(RecipeHandler.getWorldContext()).getItem() == Items.HONEY_BLOCK) {
                slotClick(1, 0, SlotActionType.QUICK_MOVE);
                slotClick(2, 0, SlotActionType.QUICK_MOVE);
                slotClick(4, 0, SlotActionType.QUICK_MOVE);

                // click result slot
                if (gridSize == 2) {
                    slotClick(3, 0, SlotActionType.QUICK_MOVE);
                } else {
                    slotClick(5, 0, SlotActionType.QUICK_MOVE);
                }
            }
        }
//            LOGGER.info("mousebutton = "+mouseButton);
//            LOGGER.info("hasControl = "+Screen.hasControlDown());
//            LOGGER.info("hasShift = "+Screen.hasShiftDown());
//            LOGGER.info("canCraft = "+canCraftRecipe(underMouse, screen.getContainer(), gridSize));
//        } while (mouseButton==0 && Screen.hasControlDown() && Screen.hasShiftDown() && canCraftRecipe(underMouse, screen.getContainer(), gridSize));
    }

    private void fillCraftSlotsWithAnyMaterials(RecipeDisplayEntry underMouse) {
        // 1.21.4: Get ingredients safely from craftingRequirements
        // move stuffs onto grid
        ClientWorld world = client.world;

        List<Ingredient> recipeInput = underMouse.craftingRequirements().orElse(Collections.emptyList());
        if (recipeInput.isEmpty()) return;

        int maxCraftableStacks = 1;


        int rowadjust = 0;
        int gridWidth = 3; // Default for your UI grid

        // Check if it's a shaped recipe to handle the grid logic
        int recipeWidth = 0;
        List<SlotDisplay> ingredients;

        if (underMouse.display() instanceof ShapedCraftingRecipeDisplay shaped) {
            recipeWidth = shaped.width();
            ingredients = shaped.ingredients();
            //1 4 7
            //2 5 8
            //3 6 9
            if (Screen.hasShiftDown()) {
                // todo craft all...
                // find out max can craft
                maxCraftableStacks = 64;
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
                for (Map.Entry<Item, Integer> entry : ingredientMap.entrySet()) {
                    maxCraftableStacks = Math.min(maxCraftableStacks,itemMap.get(entry.getKey())/entry.getValue());
                }
                System.out.println("Can max craft " + underMouse.display().result().getFirst(RecipeHandler.getWorldContext()).getName() + ": " + maxCraftableStacks);
            }
        } else if (underMouse.display() instanceof ShapelessCraftingRecipeDisplay shapeless) {
            ingredients = shapeless.ingredients();
            recipeWidth = ingredients.size()<=4 ? 2 : 3;

            if (Screen.hasShiftDown()) {
                // todo craft all...
                // find out max can craft
                maxCraftableStacks = 64;
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
                for (Map.Entry<Item, Integer> entry : ingredientMap.entrySet()) {
                    maxCraftableStacks = Math.min(maxCraftableStacks,itemMap.get(entry.getKey())/entry.getValue());
                }
                System.out.println("Can max craft " + underMouse.display().result().getFirst(RecipeHandler.getWorldContext()).getName() + ": " + maxCraftableStacks);
            }

        } else {
            // not shaped or shapeless
            // todo make other crafting
            return;
        }


        for (int i = 0; i < ingredients.size(); i++) {
            int remaining = maxCraftableStacks;
            SlotDisplay ingredient = ingredients.get(i);
            if (RecipeHandler.getAllIngredients(ingredient).isEmpty()) continue;

            // Standard inventory search loop
            for (int slot = firstInventorySlotNo; remaining > 0 && slot < 36+firstInventorySlotNo; slot++) {
                // firstInventorySlotNo = slot of first main inv
                ItemStack slotcontent = screen.getScreenHandler().getSlot(slot).getStack();

                if (canActAsIngredient(ingredient, slotcontent)) {
                    transfer(slot, i + firstCraftSlot + rowadjust, remaining);

                    // Refresh remaining count based on what actually moved
                    ItemStack inCraftSlot = screen.getScreenHandler().getSlot(i + firstCraftSlot + rowadjust).getStack();
                    remaining = maxCraftableStacks - inCraftSlot.getCount();
                }
            }

            // Adjust for shaped grid layout
            if (recipeWidth > 0 && (i + 1) % recipeWidth == 0) {
                rowadjust += gridWidth - recipeWidth;
            }
        }
    }

    // todo make repair
//    private void fillCraftSlotsWithBestRepair(RepairRecipe repairRecipe) {
//
//        // New algorithm: acutally, combining the best item with the worst item
//        // is almost always right as it maximizes the 10% bonus from good items
//
//        int bestItemSlot = -1, worstItemSlot = -1;
//        for (int slot = 0; slot < 36; slot++) {
//            Slot invitem = screen.getScreenHandler().getSlot(slot + firstInventorySlotNo);
//            ItemStack slotcontent = invitem.getStack();
//            if (slotcontent.getItem() == repairRecipe.getItem()
//                    && slotcontent.getDamage() > 0
//                    && slotcontent.getEnchantments().getSize() <= ModConfig.getMaxEnchantsAllowedForRepair()
//            ) {
//                if (bestItemSlot == -1)
//                    bestItemSlot = worstItemSlot = slot;
//                else if (getDamage(bestItemSlot) > slotcontent.getDamage())
//                    bestItemSlot = slot;
//                else if (getDamage(worstItemSlot) < slotcontent.getDamage())
//                    worstItemSlot = slot;
//            }
//        }
//        if (bestItemSlot == -1 || worstItemSlot == -1 || worstItemSlot == bestItemSlot) {
//            return;
//        }
//
//        transfer(bestItemSlot + firstInventorySlotNo, firstCraftSlot, 1);
//        transfer(worstItemSlot + firstInventorySlotNo, firstCraftSlot + 1, 1);
//    }

    // ditch brewingstand

//
//    private int getDamage(int slot) {
//        ItemStack stack = screen.getScreenHandler().getSlot(slot + firstInventorySlotNo).getStack();
//        return stack.getDamage();
//    }

    public boolean keyPressed(int code, int scancode, int modifiers) {
        if (pattern == null)
            return false;
        // System.out.println("key code="+code+", scancode="+scancode+", modifiers="+modifiers);
        if (code == GLFW.GLFW_KEY_ENTER || code == GLFW.GLFW_KEY_KP_ENTER || code == GLFW.GLFW_KEY_ESCAPE) {
            updatePatternMatch();
            pattern.setFocused(false);
            return true;
        } else if (pattern.isFocused()) {
            // System.out.println("-> sending to pattern");
            pattern.keyPressed(code, scancode, modifiers);
            updatePatternMatch();
            return true;            // prevent 'e' from closing screen
        } else {
            return false;
        }
    }

    public boolean charTyped(char codepoint, int modifiers) {
        // System.out.println("char code="+codepoint+", modifiers="+modifiers);
        if (pattern != null && pattern.isFocused())
            return pattern.charTyped(codepoint, modifiers);
        return false;
    }

//    private DefaultedList<Ingredient> getIngredientsAsList(Recipe recipe) {
//        return recipe.getIngredients();
//    }

    private boolean canActAsIngredient(SlotDisplay ingredient, ItemStack inventoryItem) {
        // todo make shulker box and add damage
        if (inventoryItem.isEmpty()) return false;

        // Get all valid items for this ingredient slot
        List<ItemStack> validStacks = ingredient.getStacks(SlotDisplayContexts.createParameters(client.world));

        // Check if the item in our inventory matches any of the valid ingredients
        return validStacks.stream()
                .anyMatch(validStack -> validStack.getItem() == inventoryItem.getItem());
    }

    public void transfer(int from, int to, int amount) {
        Slot fromSlot = screen.getScreenHandler().getSlot(from);
        ItemStack fromContent = fromSlot.getStack();

        //System.out.println("Trying to transfer "+amount+" "+fromContent.getDisplayName()+" from slot "+from+" to "+to);
        // want as much as we have, or more? Transfer all there is
        if (amount >= fromSlot.getStack().getCount()) {
            slotClick(from, 0, SlotActionType.PICKUP);
            slotClick(to, 0, SlotActionType.PICKUP);
            return;
        }

        // Transfer as many half stacks as possible. If there is an odd number
        // of items in the source slot, right clicking will round the source slot
        // down, and the hand up, so we transfer (n+1)/2 items.
        int transfer;
        while (amount >= (transfer = ((fromSlot.getStack().getCount() + 1) / 2))) {
            slotClick(from, 1, SlotActionType.PICKUP);       // right click to get half the source
            slotClick(to, 0, SlotActionType.PICKUP);
            amount -= transfer;
            //System.out.println("transferred "+transfer+", amount is now "+amount);
        }

        if (amount > 0) {
            int prevCount = fromContent.getCount();
            slotClick(from, 0, SlotActionType.PICKUP);       // left click source
            for (int i = 0; i < amount; i++)
                slotClick(to, 1, SlotActionType.PICKUP);         // right click target to deposit 1 item
            if (prevCount > amount)
                slotClick(from, 0, SlotActionType.PICKUP);       // left click source again to put stuff back
        }
    }

    public void slotClick(int slot, int mouseButton, SlotActionType clickType) {
        ((SlotClickAccepter) screen).slotClick(slot, mouseButton, clickType);
    }
}
