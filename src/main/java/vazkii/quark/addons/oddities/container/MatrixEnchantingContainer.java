package vazkii.quark.addons.oddities.container;

import javax.annotation.Nonnull;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.Tags;
import vazkii.quark.addons.oddities.block.be.MatrixEnchantingTableTileEntity;
import vazkii.quark.addons.oddities.module.MatrixEnchantingModule;

public class MatrixEnchantingContainer extends AbstractContainerMenu {

	public final MatrixEnchantingTableTileEntity enchanter;

	public MatrixEnchantingContainer(int id, Inventory playerInv, MatrixEnchantingTableTileEntity tile) {
		super(MatrixEnchantingModule.containerType, id);
		enchanter = tile;

		// Item Slot
		addSlot(new Slot(tile, 0, 15, 20) {
			@Override 
			public int getMaxStackSize() { 
				return 1; 
			}
		});

		// Lapis Slot
		addSlot(new Slot(tile, 1, 15, 44) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return isLapis(stack);
			}
		});

		// Output Slot
		addSlot(new Slot(tile, 2, 59, 32) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}

			@Nonnull
			@Override
			public void onTake(Player thePlayer, @Nonnull ItemStack stack) {
				finish(thePlayer, stack);
				super.onTake(thePlayer, stack);
			}
		});

		// Player Inv
		for(int i = 0; i < 3; ++i)
			for(int j = 0; j < 9; ++j)
				addSlot(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
		for(int k = 0; k < 9; ++k)
			addSlot(new Slot(playerInv, k, 8 + k * 18, 142));
	}
	
	public static MatrixEnchantingContainer fromNetwork(int windowId, Inventory playerInventory, FriendlyByteBuf buf) {
		BlockPos pos = buf.readBlockPos();
		MatrixEnchantingTableTileEntity te = (MatrixEnchantingTableTileEntity) playerInventory.player.level.getBlockEntity(pos);
		return new MatrixEnchantingContainer(windowId, playerInventory, te);	
	}

	private boolean isLapis(ItemStack stack) {
		return stack.is(Tags.Items.GEMS_LAPIS);
	}

	private void finish(Player player, ItemStack stack) {
		enchanter.setItem(0, ItemStack.EMPTY);

		player.awardStat(Stats.ENCHANT_ITEM);

		if(player instanceof ServerPlayer)
			CriteriaTriggers.ENCHANTED_ITEM.trigger((ServerPlayer) player, stack, 1);

		player.level.playSound(null, enchanter.getBlockPos(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F,  player.level.random.nextFloat() * 0.1F + 0.9F);
	}

	@Override
	public boolean stillValid(@Nonnull Player playerIn) {
		Level world = enchanter.getLevel();
		BlockPos pos = enchanter.getBlockPos();
		if(world.getBlockState(pos).getBlock() != MatrixEnchantingModule.matrixEnchanter)
			return false;
		else
			return playerIn.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
	}

	@Nonnull
	@Override
	public ItemStack quickMoveStack(Player playerIn, int index) {
		ItemStack originalStack = ItemStack.EMPTY;
		Slot slot = slots.get(index);

		if (slot != null && slot.hasItem()) {
			ItemStack stackInSlot = slot.getItem();
			originalStack = stackInSlot.copy();

			if(index < 3) {
				if (!moveItemStackTo(stackInSlot, 3, 39, true))
					return ItemStack.EMPTY;
			}
			else if(isLapis(stackInSlot)) {
				if(!moveItemStackTo(stackInSlot, 1, 2, true))
					return ItemStack.EMPTY;
			}
			else {
				if(slots.get(0).hasItem() || !slots.get(0).mayPlace(stackInSlot))
					return ItemStack.EMPTY;

				if(stackInSlot.hasTag()) // Forge: Fix MC-17431
					slots.get(0).set(stackInSlot.split(1));

				else if(!stackInSlot.isEmpty()) {
					slots.get(0).set(new ItemStack(stackInSlot.getItem(), 1));
					stackInSlot.shrink(1);
				}
			}

			if(stackInSlot.isEmpty())
				slot.set(ItemStack.EMPTY);
			else slot.setChanged();

			if(stackInSlot.getCount() == originalStack.getCount())
				return ItemStack.EMPTY;

			slot.onTake(playerIn, stackInSlot);
		}

		return originalStack;
	}

}