package de.guntram.mcmod.easiercrafting.mixins;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import de.guntram.mcmod.easiercrafting.modConfig.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantScreen.class)
public abstract class MerchantMixin extends Screen {
    @Shadow
    private int shopItem;

    protected MerchantMixin(Component title) {
        super(title);
    }

    @Inject(
            method = "mouseClicked",
            at = @At(value = "TAIL")
    )
    private void onTradeSelected(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        Window window = Minecraft.getInstance().getWindow();
        Minecraft client = Minecraft.getInstance();
        if (!ModConfig.get().enableTrading || InputConstants.isKeyDown(window,InputConstants.KEY_LCONTROL) || client.player.containerMenu.getSlot(2).getItem().isEmpty()) return;

        // ai = leftPos, aj = topPos
        int ai = (this.width - 276) / 2;
        int aj = (this.height - 166) / 2;

        int listStartX = ai+5;
        int listEndX = ai+5 + 88;
        int listStartY = aj + 16;
        int listEndY = aj + 16 + 140;

        boolean isOverTradeTab = event.x() >= listStartX && event.x() <= listEndX &&
                event.y() >= listStartY && event.y() <= listEndY;

        System.out.println("requirement: " + (ai+5) + "/" + (aj+16) + " to " + (ai+5+88) + "/" + (aj+16+120));
        System.out.println(event.x() + "/" + event.y() + " over trade tab: " + isOverTradeTab);
        System.out.println(this.shopItem);
        if (!isOverTradeTab) return;

        AbstractContainerMenu currentScreenHandler = client.player.containerMenu;
        var syncId = currentScreenHandler.containerId;

        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)){
            client.gameMode.handleContainerInput(client.player.containerMenu.containerId, 2, 0, ContainerInput.QUICK_MOVE, client.player);
        } else {
            if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_Q)){
                client.gameMode.handleContainerInput(syncId,2,0, ContainerInput.THROW,client.player);
                // move items back from villager
                client.gameMode.handleContainerInput(syncId,0,0, ContainerInput.QUICK_MOVE,client.player);
                client.gameMode.handleContainerInput(syncId,1,0, ContainerInput.QUICK_MOVE,client.player);

                return;
            }

            ItemStack fromStack = currentScreenHandler.getSlot(2).getItem().copy();
            client.gameMode.handleContainerInput(syncId,2,0, ContainerInput.PICKUP,client.player);
            for (int i=3; i<39; i++){
                ItemStack targetStack = currentScreenHandler.getSlot(i).getItem();
                if (targetStack.isEmpty()){
                    client.gameMode.handleContainerInput(syncId,i,0, ContainerInput.PICKUP,client.player);
                    break;
                }
                if (fromStack.getItem() != targetStack.getItem()) continue;
                if (targetStack.getCount() == targetStack.getMaxStackSize()) continue;
                fromStack.setCount(Math.max(0,targetStack.getCount()+fromStack.getCount()-targetStack.getMaxStackSize()));
                client.gameMode.handleContainerInput(syncId,i,0, ContainerInput.PICKUP,client.player);
                if (fromStack.getCount()==0) break;
            }
            client.gameMode.handleContainerInput(syncId,0,0, ContainerInput.QUICK_MOVE,client.player);
            client.gameMode.handleContainerInput(syncId,1,0, ContainerInput.QUICK_MOVE,client.player);

        }
    }
}