package kat.blasted.modhider.mixin.client;

import kat.blasted.modhider.filter.ModHiderFilter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "getHoverName", at = @At("HEAD"))
    private void beforeGetHoverName(CallbackInfoReturnable<Component> cir) {
        ModHiderFilter.setIsInItemContext(true);
    }

    @Inject(method = "getHoverName", at = @At("RETURN"))
    private void afterGetHoverName(CallbackInfoReturnable<Component> cir) {
        ModHiderFilter.setIsInItemContext(false);
        Component comp = cir.getReturnValue();
        if (comp != null) {
            ModHiderFilter.markComponent(comp);
        }
    }

    @Inject(method = "getItemName", at = @At("HEAD"))
    private void beforeGetItemName(CallbackInfoReturnable<Component> cir) {
        ModHiderFilter.setIsInItemContext(true);
    }

    @Inject(method = "getItemName", at = @At("RETURN"))
    private void afterGetItemName(CallbackInfoReturnable<Component> cir) {
        ModHiderFilter.setIsInItemContext(false);
        Component comp = cir.getReturnValue();
        if (comp != null) {
            ModHiderFilter.markComponent(comp);
        }
    }

    @Inject(method = "getCustomName", at = @At("HEAD"))
    private void beforeGetCustomName(CallbackInfoReturnable<Component> cir) {
        ModHiderFilter.setIsInItemContext(true);
    }

    @Inject(method = "getCustomName", at = @At("RETURN"))
    private void afterGetCustomName(CallbackInfoReturnable<Component> cir) {
        ModHiderFilter.setIsInItemContext(false);
        Component comp = cir.getReturnValue();
        if (comp != null) {
            ModHiderFilter.markComponent(comp);
        }
    }

    @Inject(method = "getDisplayName", at = @At("HEAD"))
    private void beforeGetDisplayName(CallbackInfoReturnable<Component> cir) {
        ModHiderFilter.setIsInItemContext(true);
    }

    @Inject(method = "getDisplayName", at = @At("RETURN"))
    private void afterGetDisplayName(CallbackInfoReturnable<Component> cir) {
        ModHiderFilter.setIsInItemContext(false);
        Component comp = cir.getReturnValue();
        if (comp != null) {
            ModHiderFilter.markComponent(comp);
        }
    }

    @Inject(method = "getStyledHoverName", at = @At("HEAD"))
    private void beforeGetStyledHoverName(CallbackInfoReturnable<Component> cir) {
        ModHiderFilter.setIsInItemContext(true);
    }

    @Inject(method = "getStyledHoverName", at = @At("RETURN"))
    private void afterGetStyledHoverName(CallbackInfoReturnable<Component> cir) {
        ModHiderFilter.setIsInItemContext(false);
        Component comp = cir.getReturnValue();
        if (comp != null) {
            ModHiderFilter.markComponent(comp);
        }
    }

    @Inject(method = "getTooltipLines", at = @At("HEAD"))
    private void beforeGetTooltipLines(Item.TooltipContext context, Player player, TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir) {
        ModHiderFilter.setIsInItemContext(true);
    }

    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void afterGetTooltipLines(Item.TooltipContext context, Player player, TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir) {
        ModHiderFilter.setIsInItemContext(false);
        List<Component> list = cir.getReturnValue();
        if (list != null) {
            for (Component comp : list) {
                ModHiderFilter.markComponent(comp);
            }
        }
    }
}
