package net.gitko.hullabaloo.mixin;

import net.gitko.hullabaloo.entity.EntityEquipmentChanges;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements EntityEquipmentChanges {

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow
    protected abstract void sendEquipmentChanges();

    @Override
    public void gitko_sendEquipmentChanges() {
        // Without this, the fake player's generic attack damage doesn't update!
        // So, this is VERY IMPORTANT!!!!
        sendEquipmentChanges();
    }
}