package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.ClientFriend;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.Target;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.Teams;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Blink;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Scaffold;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.DynamicIslandHud;
import com.heypixel.heypixelmod.obsoverlay.utils.FriendManager;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RayCastUtil;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.Rotation;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationManager;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.awt.*;
import java.util.List;

/**
 * @Author：jiuxian_baka
 * @Date：2025/12/23 11:32
 * @Filename：KillAura
 */

@ModuleInfo(
        name = "KillAura",
        cnName = "杀戮光环",
        description = "Automatically attacks entities",
        category = Category.COMBAT
)
public class KillAura extends Module {
    public Entity target;
    FloatValue aimRange = ValueBuilder.create(this, "Aim Range")
            .setDefaultFloatValue(5.0F)
            .setFloatStep(0.1F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(6.0F)
            .build()
            .getFloatValue();
    BooleanValue mode19 = ValueBuilder.create(this, "1.9 Mode")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    FloatValue cps = ValueBuilder.create(this, "CPS")
            .setDefaultFloatValue(10.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(20.0F)
            .setVisibility(() -> !mode19.getCurrentValue())
            .build()
            .getFloatValue();
    FloatValue rotateSpeed = ValueBuilder.create(this, "Rotation Speed")
            .setDefaultFloatValue(180.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(180.0F)
            .build()
            .getFloatValue();
    FloatValue fov = ValueBuilder.create(this, "FOV")
            .setDefaultFloatValue(360.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(360.0F)
            .build()
            .getFloatValue();
    private List<Entity> targets;
    private long lastAttackTime = 0;

    @EventTarget
    public void onPreTick(EventRunTicks event) {
        boolean scaffoldEnable = Naven.getInstance().getModuleManager().getModule(Scaffold.class).isEnabled();
        boolean blinkEnable = Naven.getInstance().getModuleManager().getModule(Blink.class).isEnabled();
        AttackCrystal attackCrystal = Naven.getInstance().getModuleManager().getModule(AttackCrystal.class);
        if (mc.player == null || mc.level == null || event.type() != EventType.PRE || scaffoldEnable || attackCrystal.entity != null || blinkEnable)
            return;

        findTarget();

        if (target != null) {
            Rotation calculate = RotationUtils.calculate(target);
            RotationManager.setRotations(calculate, rotateSpeed.getCurrentValue(), rotation -> {
                HitResult hitResult = RayCastUtil.rayCast(rotation, 3.0);
                return hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity().equals(target);
            });
            HitResult hitResult = mc.hitResult;
            if (hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity().equals(target))
                attackTarget();
        }
    }

    private void attackTarget() {
        if (mode19.getCurrentValue()) {
            if (mc.player.getAttackStrengthScale(0.0F) >= 1.0F) {
                mc.gameMode.attack(mc.player, target);
                mc.player.swing(InteractionHand.MAIN_HAND);
                if (target instanceof Player player) {
                    DynamicIslandHud.onPlayerAttack(player);
                }
            }
            return;
        }
        long time = System.currentTimeMillis();
        double baseDelay = 1000.0 / cps.getCurrentValue();

        long delay = (long) (baseDelay + (Math.random() - 0.5) * baseDelay * 0.4);

        if (time - lastAttackTime >= delay) {
            mc.gameMode.attack(mc.player, target);
            mc.player.swing(InteractionHand.MAIN_HAND);
            if (target instanceof Player player) {
                DynamicIslandHud.onPlayerAttack(player);
            }

            lastAttackTime = time;
        }
    }

    private void findTarget() {
        float range = aimRange.getCurrentValue();
        double rangeSq = range * range;
        float currentFov = fov.getCurrentValue();

        this.target = null;
        double minDstSq = Double.MAX_VALUE;

        AABB searchBox = mc.player.getBoundingBox().inflate(range);

        List<Entity> candidates = mc.level.getEntities(
                mc.player,
                searchBox,
                e -> e instanceof LivingEntity
                        && e != mc.player
                        && e.isAlive()
                        && !e.isSpectator()
                        && !AntiBots.isBot(e)
                        && Naven.getInstance().getModuleManager().getModule(Target.class).isTarget(e)
                        && !Teams.isSameTeam(e)
                        && !FriendManager.isFriend(e)
                        && !ClientFriend.isUser(e)
                        && Math.abs(Mth.wrapDegrees(RotationUtils.calculate(e).getYaw() - mc.player.getYRot())) <= currentFov
        );

        targets = candidates;

        for (Entity entity : candidates) {
            double distSq = mc.player.distanceToSqr(entity);

            if (distSq < minDstSq && distSq <= rangeSq) {
                minDstSq = distSq;
                this.target = entity;
            }
        }

        this.setSuffix(candidates.size() + " Targets");
    }

    public List<Entity> getTargets() {
        return targets;
    }

    @EventTarget
    public void onRender(EventRender event) {
        PoseStack poseStack = event.getPMatrixStack();
        for (Entity entity : targets) {
            if (entity.equals(target))
                RenderUtils.drawEntitySolidBox(poseStack, entity.getX(), entity.getY(), entity.getZ(), entity.getBbWidth(), entity.getBbHeight(), new Color(200, 0, 0, 60).getRGB());
            else
                RenderUtils.drawEntitySolidBox(poseStack, entity.getX(), entity.getY(), entity.getZ(), entity.getBbWidth(), entity.getBbHeight(), new Color(0, 200, 0, 60).getRGB());
        }
    }
}
