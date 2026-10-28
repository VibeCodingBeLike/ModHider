package kat.blasted.modhider;

import kat.blasted.modhider.config.ModHiderConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ModHiderConfigTest {

    @BeforeEach
    public void setUp() {
        ModHiderConfig.showAll();
    }

    @Test
    public void testModHiderAlwaysHidden() {
        // By default, modhider itself MUST always be hidden
        assertTrue(ModHiderConfig.isModHidden("modhider"), "ModHider must be hidden by default");
        assertTrue(ModHiderConfig.isModHidden("ModHider"), "Should be case-insensitive");
        assertTrue(ModHiderConfig.isModHidden("MODHIDER"), "Should be case-insensitive");

        // Attempting to set modhider visible should have no effect
        ModHiderConfig.setModHidden("modhider", false);
        assertTrue(ModHiderConfig.isModHidden("modhider"), "ModHider must remain hidden even if setModHidden(false) is called");

        ModHiderConfig.toggleMod("modhider");
        assertTrue(ModHiderConfig.isModHidden("modhider"), "ModHider must remain hidden after toggle");

        ModHiderConfig.showAll();
        assertTrue(ModHiderConfig.isModHidden("modhider"), "ModHider must remain hidden after showAll");
    }

    @Test
    public void testCoreComponentsCannotBeHidden() {
        // Core components must never be hidden
        assertTrue(ModHiderConfig.isCoreComponent("minecraft"));
        assertTrue(ModHiderConfig.isCoreComponent("java"));
        assertTrue(ModHiderConfig.isCoreComponent("fabricloader"));
        assertTrue(ModHiderConfig.isCoreComponent("fabric"));
        assertTrue(ModHiderConfig.isCoreComponent("fabric-api"));
        assertTrue(ModHiderConfig.isCoreComponent("fabric-networking-api-v1"));
        assertFalse(ModHiderConfig.isCoreComponent("camerautils"));
        assertFalse(ModHiderConfig.isCoreComponent("autoclicker"));

        // Attempting to set core component hidden should be ignored
        ModHiderConfig.setModHidden("fabricloader", true);
        assertFalse(ModHiderConfig.isModHidden("fabricloader"));

        ModHiderConfig.setModHidden("fabric-api", true);
        assertFalse(ModHiderConfig.isModHidden("fabric-api"));

        ModHiderConfig.setModHidden("fabric-networking-api-v1", true);
        assertFalse(ModHiderConfig.isModHidden("fabric-networking-api-v1"));

        ModHiderConfig.hideAll(List.of("fabricloader", "fabric-api", "camerautils"));
        assertFalse(ModHiderConfig.isModHidden("fabricloader"));
        assertFalse(ModHiderConfig.isModHidden("fabric-api"));
        assertTrue(ModHiderConfig.isModHidden("camerautils"));
    }

    @Test
    public void testDefaultOtherModsShown() {
        // By default, all other mods should be shown (not hidden)
        assertFalse(ModHiderConfig.isModHidden("appleskin"));
        assertFalse(ModHiderConfig.isModHidden("modmenu"));
        assertFalse(ModHiderConfig.isModHidden("custom_cheat_mod"));
    }

    @Test
    public void testToggleAndSetHidden() {
        assertFalse(ModHiderConfig.isModHidden("testmod"));

        ModHiderConfig.setModHidden("testmod", true);
        assertTrue(ModHiderConfig.isModHidden("testmod"));
        assertTrue(ModHiderConfig.isModHidden("TestMod"), "Case insensitivity check");

        ModHiderConfig.toggleMod("testmod");
        assertFalse(ModHiderConfig.isModHidden("testmod"));

        ModHiderConfig.toggleMod("testmod");
        assertTrue(ModHiderConfig.isModHidden("testmod"));

        // Normalized matching with hyphens/underscores
        ModHiderConfig.setModHidden("camerautils", true);
        assertTrue(ModHiderConfig.isModHidden("camerautils"));
        assertTrue(ModHiderConfig.isModHidden("camera-utils"));
        assertTrue(ModHiderConfig.isModHidden("camera_utils"));
    }

    @Test
    public void testHideAllAndShowAll() {
        List<String> mods = List.of("minecraft", "java", "modhider", "fabric-api", "xaerominimap", "freecam");
        ModHiderConfig.hideAll(mods);

        // core components are protected
        assertFalse(ModHiderConfig.isModHidden("minecraft"));
        assertFalse(ModHiderConfig.isModHidden("java"));
        assertFalse(ModHiderConfig.isModHidden("fabric-api"));

        // other mods are now hidden
        assertTrue(ModHiderConfig.isModHidden("xaerominimap"));
        assertTrue(ModHiderConfig.isModHidden("freecam"));
        assertTrue(ModHiderConfig.isModHidden("modhider"));

        ModHiderConfig.showAll();
        assertFalse(ModHiderConfig.isModHidden("xaerominimap"));
        assertFalse(ModHiderConfig.isModHidden("freecam"));
        assertTrue(ModHiderConfig.isModHidden("modhider"));
    }

    @Test
    public void testTranslationKeyAndAnvilFilter() {
        ModHiderConfig.setModHidden("camerautils", true);
        ModHiderConfig.setModHidden("autoclicker", true);

        // Translation key checks
        assertTrue(kat.blasted.modhider.filter.ModHiderFilter.isTranslationKeyHidden("key.category.autoclicker.keybinding-title"));
        assertTrue(kat.blasted.modhider.filter.ModHiderFilter.isTranslationKeyHidden("key.category.camerautils.camerautils"));
        assertTrue(kat.blasted.modhider.filter.ModHiderFilter.isTranslationKeyHidden("autoclicker-fabric.gui.speed"));
        assertFalse(kat.blasted.modhider.filter.ModHiderFilter.isTranslationKeyHidden("gui.cancel"));
        assertFalse(kat.blasted.modhider.filter.ModHiderFilter.isTranslationKeyHidden("fabric.menu.title"));

        // Anvil item rename probes
        assertTrue(kat.blasted.modhider.filter.ModHiderFilter.isItemRenameHidden("AutoClicker"));
        assertTrue(kat.blasted.modhider.filter.ModHiderFilter.isItemRenameHidden("Auto Clicker"));
        assertTrue(kat.blasted.modhider.filter.ModHiderFilter.isItemRenameHidden("CameraUtils"));
        assertTrue(kat.blasted.modhider.filter.ModHiderFilter.isItemRenameHidden("Camera Utils"));
        assertFalse(kat.blasted.modhider.filter.ModHiderFilter.isItemRenameHidden("Diamond Sword"));
        assertFalse(kat.blasted.modhider.filter.ModHiderFilter.isItemRenameHidden("Dirt"));
    }

    @Test
    public void testDecodingAndItemContextState() {
        assertFalse(kat.blasted.modhider.filter.ModHiderFilter.isDecodingServerPacket());
        assertFalse(kat.blasted.modhider.filter.ModHiderFilter.isInItemContext());

        kat.blasted.modhider.filter.ModHiderFilter.setIsDecodingServerPacket(true);
        assertTrue(kat.blasted.modhider.filter.ModHiderFilter.isDecodingServerPacket());
        kat.blasted.modhider.filter.ModHiderFilter.setIsDecodingServerPacket(false);
        assertFalse(kat.blasted.modhider.filter.ModHiderFilter.isDecodingServerPacket());

        kat.blasted.modhider.filter.ModHiderFilter.setIsInItemContext(true);
        assertTrue(kat.blasted.modhider.filter.ModHiderFilter.isInItemContext());
        kat.blasted.modhider.filter.ModHiderFilter.setIsInItemContext(false);
        assertFalse(kat.blasted.modhider.filter.ModHiderFilter.isInItemContext());
    }
}
