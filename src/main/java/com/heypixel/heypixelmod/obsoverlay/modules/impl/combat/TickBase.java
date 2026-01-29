package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderAfterWorld;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.ClientFriend;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.Target;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.Teams;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.FriendManager;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

@ModuleInfo(
        name = "TickBase",
        cnName = "Tick基",
        description = "Manipulates client ticks for combat timing",
        category = Category.COMBAT
)
public class TickBase extends Module {
    private final FloatValue lagRange = ValueBuilder.create(this, "Range")
            .setDefaultFloatValue(8.0F)
            .setFloatStep(0.1F)
            .setMinFloatValue(3.0F)
            .setMaxFloatValue(15.0F)
            .build()
            .getFloatValue();
    private final BooleanValue debug = ValueBuilder.create(this, "Debug")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    private Mode mode = Mode.NONE;
    private long time;
    private long balance;
    private double range;
    private double distance;
    private boolean boostTick;
    private Entity target;

    @Override
    public void onEnable() {
        resetState();
    }

    @Override
    public void onDisable() {
        resetState();
    }

    @EventTarget
    public void onPreTick(EventRunTicks event) {
        if (event.type() != EventType.PRE || mc.player == null || mc.level == null) return;
        if (mode == Mode.REDUCING) return;

        target = findTarget(20.0);
        if (target == null) return;

        distance = RotationUtils.getDistance(target);
        double currentRange = distance;

        double catchupDistance = Math.max(2.0, lagRange.getCurrentValue() - 1.0);
        if (currentRange > catchupDistance && balance >= 50 && mode == Mode.BASING) {
            balance -= 50;
            boostTick = true;
        } else {
            balance = 0;
            mode = Mode.NONE;
            boostTick = false;
        }

        if (currentRange <= lagRange.getCurrentValue() && this.range > lagRange.getCurrentValue() && mode == Mode.NONE) {
            mode = Mode.REDUCING;
            time = System.currentTimeMillis();
            balance = 0;
            boostTick = false;
            log(String.format("TickBase REDUCING range=%.2f prev=%.2f limit=%.2f", currentRange, this.range, lagRange.getCurrentValue()));
        }

        this.range = currentRange;
    }

    @EventTarget
    public void onRenderAfterWorld(EventRenderAfterWorld event) {
        if (mc.player == null || mc.level == null) return;

        if (mode != Mode.REDUCING) {
            Naven.TICK_TIMER = boostTick ? 2.0F : 1.0F;
            boostTick = false;
            return;
        }

        if (target == null || !target.isAlive()) {
            mode = Mode.NONE;
            balance = 0;
            Naven.TICK_TIMER = 1.0F;
            boostTick = false;
            log("TickBase target lost");
            return;
        }

        distance = RotationUtils.getDistance(target);
        double speedDiv = mc.player.hasEffect(MobEffects.MOVEMENT_SPEED) ? 0.36 : 0.25;
        double stopDistance = Math.max(2.0, lagRange.getCurrentValue() - 1.0);
        if (distance <= stopDistance || System.currentTimeMillis() - time >= ((range / speedDiv) * 25) + 25) {
            Naven.TICK_TIMER = 1.0F;
            mode = Mode.BASING;
            balance = System.currentTimeMillis() - time;
            log(String.format("TickBase BASING balance=%d", balance));
            return;
        }

        Naven.TICK_TIMER = 0.0F;
    }

    private void resetState() {
        mode = Mode.NONE;
        time = 0L;
        balance = 0L;
        range = Double.MAX_VALUE;
        distance = 0.0;
        boostTick = false;
        target = null;
        Naven.TICK_TIMER = 1.0F;
    }

    private void log(String message) {
        if (debug.getCurrentValue()) {
            ChatUtils.addChatMessage(message);
        }
    }

    private Entity findTarget(double range) {
        if (mc.player == null || mc.level == null) return null;

        double rangeSq = range * range;
        Entity closest = null;
        double closestDist = Double.MAX_VALUE;
        Target targetModule = Naven.getInstance().getModuleManager().getModule(Target.class);

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity)) continue;
            if (entity == mc.player) continue;
            if (!entity.isAlive()) continue;
            if (entity.isSpectator()) continue;
            if (AntiBots.isBot(entity)) continue;
            if (targetModule != null && !targetModule.isTarget(entity)) continue;
            if (Teams.isSameTeam(entity)) continue;
            if (FriendManager.isFriend(entity)) continue;
            if (ClientFriend.isUser(entity)) continue;

            double distSq = mc.player.distanceToSqr(entity);
            if (distSq <= rangeSq && distSq < closestDist) {
                closestDist = distSq;
                closest = entity;
            }
        }

        return closest;
    }

    private enum Mode {
        REDUCING,
        BASING,
        NONE
    }
}
