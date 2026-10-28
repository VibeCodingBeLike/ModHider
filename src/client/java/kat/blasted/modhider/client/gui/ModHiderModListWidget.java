package kat.blasted.modhider.client.gui;

import kat.blasted.modhider.config.ModHiderConfig;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class ModHiderModListWidget extends ContainerObjectSelectionList<ModHiderModListWidget.ModEntry> {
    private final ModHiderConfigScreen screen;
    private final List<ModContainer> allMods = new ArrayList<>();

    public ModHiderModListWidget(Minecraft client, int width, int height, int y, int itemHeight, ModHiderConfigScreen screen) {
        super(client, width, height, y, itemHeight);
        this.screen = screen;
    }

    @Override
    public int getRowWidth() {
        return Math.min(440, this.width - 32);
    }

    @Override
    protected void extractListBackground(GuiGraphicsExtractor extractor) {
        // Transparent to allow sleek dark gradient from ModHiderConfigScreen
    }

    @Override
    protected void extractListSeparators(GuiGraphicsExtractor extractor) {
        // Header and footer are framed by the screen's frosted bars
    }

    public void setMods(Collection<ModContainer> mods) {
        this.allMods.clear();
        this.allMods.addAll(mods);
        this.allMods.sort(Comparator.comparing(
                (ModContainer m) -> !m.getMetadata().getId().equalsIgnoreCase(ModHiderConfig.MOD_ID)
        ).thenComparing(m -> m.getMetadata().getName().toLowerCase(Locale.ROOT)));
        filter("");
    }

    public void filter(String query) {
        this.clearEntries();
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT);
        for (ModContainer mod : this.allMods) {
            ModMetadata meta = mod.getMetadata();
            String id = meta.getId().toLowerCase(Locale.ROOT);
            String name = meta.getName().toLowerCase(Locale.ROOT);
            if (q.isEmpty() || id.contains(q) || name.contains(q)) {
                this.addEntry(new ModEntry(mod));
            }
        }
        this.setScrollAmount(0.0);
    }

    public void refreshAll() {
        for (ModEntry entry : this.children()) {
            entry.updateButtonState();
        }
    }

    public List<ModContainer> getAllMods() {
        return allMods;
    }

    public class ModEntry extends ContainerObjectSelectionList.Entry<ModEntry> {
        private static final int[] AVATAR_PALETTE = {
                0xFF2980B9, // Sapphire Blue
                0xFF16A085, // Teal
                0xFF27AE60, // Emerald
                0xFF8E44AD, // Amethyst
                0xFFD35400, // Burnt Orange
                0xFF2C3E50, // Midnight Slate
                0xFF0097A7, // Cyan
                0xFF5C6BC0, // Indigo
                0xFFE67E22, // Orange
                0xFF6D4C41  // Deep Walnut
        };

        private final ModContainer mod;
        private final String modId;
        private final boolean isSelf;
        private final int avatarBgColor;

        private final StringWidget avatarWidget;
        private final StringWidget nameWidget;
        private final StringWidget detailWidget;
        private final Button toggleButton;

        public ModEntry(ModContainer mod) {
            this.mod = mod;
            this.modId = mod.getMetadata().getId();
            this.isSelf = ModHiderConfig.MOD_ID.equalsIgnoreCase(this.modId);

            String name = mod.getMetadata().getName();
            String version = mod.getMetadata().getVersion().getFriendlyString();
            String initials = this.isSelf ? "MH" : getInitials(name);
            this.avatarBgColor = this.isSelf ? 0xFF6C5CE7 : AVATAR_PALETTE[Math.abs(this.modId.hashCode()) % AVATAR_PALETTE.length];

            Font font = minecraft.font;

            // Avatar initial badge widget
            Component avatarText = Component.literal(initials).withStyle(ChatFormatting.BOLD, ChatFormatting.WHITE);
            int initW = font.width(avatarText);
            this.avatarWidget = new StringWidget(0, 0, initW, 26, avatarText, font);

            // Name widget
            Component nameComp = Component.literal(name).withStyle(
                    this.isSelf ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.WHITE,
                    ChatFormatting.BOLD
            );
            this.nameWidget = new StringWidget(0, 0, 160, 12, nameComp, font);

            // Detail widget (mod ID & version)
            Component detailComp = Component.literal(this.modId).withStyle(this.isSelf ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA)
                    .append(Component.literal("  v" + version).withStyle(ChatFormatting.DARK_GRAY));
            this.detailWidget = new StringWidget(0, 0, 160, 11, detailComp, font);

            // Toggle button
            this.toggleButton = Button.builder(CommonComponents.EMPTY, btn -> {
                if (!this.isSelf) {
                    ModHiderConfig.toggleMod(this.modId);
                    updateButtonState();
                    screen.onModToggled();
                }
            }).bounds(0, 0, 86, 20).build();

            updateButtonState();
        }

        private static String getInitials(String name) {
            if (name == null || name.isBlank()) return "?";
            String clean = name.trim();
            String[] parts = clean.split("\\s+");
            if (parts.length >= 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
                return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.ROOT);
            }
            if (clean.length() >= 2) {
                return clean.substring(0, 2).toUpperCase(Locale.ROOT);
            }
            return clean.toUpperCase(Locale.ROOT);
        }

        public void updateButtonState() {
            if (this.isSelf) {
                this.toggleButton.setMessage(Component.literal("🔒 LOCKED").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
                this.toggleButton.active = false;
            } else {
                boolean hidden = ModHiderConfig.isModHidden(this.modId);
                if (hidden) {
                    this.toggleButton.setMessage(Component.literal("✕ HIDDEN").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                } else {
                    this.toggleButton.setMessage(Component.literal("● VISIBLE").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
                }
                this.toggleButton.active = true;
            }
        }

        @Override
        public void extractContent(GuiGraphicsExtractor extractor, int mouseX, int mouseY, boolean isHovered, float tickDelta) {
            int x = getContentX();
            int y = getContentY();
            int right = getContentRight();
            int h = getContentHeight();

            boolean hidden = !this.isSelf && ModHiderConfig.isModHidden(this.modId);

            // Card styling
            int cardBg;
            int cardBorder;
            int accentStripe;

            if (this.isSelf) {
                cardBg = isHovered ? 0x446C5CE7 : 0x2A5041C8;
                cardBorder = isHovered ? 0xAA9B59B6 : 0x666C5CE7;
                accentStripe = 0xFFA29BFE;
            } else if (hidden) {
                cardBg = isHovered ? 0x3DE74C3C : 0x24C0392B;
                cardBorder = isHovered ? 0xAAFF7675 : 0x55E74C3C;
                accentStripe = 0xFFFF6B6B;
            } else {
                cardBg = isHovered ? 0x3834495E : 0x221E272E;
                cardBorder = isHovered ? 0x777F8C8D : 0x334B6584;
                accentStripe = 0xFF2ED573;
            }

            // Draw card background
            extractor.fill(x, y + 2, right, y + h - 2, cardBg);
            extractor.outline(x, y + 2, right - x, h - 4, cardBorder);

            // Left accent indicator stripe
            extractor.fill(x, y + 2, x + 3, y + h - 2, accentStripe);

            // Mod avatar square badge
            int avX = x + 8;
            int avY = y + 5;
            int avSize = 26;
            extractor.fill(avX, avY, avX + avSize, avY + avSize, this.avatarBgColor);
            extractor.outline(avX, avY, avSize, avSize, 0x44FFFFFF);

            // Avatar initial letters
            int initW = this.avatarWidget.getWidth();
            this.avatarWidget.setX(avX + (avSize - initW) / 2);
            this.avatarWidget.setY(avY);
            this.avatarWidget.extractRenderState(extractor, mouseX, mouseY, tickDelta);

            // Name and detail text widgets
            int textX = x + 40;
            int textMaxW = Math.max(10, right - 98 - textX);

            this.nameWidget.setX(textX);
            this.nameWidget.setY(y + 6);
            this.nameWidget.setWidth(textMaxW);
            this.nameWidget.setMaxWidth(textMaxW);
            this.nameWidget.extractRenderState(extractor, mouseX, mouseY, tickDelta);

            this.detailWidget.setX(textX);
            this.detailWidget.setY(y + 20);
            this.detailWidget.setWidth(textMaxW);
            this.detailWidget.setMaxWidth(textMaxW);
            this.detailWidget.extractRenderState(extractor, mouseX, mouseY, tickDelta);

            // Toggle button
            int btnW = 86;
            int btnH = 20;
            this.toggleButton.setX(right - btnW - 6);
            this.toggleButton.setY(y + (h - btnH) / 2);
            this.toggleButton.extractRenderState(extractor, mouseX, mouseY, tickDelta);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean isHovered) {
            if (super.mouseClicked(event, isHovered)) {
                return true;
            }
            if (event.button() == 0 && isHovered && !this.isSelf) {
                ModHiderConfig.toggleMod(this.modId);
                updateButtonState();
                screen.onModToggled();
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
            return false;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.avatarWidget, this.nameWidget, this.detailWidget, this.toggleButton);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.nameWidget, this.toggleButton);
        }

        @Override
        public void visitWidgets(Consumer<AbstractWidget> consumer) {
            consumer.accept(this.avatarWidget);
            consumer.accept(this.nameWidget);
            consumer.accept(this.detailWidget);
            consumer.accept(this.toggleButton);
        }

        public String getModId() {
            return modId;
        }
    }
}
