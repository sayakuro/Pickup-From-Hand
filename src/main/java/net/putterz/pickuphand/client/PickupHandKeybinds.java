package net.putterz.pickuphand.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.putterz.pickuphand.PickupHandMod;
import net.putterz.pickuphand.network.PickupHandPackets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = PickupHandMod.MOD_ID, value = Dist.CLIENT)
final class PickupHandKeybinds {
	private static final String CATEGORY = "key.categories.pickuphand";
	private static final KeyMapping GIVE_ITEM_KEY = new KeyMapping(
			"key.pickuphand.give_item",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_G,
			CATEGORY
	);

	private PickupHandKeybinds() {
	}

	static void register(RegisterKeyMappingsEvent event) {
		event.register(GIVE_ITEM_KEY);
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}

		while (GIVE_ITEM_KEY.consumeClick()) {
			if (net.minecraft.client.Minecraft.getInstance().player != null && !net.minecraft.client.Minecraft.getInstance().player.isSpectator()) {
				PickupHandPackets.sendGiveItem();
			}
		}
	}
}
