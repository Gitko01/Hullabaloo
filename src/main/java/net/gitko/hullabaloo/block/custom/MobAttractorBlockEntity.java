package net.gitko.hullabaloo.block.custom;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.gitko.hullabaloo.block.ModBlocks;
import net.gitko.hullabaloo.gui.MobAttractorScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
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

public class MobAttractorBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory {

    private int attractorCooldown = 0;
    public static final int SPEED = 200;
    private Vector3f range = new Vector3f(64, 64, 64);
    public static final int MAX_RANGE = 256;

    // Create energy storage
    public static final int MAX_ENERGY_CAPACITY = 20000000;

    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(MAX_ENERGY_CAPACITY, MAX_ENERGY_CAPACITY, 0) {
        @Override
        protected void onFinalCommit() {
            markDirty();
        }
    };

    // Sync energy amount to the screen
    private final PropertyDelegate energyAmountPropertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            if (index == 0) {
                return (int) energyStorage.getAmount();
            } else if (index == 1) {
                // energy consumption
                return (int) (range.x() * range.y() * range.z());
            } else {
                return getCooldown();
            }
        }

        @Override
        public void set(int index, int value) {
            energyStorage.amount = value;
        }

        @Override
        public int size() {
            return 3;
        }
    };

    public MobAttractorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MOB_ATTRACTOR_BLOCK_ENTITY, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, MobAttractorBlockEntity be) {
        if (world.isClient()) return;

        // Takes entities in a given range and teleports them near block
        if (be.attractorCooldown >= SPEED && world.isReceivingRedstonePower(pos) && be.energyStorage.getAmount() >= be.range.x() * be.range.y() * be.range.z()) {
            be.attractorCooldown = 0;
            be.energyStorage.amount -= be.range.x() * be.range.y() * be.range.z();

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
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        // used in ScreenHandler
        buf.writeBlockPos(this.getPos());
        buf.writeVector3f(this.range);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable(getCachedState().getBlock().getTranslationKey());
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);

        this.range = new Vector3f(nbt.getFloat("rangeX"), nbt.getFloat("rangeY"), nbt.getFloat("rangeZ"));
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        nbt.putFloat("rangeX", this.range.x());
        nbt.putFloat("rangeY", this.range.y());
        nbt.putFloat("rangeZ", this.range.z());

        super.writeNbt(nbt);
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    public void sync() {
        assert world != null;
        if (!world.isClient()) {
            // updates comparators and marks dirty
            this.markDirty();
        }
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new MobAttractorScreenHandler(syncId, inv, this.energyAmountPropertyDelegate);
    }

    public void setRange(Vector3f newRange) {
        this.range = newRange;
    }

    public int getCooldown() {
        return this.attractorCooldown;
    }
}
