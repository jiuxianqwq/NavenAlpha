package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.mixin.O.accessors.MinecraftAccessor;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.MathUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.HitResult.Type;

@ModuleInfo(
        name = "AutoClicker",
        cnName = "自动点击",
        description = "Automatically clicks for you",
        category = Category.COMBAT
)
public class AutoClicker extends Module {
    private final FloatValue cps = ValueBuilder.create(this, "Min CPS")
            .setDefaultFloatValue(12.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(20.0F)
            .build()
            .getFloatValue();
    private final FloatValue maxCps = ValueBuilder.create(this, "Max CPS")
            .setDefaultFloatValue(18.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(20.0F)
            .build()
            .getFloatValue();
    private final BooleanValue itemCheck = ValueBuilder.create(this, "Item Check").setDefaultBooleanValue(false).build().getBooleanValue();
    private long lastAttackTime = 0;

    @EventTarget
    public void onMotion(EventMotion e) {
        if (e.getType() == EventType.PRE) {
            MinecraftAccessor accessor = (MinecraftAccessor) mc;
            Item item = mc.player.getMainHandItem().getItem();
            if (mc.options.keyAttack.isDown()
                    && (item instanceof SwordItem || item instanceof AxeItem || !this.itemCheck.getCurrentValue())
                    && mc.hitResult.getType() != Type.BLOCK) {
                long time = System.currentTimeMillis();
                double baseDelay = 1000.0 / MathUtils.getRandomIntInRange((int)cps.getCurrentValue(), (int)maxCps.getCurrentValue());

                long delay = (long) (baseDelay + (Math.random() - 0.5) * baseDelay * 0.4);

                if (time - lastAttackTime >= delay) {
                    accessor.setMissTime(0);
                    KeyMapping.click(mc.options.keyAttack.getKey());
                    lastAttackTime = time;
                }
            }
        }
    }
}
