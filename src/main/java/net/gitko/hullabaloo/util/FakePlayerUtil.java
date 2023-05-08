package net.gitko.hullabaloo.util;

import com.mojang.authlib.GameProfile;
import dev.cafeteria.fakeplayerapi.server.FakePlayerBuilder;
import dev.cafeteria.fakeplayerapi.server.FakeServerPlayer;
import dev.cafeteria.fakeplayerapi.server.FakeServerPlayerFactory;
import net.gitko.hullabaloo.Hullabaloo;
import net.gitko.hullabaloo.entity.EntityEquipmentChanges;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

public class FakePlayerUtil {
    public static FakePlayerBuilder createFakePlayerBuilder() {
        return new FakePlayerBuilder(new Identifier(Hullabaloo.MOD_ID, "fake_player"), (new FakeServerPlayerFactory() {
            @Override
            public FakeServerPlayer create(FakePlayerBuilder builder, MinecraftServer server, ServerWorld world, GameProfile profile) {
                return new FakeServerPlayer(builder, server, world, profile) {
                    @Override
                    public boolean isCreative() {
                        return false;
                    }

                    @Override
                    public boolean isSpectator() {
                        return false;
                    }

                    @Override
                    public void attack(Entity target) {
                        // IMPORTANT: GENERIC_ATTACK_DAMAGE NOT being set automatically by game, so using mixin for LivingEntity to allow me
                        // to run sendEquipmentChanges() to update the GENERIC_ATTACK_DAMAGE

                        // last attack ticks is set to 1000 just to ensure MC thinks it has been a long time since the block activator has last attacked
                        this.lastAttackedTicks = 1000;

                        ((EntityEquipmentChanges) this).gitko_sendEquipmentChanges();
                        // if you open this file up, the line above gives an error about this not being castable to EntityEquipmentChanges.
                        // the only reason it is attempting to cast to it is to allow me to run the mod :D
                        // otherwise, it gives me a no method found error which keeps me from running the mod.
                        // the no method found error doesn't matter when the mod is in production bc the method is added
                        // by a mixin which IntelliJ Idea has no clue about

                        super.attack(target);
                    }
                };
            }
        }));
    }
}
