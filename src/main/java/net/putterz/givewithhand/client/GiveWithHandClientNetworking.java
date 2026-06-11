package net.putterz.givewithhand.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.putterz.givewithhand.network.GiveWithHandPackets;

final class GiveWithHandClientNetworking {
	private GiveWithHandClientNetworking() {
	}

	static void register() {
		GiveWithHandPackets.registerPayloads();
		ClientPlayNetworking.registerGlobalReceiver(GiveWithHandPackets.OfferStatePayload.ID, (payload, context) -> {
		});
	}
}
