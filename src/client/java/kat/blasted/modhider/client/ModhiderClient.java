package kat.blasted.modhider.client;

import kat.blasted.modhider.config.ModHiderConfig;
import net.fabricmc.api.ClientModInitializer;

public class ModhiderClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModHiderConfig.init();
    }
}
