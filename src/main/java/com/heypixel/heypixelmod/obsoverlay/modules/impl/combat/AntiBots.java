package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRespawn;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ModuleInfo(
        name = "AntiBots",
        cnName = "反人机",
        category = Category.COMBAT,
        description = "Prevents bots from attacking you"
)
public class AntiBots extends Module {
    private static final Map<UUID, String> uuidDisplayNames = new ConcurrentHashMap<>();
    private static final Map<Integer, String> entityIdDisplayNames = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> uuids = new ConcurrentHashMap<>();
    private static final Set<Integer> ids = new HashSet<>();
    private static final Map<UUID, Long> respawnTime = new ConcurrentHashMap<>();
    private final FloatValue respawnTimeValue = ValueBuilder.create(this, "Respawn Time")
            .setDefaultFloatValue(2500.0F)
            .setFloatStep(100.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(10000.0F)
            .build()
            .getFloatValue();

    public static boolean isBedWarsBot(Entity entity) {
        AntiBots module = Naven.getInstance().getModuleManager().getModule(AntiBots.class);
        if (module.respawnTimeValue.getCurrentValue() < 1.0F) {
            return false;
        } else {
            return respawnTime.containsKey(entity.getUUID()) && (float) (System.currentTimeMillis() - respawnTime.get(entity.getUUID())) < module.respawnTimeValue.getCurrentValue();
        }
    }

    public static boolean isBot(Entity entity) {
        return (ids.contains(entity.getId()) || !mc.getConnection().getOnlinePlayerIds().contains(entity.getUUID())) && Naven.getInstance().getModuleManager().getModule(AntiBots.class).isEnabled();
    }

    @EventTarget
    public void bedWarsBot(EventPacket e) {
        if (e.getType() == EventType.RECEIVE && mc.level != null) {
            if (e.getPacket() instanceof ClientboundPlayerInfoUpdatePacket packet) {
                if (packet.actions().contains(Action.ADD_PLAYER)) {
                    for (net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry entry : packet.entries()) {
                        GameProfile profile = entry.profile();
                        UUID id = profile.getId();
                        respawnTime.put(id, System.currentTimeMillis());
                    }
                }
            } else if (e.getPacket() instanceof ClientboundAnimatePacket packet) {
                Entity entity = mc.level.getEntity(packet.getId());
                if (entity != null && packet.getAction() == 0) {
                    respawnTime.remove(entity.getUUID());
                }
            }
        }
    }

    @EventTarget
    public void onRespawn(EventRespawn e) {
        uuidDisplayNames.clear();
        entityIdDisplayNames.clear();
        ids.clear();
        uuids.clear();
    }

    @EventTarget
    public void onMotion(EventMotion e) {
        if (e.getType() == EventType.PRE) {
            for (Entry<UUID, Long> entry : uuids.entrySet()) {
                if (System.currentTimeMillis() - entry.getValue() > 500L) {
                    uuids.remove(entry.getKey());
                }
            }
        }
    }

    @EventTarget
    public void onPacket(EventPacket e) {
        if (e.getType() == EventType.RECEIVE) {
            if (e.getPacket() instanceof ClientboundPlayerInfoUpdatePacket packet) {
                if (packet.actions().contains(Action.ADD_PLAYER)) {
                    for (net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry entry : packet.entries()) {
                        if (entry.displayName() != null && entry.displayName().getSiblings().isEmpty() && entry.gameMode() == GameType.SURVIVAL) {
                            UUID uuid = entry.profile().getId();
                            uuids.put(uuid, System.currentTimeMillis());
                            uuidDisplayNames.put(uuid, entry.displayName().getString());
                        }
                    }
                }
            } else if (e.getPacket() instanceof ClientboundAddPlayerPacket packet) {
                if (uuids.containsKey(packet.getPlayerId())) {
                    String displayName = uuidDisplayNames.get(packet.getPlayerId());
                    entityIdDisplayNames.put(packet.getEntityId(), displayName);
                    uuids.remove(packet.getPlayerId());
                    ids.add(packet.getEntityId());
                }
            } else if (e.getPacket() instanceof ClientboundRemoveEntitiesPacket packet) {
                IntListIterator var9 = packet.getEntityIds().iterator();

                while (var9.hasNext()) {
                    Integer entityId = var9.next();
                    if (ids.contains(entityId)) {
                        String displayName = entityIdDisplayNames.get(entityId);
                        ids.remove(entityId);
                    }
                }
            }
        }
    }
}
