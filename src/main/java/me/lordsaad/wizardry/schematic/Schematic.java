package me.lordsaad.wizardry.schematic;

import com.google.common.primitives.UnsignedBytes;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Saad on 6/10/2016.
 */
public class Schematic {

    private BlockObject[] blockObjects;

    public Schematic(String fileName) {
        try {
            InputStream is = Schematic.class.getResourceAsStream("/assets/wizardry/schematics/" + fileName + ".schematic");
            NBTTagCompound nbtdata = CompressedStreamTools.readCompressed(is);

            is.close();
            short width = nbtdata.getShort("Width");
            short height = nbtdata.getShort("Height");
            short length = nbtdata.getShort("Length");
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void check(World world, BlockPos pos, Block centerBlock) {
        List<BlockObject> blocks = new ArrayList<>();
        for (BlockObject obj : blockObjects) {
            if (obj.getState().getBlock() == Blocks.STAINED_HARDENED_CLAY) {
                if (obj.getState().getBlock().getMetaFromState(obj.getState()) == 14)
                    blocks.add(new BlockObject(pos, centerBlock.getDefaultState()));
            } else
                blocks.add(new BlockObject(new BlockPos(pos.add(obj.getPos().getX(), obj.getPos().getY(), obj.getPos().getZ())), obj.getState()));
        }

        for (BlockObject obj : blocks)
            if (world.getBlockState(obj.getPos()) != obj.getState()) {
                for (EntityPlayer p : world.playerEntities)
                    p.addChatComponentMessage(new TextComponentString("STRUCTURE INCORRECT"));
                break;
            }
    }
}