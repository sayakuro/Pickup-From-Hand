package net.putterz.pickuphand.network;

import net.minecraft.util.Identifier;
import net.putterz.pickuphand.PickupHandMod;

public final class PickupHandPackets {
	public static final Identifier GIVE_ITEM = new Identifier(PickupHandMod.MOD_ID, "give_item");
	public static final Identifier OFFER_STATE = new Identifier(PickupHandMod.MOD_ID, "offer_state");

	private PickupHandPackets() {
	}
}
