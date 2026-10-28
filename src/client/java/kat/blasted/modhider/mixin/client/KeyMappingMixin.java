package kat.blasted.modhider.mixin.client;

import kat.blasted.modhider.filter.ModHiderFilter;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

@Mixin(KeyMapping.class)
public class KeyMappingMixin {

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private static void onGet(String name, CallbackInfoReturnable<KeyMapping> cir) {
        if (ModHiderFilter.isTranslationKeyHidden(name)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "createNameSupplier", at = @At("HEAD"), cancellable = true)
    private static void onCreateNameSupplier(String name, CallbackInfoReturnable<Supplier<Component>> cir) {
        if (ModHiderFilter.isTranslationKeyHidden(name)) {
            cir.setReturnValue(() -> Component.literal(name));
        }
    }
}
