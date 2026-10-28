package kat.blasted.modhider.client.gui;

import kat.blasted.modhider.config.ModHiderConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ModHiderConfigScreen extends Screen {
    private final Screen parent;
    private ModHiderModListWidget list;
    private EditBox searchBox;
    private int totalModsCount = 0;

    public ModHiderConfigScreen(Screen parent) {
        super(Component.literal("Mod Hider"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        Collection<ModContainer> allMods = FabricLoader.getInstance().getAllMods();
        List<ModContainer> userMods = new ArrayList<>();
        for (ModContainer mod : allMods) {
            if (!ModHiderConfig.isCoreComponent(mod.getMetadata().getId())) {
                userMods.add(mod);
            }
        }
        this.totalModsCount = userMods.size();

        int searchWidth = Math.min(300, this.width - 40);
        int searchX = (this.width - searchWidth) / 2;

        this.searchBox = new EditBox(this.font, searchX, 42, searchWidth, 18, Component.literal("Search mods..."));
        this.searchBox.setHint(Component.literal("Search mods by name or ID...").withStyle(ChatFormatting.DARK_GRAY));
        this.searchBox.setResponder(query -> {
            if (this.list != null) {
                this.list.filter(query);
            }
        });
        this.addRenderableWidget(this.searchBox);

        int listTop = 66;
        int listBottomMargin = 36;
        int listHeight = this.height - listTop - listBottomMargin;

        this.list = new ModHiderModListWidget(this.minecraft, this.width, listHeight, listTop, 36, this);
        this.list.setMods(userMods);
        this.addRenderableWidget(this.list);

        int buttonY = this.height - 28;
        int center = this.width / 2;

        // "Hide All" button
        this.addRenderableWidget(Button.builder(Component.literal("Hide All"), btn -> {
            List<String> ids = new ArrayList<>();
            for (ModContainer mod : this.list.getAllMods()) {
                ids.add(mod.getMetadata().getId());
            }
            ModHiderConfig.hideAll(ids);
            this.list.refreshAll();
        }).bounds(center - 154, buttonY, 74, 20).build());

        // "Show All" button
        this.addRenderableWidget(Button.builder(Component.literal("Show All"), btn -> {
            ModHiderConfig.showAll();
            this.list.refreshAll();
        }).bounds(center - 76, buttonY, 74, 20).build());

        // "Done" button
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, btn -> onClose())
                .bounds(center + 2, buttonY, 150, 20)
                .build());
    }

    public void onModToggled() {
        // Callback when any mod toggle changes state
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float tickDelta) {
        // 1. Dark sleek obsidian gradient for entire screen background
        extractor.fillGradient(0, 0, this.width, this.height, 0xFF0B0D13, 0xFF141722);

        // 2. Translucent header bar background
        extractor.fill(0, 0, this.width, 64, 0xEE0E1017);
        extractor.fill(0, 63, this.width, 64, 0x336C5CE7);

        // 3. Translucent footer bar background
        extractor.fill(0, this.height - 34, this.width, this.height, 0xEE0E1017);
        extractor.fill(0, this.height - 34, this.width, this.height - 33, 0x22FFFFFF);

        // 4. Render list and widgets
        super.extractRenderState(extractor, mouseX, mouseY, tickDelta);

        // 5. Header labels
        Component titleBadge = Component.literal("MOD HIDER").withStyle(ChatFormatting.BOLD, ChatFormatting.WHITE);
        extractor.centeredText(this.font, titleBadge, this.width / 2, 7, 0xFFFFFFFF);

        int hiddenCount = ModHiderConfig.getHiddenCount();
        Component stats = Component.literal(hiddenCount + " mod" + (hiddenCount == 1 ? "" : "s") + " hidden")
                .withStyle(hiddenCount > 0 ? ChatFormatting.RED : ChatFormatting.GREEN)
                .append(Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(this.totalModsCount + " installed").withStyle(ChatFormatting.GRAY));
        extractor.centeredText(this.font, stats, this.width / 2, 21, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null && this.minecraft.gui != null) {
            this.minecraft.gui.setScreen(this.parent);
        }
    }
}
