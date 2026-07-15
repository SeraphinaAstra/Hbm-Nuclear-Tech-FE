package com.hbm.items.tool;

import com.hbm.items.ModItems;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.util.ContaminationUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;

import baubles.api.BaubleType;
import baubles.api.IBauble;

/**
 * Cancer screener — a hand-held diagnostic, analogous to the digamma diagnostic
 * or geiger counter. Right-click prints a chat readout (see
 * ContaminationUtil.printCancerData) reporting the player's cancer diagnosis,
 * burden, current dose, monthly threshold/excess, the current month's roll
 * probability, and related ARS death-timer status (spec §4).
 */
@Optional.InterfaceList({@Optional.Interface(iface = "baubles.api.IBauble", modid = "baubles")})
public class ItemCancerScreener extends Item implements IBauble {

	public ItemCancerScreener(String s) {
		this.setTranslationKey(s);
		this.setRegistryName(s);
		this.setMaxStackSize(1);

		ModItems.ALL_ITEMS.add(this);
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand handIn) {
		if (!world.isRemote) {
			world.playSound(null, player.posX, player.posY, player.posZ, HBMSoundHandler.techBoop, SoundCategory.PLAYERS, 1.0F, 1.0F);
			ContaminationUtil.printCancerData(player);
		}

		return super.onItemRightClick(world, player, handIn);
	}

	@Override
	public BaubleType getBaubleType(ItemStack itemstack) {
		return BaubleType.TRINKET;
	}
}