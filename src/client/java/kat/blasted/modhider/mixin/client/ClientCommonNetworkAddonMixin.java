package kat.blasted.modhider.mixin.client;

import kat.blasted.modhider.filter.ModHiderFilter;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.fabricmc.fabric.impl.networking.client.ClientCommonNetworkAddon", remap = false)
public abstract class ClientCommonNetworkAddonMixin {

    @Inject(method = "handleRegistration", at = @At("HEAD"), cancellable = true)
    private void onHandleRegistration(Identifier channelName, CallbackInfo ci) {
        if (channelName != null && ModHiderFilter.isChannelHidden(channelName)) {
            ci.cancel();
        }
    }
}
