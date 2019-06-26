package wouldbeashame;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = WouldBeAShame.MODID, acceptedMinecraftVersions = "[1.12, 1.13)", clientSideOnly = true)
@Mod.EventBusSubscriber
public class WouldBeAShame
{
    public static final String MODID = "wouldbeashame";

    static float multiplierPerBlock = 0.1f;
    static float offsetPower = 0f;
    static int maxPower = 10;
    static int minPower = 2;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        Configuration configuration = new Configuration(event.getSuggestedConfigurationFile(),true);
        configuration.load();

        minPower = configuration.getInt("minPower", "explosion", minPower, 0, Integer.MAX_VALUE, "The minimum power a creeper explosion must have");
        maxPower = configuration.getInt("maxPower", "explosion", maxPower, 0, Integer.MAX_VALUE, "The maximum power a creeper explosion can have");
        multiplierPerBlock = configuration.getFloat("multiplierPerBlock", "explosion", multiplierPerBlock, Float.MIN_VALUE, Float.MAX_VALUE, "How much the explosion is modified by per block");
        offsetPower = configuration.getFloat("offsetPower", "explosion", offsetPower, Float.MIN_VALUE, Float.MAX_VALUE, "How much power the explosion starts with");

        if (configuration.hasChanged())
            configuration.save();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlace(BlockEvent.PlaceEvent event)
    {
        if(event.isCanceled())
            return;
        ShameData data = ShameData.getInstance(event.getWorld());
        data.incrementBlocks();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onUpdate(LivingEvent.LivingUpdateEvent event)
    {
        EntityLivingBase entity = event.getEntityLiving();
        ShameData data = ShameData.getInstance(entity.getEntityWorld());
        if(entity instanceof EntityCreeper) {
            EntityCreeper creeper = (EntityCreeper) entity;
            creeper.explosionRadius = calculatePower(data.getBlocksPlaced());
        }
    }

    public static void onExplode(ExplosionEvent event) {
        Explosion explosion = event.getExplosion();
        Entity exploder = explosion.getExplosivePlacedBy();
        if(exploder instanceof EntityCreeper) {
            ShameData data = ShameData.getInstance(event.getWorld());
            data.resetBlocks();
        }
    }

    private static int calculatePower(int blocks) {
        return MathHelper.clamp((int)(offsetPower + blocks / multiplierPerBlock),minPower,maxPower);
    }

    public static class ShameData extends WorldSavedData
    {
        private static final String ID = "ShameData";
        private int blocksPlaced;

        public ShameData(String id) {
            super(id);
        }

        public int getBlocksPlaced() {
            return blocksPlaced;
        }

        public void incrementBlocks() {
            blocksPlaced++;
            setDirty(true);
        }

        public void resetBlocks() {
            blocksPlaced = 0;
            setDirty(true);
        }


        @Override
        public void readFromNBT(NBTTagCompound compound) {
            blocksPlaced = compound.getInteger("blocksPlaced");
        }

        @Override
        public NBTTagCompound writeToNBT(NBTTagCompound compound) {
            compound.setInteger("blocksPlaced",blocksPlaced);
            return compound;
        }

        public static ShameData getInstance(World world)
        {
            if (world != null)
            {
                WorldSavedData handler = world.getPerWorldStorage().getOrLoadData(ShameData.class, ID);
                if (handler == null) {
                    handler = new ShameData(ID);
                    world.getPerWorldStorage().setData(ID, handler);
                }

                return (ShameData) handler;
            }
            return null;
        }
    }
}
