package net.putterz.pickuphand.client;

import net.minecraft.client.Minecraft;
import net.putterz.pickuphand.PickupHandMod;
import net.putterz.pickuphand.network.PickupHandPackets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PickupHandMod.MOD_ID, value = Dist.CLIENT)
public final class PickupHandClientNetworking {
	private PickupHandClientNetworking() {
	}

	public static void handleOfferState(PickupHandPackets.OfferStateMessage message) {
		Minecraft client = Minecraft.getInstance();
		if (message.active()) {
			GivingItemAnimation.setGiving(client, message.giverId(), message.hand());
		} else {
			GivingItemAnimation.clearGiving(client, message.giverId());
		}
	}

	@SubscribeEvent
	public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
		GivingItemAnimation.clearAll(Minecraft.getInstance());
	}
}
