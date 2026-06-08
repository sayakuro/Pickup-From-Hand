package net.putterz.pickuphand.mixin.client;

import com.mojang.authlib.GameProfile;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.putterz.pickuphand.client.GivingItemAnimation;
import net.putterz.pickuphand.client.PickupHandAnimatedPlayer;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerEntityAnimationMixin implements PickupHandAnimatedPlayer {
	@Unique
	private final ModifierLayer<IAnimation> pickuphand$animationLayer = new ModifierLayer<>();

	@Inject(method = "<init>", at = @At("RETURN"))
	private void pickuphand$registerAnimationLayer(ClientLevel world, GameProfile profile, CallbackInfo ci) {
		AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
		PlayerAnimationAccess.getPlayerAnimLayer(player).addAnimLayer(1200, this.pickuphand$animationLayer);
		GivingItemAnimation.applyToPlayer(player);
	}

	@Override
	public ModifierLayer<IAnimation> pickuphand$getAnimationLayer() {
		return this.pickuphand$animationLayer;
	}
}
