package net.blay09.mods.excompressum.block;

import com.mojang.authlib.GameProfile;
import net.blay09.mods.excompressum.ExCompressum;
import net.blay09.mods.excompressum.StupidUtils;
import net.blay09.mods.excompressum.handler.GuiHandler;
import net.blay09.mods.excompressum.registry.AutoSieveSkinRegistry;
import net.blay09.mods.excompressum.tile.TileEntityAutoSieveBase;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

public abstract class BlockAutoSieveBase extends BlockContainer {

	public static final PropertyEnum<EnumFacing> FACING = PropertyEnum.create("facing", EnumFacing.class);

	protected BlockAutoSieveBase(Material material) {
		super(material);
		setCreativeTab(ExCompressum.creativeTab);
		setHardness(2f);
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, FACING);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(FACING).ordinal();
	}

	@Override
	@SuppressWarnings("deprecation")
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(FACING, EnumFacing.getFront(meta));
	}

	@Nullable
	@Override
	protected ItemStack createStackedBlock(IBlockState state) {
		return new ItemStack(this, 1, state.getValue(FACING).ordinal());
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.MODEL;
	}

	@Override
	public BlockRenderLayer getBlockLayer() {
		return BlockRenderLayer.CUTOUT;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, @Nullable ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ) {
		if (heldItem != null) {
			TileEntityAutoSieveBase tileEntity = (TileEntityAutoSieveBase) world.getTileEntity(pos);
			if(tileEntity != null) {
				if (heldItem.getItem() instanceof ItemFood) {
					if (tileEntity.getSpeedBoost() <= 1f) {
						tileEntity.setSpeedBoost((int) (((ItemFood) heldItem.getItem()).getSaturationModifier(heldItem) * 640), Math.max(1f, ((ItemFood) heldItem.getItem()).getHealAmount(heldItem) * 0.75f));
						if (!player.capabilities.isCreativeMode) {
							heldItem.stackSize--;
						}
						if (!world.isRemote) {
							world.playEvent(2005, pos, 0);
						}
					}
					return true;
				} else if (heldItem.getItem() == Items.NAME_TAG && heldItem.hasDisplayName()) {
					tileEntity.setCustomSkin(new GameProfile(null, heldItem.getDisplayName()));
					if (!player.capabilities.isCreativeMode) {
						heldItem.stackSize--;
					}
					return true;
				}
			}
		}
		if(!player.isSneaking()) {
			player.openGui(ExCompressum.instance, GuiHandler.GUI_AUTO_SIEVE, world, pos.getX(), pos.getY(), pos.getZ());
		}
		return true;
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		TileEntity tileEntity = world.getTileEntity(pos);
		if(tileEntity != null) {
			//noinspection ConstantConditions /// thanks lex
			IItemHandler itemHandler = tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
			for (int i = 0; i < itemHandler.getSlots(); i++) {
				if (itemHandler.getStackInSlot(i) != null) {
					EntityItem entityItem = new EntityItem(world, pos.getX(), pos.getY(), pos.getZ(), itemHandler.getStackInSlot(i));
					double motion = 0.05;
					entityItem.motionX = world.rand.nextGaussian() * motion;
					entityItem.motionY = 0.2;
					entityItem.motionZ = world.rand.nextGaussian() * motion;
					world.spawnEntityInWorld(entityItem);
				}
			}
			ItemStack currentStack = ((TileEntityAutoSieveBase) tileEntity).getCurrentStack();
			if (currentStack != null) {
				EntityItem entityItem = new EntityItem(world, pos.getX(), pos.getY(), pos.getZ(), currentStack);
				double motion = 0.05;
				entityItem.motionX = world.rand.nextGaussian() * motion;
				entityItem.motionY = 0.2;
				entityItem.motionZ = world.rand.nextGaussian() * motion;
				world.spawnEntityInWorld(entityItem);
			}
		}
		super.breakBlock(world, pos, state);
	}

	@Override
	public IBlockState onBlockPlaced(World worldIn, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
		EnumFacing facing = BlockPistonBase.getFacingFromEntity(pos, placer);
		if(facing.getAxis() == EnumFacing.Axis.Y) {
			facing = EnumFacing.NORTH;
		}
		return getStateFromMeta(meta).withProperty(FACING, facing);
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		TileEntityAutoSieveBase tileEntity = (TileEntityAutoSieveBase) world.getTileEntity(pos);
		if(tileEntity != null) {
			boolean useRandomSkin = true;
			NBTTagCompound tagCompound = stack.getTagCompound();
			if (tagCompound != null) {
				if (tagCompound.hasKey("CustomSkin")) {
					GameProfile customSkin = NBTUtil.readGameProfileFromNBT(tagCompound.getCompoundTag("CustomSkin"));
					if (customSkin != null) {
						tileEntity.setCustomSkin(customSkin);
						useRandomSkin = false;
					}
				}
			}
			if (!world.isRemote && useRandomSkin) {
				tileEntity.setCustomSkin(new GameProfile(null, AutoSieveSkinRegistry.getRandomSkin()));
			}
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean hasComparatorInputOverride(IBlockState state) {
		return true;
	}

	@Override
	@SuppressWarnings("deprecation")
	public int getComparatorInputOverride(IBlockState blockState, World world, BlockPos pos) {
		return StupidUtils.getComparatorOutput64(world, pos);
	}

}
