package net.putterz.givewithhand.client;

import net.fabricmc.api.ClientModInitializer;

public class GiveWithHandClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		GiveWithHandKeybinds.register();
		GiveWithHandClientNetworking.register();
	}
}
