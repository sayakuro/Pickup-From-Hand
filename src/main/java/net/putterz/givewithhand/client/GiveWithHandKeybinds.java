package net.putterz.givewithhand.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import net.putterz.givewithhand.network.GiveWithHandPackets;

final class GiveWithHandKeybinds {
	private static final KeyBinding GIVE_ITEM_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.givewithhand.give_item",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_G,
			KeyBinding.Category.GAMEPLAY
	));

	private GiveWithHandKeybinds() {
	}

	static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (GIVE_ITEM_KEY.wasPressed()) {
				if (client.player != null && !client.player.isSpectator()) {
					ClientPlayNetworking.send(new GiveWithHandPackets.GiveItemPayload());
				}
			}
		});
	}
}
