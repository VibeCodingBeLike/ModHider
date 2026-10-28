package kat.blasted.modhider.mixin.client;

import kat.blasted.modhider.filter.ModHiderFilter;
import net.fabricmc.fabric.impl.networking.AbstractNetworkAddon;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

@Mixin(value = AbstractNetworkAddon.class, remap = false)
public abstract class AbstractNetworkAddonMixin {

    @Inject(method = "getReceivableChannels", at = @At("RETURN"), cancellable = true)
    private void filterReceivableChannels(CallbackInfoReturnable<Set<Identifier>> cir) {
        Set<Identifier> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) {
            return;
        }

        Set<Identifier> filtered = new HashSet<>();
        boolean modified = false;

        for (Identifier id : original) {
            if (ModHiderFilter.isChannelHidden(id)) {
                modified = true;
            } else {
                filtered.add(id);
            }
        }

        if (modified) {
            cir.setReturnValue(filtered);
        }
    }
}
