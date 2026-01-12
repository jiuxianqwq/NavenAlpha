package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.combat.KillAura;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * @Author：jiuxian_baka
 * @Date：2025/12/26 08:50
 * @Filename：Animations
 */

@ModuleInfo(name = "Animations", cnName = "动画", description = "Customizes item animations and block animations", category = Category.RENDER)
public class Animations extends Module {

    public static boolean isBlocking = false;
    public final ModeValue BlockMods = ValueBuilder.create(this, "Block Mods")
            .setModes("None", "1.7", "Push")
            .setDefaultModeIndex(1)
            .build()
            .getModeValue();

    public final BooleanValue auraAutoBlock = ValueBuilder.create(this, "Aura Auto Block")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public final FloatValue BlockingX = ValueBuilder.create(this, "Blocking-X")
            .setDefaultFloatValue(0.5F)
            .setMinFloatValue(-2.0F)
            .setMaxFloatValue(2.0F)
            .setFloatStep(0.05F)
            .build()
            .getFloatValue();

    public final FloatValue BlockingY = ValueBuilder.create(this, "Blocking-Y")
            .setDefaultFloatValue(-0.5F)
            .setMinFloatValue(-2.0F)
            .setMaxFloatValue(2.0F)
            .setFloatStep(0.05F)
            .build()
            .getFloatValue();
    private final Minecraft mc = Minecraft.getInstance();
    public BooleanValue onlyAura = ValueBuilder.create(this, "Only Aura")
            .setDefaultBooleanValue(false)
            .build().getBooleanValue();
    private boolean flip;
    private float mainHandHeight = 0.0F;
    private float offHandHeight = 0.0F;
    private float oMainHandHeight = 0.0F;
    private float oOffHandHeight = 0.0F;
    private ItemStack mainHandItem = ItemStack.EMPTY;
    private ItemStack offHandItem = ItemStack.EMPTY;

    @Override
    public void onEnable() {
        super.onEnable();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    public void onRenderHand(RenderHandEvent event) {
        if (!this.isEnabled() || BlockMods.getCurrentMode().equals("None"))
            return;

        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getItemStack().getItem() instanceof SwordItem))
            return;

        ItemStack serverMainHandItem = mc.player.getMainHandItem();
        if (!(serverMainHandItem.getItem() instanceof SwordItem)) {
            return;
        }

        boolean isOffhandUsing = false;
        if (mc.player.isUsingItem() && mc.player.getUsedItemHand() == InteractionHand.OFF_HAND) {
            ItemStack offhandItem = mc.player.getOffhandItem();
            UseAnim useAnim = offhandItem.getUseAnimation();
            if (useAnim != UseAnim.BLOCK) {
                isOffhandUsing = true;
            }
        }

        boolean isKillauraBlocking = auraAutoBlock.getCurrentValue() && getAuraTarget() != null;

        if (onlyAura.getCurrentValue() && !isKillauraBlocking) {
            return;
        }

        if (isOffhandUsing && !isKillauraBlocking)
            return;

        if (!mc.options.keyUse.isDown() && !isKillauraBlocking)
            return;

        event.setCanceled(true);

        renderArmWithItem(
                mc.player,
                event.getPartialTick(),
                event.getEquipProgress(),
                event.getHand(),
                event.getSwingProgress(),
                event.getItemStack(),
                event.getEquipProgress(),
                event.getPoseStack(),
                event.getMultiBufferSource(),
                event.getPackedLight());
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (event.getType() == EventType.SEND && event.getPacket() instanceof ServerboundSwingPacket) {
            flip = !flip;
        }
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        if (event.getType() != EventType.PRE || mc.player == null)
            return;

        updateHandStates();
    }

    private void updateHandStates() {
        oMainHandHeight = mainHandHeight;
        oOffHandHeight = offHandHeight;

        LocalPlayer localplayer = mc.player;
        ItemStack itemstack = localplayer.getMainHandItem();
        ItemStack itemstack1 = localplayer.getOffhandItem();
        boolean isBlocking = isBlocking();

        if (isBlocking) {
            mainHandHeight = 1.0F;
            if (ItemStack.matches(mainHandItem, itemstack)) {
                mainHandItem = itemstack;
            }
            if (ItemStack.matches(offHandItem, itemstack1)) {
                offHandItem = itemstack1;
            }
            return;
        }

        if (localplayer.isHandsBusy()) {
            mainHandHeight = Mth.clamp(mainHandHeight - 0.4F, 0.0F, 1.0F);
            offHandHeight = Mth.clamp(offHandHeight - 0.4F, 0.0F, 1.0F);
        } else {
            float f = localplayer.getAttackStrengthScale(1.0F);
            boolean flag = ForgeHooksClient.shouldCauseReequipAnimation(mainHandItem, itemstack,
                    localplayer.getInventory().selected);
            boolean flag1 = ForgeHooksClient.shouldCauseReequipAnimation(offHandItem, itemstack1, -1);

            if (!flag && mainHandItem != itemstack) {
                mainHandItem = itemstack;
            }

            if (!flag1 && offHandItem != itemstack1) {
                offHandItem = itemstack1;
            }
            float targetMainHeight = !flag ? f * f * f : 0.0F;
            float targetOffHeight = !flag1 ? 1.0F : 0.0F;

            mainHandHeight += Mth.clamp(targetMainHeight - mainHandHeight, -0.2F, 0.2F);
            offHandHeight += Mth.clamp(targetOffHeight - offHandHeight, -0.2F, 0.2F);
        }

        if (mainHandHeight < 0.1F) {
            mainHandItem = itemstack;
        }

        if (offHandHeight < 0.1F) {
            offHandItem = itemstack1;
        }
    }

    private boolean isBlocking() {
        if (!this.isEnabled() || BlockMods.getCurrentMode().equals("None"))
            return false;

        LocalPlayer player = mc.player;
        if (player == null)
            return false;

        ItemStack serverMainHandItem = player.getMainHandItem();
        if (!(serverMainHandItem.getItem() instanceof SwordItem))
            return false;

        ItemStack mainHandItem = player.getMainHandItem();
        if (!(mainHandItem.getItem() instanceof SwordItem))
            return false;

        boolean isOffhandUsing = false;
        if (player.isUsingItem() && player.getUsedItemHand() == InteractionHand.OFF_HAND) {
            ItemStack offhandItem = player.getOffhandItem();
            UseAnim useAnim = offhandItem.getUseAnimation();
            if (useAnim != UseAnim.BLOCK) {
                isOffhandUsing = true;
            }
        }

        boolean isKillauraBlocking = auraAutoBlock.getCurrentValue() && getAuraTarget() != null;

        if (onlyAura.getCurrentValue()) {
            return isKillauraBlocking;
        }

        if (isKillauraBlocking) {
            return true;
        }

        if (isOffhandUsing) {
            return false;
        }

        return mc.options.keyUse.isDown();
    }

    private void renderArmWithItem(
            AbstractClientPlayer player,
            float partialTicks,
            float equipProgress,
            InteractionHand interactionHand,
            float swingProgress,
            ItemStack itemStack,
            float equippedProg,
            PoseStack poseStack,
            MultiBufferSource multiBufferSource,
            int light) {
        if (!player.isScoping()) {
            boolean flag = interactionHand == InteractionHand.MAIN_HAND;
            HumanoidArm humanoidarm = flag ? player.getMainArm() : player.getMainArm().getOpposite();
            Animations animations = this;
            poseStack.pushPose();

            {
                if (itemStack.isEmpty()) {
                    if (flag && !player.isInvisible()) {
                        renderPlayerArm(poseStack, multiBufferSource, light, equippedProg, swingProgress, humanoidarm);
                    }
                } else if (itemStack.is(Items.FILLED_MAP)) {
                    if (flag && offHandItem.isEmpty()) {
                        renderTwoHandedMap(poseStack, multiBufferSource, light, equipProgress, equippedProg,
                                swingProgress);
                    } else {
                        renderOneHandedMap(poseStack, multiBufferSource, light, equippedProg, humanoidarm,
                                swingProgress, itemStack);
                    }
                } else {
                    boolean flag1 = itemStack.is(Items.CROSSBOW) && CrossbowItem.isCharged(itemStack);
                    int i = humanoidarm == HumanoidArm.RIGHT ? 1 : -1;
                    if (itemStack.is(Items.CROSSBOW)) {
                        if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0
                                && player.getUsedItemHand() == interactionHand) {
                            applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                            poseStack.translate((double) ((float) i * -0.4785682F), -0.094387F, 0.0573153F);
                            poseStack.mulPose(Axis.XP.rotation(-11.935F * (float) Math.PI / 180.0F));
                            poseStack.mulPose(Axis.YP.rotation((float) i * 65.3F * (float) Math.PI / 180.0F));
                            poseStack.mulPose(Axis.ZP.rotation((float) i * -9.785F * (float) Math.PI / 180.0F));
                            float f6 = (float) itemStack.getUseDuration()
                                    - ((float) player.getUseItemRemainingTicks() - partialTicks + 1.0F);
                            float f10 = f6 / (float) CrossbowItem.getChargeDuration(itemStack);
                            f10 = Math.min(f10, 1.0F);
                            if (f10 > 0.1F) {
                                float f14 = Mth.sin((f6 - 0.1F) * 1.3F);
                                float f20 = f10 - 0.1F;
                                float f25 = f14 * f20;
                                poseStack.translate(f25 * 0.0F, f25 * 0.004F,
                                        (double) (f25 * 0.0F));
                            }

                            poseStack.translate(f10 * 0.0F, f10 * 0.0F, (double) (f10 * 0.04F));
                            poseStack.scale(1.0F, 1.0F, 1.0F + f10 * 0.2F);
                            poseStack.mulPose(Axis.YP.rotation((float) i * -45.0F * (float) Math.PI / 180.0F));
                        } else {
                            float f5 = -0.4F * Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
                            float f9 = 0.2F * Mth.sin(Mth.sqrt(swingProgress) * (float) (Math.PI * 2));
                            float f13 = -0.2F * Mth.sin(swingProgress * (float) Math.PI);
                            poseStack.translate((float) i * f5, f9, (double) f13);
                            applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                            applyItemArmAttackTransform(poseStack, humanoidarm, swingProgress);
                            if (flag1 && swingProgress < 0.001F && flag) {
                                poseStack.translate((float) i * -0.641864F, 0.0, 0.0);
                                poseStack.mulPose(Axis.YP.rotation((float) i * 10.0F * (float) Math.PI / 180.0F));
                            }
                        }

                        renderItem(
                                player,
                                itemStack,
                                i == 1 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                                        : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                                i != 1,
                                poseStack,
                                multiBufferSource,
                                light);
                    } else {
                        boolean flag2 = humanoidarm == HumanoidArm.RIGHT;
                        if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0
                                && player.getUsedItemHand() == interactionHand) {
                            switch (itemStack.getUseAnimation()) {
                                case NONE:
                                case BLOCK:
                                    applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                                    break;
                                case EAT:
                                case DRINK:
                                    applyEatTransform(poseStack, partialTicks, humanoidarm, itemStack);
                                    applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                                    break;
                                case BOW:
                                    applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                                    poseStack.translate((double) ((float) i * -0.2785682F), 0.183444F, 0.1573153F);
                                    poseStack.mulPose(Axis.XP.rotation(-13.935F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.YP.rotation((float) i * 35.3F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.ZP.rotation((float) i * -9.785F * (float) Math.PI / 180.0F));
                                    float f8 = (float) itemStack.getUseDuration()
                                            - ((float) player.getUseItemRemainingTicks() - partialTicks + 1.0F);
                                    float f12 = f8 / 20.0F;
                                    f12 = (f12 * f12 + f12 * 2.0F) / 3.0F;
                                    f12 = Math.min(f12, 1.0F);
                                    if (f12 > 0.1F) {
                                        float f19 = Mth.sin((f8 - 0.1F) * 1.3F);
                                        float f24 = f12 - 0.1F;
                                        float f26 = f19 * f24;
                                        poseStack.translate(f26 * 0.0F, f26 * 0.004F,
                                                (double) (f26 * 0.0F));
                                    }

                                    poseStack.translate(f12 * 0.0F, f12 * 0.0F,
                                            (double) (f12 * 0.04F));
                                    poseStack.scale(1.0F, 1.0F, 1.0F + f12 * 0.2F);
                                    poseStack.mulPose(Axis.YP.rotation((float) i * -45.0F * (float) Math.PI / 180.0F));
                                    break;
                                case SPEAR:
                                    applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                                    poseStack.translate((double) ((float) i * -0.5F), 0.7F, 0.1F);
                                    poseStack.mulPose(Axis.XP.rotation(-55.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.YP.rotation((float) i * 35.3F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.ZP.rotation((float) i * -9.785F * (float) Math.PI / 180.0F));
                                    float f7 = (float) itemStack.getUseDuration()
                                            - ((float) player.getUseItemRemainingTicks() - partialTicks + 1.0F);
                                    float f11 = f7 / 10.0F;
                                    f11 = Math.min(f11, 1.0F);
                                    if (f11 > 0.1F) {
                                        float f18 = Mth.sin((f7 - 0.1F) * 1.3F);
                                        float f23 = f11 - 0.1F;
                                        float f4 = f18 * f23;
                                        poseStack.translate(f4 * 0.0F, f4 * 0.004F,
                                                (double) (f4 * 0.0F));
                                    }

                                    poseStack.translate(0.0, 0.0, f11 * 0.2F);
                                    poseStack.scale(1.0F, 1.0F, 1.0F + f11 * 0.2F);
                                    poseStack.mulPose(Axis.YP.rotation((float) i * -45.0F * (float) Math.PI / 180.0F));
                            }
                        } else if ((player.isUsingItem()
                                || Minecraft.getInstance().options.keyUse.isDown()
                                || animations.auraAutoBlock.getCurrentValue() && getAuraTarget() != null)
                                && player.getMainHandItem().getItem() instanceof SwordItem
                                && !animations.BlockMods.getCurrentMode().equals("None")) {
                            String s = animations.BlockMods.getCurrentMode().toLowerCase();
                            switch (s) {
                                case "1.7":
                                    poseStack.translate((float) i * BlockingX.getCurrentValue(),
                                            (double) (BlockingY.getCurrentValue()), -0.72F);
                                    float f17 = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
                                    float f22 = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
                                    poseStack.mulPose(Axis.YP
                                            .rotation((float) i * (45.0F + f17 * -20.0F) * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(
                                            Axis.ZP.rotation((float) i * f22 * -10.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.XP.rotation(f22 * -80.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.YP.rotation((float) i * -45.0F * (float) Math.PI / 180.0F));
                                    poseStack.scale(0.9F, 0.9F, 0.9F);
                                    poseStack.translate(-0.2F, 0.126F, 0.2F);
                                    poseStack.mulPose(Axis.XP.rotation(-102.25F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.YP.rotation((float) i * 15.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.ZP.rotation((float) i * 80.0F * (float) Math.PI / 180.0F));
                                    break;
                                case "push":
                                    poseStack.translate((float) i * BlockingX.getCurrentValue(),
                                            (double) (BlockingY.getCurrentValue()), -0.82F);
                                    poseStack.translate((double) ((float) i * -0.1414214F), 0.08F, 0.1414214F);
                                    poseStack.mulPose(Axis.XP.rotation(-102.25F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.YP.rotation((float) i * 13.365F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.ZP.rotation((float) i * 78.05F * (float) Math.PI / 180.0F));
                                    float f15 = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
                                    float f3 = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
                                    poseStack.mulPose(Axis.XP.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.YP.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.ZP.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.XP.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.YP.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.ZP.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                            }
                        } else if (player.isAutoSpinAttack()) {
                            applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                            poseStack.translate((double) ((float) i * -0.4F), 0.8F, 0.3F);
                            poseStack.mulPose(Axis.YP.rotation((float) i * 65.0F * (float) Math.PI / 180.0F));
                            poseStack.mulPose(Axis.ZP.rotation((float) i * -85.0F * (float) Math.PI / 180.0F));
                        } else {
                            applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                            if (itemStack.getItem() instanceof SwordItem &&
                                    (mc.options.keyUse.isDown() || (animations.auraAutoBlock.getCurrentValue()
                                            && getAuraTarget() != null && getAuraTarget() instanceof LivingEntity
                                    ))) {
                                String s = animations.BlockMods.getCurrentMode().toLowerCase();
                                switch (s) {
                                    case "1.7":
                                        poseStack.translate((float) i * 0.56F,
                                                (double) (-0.52F), -0.72F);
                                        float f17 = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
                                        float f22 = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
                                        poseStack.mulPose(Axis.YP.rotation(
                                                (float) i * (45.0F + f17 * -20.0F) * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(
                                                Axis.ZP.rotation((float) i * f22 * -20.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(Axis.XP.rotation(f22 * -80.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(
                                                Axis.YP.rotation((float) i * -45.0F * (float) Math.PI / 180.0F));
                                        poseStack.scale(0.9F, 0.9F, 0.9F);
                                        poseStack.translate(-0.2F, 0.126F, 0.2F);
                                        poseStack.mulPose(Axis.XP.rotation(-102.25F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(
                                                Axis.YP.rotation((float) i * 15.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(
                                                Axis.ZP.rotation((float) i * 80.0F * (float) Math.PI / 180.0F));
                                        break;
                                    case "push":
                                        poseStack.translate((float) i * 0.56F,
                                                (double) (-0.52F), -0.72F);
                                        poseStack.translate((double) ((float) i * -0.1414214F), 0.08F, 0.1414214F);
                                        poseStack.mulPose(Axis.XP.rotation(-102.25F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(
                                                Axis.YP.rotation((float) i * 13.365F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(
                                                Axis.ZP.rotation((float) i * 78.05F * (float) Math.PI / 180.0F));
                                        float f15 = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
                                        float f3 = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
                                        poseStack.mulPose(Axis.XP.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(Axis.YP.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(Axis.ZP.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(Axis.XP.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(Axis.YP.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(Axis.ZP.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                                        break;
                                    default:
                                        applyItemArmAttackTransform(poseStack, humanoidarm, swingProgress);
                                }
                            } else {
                                applyItemArmAttackTransform(poseStack, humanoidarm, swingProgress);
                            }
                        }

                        renderItem(
                                player,
                                itemStack,
                                flag2 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                                        : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                                !flag2,
                                poseStack,
                                multiBufferSource,
                                light);
                    }
                }
            }

            poseStack.popPose();
        }
    }

    private LivingEntity getAuraTarget() {
        KillAura aura = Naven.getInstance().getModuleManager().getModule(KillAura.class);
        if (aura != null && aura.isEnabled()) {
            try {
                return (LivingEntity) aura.target;
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private void renderPlayerArm(PoseStack poseStack, MultiBufferSource bufferSource, int light, float equippedProg,
                                 float swingProgress, HumanoidArm arm) {
        boolean flag = arm == HumanoidArm.RIGHT;
        float f = flag ? 1.0F : -1.0F;
        float f1 = Mth.sqrt(swingProgress);
        float f2 = -0.3F * Mth.sin(f1 * (float) Math.PI);
        float f3 = 0.4F * Mth.sin(f1 * (float) (Math.PI * 2));
        float f4 = -0.4F * Mth.sin(swingProgress * (float) Math.PI);
        poseStack.translate(f * (0.644764F + f2), 0.644764F + f3, (double) (0.644764F + f4));
        poseStack.mulPose(Axis.XP.rotation(-0.3F * Mth.sin(f1 * (float) (Math.PI * 2))));
        poseStack.mulPose(Axis.YP.rotation(f * 0.4F * Mth.sin(f1 * (float) Math.PI)));
        poseStack.mulPose(Axis.ZP.rotation(f * -0.4F * Mth.sin(swingProgress * (float) Math.PI)));
        float f5 = Mth.lerp(equippedProg, oMainHandHeight, mainHandHeight);
        float f6 = Mth.lerp(equippedProg, oOffHandHeight, offHandHeight);
        this.renderItem(mc.player, flag ? mainHandItem : offHandItem,
                flag ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, !flag,
                poseStack, bufferSource, light);
    }

    private void renderTwoHandedMap(PoseStack poseStack, MultiBufferSource bufferSource, int light, float equipProgress,
                                    float equippedProg, float swingProgress) {
        float f = Mth.sqrt(swingProgress);
        float f1 = -0.2F * Mth.sin(swingProgress * (float) Math.PI);
        float f2 = -0.4F * Mth.sin(f * (float) Math.PI);
        poseStack.translate(0.0D, -f1 / 2.0F, f2);
        float f3 = Mth.lerp(equippedProg, oMainHandHeight, mainHandHeight);
        float f4 = Mth.lerp(equippedProg, oOffHandHeight, offHandHeight);
        this.renderItem(mc.player, mainHandItem, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, false, poseStack,
                bufferSource, light);
        this.renderItem(mc.player, offHandItem, ItemDisplayContext.FIRST_PERSON_LEFT_HAND, true, poseStack,
                bufferSource, light);
    }

    private void renderOneHandedMap(PoseStack poseStack, MultiBufferSource bufferSource, int light, float equippedProg,
                                    HumanoidArm arm, float swingProgress, ItemStack item) {
        float f = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        poseStack.translate(f * 0.125F, 0.0D, 0.0D);
        float f1 = Mth.sqrt(swingProgress);
        float f2 = -0.1F * Mth.sin(f1 * (float) Math.PI);
        float f3 = -0.3F * Mth.sin(f1 * (float) (Math.PI * 2));
        float f4 = -0.4F * Mth.sin(swingProgress * (float) Math.PI);
        poseStack.translate(0.0D, -f2 / 2.0F, f4);
        poseStack.mulPose(Axis.XP.rotation(f3 * (float) Math.PI / 180.0F));
        poseStack.mulPose(Axis.YP.rotation(f * f1 * (float) Math.PI / 180.0F));
        poseStack.mulPose(Axis.ZP.rotation(f * f2 * (float) Math.PI / 180.0F));
        float f5 = Mth.lerp(equippedProg, oMainHandHeight, mainHandHeight);
        float f6 = Mth.lerp(equippedProg, oOffHandHeight, offHandHeight);
        this.renderItem(mc.player, item,
                arm == HumanoidArm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                        : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                arm != HumanoidArm.RIGHT, poseStack, bufferSource, light);
    }

    private void applyItemArmTransform(PoseStack poseStack, HumanoidArm arm, float equippedProg) {
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        float f = Mth.lerp(equippedProg, oMainHandHeight, mainHandHeight);
        float f1 = Mth.lerp(equippedProg, oOffHandHeight, offHandHeight);
        poseStack.translate((float) i * 0.56F, (double) (-0.52F + f * -0.6F), -0.72F);
    }

    private void applyItemArmAttackTransform(PoseStack poseStack, HumanoidArm arm, float swingProgress) {
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        float f = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        float f1 = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        poseStack.translate((float) i * 0.56F, (double) (-0.52F), -0.72F);
        poseStack.mulPose(Axis.XP.rotation(-102.25F * (float) Math.PI / 180.0F));
        poseStack.mulPose(Axis.YP.rotation((float) i * 13.365F * (float) Math.PI / 180.0F));
        poseStack.mulPose(Axis.ZP.rotation((float) i * 78.05F * (float) Math.PI / 180.0F));
        float swingFactor = Mth.clamp(swingProgress, 0.0F, 1.0F);
        poseStack.mulPose(Axis.XP.rotation(f * -15.0F * swingFactor * (float) Math.PI / 180.0F));
        poseStack.mulPose(Axis.YP.rotation(f1 * -15.0F * swingFactor * (float) Math.PI / 180.0F));
        poseStack.mulPose(Axis.ZP.rotation(f1 * -70.0F * swingFactor * (float) Math.PI / 180.0F));
    }

    private void applyEatTransform(PoseStack poseStack, float partialTicks, HumanoidArm arm, ItemStack item) {
        float f = (float) item.getUseDuration() - ((float) mc.player.getUseItemRemainingTicks() - partialTicks + 1.0F);
        float f1 = f / (float) item.getUseDuration();
        if (f1 < 0.8F) {
            float f2 = Mth.abs(Mth.cos(f / 4.0F * (float) Math.PI) * 0.1F);
            poseStack.translate(0.0D, f2, 0.0D);
        }
        float f3 = 1.0F - (float) Math.pow(1.0F - f1, 27.0D);
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.translate(f3 * 0.6F * (float) i, f3 * -0.5F, (double) (f3 * 0.0F));
        poseStack.mulPose(Axis.YP.rotation((float) i * f3 * 90.0F * (float) Math.PI / 180.0F));
        poseStack.mulPose(Axis.XP.rotation(f3 * 10.0F * (float) Math.PI / 180.0F));
        poseStack.mulPose(Axis.ZP.rotation((float) i * f3 * 30.0F * (float) Math.PI / 180.0F));
    }

    private void renderItem(LivingEntity entity, ItemStack stack,
                            ItemDisplayContext transformType, boolean leftHand,
                            PoseStack poseStack, MultiBufferSource buffer, int light) {
        if (stack.isEmpty())
            return;
        ItemRenderer itemRenderer = mc.getItemRenderer();
        itemRenderer.renderStatic(entity, stack, transformType, leftHand, poseStack, buffer, entity.level(), light, 0,
                0);
    }
}
