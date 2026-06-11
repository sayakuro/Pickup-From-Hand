package net.putterz.givewithhand.api;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

import java.util.UUID;

public record GiveOffer(UUID receiverId, Hand hand, ItemStack offeredStack) {
	public GiveOffer {
		offeredStack = offeredStack.copy();
	}

	@Override
	public ItemStack offeredStack() {
		return offeredStack.copy();
	}

	public boolean matchesHeldStack(ItemStack stack) {
		return !stack.isEmpty() && ItemStack.canCombine(stack, offeredStack);
	}
}
