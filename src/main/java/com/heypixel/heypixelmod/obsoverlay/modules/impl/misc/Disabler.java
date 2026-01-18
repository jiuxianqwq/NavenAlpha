//package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;
//
//import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
//import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
//import com.heypixel.heypixelmod.obsoverlay.events.api.types.Priority;
//import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
//import com.heypixel.heypixelmod.obsoverlay.files.FileManager;
//import com.heypixel.heypixelmod.obsoverlay.modules.Category;
//import com.heypixel.heypixelmod.obsoverlay.modules.Module;
//import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
//import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
//import com.heypixel.heypixelmod.obsoverlay.utils.MathUtils;
//import com.heypixel.heypixelmod.obsoverlay.utils.rotation.Rotation;
//import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
//import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
//import net.minecraft.network.protocol.Packet;
//import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
//import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
//import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
//import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
//import sun.misc.Unsafe;
//
//import java.lang.reflect.Field;
//import java.util.Random;
//
//@ModuleInfo(
//        name = "Disabler",
//        cnName = "禁用器",
//        category = Category.MISC,
//        description = "Disables some checks of the anti cheat."
//)
//public class Disabler extends Module {
//    private final BooleanValue logging = ValueBuilder.create(this, "Logging")
//            .setDefaultBooleanValue(false)
//            .build()
//            .getBooleanValue();
//    private final BooleanValue vulcanAimA = ValueBuilder.create(this, "Vulcan Aim (A)")
//            .setDefaultBooleanValue(false)
//            .build()
//            .getBooleanValue();
//    private final BooleanValue grimAimDuplicateLook = ValueBuilder.create(this, "Grim AimDuplicateLook")
//            .setDefaultBooleanValue(false)
//            .build()
//            .getBooleanValue();
//
//    private Rotation lastRot;
//
//    private void log(String message) {
//        if (this.logging.getCurrentValue()) {
//            ChatUtils.addChatMessage(message);
//        }
//    }
//
//
//    @EventTarget(Priority.LOW)
//    public void onPacket(EventPacket e) {
//        Packet<?> packet = e.getPacket();
//        if (packet instanceof ServerboundMovePlayerPacket c03) {
//            if (c03.hasRotation()) {
//                if (lastRot != null) {
//                    float yawDiff = Math.abs(c03.getYRot(0) - lastRot.getYaw());
//                    float pitchDiff = Math.abs(c03.getXRot(0) - lastRot.getPitch());
//                    if (vulcanAimA.getCurrentValue()) {
//                        if (pitchDiff < 0.001) {
//                            if (c03.hasPosition()) {
//                                e.setPacket(new ServerboundMovePlayerPacket.PosRot(c03.getX(0), c03.getY(0), c03.getZ(0), c03.getXRot(0), c03.getYRot(0) + MathUtils.getRandomFloatInRange(1.0f, -1.0f), c03.isOnGround()));
//                            } else {
//                                e.setPacket(new ServerboundMovePlayerPacket.Rot(c03.getXRot(0), c03.getYRot(0) + MathUtils.getRandomFloatInRange(1.0f, -1.0f), c03.isOnGround()));
//                            }
//                        }
//                    }
//                    if (grimAimDuplicateLook.getCurrentValue()) {
//                        if (yawDiff < 0.001 && pitchDiff < 0.001) {
//                            if (c03.hasPosition()) {
//                                e.setPacket(new ServerboundMovePlayerPacket.Pos(c03.getX(0), c03.getY(0), c03.getZ(0), c03.isOnGround()));
//                            } else {
//                                e.setPacket(new ServerboundMovePlayerPacket.StatusOnly(c03.isOnGround()));
//                            }
//                        }
//                    }
//
//                }
//                lastRot = new Rotation(c03.getYRot(0), c03.getXRot(0));
//            }
//        }
//    }
//
//}