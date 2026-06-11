package net.putterz.givewithhand;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import net.putterz.givewithhand.server.GiveOfferManager;

public class GiveWithHandMod implements ModInitializer {
	public static final String MOD_ID = "givewithhand";

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		GiveOfferManager.register();
	}
}
