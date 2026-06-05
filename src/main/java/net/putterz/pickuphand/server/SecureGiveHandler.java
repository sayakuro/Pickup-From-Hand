package net.putterz.pickuphand.server;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.putterz.pickuphand.network.PickupHandPackets;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class SecureGiveHandler {
	private static final double LOOK_DOT_THRESHOLD = 0.75D;
	private static final double OFFER_RANGE_SQUARED = 4.0D;
	private static final Map<UUID, Offer> ACTIVE_OFFERS = new HashMap<>();

	private SecureGiveHandler() {
	}

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(PickupHandPackets.GIVE_ITEM, (server, player, handler, buf, responseSender) -> server.execute(() -> activateOffer(player)));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> syncActiveOffers(handler.player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> removeDisconnectedOffers(handler.player, server));
		ServerTickEvents.END_SERVER_TICK.register(SecureGiveHandler::tickOffers);
		UseEntityCallback.EVENT.register(SecureGiveHandler::tryAcceptOffer);
	}

	private static void activateOffer(ServerPlayerEntity giver) {
		if (giver.isSpectator()) {
			clearOffer(giver);
			return;
		}

		Hand offerHand = getOfferHand(giver);
		if (offerHand == null) {
			clearOffer(giver);
			return;
		}

		ServerPlayerEntity receiver = findLookedAtPlayer(giver);
		if (receiver == null) {
			clearOffer(giver);
			return;
		}

		ACTIVE_OFFERS.put(giver.getUuid(), new Offer(receiver.getUuid(), offerHand, giver.getStackInHand(offerHand).copy()));
		syncOfferState(giver, true, offerHand);
		notifyReceiver(receiver);
	}

	private static ActionResult tryAcceptOffer(PlayerEntity receiver, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
		if (world.isClient || !(receiver instanceof ServerPlayerEntity serverReceiver) || !(entity instanceof ServerPlayerEntity giver)) {
			return ActionResult.PASS;
		}

		if (serverReceiver == giver || serverReceiver.isSpectator() || giver.isSpectator()) {
			return ActionResult.PASS;
		}

		Offer offer = ACTIVE_OFFERS.get(giver.getUuid());
		if (offer == null || !offer.receiverId().equals(serverReceiver.getUuid())) {
			return ActionResult.PASS;
		}

		if (!isOfferValid(giver, serverReceiver, offer)) {
			clearOffer(giver);
			return ActionResult.PASS;
		}

		if (!isLookingAt(serverReceiver, giver)) {
			return ActionResult.SUCCESS;
		}

		if (!serverReceiver.getStackInHand(hand).isEmpty()) {
			return ActionResult.PASS;
		}

		ItemStack receivedStack = giver.getStackInHand(offer.hand()).copy();
		serverReceiver.setStackInHand(hand, receivedStack);
		giver.setStackInHand(offer.hand(), ItemStack.EMPTY);
		clearOffer(giver);
		return ActionResult.SUCCESS;
	}

	private static Hand getOfferHand(ServerPlayerEntity giver) {
		if (!giver.getMainHandStack().isEmpty()) {
			return Hand.MAIN_HAND;
		}

		if (!giver.getOffHandStack().isEmpty()) {
			return Hand.OFF_HAND;
		}

		return null;
	}

	private static void notifyReceiver(ServerPlayerEntity receiver) {
		receiver.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.35F, 1.15F);
	}

	private static void tickOffers(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Offer>> iterator = ACTIVE_OFFERS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, Offer> entry = iterator.next();
			ServerPlayerEntity giver = server.getPlayerManager().getPlayer(entry.getKey());

			if (giver == null) {
				iterator.remove();
				continue;
			}

			ServerPlayerEntity receiver = server.getPlayerManager().getPlayer(entry.getValue().receiverId());
			if (!isOfferValid(giver, receiver, entry.getValue())) {
				iterator.remove();
				syncOfferState(giver, false, entry.getValue().hand());
			}
		}
	}

	private static void clearOffer(ServerPlayerEntity giver) {
		Offer removedOffer = ACTIVE_OFFERS.remove(giver.getUuid());
		if (removedOffer == null) {
			return;
		}

		syncOfferState(giver, false, removedOffer.hand());
	}

	private static void syncActiveOffers(ServerPlayerEntity player) {
		for (Map.Entry<UUID, Offer> entry : ACTIVE_OFFERS.entrySet()) {
			ServerPlayerEntity giver = player.server.getPlayerManager().getPlayer(entry.getKey());
			if (giver != null) {
				syncOfferState(player, giver.getUuid(), true, entry.getValue().hand());
			}
		}
	}

	private static void removeDisconnectedOffers(ServerPlayerEntity disconnectedPlayer, MinecraftServer server) {
		Offer ownOffer = ACTIVE_OFFERS.remove(disconnectedPlayer.getUuid());
		if (ownOffer != null) {
			syncOfferState(server, disconnectedPlayer.getUuid(), false, ownOffer.hand());
		}

		Iterator<Map.Entry<UUID, Offer>> iterator = ACTIVE_OFFERS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, Offer> entry = iterator.next();
			if (entry.getValue().receiverId().equals(disconnectedPlayer.getUuid())) {
				iterator.remove();
				syncOfferState(server, entry.getKey(), false, entry.getValue().hand());
			}
		}
	}

	private static void syncOfferState(ServerPlayerEntity giver, boolean active, Hand hand) {
		for (ServerPlayerEntity player : PlayerLookup.world(giver.getServerWorld())) {
			syncOfferState(player, giver.getUuid(), active, hand);
		}
	}

	private static void syncOfferState(MinecraftServer server, UUID giverId, boolean active, Hand hand) {
		for (ServerPlayerEntity player : PlayerLookup.all(server)) {
			syncOfferState(player, giverId, active, hand);
		}
	}

	private static void syncOfferState(ServerPlayerEntity player, UUID giverId, boolean active, Hand hand) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeUuid(giverId);
		buf.writeBoolean(active);
		buf.writeEnumConstant(hand);
		ServerPlayNetworking.send(player, PickupHandPackets.OFFER_STATE, buf);
	}

	private static ServerPlayerEntity findLookedAtPlayer(ServerPlayerEntity giver) {
		ServerPlayerEntity bestTarget = null;
		double bestDot = LOOK_DOT_THRESHOLD;

		for (ServerPlayerEntity candidate : giver.getServerWorld().getPlayers()) {
			if (candidate == giver || candidate.isSpectator() || !giver.canSee(candidate) || giver.squaredDistanceTo(candidate) > OFFER_RANGE_SQUARED) {
				continue;
			}

			double dot = getLookDot(giver, candidate);
			if (dot > bestDot) {
				bestDot = dot;
				bestTarget = candidate;
			}
		}

		return bestTarget;
	}

	private static boolean isOfferValid(ServerPlayerEntity giver, ServerPlayerEntity receiver, Offer offer) {
		return receiver != null
				&& giver.getWorld() == receiver.getWorld()
				&& !giver.isSpectator()
				&& !receiver.isSpectator()
				&& giver.canSee(receiver)
				&& giver.squaredDistanceTo(receiver) <= OFFER_RANGE_SQUARED
				&& isOfferedStackStillHeld(giver, offer)
				&& isLookingAt(giver, receiver);
	}

	private static boolean isOfferedStackStillHeld(ServerPlayerEntity giver, Offer offer) {
		ItemStack currentStack = giver.getStackInHand(offer.hand());
		return !currentStack.isEmpty() && ItemStack.canCombine(currentStack, offer.offeredStack());
	}

	private static boolean isLookingAt(ServerPlayerEntity receiver, ServerPlayerEntity giver) {
		return getLookDot(receiver, giver) >= LOOK_DOT_THRESHOLD;
	}

	private static double getLookDot(ServerPlayerEntity receiver, ServerPlayerEntity giver) {
		Vec3d lookDirection = receiver.getRotationVec(1.0F).normalize();
		Vec3d directionToGiver = giver.getEyePos().subtract(receiver.getEyePos()).normalize();
		return lookDirection.dotProduct(directionToGiver);
	}

	private record Offer(UUID receiverId, Hand hand, ItemStack offeredStack) {
	}
}
