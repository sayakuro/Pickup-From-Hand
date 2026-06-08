package net.putterz.pickuphand;

import net.putterz.pickuphand.network.PickupHandPackets;
import net.putterz.pickuphand.server.SecureGiveHandler;
import net.minecraftforge.fml.common.Mod;

@Mod(PickupHandMod.MOD_ID)
public class PickupHandMod {
	public static final String MOD_ID = "pickuphand";

	public PickupHandMod() {
		PickupHandPackets.register();
		SecureGiveHandler.register();
	}
}
