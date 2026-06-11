package net.putterz.givewithhand.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.Hand;
import net.putterz.givewithhand.network.GiveWithHandPackets;

import java.util.UUID;

final class GiveWithHandClientNetworking {
	private GiveWithHandClientNetworking() {
	}

	static void register() {
		ClientPlayNetworking.registerGlobalReceiver(GiveWithHandPackets.OFFER_STATE, (client, handler, buf, responseSender) -> {
			UUID playerId = buf.readUuid();
			boolean active = buf.readBoolean();
			Hand hand = buf.readEnumConstant(Hand.class);

			client.execute(() -> {
				if (active) {
					GivingItemAnimation.setGiving(client, playerId, hand);
				} else {
					GivingItemAnimation.clearGiving(client, playerId);
				}
			});
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> GivingItemAnimation.clearAll(client));
	}
}
