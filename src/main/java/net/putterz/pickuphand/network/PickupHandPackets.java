package net.putterz.pickuphand.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.putterz.pickuphand.PickupHandMod;
import net.putterz.pickuphand.client.PickupHandClientNetworking;
import net.putterz.pickuphand.server.SecureGiveHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class PickupHandPackets {
	private static final String PROTOCOL_VERSION = "1";
	private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
			.named(new ResourceLocation(PickupHandMod.MOD_ID, "main"))
			.clientAcceptedVersions(PROTOCOL_VERSION::equals)
			.serverAcceptedVersions(PROTOCOL_VERSION::equals)
			.networkProtocolVersion(() -> PROTOCOL_VERSION)
			.simpleChannel();

	private PickupHandPackets() {
	}

	public static void register() {
		int id = 0;
		CHANNEL.registerMessage(
				id++,
				GiveItemMessage.class,
				GiveItemMessage::encode,
				GiveItemMessage::decode,
				GiveItemMessage::handle,
				Optional.of(NetworkDirection.PLAY_TO_SERVER)
		);
		CHANNEL.registerMessage(
				id,
				OfferStateMessage.class,
				OfferStateMessage::encode,
				OfferStateMessage::decode,
				OfferStateMessage::handle,
				Optional.of(NetworkDirection.PLAY_TO_CLIENT)
		);
	}

	public static void sendGiveItem() {
		CHANNEL.sendToServer(new GiveItemMessage());
	}

	public static void sendOfferState(ServerPlayer player, UUID giverId, boolean active, InteractionHand hand) {
		CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OfferStateMessage(giverId, active, hand));
	}

	private record GiveItemMessage() {
		private static void encode(GiveItemMessage message, FriendlyByteBuf buf) {
		}

		private static GiveItemMessage decode(FriendlyByteBuf buf) {
			return new GiveItemMessage();
		}

		private static void handle(GiveItemMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
			NetworkEvent.Context context = contextSupplier.get();
			context.enqueueWork(() -> {
				ServerPlayer sender = context.getSender();
				if (sender != null) {
					SecureGiveHandler.activateOffer(sender);
				}
			});
			context.setPacketHandled(true);
		}
	}

	public record OfferStateMessage(UUID giverId, boolean active, InteractionHand hand) {
		private static void encode(OfferStateMessage message, FriendlyByteBuf buf) {
			buf.writeUUID(message.giverId());
			buf.writeBoolean(message.active());
			buf.writeEnum(message.hand());
		}

		private static OfferStateMessage decode(FriendlyByteBuf buf) {
			return new OfferStateMessage(buf.readUUID(), buf.readBoolean(), buf.readEnum(InteractionHand.class));
		}

		private static void handle(OfferStateMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
			NetworkEvent.Context context = contextSupplier.get();
			context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
					Dist.CLIENT,
					() -> () -> PickupHandClientNetworking.handleOfferState(message)
			));
			context.setPacketHandled(true);
		}
	}
}
