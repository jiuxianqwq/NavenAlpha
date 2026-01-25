package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.mixin.O.accessors.MinecraftAccessor;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMoveInput;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.MathUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.TickTimeHelper;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.BlockItem;

@ModuleInfo(
        name = "Eagle",
        cnName = "安全蹲搭",
        description = "Legit trick to build faster. Auto-sneak near edges.",
        category = Category.MOVEMENT
)
public class Eagle extends Module {
    private final FloatValue delay = ValueBuilder.create(this, "MinDelay (Ticks)")
            .setDefaultFloatValue(2.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(10.0F)
            .build()
            .getFloatValue();
    private final FloatValue maxDelay = ValueBuilder.create(this, "MaxDelay (Ticks)")
            .setDefaultFloatValue(3.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(10.0F)
            .build()
            .getFloatValue();
    private final BooleanValue backwards = ValueBuilder.create(this, "OnlyBackwards")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    private final BooleanValue onlyWithBlocks = ValueBuilder.create(this, "OnlyWithBlocks")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    private final BooleanValue fastPlace = ValueBuilder.create(this, "FastPlace")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    private final FloatValue cps = ValueBuilder.create(this, "CPS")
            .setDefaultFloatValue(10.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(5.0F)
            .setMaxFloatValue(20.0F)
            .setVisibility(fastPlace::getCurrentValue)
            .build()
            .getFloatValue();

    private static final TickTimeHelper timer = new TickTimeHelper();
    private float forward = 0;
    private boolean canFast;
    private float counter = 0.0F;

    public static boolean isOnBlockEdge(float sensitivity) {
        return !mc.level
                .getCollisions(mc.player, mc.player.getBoundingBox().move(0.0, -0.5, 0.0).inflate(-sensitivity, 0.0, -sensitivity))
                .iterator()
                .hasNext();
    }

    @EventTarget
    public void onMoveInput(EventMoveInput event) {
        LocalPlayer player = mc.player;
        if (player == null) return;

        if (event.getForward() != 0) this.forward = event.getForward();

        if (backwards.getCurrentValue() && forward > 0) {
            canFast = false;
            return;
        }

        if (onlyWithBlocks.getCurrentValue() &&
                (mc.player.getMainHandItem().isEmpty() ||
                        !(mc.player.getMainHandItem().getItem() instanceof BlockItem))) {
            canFast = false;
            return;
        }
        canFast = true;

        if (!player.onGround()) {
            return;
        }

        boolean closeToEdge = isOnBlockEdge(0.3F);

        if (!player.getAbilities().flying && closeToEdge) {
            timer.reset();
        }
        if (!timer.delay(MathUtils.getRandomIntInRange((int)maxDelay.getCurrentValue(), (int)delay.getCurrentValue()))) {
            event.setSneak(true);
        }
    }

    @EventTarget
    public void onMotion(EventMotion e) {
        if (!canFast && timer.delay(MathUtils.getRandomIntInRange((int)maxDelay.getCurrentValue(), (int)delay.getCurrentValue()))) return;
        if (e.getType() == EventType.PRE) {
            MinecraftAccessor accessor = (MinecraftAccessor) mc;
            if (mc.options.keyUse.isDown() && mc.player.getMainHandItem().getItem() instanceof BlockItem) {
                this.counter = this.counter + this.cps.getCurrentValue() / 20.0F;
                if (this.counter >= 1.0F / this.cps.getCurrentValue()) {
                    accessor.setRightClickDelay(0);
                    this.counter--;
                }
            } else {
                this.counter = 0.0F;
            }
        }
    }

    @Override
    public void onEnable() {
        forward = 0;
    }

    @Override
    public void onDisable() {
        forward = 0;
    }
}


