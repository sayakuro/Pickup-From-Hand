package net.putterz.pickuphand.client;

import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.putterz.pickuphand.PickupHandMod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GivingItemAnimation {
	private static final Identifier RIGHT_HAND_ANIMATION = new Identifier(PickupHandMod.MOD_ID, "give_item_right");
	private static final Identifier LEFT_HAND_ANIMATION = new Identifier(PickupHandMod.MOD_ID, "give_item_left");
	private static final Map<UUID, Hand> GIVING_PLAYERS = new HashMap<>();

	private GivingItemAnimation() {
	}

	public static void setGiving(MinecraftClient client, UUID playerId, Hand hand) {
		GIVING_PLAYERS.put(playerId, hand);
		applyToPlayer(client.world, playerId);
	}

	public static void clearGiving(MinecraftClient client, UUID playerId) {
		GIVING_PLAYERS.remove(playerId);
		applyToPlayer(client.world, playerId);
	}

	public static void applyToPlayer(AbstractClientPlayerEntity player) {
		applyToPlayer(player, GIVING_PLAYERS.get(player.getUuid()));
	}

	public static void clearAll(MinecraftClient client) {
		GIVING_PLAYERS.clear();
		if (client.world != null) {
			for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
				stopAnimation(player);
			}
		}
	}

	private static void applyToPlayer(ClientWorld world, UUID playerId) {
		if (world == null) {
			return;
		}

		for (AbstractClientPlayerEntity player : world.getPlayers()) {
			if (player.getUuid().equals(playerId)) {
				applyToPlayer(player);
				return;
			}
		}
	}

	private static void applyToPlayer(AbstractClientPlayerEntity player, Hand hand) {
		if (hand == null) {
			stopAnimation(player);
			return;
		}

		Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
		KeyframeAnimation animation = PlayerAnimationRegistry.getAnimation(arm == Arm.RIGHT ? RIGHT_HAND_ANIMATION : LEFT_HAND_ANIMATION);
		if (animation == null) {
			stopAnimation(player);
			return;
		}

		FirstPersonConfiguration firstPerson = new FirstPersonConfiguration()
				.setShowRightArm(arm == Arm.RIGHT)
				.setShowLeftArm(arm == Arm.LEFT)
				.setShowRightItem(arm == Arm.RIGHT)
				.setShowLeftItem(arm == Arm.LEFT);

		KeyframeAnimationPlayer animationPlayer = new KeyframeAnimationPlayer(animation)
				.setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL)
				.setFirstPersonConfiguration(firstPerson);

		animationLayer(player).setAnimation(animationPlayer);
	}

	private static void stopAnimation(AbstractClientPlayerEntity player) {
		animationLayer(player).setAnimation(null);
	}

	private static ModifierLayer<IAnimation> animationLayer(AbstractClientPlayerEntity player) {
		return ((PickupHandAnimatedPlayer) player).pickuphand$getAnimationLayer();
	}
}
