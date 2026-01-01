/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.mcmod.easiercrafting;

import java.util.*;

import de.guntram.mcmod.easiercrafting.modConfig.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.recipe.book.RecipeBookCategories;
import net.minecraft.recipe.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author gbl
 */
public class InventoryRecipeScanner {
    
    private static final Logger LOGGER = LogManager.getLogger();
    
    public static List<RecipeDisplayEntry> findUnusualRecipes(ScreenHandler inventory, int firstInventorySlotNo) {
        ArrayList<RecipeDisplayEntry> result=new ArrayList<>();

        // needed for various recipes; count the number of each dye type
        Map<DyeColor, Integer> hasDye = new HashMap<>();
        
        boolean hasShulkerBox=false;
        ItemStack shulkerBoxItemStack=null;
        boolean hasDyeableBanner=false;
        boolean hasCopyableBanner=false;
        boolean hasHelmet=false, hasChest=false, hasPants=false, hasBoots=false;        // leather!
        Map<Potion, Integer> availablePotions=new HashMap<>();
        int     availableArrows=0;
        boolean hasPaper=false;
        int     availableGunPowder=0;
        boolean hasWrittenBook=false;
        boolean hasWritableBook=false;
        boolean hasFilledMap=false;
        boolean hasMap=false;
        Map<Item, Integer> hasRepairable = new HashMap<>();
        
        for (int i=0; i<36; i++) {
            Slot invitem=inventory.getSlot(i+firstInventorySlotNo);
            ItemStack stack=invitem.getStack();
            Item item = stack.getItem();

            if (stack.isDamageable() && stack.isDamaged()) {
                ItemEnchantmentsComponent enchantments = stack.getEnchantments();
                if (enchantments.getSize() <= ModConfig.getMaxEnchantsAllowedForRepair()) {
                    Integer previous=hasRepairable.get(item);
                    hasRepairable.put(item, previous == null ? 1 : previous+1);
                }
            } else if (stack.hasEnchantments()) {
                continue;
            } else if (item instanceof DyeItem) {
                DyeColor color=((DyeItem)item).getColor();
                if (hasDye.containsKey(color)) {
                    hasDye.put(color, hasDye.get(color)+stack.getCount());
                } else {
                    hasDye.put(color, stack.getCount());
                }
            }
            else if (Block.getBlockFromItem(item) instanceof ShulkerBoxBlock) {
                hasShulkerBox=true;
                shulkerBoxItemStack=stack;
            }
            /* TODO 
            else if (item == Items.BANNER) {
                int patterns=TileEntityBanner.getPatterns(stack);
                if (patterns<6)
                    hasDyeableBanner=true;
                if (patterns>0)
                    hasCopyableBanner=true;
            }
            */
            else if (item == Items.LEATHER_HELMET) {
                hasHelmet=true;
            }
            else if (item == Items.LEATHER_CHESTPLATE) {
                hasChest=true;
            }
            else if (item == Items.LEATHER_LEGGINGS) {
                hasPants=true;
            }
            else if (item == Items.LEATHER_BOOTS) {
                hasBoots=true;
            }
            else if (item == Items.ARROW) {
                availableArrows+=stack.getCount();
            }
            else if (item == Items.GUNPOWDER) {
                availableGunPowder+=stack.getCount();
            }
            else if (item == Items.PAPER) {
                hasPaper=true;
            }
            else if (item == Items.WRITTEN_BOOK) {
                hasWrittenBook=true;
            }
            else if (item == Items.WRITABLE_BOOK) {
                hasWritableBook=true;
            }
            else if (item == Items.FILLED_MAP) {
                hasFilledMap=true;
            }
            else if (item == Items.MAP) {
                hasMap=true;
            }
        }

        // todo dye shulkbox
//        if (hasShulkerBox) {
//            // 1. Get the current box from the player's inventory to preserve its contents (NBT/Components)
//            // Assuming 'availableShulkerBoxSlot' is the index you stored earlier
//            MinecraftClient client = MinecraftClient.getInstance();
//            ItemStack currentBox = client.player.getInventory().getStack(availableShulkerBoxSlot);
//            SlotDisplay station = new SlotDisplay.StackSlotDisplay(new ItemStack(Items.CRAFTING_TABLE));
//
//            for (DyeColor dye : hasDye.keySet()) {
//                // 2. Determine the resulting item
//                Item coloredBoxItem = ShulkerBoxBlock.get(dye).asItem();
//                ItemStack resultStack = new ItemStack(coloredBoxItem);
//
//                // 3. IMPORTANT: Copy the contents!
//                // In 1.21, container contents are stored in the CONTAINER component.
//                resultStack.applyComponentsFrom(currentBox);
//
//                // 4. Create Ingredients (The box + the dye)
//                List<SlotDisplay> ingredients = List.of(
//                        new SlotDisplay.StackSlotDisplay(currentBox),
//                        new SlotDisplay.StackSlotDisplay(new ItemStack(DyeItem.byColor(dye)))
//                );
//
//                // 5. Build the Display
//                ShapelessCraftingRecipeDisplay display = new ShapelessCraftingRecipeDisplay(
//                        ingredients,
//                        new SlotDisplay.StackSlotDisplay(resultStack),
//                        station
//                );
//
//                // 6. Add the Entry
//                // Use a unique ID based on the dye color ID to prevent overlap
//                int networkId = 8000 + dye.getId();
//                result.add(new RecipeDisplayEntry(
//                        new NetworkRecipeId(networkId),
//                        display,
//                        OptionalInt.empty(),
//                        RecipeBookCategories.CRAFTING_MISC,
//                        Optional.empty()
//                ));
//            }
//        }

        // do not implement banners at the moment
        // do not implement colored leather at the moment

        //System.out.println("Arrows: "+availableArrows);
        // todo make potion arrow
        if (availableArrows >= 8) {
            // Standard station icon
            SlotDisplay station = new SlotDisplay.StackSlotDisplay(new ItemStack(Items.CRAFTING_TABLE));

            for (Potion type : availablePotions.keySet()) {
                // 1. Get the potion stack and its contents
                ItemStack potionStack = inventory.getSlot(firstInventorySlotNo + availablePotions.get(type)).getStack();
                // In 1.21, we fetch the PotionContentsComponent record
                PotionContentsComponent contents = potionStack.get(DataComponentTypes.POTION_CONTENTS);

                if (contents == null) continue;

                // 2. Create the Result Arrow (8 qty)
                ItemStack resultArrow = new ItemStack(Items.TIPPED_ARROW, 8);
                // Apply the potion component directly to the arrow
                resultArrow.set(DataComponentTypes.POTION_CONTENTS, contents);

                // 3. Create the Ingredients Display (8 arrows + 1 potion)
                List<SlotDisplay> ingredientsDisplay = new ArrayList<>();
                for (int i = 0; i < 8; i++) {
                    ingredientsDisplay.add(new SlotDisplay.StackSlotDisplay(new ItemStack(Items.ARROW)));
                }
                ingredientsDisplay.add(new SlotDisplay.StackSlotDisplay(new ItemStack(potionStack.getItem())));

                // 4. Build the Display
                ShapelessCraftingRecipeDisplay display = new ShapelessCraftingRecipeDisplay(
                        ingredientsDisplay,
                        new SlotDisplay.StackSlotDisplay(resultArrow),
                        station
                );

                // 5. Add to results with a unique Network ID
                // Using hashCode of the potion name to keep IDs distinct for different types
                int networkId = 7000 + type.hashCode();
                result.add(new RecipeDisplayEntry(
                        new NetworkRecipeId(networkId),
                        display,
                        OptionalInt.empty(),
                        RecipeBookCategories.CRAFTING_MISC,
                        Optional.empty()
                ));
            }
        }

        //System.out.println("Paper: "+hasPaper);
        //System.out.println("Gunpowder: "+availableGunPowder);

        // todo custom firework
        if (hasPaper && availableGunPowder > 0) {
            // 1. Base ingredient for station icon
            SlotDisplay station = new SlotDisplay.StackSlotDisplay(new ItemStack(Items.CRAFTING_TABLE));

            for (int power = 1; power <= 3; power++) {
                if (availableGunPowder >= power) {
                    // 2. Prepare the Result Item
                    ItemStack resultItem = new ItemStack(Items.FIREWORK_ROCKET, 3);

                    // In 1.21, FireworksComponent is a record: (int flightDuration, List<FireworkExplosionComponent> explosions)
                    // We pass an empty list for explosions if there are none.
                    resultItem.set(DataComponentTypes.FIREWORKS, new FireworksComponent(power, List.of()));
                    resultItem.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Strength " + power));

                    // 3. Prepare the Ingredients List for the Display
                    List<SlotDisplay> ingredientsDisplay = new ArrayList<>();
                    ingredientsDisplay.add(new SlotDisplay.StackSlotDisplay(new ItemStack(Items.PAPER)));
                    for (int k = 0; k < power; k++) {
                        ingredientsDisplay.add(new SlotDisplay.StackSlotDisplay(new ItemStack(Items.GUNPOWDER)));
                    }

                    // 4. Create the Display logic
                    ShapelessCraftingRecipeDisplay display = new ShapelessCraftingRecipeDisplay(
                            ingredientsDisplay,
                            new SlotDisplay.StackSlotDisplay(resultItem),
                            station
                    );

                    // 5. Add to your results list
                    result.add(new RecipeDisplayEntry(
                            new NetworkRecipeId(500 + power), // Use unique IDs to avoid UI glitches
                            display,
                            OptionalInt.empty(),
                            RecipeBookCategories.CRAFTING_MISC,
                            Optional.empty()
                    ));
                }
            }
        }

        //todo add repair item support
//        for (Item item:hasRepairable.keySet()) {
//            LOGGER.debug("repairable "+item.getTranslationKey()+": "+hasRepairable.get(item));
//            if (hasRepairable.get(item)>=2)
//                result.add(new RepairRecipe(item));
//        }


        //System.out.println("returning "+result.size()+" custom recipes");
        return result;
    }
}
