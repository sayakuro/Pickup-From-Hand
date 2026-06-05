package net.putterz.pickuphand.mixin.client;

import com.mojang.authlib.GameProfile;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.putterz.pickuphand.client.GivingItemAnimation;
import net.putterz.pickuphand.client.PickupHandAnimatedPlayer;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityAnimationMixin implements PickupHandAnimatedPlayer {
	@Unique
	private final ModifierLayer<IAnimation> pickuphand$animationLayer = new ModifierLayer<>();

	@Inject(method = "<init>", at = @At("RETURN"))
	private void pickuphand$registerAnimationLayer(ClientWorld world, GameProfile profile, CallbackInfo ci) {
		AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) (Object) this;
		PlayerAnimationAccess.getPlayerAnimLayer(player).addAnimLayer(1200, this.pickuphand$animationLayer);
		GivingItemAnimation.applyToPlayer(player);
	}

	@Override
	public ModifierLayer<IAnimation> pickuphand$getAnimationLayer() {
		return this.pickuphand$animationLayer;
	}
}
