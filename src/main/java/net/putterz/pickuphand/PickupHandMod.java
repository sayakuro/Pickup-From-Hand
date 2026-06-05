package net.putterz.pickuphand;

import net.fabricmc.api.ModInitializer;
import net.putterz.pickuphand.server.SecureGiveHandler;

public class PickupHandMod implements ModInitializer {
	public static final String MOD_ID = "pickuphand";

	@Override
	public void onInitialize() {
		SecureGiveHandler.register();
	}
}
