package kat.blasted.modhider.mixin.client;

import kat.blasted.modhider.filter.ModHiderFilter;
import net.fabricmc.fabric.impl.networking.client.ClientLoginNetworkAddon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClientLoginNetworkAddon.class, remap = false)
public abstract class ClientLoginNetworkAddonMixin {

    @Inject(method = "handlePacket(ILnet/minecraft/resources/Identifier;Lnet/minecraft/network/FriendlyByteBuf;)Z", at = @At("HEAD"), cancellable = true)
    private void onHandleLoginPacket(int queryId, Identifier channelName, FriendlyByteBuf originalBuf, CallbackInfoReturnable<Boolean> cir) {
        if (channelName != null && ModHiderFilter.isChannelHidden(channelName)) {
            cir.setReturnValue(false);
        }
    }
}
