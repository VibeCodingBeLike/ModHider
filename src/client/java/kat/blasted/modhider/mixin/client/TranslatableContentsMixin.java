package kat.blasted.modhider.mixin.client;

import com.google.common.collect.ImmutableList;
import kat.blasted.modhider.filter.ComponentMarker;
import kat.blasted.modhider.filter.ModHiderFilter;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.chat.contents.TranslatableFormatException;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

@Mixin(TranslatableContents.class)
public abstract class TranslatableContentsMixin implements ComponentMarker {

    @Shadow
    @Final
    private String key;

    @Shadow
    @Final
    private String fallback;

    @Shadow
    private Language decomposedWith;

    @Shadow
    private List<FormattedText> decomposedParts;

    @Shadow
    protected abstract void decomposeTemplate(String template, Consumer<FormattedText> consumer);

    @Unique
    private boolean modhider$marked = false;

    @Override
    public void modhider$mark() {
        this.modhider$marked = true;
        this.decomposedWith = null;
    }

    @Override
    public boolean modhider$isMarked() {
        return this.modhider$marked;
    }

    @Inject(method = "<init>(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)V", at = @At("RETURN"))
    private void onInit(String key, String fallback, Object[] args, CallbackInfo ci) {
        if (ModHiderFilter.isDecodingServerPacket() || ModHiderFilter.isInItemContext()) {
            this.modhider$marked = true;
        }
    }

    @Inject(method = "decompose", at = @At("HEAD"), cancellable = true)
    private void onDecompose(CallbackInfo ci) {
        if ((this.modhider$marked || ModHiderFilter.isInItemContext()) && ModHiderFilter.isTranslationKeyHidden(this.key)) {
            Language language = Language.getInstance();
            this.decomposedWith = language;
            String string = this.fallback != null ? this.fallback : this.key;
            try {
                ImmutableList.Builder<FormattedText> builder = ImmutableList.builder();
                this.decomposeTemplate(string, builder::add);
                this.decomposedParts = builder.build();
            } catch (TranslatableFormatException e) {
                this.decomposedParts = ImmutableList.of(FormattedText.of(string));
            }
            ci.cancel();
        }
    }
}
