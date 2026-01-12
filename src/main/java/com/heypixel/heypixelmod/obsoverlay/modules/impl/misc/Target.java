package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;

/**
 * @Author：jiuxian_baka
 * @Date：2025/12/23 13:25
 * @Filename：Target
 */
@ModuleInfo(
        name = "Target",
        cnName = "目标",
        description = "Prevent attack teammates",
        category = Category.MISC
)
public class Target extends Module {

    BooleanValue player = ValueBuilder.create(this, "Player").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue invisibles = ValueBuilder.create(this, "Invisibles").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue animals = ValueBuilder.create(this, "Animals").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue mobs = ValueBuilder.create(this, "Mobs").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue villager = ValueBuilder.create(this, "Villager").setDefaultBooleanValue(true).build().getBooleanValue();


    @Override
    public void onEnable() {
        this.setEnabled(false);
    }

    public boolean isTarget(Entity entity) {
        if (!(entity instanceof LivingEntity)) return false;
        if (entity == mc.player) return false;
        if (entity instanceof Player && entity.isSpectator()) return false;
        if (!entity.isAlive()) return false;

        if (entity.isInvisible() && !invisibles.getCurrentValue()) {
            return false;
        }

        if (entity instanceof Player) {
            return player.getCurrentValue();
        }

        if (entity instanceof AbstractVillager) {
            return villager.getCurrentValue();
        }

        if (entity instanceof Monster || entity instanceof Slime || entity instanceof EnderDragon || entity instanceof EnderDragonPart) {
            return mobs.getCurrentValue();
        }

        if (entity instanceof Animal || entity instanceof AmbientCreature || entity instanceof WaterAnimal || entity instanceof AbstractHorse) {
            return animals.getCurrentValue();
        }

        return false;
    }
}