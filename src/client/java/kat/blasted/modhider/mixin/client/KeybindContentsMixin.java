package kat.blasted.modhider.mixin.client;

import kat.blasted.modhider.filter.ComponentMarker;
import kat.blasted.modhider.filter.ModHiderFilter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.KeybindContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeybindContents.class)
public abstract class KeybindContentsMixin implements ComponentMarker {

    @Shadow
    @Final
    private String name;

    @Unique
    private boolean modhider$marked = false;

    @Override
    public void modhider$mark() {
        this.modhider$marked = true;
    }

    @Override
    public boolean modhider$isMarked() {
        return this.modhider$marked;
    }

    @Inject(method = "<init>(Ljava/lang/String;)V", at = @At("RETURN"))
    private void onInit(String name, CallbackInfo ci) {
        if (ModHiderFilter.isDecodingServerPacket() || ModHiderFilter.isInItemContext()) {
            this.modhider$marked = true;
        }
    }

    @Inject(method = "getNestedComponent", at = @At("HEAD"), cancellable = true)
    private void onGetNestedComponent(CallbackInfoReturnable<Component> cir) {
        if ((this.modhider$marked || ModHiderFilter.isInItemContext()) && ModHiderFilter.isTranslationKeyHidden(this.name)) {
            cir.setReturnValue(Component.literal(this.name));
        }
    }
}
