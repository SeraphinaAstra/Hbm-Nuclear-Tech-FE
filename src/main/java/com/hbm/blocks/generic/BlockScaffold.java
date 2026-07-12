package com.hbm.blocks.generic;

import com.hbm.blocks.IBlockMulti;
import com.hbm.blocks.ICustomBlockItem;
import com.hbm.items.IDynamicModels;
import com.hbm.items.IModelRegister;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.model.BlockScaffoldBakedModel;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Random;

public class BlockScaffold extends BlockBakeBase implements ICustomBlockItem, IBlockMulti, IDynamicModels {
    public static final PropertyEnum<EnumScaffoldOrient> ORIENT = PropertyEnum.create("orient", EnumScaffoldOrient.class);

    @SideOnly(Side.CLIENT)
    private TextureAtlasSprite[] sprites;
    private final EnumScaffoldVariant fixedVariant;

    public BlockScaffold(EnumScaffoldVariant variant) {
        super(Material.IRON, variant.getName());
        this.fixedVariant = variant;

        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            sprites = new TextureAtlasSprite[1];
        }

        setDefaultState(this.blockState.getBaseState().withProperty(ORIENT, EnumScaffoldOrient.HORIZONTAL_NS));
    }

    public BlockScaffold(String name) {
        super(Material.IRON, name);
        this.fixedVariant = EnumScaffoldVariant.STEEL;

        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            sprites = new TextureAtlasSprite[1];
        }

        setDefaultState(this.blockState.getBaseState().withProperty(ORIENT, EnumScaffoldOrient.HORIZONTAL_NS));
    }

    @Override
    protected @NotNull BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, ORIENT);
    }

    @Override
    public @NotNull IBlockState getStateFromMeta(int meta) {
        EnumScaffoldOrient orient = EnumScaffoldOrient.VALUES[meta & 3];
        return getDefaultState().withProperty(ORIENT, orient);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(ORIENT).ordinal() & 3;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return new ItemStack(Item.getItemFromBlock(this), 1, getMetaFromState(state));
    }

    @Override
    public @NotNull IBlockState getStateForPlacement(@NotNull World world, @NotNull BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, @NotNull EntityLivingBase placer, @NotNull EnumHand hand) {
        EnumScaffoldOrient orient;

        if (facing == EnumFacing.UP || facing == EnumFacing.DOWN) {
            int rot = MathHelper.floor((placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
            orient = (rot % 2 == 0) ? EnumScaffoldOrient.HORIZONTAL_NS : EnumScaffoldOrient.HORIZONTAL_EW;
        } else if (facing == EnumFacing.NORTH || facing == EnumFacing.SOUTH) {
            orient = EnumScaffoldOrient.VERTICAL_NS;
        } else {
            orient = EnumScaffoldOrient.VERTICAL_EW;
        }

        return getDefaultState().withProperty(ORIENT, orient);
    }

    @Override
    public @NotNull EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public @NotNull Item getItemDropped(IBlockState state, @NotNull Random rand, int fortune) {
        return Item.getItemFromBlock(this);
    }

    @Override
    public int quantityDropped(@NotNull IBlockState state, int fortune, @NotNull Random random) {
        return 1;
    }

    @Override
    public int damageDropped(IBlockState state) {
        return getMetaFromState(state);
    }


    @Override
    public int getSubCount() {
        return 1;
    }

    @Override
    public @NotNull AxisAlignedBB getBoundingBox(@NotNull IBlockState state, @NotNull IBlockAccess world, @NotNull BlockPos pos) {
        float f = 0.0625F;
        EnumScaffoldOrient orient = state.getValue(ORIENT);
        return switch (orient) {
            case HORIZONTAL_EW -> new AxisAlignedBB(2 * f, 0.0F, 0.0F, 14 * f, 1.0F, 1.0F);
            case HORIZONTAL_NS -> new AxisAlignedBB(0.0F, 0.0F, 2 * f, 1.0F, 1.0F, 14 * f);
            default -> new AxisAlignedBB(0.0F, 2 * f, 0.0F, 1.0F, 14 * f, 1.0F);
        };
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity, boolean isActualState) {
        AxisAlignedBB bb = getBoundingBox(state, world, pos);
        addCollisionBoxToList(pos, entityBox, collidingBoxes, bb);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerModel() {
        Item item = Item.getItemFromBlock(this);

        for (EnumScaffoldOrient orient : EnumScaffoldOrient.VALUES) {
            ModelLoader.setCustomModelResourceLocation(item, orient.ordinal(), new ModelResourceLocation(Objects.requireNonNull(getRegistryName()), "inventory,meta=" + orient.ordinal()));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public StateMapperBase getStateMapper(ResourceLocation loc) {
        return new StateMapperBase() {
            @Override
            protected @NotNull ModelResourceLocation getModelResourceLocation(@NotNull IBlockState state) {
                return new ModelResourceLocation(loc, "meta=" + state.getValue(ORIENT).ordinal());
            }
        };
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerSprite(TextureMap map) {
        sprites[0] = map.registerSprite(new ResourceLocation(Objects.requireNonNull(getRegistryName()).getNamespace(), "blocks/" + fixedVariant.getName()));
    }

    @Override
    public void registerItem() {
        ItemBlock itemBlock = new ItemBlock(this);
        itemBlock.setRegistryName(Objects.requireNonNull(getRegistryName()));
        ForgeRegistries.ITEMS.register(itemBlock);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void bakeModel(ModelBakeEvent event) {
        ResourceLocation rl = Objects.requireNonNull(getRegistryName());

        for (EnumScaffoldOrient orient : EnumScaffoldOrient.VALUES) {
            int meta = orient.ordinal();
            IBakedModel blockModel = BlockScaffoldBakedModel.forBlock((HFRWavefrontObject) ResourceManager.scaffold, sprites[0]);
            IBakedModel itemModel = BlockScaffoldBakedModel.forItem((HFRWavefrontObject) ResourceManager.scaffold, sprites[0]);

            ModelResourceLocation mrlBlock = new ModelResourceLocation(rl, "meta=" + meta);
            ModelResourceLocation mrlItem = new ModelResourceLocation(rl, "inventory,meta=" + meta);

            event.getModelRegistry().putObject(mrlBlock, blockModel);
            event.getModelRegistry().putObject(mrlItem, itemModel);
        }
    }

    public enum EnumScaffoldVariant implements IStringSerializable {
        STEEL("steel_scaffold"),
        RED("scaffold_red"),
        WHITE("scaffold_white"),
        YELLOW("scaffold_yellow");
        public static final EnumScaffoldVariant[] VALUES = values();
        private final String name;

        EnumScaffoldVariant(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getName() {
            return name;
        }
    }

    public enum EnumScaffoldOrient implements IStringSerializable {
        HORIZONTAL_NS("horizontal_north_south"),
        HORIZONTAL_EW("horizontal_east_west"),
        VERTICAL_NS( "vertical_north_south"),
        VERTICAL_EW("vertical_east_west");
        public static final EnumScaffoldOrient[] VALUES = values();
        private final String name;

        EnumScaffoldOrient(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getName() {
            return name;
        }
    }
}