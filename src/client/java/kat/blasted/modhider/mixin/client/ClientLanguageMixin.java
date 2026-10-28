package kat.blasted.modhider.mixin.client;

import kat.blasted.modhider.filter.ModHiderFilter;
import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLanguage.class)
public abstract class ClientLanguageMixin {

    @Inject(method = "has", at = @At("HEAD"), cancellable = true)
    private void onHas(String key, CallbackInfoReturnable<Boolean> cir) {
        if (ModHiderFilter.isTranslationKeyHidden(key)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getOrDefault(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private void onGetOrDefault(String key, String fallback, CallbackInfoReturnable<String> cir) {
        if (ModHiderFilter.isTranslationKeyHidden(key)) {
            cir.setReturnValue(fallback != null ? fallback : key);
        }
    }
}
