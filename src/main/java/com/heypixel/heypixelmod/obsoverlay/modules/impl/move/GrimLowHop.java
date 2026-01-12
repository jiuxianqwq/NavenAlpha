package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMoveInput;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdate;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.combat.KillAura;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.MathUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.MoveUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.NetworkUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.Rotation;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationManager;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * @Author：jiuxian_baka
 * @Date：2026/1/10 02:49
 * @Filename：GrimSpeed
 */

@ModuleInfo(
        name = "GrimLowHop", cnName = "严峻的嘻嘻哈哈", description = "Movement speed adjustments with Grim modes", category = Category.MOVEMENT)
public class GrimLowHop extends Module {

    private final BooleanValue logging = ValueBuilder.create(this, "Logging").setDefaultBooleanValue(false).build().getBooleanValue();

    private final FloatValue startTicks = ValueBuilder.create(this, "Start Tick")
            .setDefaultFloatValue(2f)
            .setMinFloatValue(0F)
            .setMaxFloatValue(10F)
            .setFloatStep(1F)
            .build()
            .getFloatValue();
    private final FloatValue skipTicks = ValueBuilder.create(this, "Skip Ticks")
            .setDefaultFloatValue(2f)
            .setMinFloatValue(1F)
            .setMaxFloatValue(10F)
            .setFloatStep(1F)
            .build()
            .getFloatValue();
    private final FloatValue ticks = ValueBuilder.create(this, "Ticks")
            .setDefaultFloatValue(3f)
            .setMinFloatValue(1F)
            .setMaxFloatValue(10F)
            .setFloatStep(1F)
            .build()
            .getFloatValue();

    private void log(String message) {
        if (this.logging.getCurrentValue()) {
            ChatUtils.addChatMessage(message);
        }
    }

    private int airTicks;
    private boolean canTimer;
    private boolean timer;

    @Override
    public void onEnable() {
        airTicks = 0;
        canTimer = false;
    }

    @Override
    public void onDisable() {
        airTicks = 0;
        canTimer = false;
    }

    @EventTarget
    public void onPreTick(EventRunTicks event) {
        if (mc.player == null || event.type() != EventType.PRE) return;

        if (airTicks >= startTicks.getCurrentValue() && !canTimer) {
            Naven.skipTicks += skipTicks.getCurrentValue();
            canTimer = true;
            timer = false;
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player.onGround()) {
            airTicks = 0;
            canTimer = false;
        } else airTicks++;
        if (canTimer && !timer) {
            timer = true;
            for (int i = 0; i < (int) ticks.getCurrentValue(); i++) {
                mc.player.tick();
            }
        }
    }

    @EventTarget
    public void onMoveInput(EventMoveInput event) {
        if (mc.player == null || mc.level == null) {
            return;
        }

        event.setJump(true);
    }


}
