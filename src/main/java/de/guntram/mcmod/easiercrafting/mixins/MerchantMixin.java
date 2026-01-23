package de.guntram.mcmod.easiercrafting.mixins;

import de.guntram.mcmod.easiercrafting.modConfig.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantScreen.class)
public abstract class MerchantMixin extends Screen {
    protected MerchantMixin(Text title) {
        super(title);
    }

    @Shadow private int selectedIndex;

    @Inject(
            method = "mouseClicked",
            at = @At(value = "TAIL")
    )
    private void onTradeSelected(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.get().enableTrading || client.player.currentScreenHandler.getSlot(2).getStack().isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        long window = client.getWindow().getHandle();
        if (Screen.hasShiftDown()){
            client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId,2,1, InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_Q) ? SlotActionType.THROW : SlotActionType.QUICK_MOVE,client.player);
        } else {
            if (InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_Q)){
                client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId,2,0, SlotActionType.THROW,client.player);
                return;
            }

            ItemStack fromStack = client.player.currentScreenHandler.getSlot(2).getStack().copy();
            client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId,2,0, SlotActionType.PICKUP,client.player);
            for (int i=3; i<39; i++){
                ItemStack targetStack = client.player.currentScreenHandler.getSlot(i).getStack();
                if (targetStack.isEmpty()){
                    client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId,i,0, SlotActionType.PICKUP,client.player);
                    return;
                }
                if (fromStack.getItem() != targetStack.getItem()) continue;
                if (targetStack.getCount() == targetStack.getMaxCount()) continue;
                fromStack.setCount(Math.max(0,targetStack.getCount()+fromStack.getCount()-targetStack.getMaxCount()));
                client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId,i,0, SlotActionType.PICKUP,client.player);
                if (fromStack.getCount()==0) return;

            }
        }
    }
}