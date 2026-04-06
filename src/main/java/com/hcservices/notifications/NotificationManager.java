package com.hcservices.notifications;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class NotificationManager {

    private static final HytaleLogger LOGGER = HytaleLogger.getLogger().getSubLogger("HC_Services-Notify");
    private static final Color SERVICES_COLOR = new Color(0x4A, 0x9E, 0x5C);

    private final ConcurrentHashMap<UUID, List<String>> offlineQueue = new ConcurrentHashMap<>();
    private int notifySoundIndex = -1;

    public void init() {
        try {
            notifySoundIndex = SoundEvent.getAssetMap().getIndex("SFX_Discovery_Z1_Medium");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Could not find notification sound: " + e.getMessage());
        }
    }

    public void notifyNewRequest(UUID providerUuid, String clientName, String serviceTitle) {
        String msg = "[Services] " + clientName + " wants to hire you for: " + serviceTitle;
        sendOrQueue(providerUuid, msg);
    }

    public void notifyStatusChange(UUID targetUuid, String otherName, String serviceTitle, String newStatus) {
        String msg = "[Services] " + otherName + " updated contract for \"" + serviceTitle + "\" to: " + newStatus;
        sendOrQueue(targetUuid, msg);
    }

    public void deliverQueuedNotifications(PlayerRef playerRef) {
        List<String> queued = offlineQueue.remove(playerRef.getUuid());
        if (queued == null || queued.isEmpty()) return;

        for (String msg : queued) {
            playerRef.sendMessage(Message.raw(msg).color(SERVICES_COLOR));
        }
        playSound(playerRef);
    }

    private void sendOrQueue(UUID targetUuid, String message) {
        PlayerRef playerRef = Universe.get().getPlayer(targetUuid);
        if (playerRef != null) {
            playerRef.sendMessage(Message.raw(message).color(SERVICES_COLOR));
            playSound(playerRef);
        } else {
            offlineQueue.computeIfAbsent(targetUuid, k -> new ArrayList<>()).add(message);
        }
    }

    private void playSound(PlayerRef playerRef) {
        if (notifySoundIndex >= 0) {
            try {
                SoundUtil.playSoundEvent2dToPlayer(playerRef, notifySoundIndex, SoundCategory.UI);
            } catch (Exception e) {
                // Silently ignore sound failures
            }
        }
    }
}
