package net.putterz.pickuphand.client;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;

public interface PickupHandAnimatedPlayer {
	ModifierLayer<IAnimation> pickuphand$getAnimationLayer();
}
