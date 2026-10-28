package kat.blasted.modhider.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class ModHiderConfig {
    public static final String MOD_ID = "modhider";
    private static final Logger LOGGER = LoggerFactory.getLogger("ModHider");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Set of lowercase mod IDs that are configured as hidden
    private static final Set<String> hiddenMods = Collections.synchronizedSet(new HashSet<>());

    private ModHiderConfig() {
    }

    private static Path getConfigPath() {
        try {
            return FabricLoader.getInstance().getConfigDir().resolve("modhider.json");
        } catch (Throwable t) {
            return Path.of("build", "test-config", "modhider.json");
        }
    }

    public static void init() {
        load();
    }

    public static boolean isCoreComponent(String modId) {
        if (modId == null) {
            return false;
        }
        String id = modId.toLowerCase(Locale.ROOT).trim();
        return id.equals("minecraft")
                || id.equals("java")
                || id.equals("fabricloader")
                || id.equals("fabric")
                || id.startsWith("fabric-")
                || id.startsWith("fabric_");
    }

    public static boolean isModHidden(String modId) {
        if (modId == null) {
            return false;
        }
        // ModHider itself must NEVER be shown to servers
        if (MOD_ID.equalsIgnoreCase(modId)) {
            return true;
        }
        // Core Fabric / Minecraft components must never be hidden
        if (isCoreComponent(modId)) {
            return false;
        }
        String normalized = modId.toLowerCase(Locale.ROOT);
        if (hiddenMods.contains(normalized)) {
            return true;
        }
        // Also match normalized without hyphens or underscores
        String stripped = normalized.replace("-", "").replace("_", "");
        if (!stripped.isEmpty()) {
            for (String hidden : hiddenMods) {
                if (hidden.replace("-", "").replace("_", "").equals(stripped)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void setModHidden(String modId, boolean hidden) {
        if (modId == null || MOD_ID.equalsIgnoreCase(modId) || isCoreComponent(modId)) {
            return;
        }
        String normalized = modId.toLowerCase(Locale.ROOT);
        if (hidden) {
            hiddenMods.add(normalized);
        } else {
            hiddenMods.remove(normalized);
        }
        save();
    }

    public static void toggleMod(String modId) {
        if (modId == null || MOD_ID.equalsIgnoreCase(modId) || isCoreComponent(modId)) {
            return;
        }
        setModHidden(modId, !isModHidden(modId));
    }

    public static void showAll() {
        hiddenMods.clear();
        save();
    }

    public static void hideAll(Collection<String> modIds) {
        if (modIds != null) {
            for (String id : modIds) {
                if (id != null && !isCoreComponent(id) && !id.equalsIgnoreCase(MOD_ID)) {
                    hiddenMods.add(id.toLowerCase(Locale.ROOT));
                }
            }
        }
        save();
    }

    public static Set<String> getHiddenMods() {
        Set<String> copy = new HashSet<>(hiddenMods);
        copy.add(MOD_ID);
        return Collections.unmodifiableSet(copy);
    }

    public static int getHiddenCount() {
        // Includes modhider itself
        return hiddenMods.contains(MOD_ID) ? hiddenMods.size() : hiddenMods.size() + 1;
    }

    public static synchronized void load() {
        hiddenMods.clear();
        Path path = getConfigPath();
        if (!Files.exists(path)) {
            save();
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element != null && element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();
                if (obj.has("hiddenMods") && obj.get("hiddenMods").isJsonArray()) {
                    JsonArray array = obj.getAsJsonArray("hiddenMods");
                    for (JsonElement item : array) {
                        if (item.isJsonPrimitive() && item.getAsJsonPrimitive().isString()) {
                            String id = item.getAsString().trim().toLowerCase(Locale.ROOT);
                            if (!id.isEmpty() && !id.equalsIgnoreCase(MOD_ID) && !isCoreComponent(id)) {
                                hiddenMods.add(id);
                            }
                        }
                    }
                }
            }
            LOGGER.info("[ModHider] Loaded configuration with {} hidden mod(s)", hiddenMods.size());
        } catch (Exception e) {
            LOGGER.error("[ModHider] Failed to load configuration file", e);
        }
        kat.blasted.modhider.filter.ModHiderFilter.reloadHiddenModKeys();
    }

    public static synchronized void save() {
        try {
            Path path = getConfigPath();
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            JsonObject root = new JsonObject();
            JsonArray array = new JsonArray();
            for (String id : hiddenMods) {
                array.add(id);
            }
            root.add("hiddenMods", array);

            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                GSON.toJson(root, writer);
            }
        } catch (Exception e) {
            LOGGER.error("[ModHider] Failed to save configuration file", e);
        }
        kat.blasted.modhider.filter.ModHiderFilter.reloadHiddenModKeys();
    }
}
