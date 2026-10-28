package kat.blasted.modhider.filter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kat.blasted.modhider.config.ModHiderConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.KnownPack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public final class ModHiderFilter {
    public static final Logger LOGGER = LoggerFactory.getLogger("ModHider");
    private static final Set<String> DYNAMIC_HIDDEN_KEYS = Collections.synchronizedSet(new HashSet<>());
    private static final ThreadLocal<Boolean> IS_DECODING_SERVER_PACKET = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> IS_IN_ITEM_CONTEXT = ThreadLocal.withInitial(() -> false);

    private ModHiderFilter() {
    }

    public static boolean isDecodingServerPacket() {
        return IS_DECODING_SERVER_PACKET.get();
    }

    public static void setIsDecodingServerPacket(boolean value) {
        IS_DECODING_SERVER_PACKET.set(value);
    }

    public static boolean isInItemContext() {
        return IS_IN_ITEM_CONTEXT.get();
    }

    public static void setIsInItemContext(boolean value) {
        IS_IN_ITEM_CONTEXT.set(value);
    }

    public static Component markComponent(Component component) {
        if (component == null) {
            return null;
        }
        if (component.getContents() instanceof ComponentMarker marker) {
            marker.modhider$mark();
        }
        if (component.getContents() instanceof TranslatableContents translatable) {
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Component childComp) {
                    markComponent(childComp);
                }
            }
        }
        for (Component sibling : component.getSiblings()) {
            markComponent(sibling);
        }
        return component;
    }

    public static void reloadHiddenModKeys() {
        DYNAMIC_HIDDEN_KEYS.clear();
        try {
            for (String hiddenModId : ModHiderConfig.getHiddenMods()) {
                if (ModHiderConfig.isCoreComponent(hiddenModId)) {
                    continue;
                }
                FabricLoader.getInstance().getModContainer(hiddenModId).ifPresent(ModHiderFilter::collectKeysFromMod);
            }
            LOGGER.info("[ModHider] Loaded {} hidden translation key(s) dynamically", DYNAMIC_HIDDEN_KEYS.size());
        } catch (Throwable t) {
            // FabricLoader might not be available in unit tests
        }
    }

    private static void collectKeysFromMod(ModContainer container) {
        try {
            Optional<Path> assetsOpt = container.findPath("assets");
            if (assetsOpt.isEmpty()) {
                return;
            }
            Path assetsPath = assetsOpt.get();
            try (Stream<Path> walk = Files.walk(assetsPath)) {
                walk.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".json") && p.toString().contains("lang"))
                    .forEach(jsonPath -> {
                        try (Reader reader = Files.newBufferedReader(jsonPath)) {
                            JsonElement element = JsonParser.parseReader(reader);
                            if (element != null && element.isJsonObject()) {
                                JsonObject obj = element.getAsJsonObject();
                                for (String key : obj.keySet()) {
                                    DYNAMIC_HIDDEN_KEYS.add(key.toLowerCase(Locale.ROOT));
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    });
            }
        } catch (Throwable t) {
            LOGGER.warn("[ModHider] Failed scanning lang files for mod {}: {}", container.getMetadata().getId(), t.getMessage());
        }
    }

    public static boolean isChannelHidden(Identifier id) {
        if (id == null) {
            return false;
        }
        String ns = id.getNamespace().toLowerCase(Locale.ROOT);
        String path = id.getPath().toLowerCase(Locale.ROOT);

        if (ModHiderConfig.isCoreComponent(ns)) {
            return false;
        }

        if (ns.contains("modhider") || path.contains("modhider")) {
            return true;
        }

        if (ModHiderConfig.isModHidden(ns) || ModHiderConfig.isModHidden(path)) {
            return true;
        }

        String[] pathParts = path.split("[/:_\\\\-]");
        for (String part : pathParts) {
            if (!part.isEmpty() && !ModHiderConfig.isCoreComponent(part) && ModHiderConfig.isModHidden(part)) {
                return true;
            }
        }

        for (String hidden : ModHiderConfig.getHiddenMods()) {
            if (ModHiderConfig.isCoreComponent(hidden)) {
                continue;
            }
            String lowerHidden = hidden.toLowerCase(Locale.ROOT);
            if (ns.equals(lowerHidden) || path.equals(lowerHidden)) {
                return true;
            }
            if (path.startsWith(lowerHidden + "/") || path.endsWith("/" + lowerHidden) || path.contains("/" + lowerHidden + "/")) {
                return true;
            }
        }

        return false;
    }

    public static boolean shouldHideKnownPack(KnownPack pack) {
        if (pack == null) {
            return false;
        }
        if (pack.isVanilla()) {
            return false;
        }

        String ns = pack.namespace().toLowerCase(Locale.ROOT);
        String id = pack.id().toLowerCase(Locale.ROOT);

        // Core fabric packs like fabricloader or fabric-api are never hidden
        if (ModHiderConfig.isCoreComponent(id)) {
            return false;
        }

        // ModHider itself must NEVER be leaked
        if (ns.contains("modhider") || id.contains("modhider")) {
            return true;
        }

        if (ModHiderConfig.isModHidden(id)) {
            return true;
        }

        String[] idParts = id.split("[/:_\\\\-]");
        for (String part : idParts) {
            if (!part.isEmpty() && !ModHiderConfig.isCoreComponent(part) && ModHiderConfig.isModHidden(part)) {
                return true;
            }
        }

        for (String hidden : ModHiderConfig.getHiddenMods()) {
            if (ModHiderConfig.isCoreComponent(hidden)) {
                continue;
            }
            String lowerHidden = hidden.toLowerCase(Locale.ROOT);
            if (id.equals(lowerHidden)) {
                return true;
            }
            if (id.startsWith(lowerHidden + "/") || id.endsWith("/" + lowerHidden)
                    || id.contains("/" + lowerHidden + "/") || id.contains(":" + lowerHidden)) {
                return true;
            }
            String strippedId = id.replace("-", "").replace("_", "");
            String strippedHidden = lowerHidden.replace("-", "").replace("_", "");
            if (!strippedHidden.isEmpty() && strippedId.equals(strippedHidden)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isTranslationKeyHidden(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        String lowerKey = key.toLowerCase(Locale.ROOT);

        if (lowerKey.contains("modhider")) {
            return true;
        }

        if (DYNAMIC_HIDDEN_KEYS.contains(lowerKey)) {
            return true;
        }

        String[] parts = lowerKey.split("[._:\\\\/-]");
        for (String part : parts) {
            if (!part.isEmpty() && !ModHiderConfig.isCoreComponent(part) && ModHiderConfig.isModHidden(part)) {
                return true;
            }
        }

        for (String hidden : ModHiderConfig.getHiddenMods()) {
            if (ModHiderConfig.isCoreComponent(hidden)) {
                continue;
            }
            String lowerHidden = hidden.toLowerCase(Locale.ROOT);
            if (lowerKey.contains(lowerHidden)) {
                return true;
            }
            String strippedKey = lowerKey.replace("-", "").replace("_", "");
            String strippedHidden = lowerHidden.replace("-", "").replace("_", "");
            if (!strippedHidden.isEmpty() && strippedKey.contains(strippedHidden)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isItemRenameHidden(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("modhider")) {
            return true;
        }

        String strippedText = lower.replaceAll("[^a-z0-9]", "");
        if (strippedText.isEmpty()) {
            return false;
        }

        for (String hidden : ModHiderConfig.getHiddenMods()) {
            if (ModHiderConfig.isCoreComponent(hidden)) {
                continue;
            }
            String lowerHidden = hidden.toLowerCase(Locale.ROOT);
            String strippedHidden = lowerHidden.replaceAll("[^a-z0-9]", "");
            if (!strippedHidden.isEmpty() && strippedText.contains(strippedHidden)) {
                return true;
            }
        }

        return false;
    }
}
