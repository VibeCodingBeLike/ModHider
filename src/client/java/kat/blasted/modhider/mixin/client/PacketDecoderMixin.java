package kat.blasted.modhider.mixin.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import kat.blasted.modhider.filter.ModHiderFilter;
import net.minecraft.network.PacketDecoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PacketDecoder.class)
public abstract class PacketDecoderMixin {

    @Inject(method = "decode", at = @At("HEAD"))
    private void beforeDecode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out, CallbackInfo ci) {
        ModHiderFilter.setIsDecodingServerPacket(true);
    }

    @Inject(method = "decode", at = @At("RETURN"))
    private void afterDecode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out, CallbackInfo ci) {
        ModHiderFilter.setIsDecodingServerPacket(false);
    }
}
