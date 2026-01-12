package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventClick;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RayCastUtil;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.Rotation;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationManager;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

@ModuleInfo(
        name = "CrystalAura",
        cnName = "水晶光环",
        category = Category.COMBAT,
        description = "Automatically attacks end crystals"
)
public class AttackCrystal extends Module {
    public Entity entity;

    @EventTarget
    public void onEarlyTick(EventRunTicks e) {
        if (e.type() == EventType.PRE && mc.player != null && mc.level != null) {
            double range = 4.0;
            double rangeSq = range * range;

            Entity closestCrystal = null;
            double closestDistSq = Double.MAX_VALUE;

            AABB searchBox = mc.player.getBoundingBox().inflate(range);
            List<EndCrystal> crystals = mc.level.getEntitiesOfClass(EndCrystal.class, searchBox);

            for (EndCrystal crystal : crystals) {
                if (!crystal.isAlive()) continue;

                double distSq = mc.player.distanceToSqr(crystal);

                if (distSq < closestDistSq && distSq <= rangeSq) {
                    closestDistSq = distSq;
                    closestCrystal = crystal;
                }
            }

            if (closestCrystal != null) {
                Rotation targetRot = RotationUtils.calculate(closestCrystal);
                HitResult hitResult = RayCastUtil.rayCast(targetRot, 3.0);
                if (hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity().equals(closestCrystal)) {
                    RotationManager.setRotations(targetRot, 180);
                    this.entity = closestCrystal;
                }
            }
        }
    }

    @EventTarget
    public void onClick(EventClick e) {
        if (this.entity != null) {
            HitResult hitResult = mc.hitResult;
            if (hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity().equals(entity)) {
                mc.getConnection().send(ServerboundInteractPacket.createAttackPacket(this.entity, false));
                mc.player.swing(InteractionHand.MAIN_HAND);
                this.entity = null;
            }
        }
    }
}
