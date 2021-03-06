package me.lordsaad.wizardry.schematic;

import com.google.common.primitives.UnsignedBytes;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.registry.FMLControlledNamespacedRegistry;
import net.minecraftforge.fml.common.registry.GameData;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Saad on 6/10/2016.
 */
public class Schematic {

    private static final FMLControlledNamespacedRegistry<Block> BLOCK_REGISTRY = GameData.getBlockRegistry();
    private short width;
    private short height;
    private short length;
    private BlockObject[] blockObjects;

    public Schematic(String fileName) {
        try {
            InputStream is = Schematic.class.getResourceAsStream("/assets/wizardry/schematics/" + fileName + ".schematic");
            NBTTagCompound nbtdata = CompressedStreamTools.readCompressed(is);

            is.close();
            width = nbtdata.getShort("Width");
            height = nbtdata.getShort("Height");
            length = nbtdata.getShort("Length");
            int size = width * height * length;
            blockObjects = new BlockObject[size];

            byte[] blockIDs = nbtdata.getByteArray("Blocks");
            byte[] metadata = nbtdata.getByteArray("Data");

            int counter = 0;
            for (int schemY = 0; schemY < height; schemY++) {
                for (int schemZ = 0; schemZ < length; schemZ++) {
                    for (int schemX = 0; schemX < width; schemX++) {
                        int blockId = UnsignedBytes.toInt(blockIDs[counter]);
                        BlockPos pos = new BlockPos(schemX, schemY, schemZ);
                        IBlockState state = Block.getBlockById(blockId).getStateFromMeta(metadata[counter]);

                        blockObjects[counter] = new BlockObject(pos, state);
                        counter++;
                    }
                }
            }

            NBTTagList tileEntitiesList = nbtdata.getTagList("TileEntities", Constants.NBT.TAG_COMPOUND);

            for (int i = 0; i < tileEntitiesList.tagCount(); i++) {
                TileEntity tileEntity = readTileEntityFromCompound(tileEntitiesList.getCompoundTagAt(i));
                NBTTagCompound tag = tileEntitiesList.getCompoundTagAt(i);
                int schemX = tag.getInteger("x");
                int schemY = tag.getInteger("y");
                int schemZ = tag.getInteger("z");
                BlockPos pos = new BlockPos(schemX, schemY, schemZ);
                IBlockState state = Block.getBlockById(tileEntity.getTileData().getId()).getDefaultState();

                blockObjects[counter] = new BlockObject(pos, state);
            }

            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<TileEntity> readTileEntitiesFromCompound(final NBTTagCompound compound) {
        return readTileEntitiesFromCompound(compound, new ArrayList<>());
    }

    private static List<TileEntity> readTileEntitiesFromCompound(final NBTTagCompound compound, final List<TileEntity> tileEntities) {
        final NBTTagList tagList = compound.getTagList("TileEntities", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < tagList.tagCount(); i++) {
            final NBTTagCompound tileEntityCompound = tagList.getCompoundTagAt(i);
            final TileEntity tileEntity = readTileEntityFromCompound(tileEntityCompound);
            tileEntities.add(tileEntity);
        }

        return tileEntities;
    }

    private static TileEntity readTileEntityFromCompound(final NBTTagCompound tileEntityCompound) {
        return TileEntity.create(tileEntityCompound);
    }

    public short getWidth() {
        return width;
    }

    public short getHeight() {
        return height;
    }

    public short getLength() {
        return length;
    }

    public HashMap<Integer, ArrayList<BlockObject>> getSchematicLayers() {
        HashMap<Integer, ArrayList<BlockObject>> layers = new HashMap<>();
        for (BlockObject object : blockObjects) {
            if (!layers.containsKey(object.getPos().getY())) {
                ArrayList<BlockObject> temp = new ArrayList<>();
                temp.add(object);
                layers.put(object.getPos().getY(), temp);
            }
            layers.get(object.getPos().getY()).add(object);
        }
        return layers;
    }

    public boolean check(World world, BlockPos pos, Block centerBlock, EntityPlayer player) {
        boolean success = true;
        List<BlockObject> blocks = new ArrayList<>();
        for (BlockObject obj : blockObjects) {
            if (obj != null) {
                if (obj.getState().getBlock() == Blocks.STAINED_HARDENED_CLAY && obj.getState().getBlock().getMetaFromState(obj.getState()) == 14) {
                    blocks.add(new BlockObject(pos, centerBlock.getDefaultState()));
                } else
                    blocks.add(new BlockObject(new BlockPos(pos.add(obj.getPos().getX(), obj.getPos().getY(), obj.getPos().getZ())).add(-(width / 2), -(height / 2) + 1, -(length / 2)), obj.getState()));
            }
        }

        for (BlockObject obj : blocks) {
            // fix a block that turned to dirt and was supposed to be grass
            if (world.getBlockState(obj.getPos()).getBlock() == Blocks.DIRT && obj.getState().getBlock() == Blocks.GRASS)
                world.setBlockState(obj.getPos(), obj.getState());

            // fix any wrong metadata so the structure isn't stupidly strict
            if (world.getBlockState(obj.getPos()).getBlock() == obj.getState().getBlock() && world.getBlockState(obj.getPos()) != obj.getState() && obj.getState().getBlock() != centerBlock)
                world.setBlockState(obj.getPos(), obj.getState());

            if (world.getBlockState(obj.getPos()).getBlock() != obj.getState().getBlock()) {
                success = false;
                player.addChatMessage(new TextComponentString(obj.getPos() + " is " + world.getBlockState(obj.getPos()) + " but should be " + obj.getState()));
            }
        }
        if (success)
            player.addChatMessage(new TextComponentString(TextFormatting.GREEN + "Structure complete."));
        else player.addChatMessage(new TextComponentString(TextFormatting.RED + "Structure incomplete."));
        return success;
    }
}
