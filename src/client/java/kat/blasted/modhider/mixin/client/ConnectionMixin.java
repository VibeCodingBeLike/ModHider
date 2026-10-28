package kat.blasted.modhider.mixin.client;

import io.netty.channel.ChannelFutureListener;
import kat.blasted.modhider.config.ModHiderConfig;
import kat.blasted.modhider.filter.ModHiderFilter;
import net.fabricmc.fabric.impl.networking.CommonRegisterPayload;
import net.fabricmc.fabric.impl.networking.RegistrationPayload;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ServerboundSelectKnownPacks;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.KnownPack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mixin(Connection.class)
public abstract class ConnectionMixin {

    @Shadow
    public abstract boolean isMemoryConnection();

    @Shadow
    public abstract void send(Packet<?> packet, ChannelFutureListener listener, boolean flush);

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, ChannelFutureListener callback, boolean flush, CallbackInfo ci) {
        if (this.isMemoryConnection()) {
            return;
        }

        // 1. CustomPayload packets
        if (packet instanceof ServerboundCustomPayloadPacket customPacket) {
            CustomPacketPayload payload = customPacket.payload();
            if (payload == null) {
                return;
            }

            // Client brand spoofing
            if (payload instanceof BrandPayload brandPayload) {
                if (ModHiderConfig.isModHidden("fabric") || ModHiderConfig.isModHidden("fabricloader")) {
                    if (!"vanilla".equals(brandPayload.brand())) {
                        ci.cancel();
                        ModHiderFilter.LOGGER.info("[ModHider] Spoofing client brand to vanilla");
                        this.send(new ServerboundCustomPayloadPacket(new BrandPayload("vanilla")), callback, flush);
                    }
                    return;
                }
            }

            // RegistrationPayload filtering (minecraft:register and minecraft:unregister)
            if (payload instanceof RegistrationPayload regPayload) {
                List<Identifier> channels = regPayload.channels();
                List<Identifier> filtered = new ArrayList<>();
                for (Identifier id : channels) {
                    if (!ModHiderFilter.isChannelHidden(id)) {
                        filtered.add(id);
                    } else {
                        ModHiderFilter.LOGGER.info("[ModHider] Filtered RegistrationPayload channel: {}", id);
                    }
                }

                if (filtered.size() != channels.size()) {
                    ci.cancel();
                    if (!filtered.isEmpty()) {
                        this.send(new ServerboundCustomPayloadPacket(new RegistrationPayload(regPayload.type(), filtered)), callback, flush);
                    }
                    return;
                }
            }

            // CommonRegisterPayload filtering (fabric-networking-api-v1:c_register)
            if (payload instanceof CommonRegisterPayload commonPayload) {
                Set<Identifier> channels = commonPayload.channels();
                Set<Identifier> filtered = new HashSet<>();
                for (Identifier id : channels) {
                    if (!ModHiderFilter.isChannelHidden(id)) {
                        filtered.add(id);
                    } else {
                        ModHiderFilter.LOGGER.info("[ModHider] Filtered CommonRegisterPayload channel: {}", id);
                    }
                }

                if (filtered.size() != channels.size()) {
                    ci.cancel();
                    if (!filtered.isEmpty()) {
                        this.send(new ServerboundCustomPayloadPacket(new CommonRegisterPayload(commonPayload.version(), commonPayload.protocol(), filtered)), callback, flush);
                    }
                    return;
                }
            }

            // Direct packet from a hidden mod or modhider
            if (payload.type() != null && payload.type().id() != null) {
                Identifier channel = payload.type().id();
                if (ModHiderFilter.isChannelHidden(channel)) {
                    ci.cancel();
                    ModHiderFilter.LOGGER.info("[ModHider] Blocked outgoing packet on channel: {}", channel);
                    return;
                }
            }
        }

        // 4. Filter KnownPacks
        if (packet instanceof ServerboundSelectKnownPacks knownPacksPacket) {
            List<KnownPack> packs = knownPacksPacket.knownPacks();
            List<KnownPack> filtered = new ArrayList<>();
            for (KnownPack pack : packs) {
                if (!ModHiderFilter.shouldHideKnownPack(pack)) {
                    filtered.add(pack);
                } else {
                    ModHiderFilter.LOGGER.info("[ModHider] Filtered outgoing KnownPack: {}:{} ({})", pack.namespace(), pack.id(), pack.version());
                }
            }

            if (filtered.size() != packs.size()) {
                ci.cancel();
                ModHiderFilter.LOGGER.info("[ModHider] Re-sending filtered ServerboundSelectKnownPacks ({} -> {} packs)", packs.size(), filtered.size());
                this.send(new ServerboundSelectKnownPacks(filtered), callback, flush);
                return;
            }
        }

        // 5. Sign text leakage prevention
        if (packet instanceof ServerboundSignUpdatePacket signPacket) {
            String[] lines = signPacket.getLines();
            boolean modified = false;
            String[] newLines = new String[lines.length];
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (ModHiderFilter.isTranslationKeyHidden(line) || ModHiderFilter.isItemRenameHidden(line)) {
                    newLines[i] = "";
                    modified = true;
                    ModHiderFilter.LOGGER.info("[ModHider] Sanitized ServerboundSignUpdatePacket line {}: {}", i, line);
                } else {
                    newLines[i] = line;
                }
            }
            if (modified) {
                ci.cancel();
                this.send(new ServerboundSignUpdatePacket(signPacket.getPos(), signPacket.isFrontText(), newLines[0], newLines[1], newLines[2], newLines[3]), callback, flush);
                return;
            }
        }

        // 6. Anvil item rename leakage prevention
        if (packet instanceof ServerboundRenameItemPacket renamePacket) {
            String name = renamePacket.getName();
            if (ModHiderFilter.isTranslationKeyHidden(name) || ModHiderFilter.isItemRenameHidden(name)) {
                ci.cancel();
                ModHiderFilter.LOGGER.info("[ModHider] Blocked ServerboundRenameItemPacket: {}", name);
                this.send(new ServerboundRenameItemPacket(""), callback, flush);
                return;
            }
        }
    }
}
