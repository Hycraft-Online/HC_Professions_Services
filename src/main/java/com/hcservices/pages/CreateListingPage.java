package com.hcservices.pages;

import com.hcservices.HC_ServicesPlugin;
import com.hcservices.database.ListingRepository;
import com.hcservices.models.ServiceEventData;
import com.hcservices.models.ServiceListing;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.logging.Level;

public class CreateListingPage extends InteractiveCustomUIPage<ServiceEventData> {

    private static final HytaleLogger LOGGER = HytaleLogger.getLogger().getSubLogger("HC_Services-Create");
    private static final int MAX_LISTINGS = 5;

    private final PlayerRef playerRef;
    @Nullable
    private final UUID editListingId;

    public CreateListingPage(@Nonnull PlayerRef playerRef, @Nullable UUID editListingId) {
        super(playerRef, CustomPageLifetime.CanDismiss, ServiceEventData.CODEC);
        this.playerRef = playerRef;
        this.editListingId = editListingId;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/Services/CreateListing.ui");

        // Check profession
        String professionName = null;
        String professionDisplay = "None";
        try {
            var profession = com.hcprofessions.api.HC_ProfessionsAPI.getProfession(playerRef.getUuid());
            if (profession != null) {
                professionName = profession.name();
                professionDisplay = profession.getDisplayName();
            }
        } catch (NoClassDefFoundError e) {
            // HC_Professions not loaded
        }

        if (professionName == null) {
            cmd.set("#ProfLabel.TextSpans", Message.raw("No profession - choose one first!").color(java.awt.Color.RED));
            cmd.set("#SubmitBtn.Visible", false);
        } else {
            cmd.set("#ProfLabel.TextSpans", Message.raw(professionDisplay));
        }

        // If editing, pre-fill
        if (editListingId != null) {
            cmd.set("#FormTitle.TextSpans", Message.raw("Edit Service Listing"));
            HC_ServicesPlugin plugin = HC_ServicesPlugin.getInstance();
            if (plugin != null) {
                ServiceListing existing = plugin.getListingRepository().getListingById(editListingId);
                if (existing != null) {
                    cmd.set("#TitleInput.Value", existing.getTitle());
                    cmd.set("#DescInput.Value", existing.getDescription() != null ? existing.getDescription() : "");
                    cmd.set("#PriceInput.Value", String.valueOf(existing.getPrice()));
                    cmd.set("#SubmitBtn.Text", "SAVE");
                }
            }
        }

        // Bind submit: capture all text field values
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SubmitBtn",
            new EventData()
                .append("Action", "Submit")
                .append("@Title", "#TitleInput.Value")
                .append("@Description", "#DescInput.Value")
                .append("@Price", "#PriceInput.Value"));

        events.addEventBinding(CustomUIEventBindingType.Activating, "#CancelFormBtn",
            EventData.of("Action", "Cancel"));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull ServiceEventData data) {
        if ("Cancel".equals(data.action)) {
            navigateBack(ref, store);
            return;
        }

        if ("Submit".equals(data.action)) {
            handleSubmit(data, ref, store);
            return;
        }

        sendUpdate();
    }

    private void handleSubmit(ServiceEventData data, Ref<EntityStore> ref, Store<EntityStore> store) {
        // Validate title
        String title = data.title != null ? data.title.trim() : "";
        if (title.isEmpty()) {
            showError("Title is required!");
            return;
        }
        if (title.length() > 128) {
            showError("Title must be 128 characters or less!");
            return;
        }

        String description = data.description != null ? data.description.trim() : "";
        if (description.length() > 512) {
            showError("Description must be 512 characters or less!");
            return;
        }

        // Parse price
        int price = 0;
        if (data.price != null && !data.price.trim().isEmpty()) {
            try {
                price = Integer.parseInt(data.price.trim());
                if (price < 0) {
                    showError("Price cannot be negative!");
                    return;
                }
            } catch (NumberFormatException e) {
                showError("Price must be a whole number!");
                return;
            }
        }

        // Check profession
        String professionName = null;
        try {
            var profession = com.hcprofessions.api.HC_ProfessionsAPI.getProfession(playerRef.getUuid());
            if (profession != null) {
                professionName = profession.name();
            }
        } catch (NoClassDefFoundError e) {
            // HC_Professions not loaded
        }

        if (professionName == null) {
            showError("You must choose a profession first!");
            return;
        }

        HC_ServicesPlugin plugin = HC_ServicesPlugin.getInstance();
        if (plugin == null) {
            showError("Plugin unavailable!");
            return;
        }

        ListingRepository repo = plugin.getListingRepository();

        if (editListingId != null) {
            // Editing existing
            ServiceListing existing = repo.getListingById(editListingId);
            if (existing == null || !existing.getProviderUuid().equals(playerRef.getUuid())) {
                showError("Listing not found or not yours!");
                return;
            }
            existing.setTitle(title);
            existing.setDescription(description);
            existing.setPrice(price);
            repo.updateListing(existing);
            playerRef.sendMessage(Message.raw("[Services] Listing updated!").color(new java.awt.Color(0x4A, 0x9E, 0x5C)));
        } else {
            // Creating new - enforce limit
            int currentCount = repo.countActiveListingsByProvider(playerRef.getUuid());
            if (currentCount >= MAX_LISTINGS) {
                showError("Max " + MAX_LISTINGS + " active listings allowed!");
                return;
            }

            ServiceListing listing = new ServiceListing();
            listing.setId(UUID.randomUUID());
            listing.setProviderUuid(playerRef.getUuid());
            listing.setProviderName(playerRef.getUsername());
            listing.setProfession(professionName);
            listing.setTitle(title);
            listing.setDescription(description);
            listing.setPrice(price);
            listing.setStatus("active");
            repo.createListing(listing);
            playerRef.sendMessage(Message.raw("[Services] Listing created!").color(new java.awt.Color(0x4A, 0x9E, 0x5C)));
        }

        navigateBack(ref, store);
    }

    private void navigateBack(Ref<EntityStore> ref, Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            ServicesBoardPage boardPage = new ServicesBoardPage(playerRef);
            player.getPageManager().openCustomPage(ref, store, (CustomUIPage) boardPage);
        }
    }

    private void showError(String message) {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#ErrorLabel.Visible", true);
        cmd.set("#ErrorLabel.TextSpans", Message.raw(message).color(java.awt.Color.RED));
        sendUpdate(cmd);
    }
}
