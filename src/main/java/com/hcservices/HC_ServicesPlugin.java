package com.hcservices;

import com.hcservices.commands.ServicesCommand;
import com.hcservices.database.ContractRepository;
import com.hcservices.database.DatabaseConfig;
import com.hcservices.database.DatabaseManager;
import com.hcservices.database.ListingRepository;
import com.hcservices.interaction.ServicesBoardBlockSystem;
import com.hcservices.notifications.NotificationManager;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.io.File;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class HC_ServicesPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.getLogger().getSubLogger("HC_Services");

    private static HC_ServicesPlugin instance;

    private DatabaseManager databaseManager;
    private ListingRepository listingRepository;
    private ContractRepository contractRepository;
    private NotificationManager notificationManager;
    private ScheduledFuture<?> expiryTask;

    public HC_ServicesPlugin(@NonNullDecl JavaPluginInit init) {
        super(init);
    }

    @Override
    public void setup() {
        instance = this;

        // Initialize database
        File configDir = new File("mods/.hc_config/HC_Professions_Services");
        DatabaseConfig dbConfig = DatabaseConfig.load(configDir);
        databaseManager = new DatabaseManager(dbConfig.getUrl(), dbConfig.getUsername(), dbConfig.getPassword(), dbConfig.getPoolSize());
        listingRepository = new ListingRepository(databaseManager);
        contractRepository = new ContractRepository(databaseManager);

        // Notification manager
        notificationManager = new NotificationManager();

        // Register block interaction system
        getEntityStoreRegistry().registerSystem(new ServicesBoardBlockSystem());

        // Register events
        getEventRegistry().register(PlayerConnectEvent.class, event -> {
            notificationManager.deliverQueuedNotifications(event.getPlayerRef());
        });

        // Register command
        getCommandRegistry().registerCommand(new ServicesCommand());

        // Schedule hourly listing expiry (7-day old listings)
        expiryTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                listingRepository.expireOldListings(7);
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("Listing expiry failed: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.HOURS);

        LOGGER.at(Level.INFO).log("HC_Professions_Services setup complete");
    }

    @Override
    public void start() {
        // Init sound indexes after assets are loaded
        notificationManager.init();
        LOGGER.at(Level.INFO).log("HC_Professions_Services started");
    }

    @Override
    public void shutdown() {
        if (expiryTask != null) {
            expiryTask.cancel(false);
        }

        if (databaseManager != null) {
            databaseManager.close();
        }

        instance = null;
        LOGGER.at(Level.INFO).log("HC_Professions_Services shut down");
    }

    public static HC_ServicesPlugin getInstance() {
        return instance;
    }

    public ListingRepository getListingRepository() {
        return listingRepository;
    }

    public ContractRepository getContractRepository() {
        return contractRepository;
    }

    public NotificationManager getNotificationManager() {
        return notificationManager;
    }
}
