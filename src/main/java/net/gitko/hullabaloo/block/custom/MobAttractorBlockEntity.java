package net.gitko.hullabaloo.block.custom;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.gitko.hullabaloo.block.ModBlocks;
import net.gitko.hullabaloo.gui.MobAttractorScreenHandler;
import net.gitko.hullabaloo.network.packet.s2c.DisplayMobAttractorEnergyAmountPacket;
import net.gitko.hullabaloo.network.payload.MobAttractorData;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.List;

public class MobAttractorBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<MobAttractorData> {
    private int attractorCooldown = 0;
    public static final int SPEED = 200;
    private Vector3f range = new Vector3f(64, 64, 64);
    public static final int MAX_RANGE = 256;
    public static final float ENERGY_MULTIPLIER = 0.25f;

    // Create energy storage
    public static final int MAX_ENERGY_CAPACITY = 20000000;

    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(MAX_ENERGY_CAPACITY, MAX_ENERGY_CAPACITY, 0) {
        @Override
        protected void onFinalCommit() {
            markDirty();
        }
    };

    // Sync cooldown to the screen
    // note: not using property delegate for energy amount anymore because it seems as though values of property delegates are now limited to the size of a 16 bit signed integer,
    // and since the energy storage has a maximum capacity greater than that of a 16 bit signed integer, custom packets have to be used instead
    private final PropertyDelegate cooldownPropertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return getCooldown();
        }

        @Override
        public void set(int index, int value) {}

        // make sure this is set correctly!
        // it will not work if set incorrectly
        @Override
        public int size() {
            return 1;
        }
    };

    public MobAttractorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MOB_ATTRACTOR_BLOCK_ENTITY, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, MobAttractorBlockEntity be) {
        if (world.isClient()) return;

        // Takes entities in a given range and teleports them near block
        if (be.attractorCooldown >= SPEED && world.isReceivingRedstonePower(pos) && be.energyStorage.getAmount() >= calculateEnergyConsumption(be.range)) {
            be.attractorCooldown = 0;
            be.energyStorage.amount -= calculateEnergyConsumption(be.range);
            be.markDirty();

            List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, Box.of(pos.toCenterPos(), be.range.x(), be.range.y(), be.range.z()), e -> (
                    e.getType() != EntityType.PLAYER &&
                    e.getType() != EntityType.ARMOR_STAND &&
                    //!e.isInvulnerableTo(dmgSource) &&
                            !e.isDead()
            ));
            entities.forEach(entity -> {
                entity.teleport(pos.getX(), pos.getY() + 1, pos.getZ());
            });

            world.playSound(
                    null, // Player - if non-null, will play sound for every nearby player *except* the specified player
                    pos, // The position of where the sound will come from
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT, // The sound that will play, in this case, the sound the anvil plays when it lands.
                    SoundCategory.BLOCKS, // This determines which of the volume sliders affect this sound
                    1f, //Volume multiplier, 1 is normal, 0.5 is half volume, etc
                    1f // Pitch multiplier, 1 is normal, 0.5 is half pitch, etc
            );
        } else {
            be.attractorCooldown++;
        }
    }

    @Override
    public MobAttractorData getScreenOpeningData(ServerPlayerEntity player) {
        // used in ScreenHandler
        return new MobAttractorData(this.getPos(), this.range);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable(getCachedState().getBlock().getTranslationKey());
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);

        this.range = new Vector3f(nbt.getFloat("rangeX"), nbt.getFloat("rangeY"), nbt.getFloat("rangeZ"));
        this.energyStorage.amount = nbt.getLong("energyAmount");
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        nbt.putFloat("rangeX", this.range.x());
        nbt.putFloat("rangeY", this.range.y());
        nbt.putFloat("rangeZ", this.range.z());
        nbt.putLong("energyAmount", this.energyStorage.amount);

        super.writeNbt(nbt, registryLookup);
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }

    // only needs to be called whenever the client needs the data for rendering immediately (blocks such as signs and banners need to use a function like this whenever data is updated)
    // just markDirty can be called for blocks such as chests and furnaces which only need the data when the GUI is opened
    public void sync() {
        assert this.getWorld() != null;
        if (!this.getWorld().isClient()) {
            // updates comparators and marks dirty
            this.markDirty();
            // let client know that the block has been updated
            this.getWorld().updateListeners(this.getPos(), this.getCachedState(), this.getWorld().getBlockState(this.getPos()), Block.NOTIFY_LISTENERS);
        }
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new MobAttractorScreenHandler(syncId, inv, this.cooldownPropertyDelegate);
    }

    public void setRange(Vector3f newRange) {
        this.range = newRange;
    }

    public int getCooldown() {
        return this.attractorCooldown;
    }

    public static int calculateEnergyConsumption(Vector3f range) {
        return (int) (((int) range.x() * (int) range.y() * (int) range.z()) * ENERGY_MULTIPLIER);
    }

    public void sendEnergyAmountToClient(ServerPlayerEntity player) {
        // send a packet to client containing the energy amount information
        ServerPlayNetworking.send(player, new DisplayMobAttractorEnergyAmountPacket(this.energyStorage.getAmount()));
    }
}
