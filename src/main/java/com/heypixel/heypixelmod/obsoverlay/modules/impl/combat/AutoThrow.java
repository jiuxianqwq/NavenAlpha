package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.ClientFriend;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.Target;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.Teams;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Scaffold;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Stuck;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.combat.AntiBots;
import com.heypixel.heypixelmod.obsoverlay.utils.FriendManager;
import com.heypixel.heypixelmod.obsoverlay.utils.PacketUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.TimeHelper;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.Rotation;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationManager;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.SplashPotionItem;

import java.util.Comparator;
import java.util.Optional;

@ModuleInfo(
        name = "AutoThrow",
        cnName = "自动投掷",
        description = "Automatically throw snowballs and eggs.",
        category = Category.COMBAT
)
public class AutoThrow extends Module {
    private final FloatValue minDistance = ValueBuilder.create(this, "Min Distance").setDefaultFloatValue(5).setFloatStep(1).setMinFloatValue(3).setMaxFloatValue(30).build().getFloatValue();
    private final FloatValue maxDistance = ValueBuilder.create(this, "Max Distance").setDefaultFloatValue(10).setFloatStep(1).setMinFloatValue(3).setMaxFloatValue(30).build().getFloatValue();
    private final FloatValue delay = ValueBuilder.create(this, "Delay").setDefaultFloatValue(500).setFloatStep(50).setMinFloatValue(50).setMaxFloatValue(2000).build().getFloatValue();
    private final TimeHelper timer = new TimeHelper();
    private Rotation rotation;
    private int rotationSet;
    private int swapBack = -1;

    @EventTarget
    public void onMotion(EventMotion e) {
        if (e.getType() == EventType.PRE) {
            if (mc.player == null || mc.level == null) {
                return;
            }

            if (Naven.getInstance().getModuleManager().getModule(Scaffold.class).isEnabled()
                    || Naven.getInstance().getModuleManager().getModule(Stuck.class).isEnabled()) {
                rotationSet = 0;
                return;
            }

            rotation = null;

            int throwableHotbar = findThrowableHotbar();
            if (throwableHotbar != -1) {
                if (rotationSet > 0) {
                    rotationSet--;
                    if (rotationSet == 0) {
                        int originalHotbar = mc.player.getInventory().selected;
                        boolean shouldSwap = originalHotbar != throwableHotbar;
                        if (shouldSwap) {
                            mc.getConnection().send(new ServerboundSetCarriedItemPacket(throwableHotbar));
                            swapBack = originalHotbar;
                        }
                        PacketUtils.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id));
                    }
                } else {
                    Optional<AbstractClientPlayer> target = getTarget();
                    if (target.isPresent() && timer.delay(delay.getCurrentValue())) {
                        if (canRotate()) {
                            rotation = getRotationToEntity(target.get());
                            RotationManager.setRotations(rotation, 180.0);
                            rotationSet = 2;
                            timer.reset();
                        }
                    }
                }
            }
        } else if (swapBack != -1) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(swapBack));
            swapBack = -1;
        }
    }

    private int findThrowableHotbar() {
        for (int hotbar = 0; hotbar < 9; hotbar++) {
            ItemStack stack = mc.player.getInventory().items.get(hotbar);
            if (!stack.isEmpty() && (stack.getItem() == Items.EGG || stack.getItem() == Items.SNOWBALL)) {
                return hotbar;
            }
        }
        return -1;
    }

    private boolean canRotate() {
        if (mc.player.isUsingItem()) {
            return false;
        }
        ItemStack stack = mc.player.getMainHandItem();
        if (stack.isEmpty()) {
            return true;
        }
        Item item = stack.getItem();
        if (item instanceof EnderpearlItem) {
            return false;
        }
        if (item instanceof BowItem) {
            return false;
        }
        if (item instanceof PotionItem || item instanceof SplashPotionItem || item instanceof LingeringPotionItem) {
            return false;
        }
        return !item.isEdible();
    }

    private Rotation getRotationToEntity(LivingEntity target) {
        double distanceToEnt = mc.player.distanceTo(target);
        double predictX = target.getX() + (target.getX() - target.xo) * (distanceToEnt * 0.8f);
        double predictY = target.getY() + (target.getY() - target.yo) * (distanceToEnt * 0.8f);
        double predictZ = target.getZ() + (target.getZ() - target.zo) * (distanceToEnt * 0.8f);

        double x = predictX - mc.player.getX();
        double h = predictY + 1.2 - (mc.player.getY() + mc.player.getEyeHeight());
        double z = predictZ - mc.player.getZ();

        double h1 = Math.sqrt(x * x + z * z);

        float yaw = (float) (Math.toDegrees(Math.atan2(z, x)) - 90.0F);
        float pitch = -getTrajAngleSolutionLow((float) h1, (float) h, 0.6f, 0.006f);
        return new Rotation(yaw, Mth.clamp(pitch, -90.0F, 90.0F));
    }

    private float getTrajAngleSolutionLow(float distance, float height, float velocity, float gravity) {
        float v2 = velocity * velocity;
        float under = v2 * v2 - gravity * (gravity * distance * distance + 2.0f * height * v2);
        if (under <= 0.0f) {
            return (float) Math.toDegrees(Math.atan2(height, distance));
        }
        return (float) Math.toDegrees(Math.atan((v2 - Math.sqrt(under)) / (gravity * distance)));
    }

    private Optional<AbstractClientPlayer> getTarget() {
        Target targetModule = Naven.getInstance().getModuleManager().getModule(Target.class);
        return mc.level.players().stream()
                .filter(e -> e != mc.player)
                .filter(LivingEntity::isAlive)
                .filter(e -> !e.isSpectator())
                .filter(e -> !AntiBots.isBot(e))
                .filter(e -> targetModule == null || targetModule.isTarget(e))
                .filter(e -> !Teams.isSameTeam(e))
                .filter(e -> !FriendManager.isFriend(e))
                .filter(e -> !ClientFriend.isUser(e))
                .filter(mc.player::hasLineOfSight)
                .filter(e -> !e.isInvisibleTo(mc.player))
                .filter(e -> {
                    double dist = getHorizontalDistance(e);
                    return dist <= maxDistance.getCurrentValue() && dist >= minDistance.getCurrentValue();
                })
                .min(Comparator.comparingDouble(e -> mc.player.distanceTo(e)));
    }

    private double getHorizontalDistance(LivingEntity entity) {
        double dx = entity.getX() - mc.player.getX();
        double dz = entity.getZ() - mc.player.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }
}
