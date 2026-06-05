package net.putterz.pickuphand.client;

import net.fabricmc.api.ClientModInitializer;

public class PickupHandClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		PickupHandKeybinds.register();
		PickupHandClientNetworking.register();
	}
}
