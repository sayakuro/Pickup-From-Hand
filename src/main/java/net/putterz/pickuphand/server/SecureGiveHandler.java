package net.putterz.pickuphand.server;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.putterz.pickuphand.network.PickupHandPackets;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

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
		MinecraftForge.EVENT_BUS.register(SecureGiveHandler.class);
	}

	public static void activateOffer(ServerPlayer giver) {
		if (giver.isSpectator()) {
			clearOffer(giver);
			return;
		}

		InteractionHand offerHand = getOfferHand(giver);
		if (offerHand == null) {
			clearOffer(giver);
			return;
		}

		ServerPlayer receiver = findLookedAtPlayer(giver);
		if (receiver == null) {
			clearOffer(giver);
			return;
		}

		ACTIVE_OFFERS.put(giver.getUUID(), new Offer(receiver.getUUID(), offerHand, giver.getItemInHand(offerHand).copy()));
		syncOfferState(giver, true, offerHand);
		notifyReceiver(receiver);
	}

	@SubscribeEvent
	public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		InteractionResult result = tryAcceptOffer(event.getEntity(), event.getLevel(), event.getHand(), event.getTarget());
		if (result.consumesAction()) {
			event.setCancellationResult(result);
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			syncActiveOffers(player);
		}
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			removeDisconnectedOffers(player, player.server);
		}
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			tickOffers(event.getServer());
		}
	}

	private static InteractionResult tryAcceptOffer(Player receiver, Level level, InteractionHand hand, Entity entity) {
		if (level.isClientSide || !(receiver instanceof ServerPlayer serverReceiver) || !(entity instanceof ServerPlayer giver)) {
			return InteractionResult.PASS;
		}

		if (serverReceiver == giver || serverReceiver.isSpectator() || giver.isSpectator()) {
			return InteractionResult.PASS;
		}

		Offer offer = ACTIVE_OFFERS.get(giver.getUUID());
		if (offer == null || !offer.receiverId().equals(serverReceiver.getUUID())) {
			return InteractionResult.PASS;
		}

		if (!isOfferValid(giver, serverReceiver, offer)) {
			clearOffer(giver);
			return InteractionResult.PASS;
		}

		if (!isLookingAt(serverReceiver, giver)) {
			return InteractionResult.SUCCESS;
		}

		if (!serverReceiver.getItemInHand(hand).isEmpty()) {
			return InteractionResult.PASS;
		}

		ItemStack receivedStack = giver.getItemInHand(offer.hand()).copy();
		serverReceiver.setItemInHand(hand, receivedStack);
		giver.setItemInHand(offer.hand(), ItemStack.EMPTY);
		clearOffer(giver);
		return InteractionResult.SUCCESS;
	}

	private static InteractionHand getOfferHand(ServerPlayer giver) {
		if (!giver.getMainHandItem().isEmpty()) {
			return InteractionHand.MAIN_HAND;
		}

		if (!giver.getOffhandItem().isEmpty()) {
			return InteractionHand.OFF_HAND;
		}

		return null;
	}

	private static void notifyReceiver(ServerPlayer receiver) {
		receiver.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.35F, 1.15F);
	}

	private static void tickOffers(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Offer>> iterator = ACTIVE_OFFERS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, Offer> entry = iterator.next();
			ServerPlayer giver = server.getPlayerList().getPlayer(entry.getKey());

			if (giver == null) {
				iterator.remove();
				continue;
			}

			ServerPlayer receiver = server.getPlayerList().getPlayer(entry.getValue().receiverId());
			if (!isOfferValid(giver, receiver, entry.getValue())) {
				iterator.remove();
				syncOfferState(giver, false, entry.getValue().hand());
			}
		}
	}

	private static void clearOffer(ServerPlayer giver) {
		Offer removedOffer = ACTIVE_OFFERS.remove(giver.getUUID());
		if (removedOffer == null) {
			return;
		}

		syncOfferState(giver, false, removedOffer.hand());
	}

	private static void syncActiveOffers(ServerPlayer player) {
		for (Map.Entry<UUID, Offer> entry : ACTIVE_OFFERS.entrySet()) {
			ServerPlayer giver = player.server.getPlayerList().getPlayer(entry.getKey());
			if (giver != null) {
				syncOfferState(player, giver.getUUID(), true, entry.getValue().hand());
			}
		}
	}

	private static void removeDisconnectedOffers(ServerPlayer disconnectedPlayer, MinecraftServer server) {
		Offer ownOffer = ACTIVE_OFFERS.remove(disconnectedPlayer.getUUID());
		if (ownOffer != null) {
			syncOfferState(server, disconnectedPlayer.getUUID(), false, ownOffer.hand());
		}

		Iterator<Map.Entry<UUID, Offer>> iterator = ACTIVE_OFFERS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, Offer> entry = iterator.next();
			if (entry.getValue().receiverId().equals(disconnectedPlayer.getUUID())) {
				iterator.remove();
				syncOfferState(server, entry.getKey(), false, entry.getValue().hand());
			}
		}
	}

	private static void syncOfferState(ServerPlayer giver, boolean active, InteractionHand hand) {
		for (ServerPlayer player : giver.serverLevel().players()) {
			syncOfferState(player, giver.getUUID(), active, hand);
		}
	}

	private static void syncOfferState(MinecraftServer server, UUID giverId, boolean active, InteractionHand hand) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			syncOfferState(player, giverId, active, hand);
		}
	}

	private static void syncOfferState(ServerPlayer player, UUID giverId, boolean active, InteractionHand hand) {
		PickupHandPackets.sendOfferState(player, giverId, active, hand);
	}

	private static ServerPlayer findLookedAtPlayer(ServerPlayer giver) {
		ServerPlayer bestTarget = null;
		double bestDot = LOOK_DOT_THRESHOLD;

		for (ServerPlayer candidate : giver.serverLevel().players()) {
			if (candidate == giver || candidate.isSpectator() || !giver.hasLineOfSight(candidate) || giver.distanceToSqr(candidate) > OFFER_RANGE_SQUARED) {
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

	private static boolean isOfferValid(ServerPlayer giver, ServerPlayer receiver, Offer offer) {
		return receiver != null
				&& giver.level() == receiver.level()
				&& !giver.isSpectator()
				&& !receiver.isSpectator()
				&& giver.hasLineOfSight(receiver)
				&& giver.distanceToSqr(receiver) <= OFFER_RANGE_SQUARED
				&& isOfferedStackStillHeld(giver, offer)
				&& isLookingAt(giver, receiver);
	}

	private static boolean isOfferedStackStillHeld(ServerPlayer giver, Offer offer) {
		ItemStack currentStack = giver.getItemInHand(offer.hand());
		return !currentStack.isEmpty() && ItemStack.isSameItemSameTags(currentStack, offer.offeredStack());
	}

	private static boolean isLookingAt(ServerPlayer receiver, ServerPlayer giver) {
		return getLookDot(receiver, giver) >= LOOK_DOT_THRESHOLD;
	}

	private static double getLookDot(ServerPlayer receiver, ServerPlayer giver) {
		Vec3 lookDirection = receiver.getViewVector(1.0F).normalize();
		Vec3 directionToGiver = giver.getEyePosition().subtract(receiver.getEyePosition()).normalize();
		return lookDirection.dot(directionToGiver);
	}

	private record Offer(UUID receiverId, InteractionHand hand, ItemStack offeredStack) {
	}
}
