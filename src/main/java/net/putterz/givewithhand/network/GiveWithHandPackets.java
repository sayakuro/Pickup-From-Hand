package net.putterz.givewithhand.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Hand;
import net.putterz.givewithhand.GiveWithHandMod;

import java.util.UUID;

public final class GiveWithHandPackets {
	private GiveWithHandPackets() {
	}

	public static void registerPayloads() {
		PayloadTypeRegistry.playC2S().register(GiveItemPayload.ID, GiveItemPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(OfferStatePayload.ID, OfferStatePayload.CODEC);
	}

	public record GiveItemPayload() implements CustomPayload {
		public static final CustomPayload.Id<GiveItemPayload> ID = new CustomPayload.Id<>(GiveWithHandMod.id("give_item"));
		public static final PacketCodec<RegistryByteBuf, GiveItemPayload> CODEC = PacketCodec.unit(new GiveItemPayload());

		@Override
		public CustomPayload.Id<? extends CustomPayload> getId() {
			return ID;
		}
	}

	public record OfferStatePayload(UUID giverId, boolean active, Hand hand) implements CustomPayload {
		public static final CustomPayload.Id<OfferStatePayload> ID = new CustomPayload.Id<>(GiveWithHandMod.id("offer_state"));
		public static final PacketCodec<RegistryByteBuf, OfferStatePayload> CODEC = CustomPayload.codecOf(OfferStatePayload::write, OfferStatePayload::new);

		private OfferStatePayload(RegistryByteBuf buf) {
			this(buf.readUuid(), buf.readBoolean(), buf.readEnumConstant(Hand.class));
		}

		private void write(RegistryByteBuf buf) {
			buf.writeUuid(this.giverId);
			buf.writeBoolean(this.active);
			buf.writeEnumConstant(this.hand);
		}

		@Override
		public CustomPayload.Id<? extends CustomPayload> getId() {
			return ID;
		}
	}
}
