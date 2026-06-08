package net.putterz.pickuphand.client;

import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.putterz.pickuphand.PickupHandMod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GivingItemAnimation {
	private static final ResourceLocation RIGHT_HAND_ANIMATION = new ResourceLocation(PickupHandMod.MOD_ID, "give_item_right");
	private static final ResourceLocation LEFT_HAND_ANIMATION = new ResourceLocation(PickupHandMod.MOD_ID, "give_item_left");
	private static final Map<UUID, InteractionHand> GIVING_PLAYERS = new HashMap<>();

	private GivingItemAnimation() {
	}

	public static void setGiving(Minecraft client, UUID playerId, InteractionHand hand) {
		GIVING_PLAYERS.put(playerId, hand);
		applyToPlayer(client.level, playerId);
	}

	public static void clearGiving(Minecraft client, UUID playerId) {
		GIVING_PLAYERS.remove(playerId);
		applyToPlayer(client.level, playerId);
	}

	public static void applyToPlayer(AbstractClientPlayer player) {
		applyToPlayer(player, GIVING_PLAYERS.get(player.getUUID()));
	}

	public static void clearAll(Minecraft client) {
		GIVING_PLAYERS.clear();
		if (client.level != null) {
			for (AbstractClientPlayer player : client.level.players()) {
				stopAnimation(player);
			}
		}
	}

	private static void applyToPlayer(ClientLevel world, UUID playerId) {
		if (world == null) {
			return;
		}

		for (AbstractClientPlayer player : world.players()) {
			if (player.getUUID().equals(playerId)) {
				applyToPlayer(player);
				return;
			}
		}
	}

	private static void applyToPlayer(AbstractClientPlayer player, InteractionHand hand) {
		if (hand == null) {
			stopAnimation(player);
			return;
		}

		HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
		KeyframeAnimation animation = PlayerAnimationRegistry.getAnimation(arm == HumanoidArm.RIGHT ? RIGHT_HAND_ANIMATION : LEFT_HAND_ANIMATION);
		if (animation == null) {
			stopAnimation(player);
			return;
		}

		FirstPersonConfiguration firstPerson = new FirstPersonConfiguration()
				.setShowRightArm(arm == HumanoidArm.RIGHT)
				.setShowLeftArm(arm == HumanoidArm.LEFT)
				.setShowRightItem(arm == HumanoidArm.RIGHT)
				.setShowLeftItem(arm == HumanoidArm.LEFT);

		KeyframeAnimationPlayer animationPlayer = new KeyframeAnimationPlayer(animation)
				.setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL)
				.setFirstPersonConfiguration(firstPerson);

		animationLayer(player).setAnimation(animationPlayer);
	}

	private static void stopAnimation(AbstractClientPlayer player) {
		animationLayer(player).setAnimation(null);
	}

	private static ModifierLayer<IAnimation> animationLayer(AbstractClientPlayer player) {
		return ((PickupHandAnimatedPlayer) player).pickuphand$getAnimationLayer();
	}
}
