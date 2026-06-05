package net.putterz.pickuphand.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import net.putterz.pickuphand.network.PickupHandPackets;

final class PickupHandKeybinds {
	private static final String CATEGORY = "key.categories.pickuphand";
	private static final KeyBinding GIVE_ITEM_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.pickuphand.give_item",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_G,
			CATEGORY
	));

	private PickupHandKeybinds() {
	}

	static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (GIVE_ITEM_KEY.wasPressed()) {
				if (client.player != null && !client.player.isSpectator()) {
					ClientPlayNetworking.send(PickupHandPackets.GIVE_ITEM, PacketByteBufs.empty());
				}
			}
		});
	}
}
