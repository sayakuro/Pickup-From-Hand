package net.putterz.givewithhand.api;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.putterz.givewithhand.GiveWithHandMod;
import net.putterz.givewithhand.server.GiveOfferManager;

import java.util.Optional;

public final class GiveWithHandApi {
	private GiveWithHandApi() {
	}

	public static Identifier id(String path) {
		return GiveWithHandMod.id(path);
	}

	public static boolean offerItemToLookedAtPlayer(ServerPlayerEntity giver) {
		return GiveOfferManager.offerItemToLookedAtPlayer(giver);
	}

	public static void clearOffer(ServerPlayerEntity giver) {
		GiveOfferManager.clearOffer(giver);
	}

	public static boolean hasActiveOffer(ServerPlayerEntity giver) {
		return GiveOfferManager.hasActiveOffer(giver);
	}

	public static Optional<GiveOffer> getActiveOffer(ServerPlayerEntity giver) {
		return GiveOfferManager.getActiveOffer(giver);
	}
}
