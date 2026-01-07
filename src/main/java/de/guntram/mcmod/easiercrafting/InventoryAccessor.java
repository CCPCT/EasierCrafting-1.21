package de.guntram.mcmod.easiercrafting;

import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import java.util.List;

// just use to get inventory... as it changed between 1.21.4 to 1.21.5
public interface InventoryAccessor {
    List<ItemStack> getCompatMain();
}