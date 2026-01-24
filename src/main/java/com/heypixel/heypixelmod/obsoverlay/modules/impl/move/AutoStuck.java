package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.TimeHelper;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(
        name = "AutoStuck",
        cnName = "自动卡空",
        description = "Automatically enable stuck when you over void",
        category = Category.MOVEMENT
)
public class AutoStuck extends Module {
    private final FloatValue fallDistance = ValueBuilder.create(this, "Fall Distance")
            .setDefaultFloatValue(10.0F)
            .setFloatStep(0.1F)
            .setMinFloatValue(3.0F)
            .setMaxFloatValue(15.0F)
            .build()
            .getFloatValue();
    private final TimeHelper timer = new TimeHelper();

    @EventTarget
    public void onMotion(EventMotion e) {
        if (e.getType() != EventType.PRE || mc.player == null || mc.level == null) {
            return;
        }

        Stuck stuck = Naven.getInstance().getModuleManager().getModule(Stuck.class);
        if (stuck == null) {
            return;
        }

        if (stuck.isEnabled()) {
            timer.reset();
        }

        int pearlSlot = -1;
        for (int i = 0; i < 9; i++) {
            if (!mc.player.getInventory().getItem(i).isEmpty()
                    && mc.player.getInventory().getItem(i).getItem() == Items.ENDER_PEARL) {
                pearlSlot = i;
                break;
            }
        }

        boolean shouldTrigger = ((pearlSlot != -1 && mc.player.fallDistance > fallDistance.getCurrentValue())
                || (mc.player.getY() + mc.player.getDeltaMovement().y < -50.0))
                && isOverVoid()
                && !mc.player.onGround()
                && timer.delay(1000.0);

        if (shouldTrigger && !stuck.isEnabled()) {
            stuck.toggle();
        }
    }

    @EventTarget
    public void onPacket(EventPacket e) {
        if (e.getPacket() instanceof ClientboundPlayerPositionPacket) {
            timer.reset();
        }
    }

    private boolean isOverVoid() {
        Vec3 start = mc.player.position();
        Vec3 end = new Vec3(start.x, mc.level.getMinBuildHeight() - 2.0, start.z);
        HitResult hit = mc.level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        return hit.getType() == HitResult.Type.MISS;
    }
}
