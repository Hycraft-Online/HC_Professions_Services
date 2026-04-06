package com.hcservices.pages;

import com.hcservices.HC_ServicesPlugin;
import com.hcservices.database.ContractRepository;
import com.hcservices.models.ContractStatus;
import com.hcservices.models.ServiceContract;
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
import java.awt.Color;
import java.util.UUID;
import java.util.logging.Level;

public class ListingDetailPage extends InteractiveCustomUIPage<ServiceEventData> {

    private static final HytaleLogger LOGGER = HytaleLogger.getLogger().getSubLogger("HC_Services-Detail");
    private static final int MAX_ACTIVE_CONTRACTS = 3;

    private final PlayerRef playerRef;
    private final UUID listingId;

    public ListingDetailPage(@Nonnull PlayerRef playerRef, @Nonnull UUID listingId) {
        super(playerRef, CustomPageLifetime.CanDismiss, ServiceEventData.CODEC);
        this.playerRef = playerRef;
        this.listingId = listingId;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/Services/ListingDetail.ui");

        HC_ServicesPlugin plugin = HC_ServicesPlugin.getInstance();
        if (plugin == null) { return; }

        ServiceListing listing = plugin.getListingRepository().getListingById(listingId);
        if (listing == null) {
            cmd.set("#DetailTitle.TextSpans", Message.raw("Listing Not Found").color(Color.RED));
            events.addEventBinding(CustomUIEventBindingType.Activating, "#BackBtn",
                EventData.of("Action", "Back"));
            return;
        }

        // Display listing info
        cmd.set("#ProviderName.TextSpans", Message.raw(listing.getProviderName()));

        // Profession display
        String profDisplay = listing.getProfession() != null ? listing.getProfession() : "Unknown";
        int profLevel = 0;
        try {
            var profession = com.hcprofessions.models.Profession.fromString(listing.getProfession());
            if (profession != null) {
                profDisplay = profession.getDisplayName();
            }
            profLevel = com.hcprofessions.api.HC_ProfessionsAPI.getProfessionLevel(listing.getProviderUuid());
        } catch (NoClassDefFoundError e) {
            // HC_Professions not loaded
        }

        String profText = profDisplay + (profLevel > 0 ? " (Lv. " + profLevel + ")" : "");
        cmd.set("#ProfessionLabel.TextSpans", Message.raw(profText));
        cmd.set("#ListingTitle.TextSpans", Message.raw(listing.getTitle()));
        cmd.set("#ListingDesc.TextSpans", Message.raw(listing.getDescription() != null ? listing.getDescription() : "No description provided."));
        cmd.set("#PriceLabel.TextSpans", Message.raw(listing.getPrice() + " coins").color(new Color(0xFF, 0xD7, 0x00)));

        // Show hire section only for non-own active listings
        boolean isOwn = listing.getProviderUuid().equals(playerRef.getUuid());
        if (!isOwn && listing.isActive()) {
            cmd.set("#HireSection.Visible", true);

            events.addEventBinding(CustomUIEventBindingType.Activating, "#HireBtn",
                new EventData()
                    .append("Action", "Hire")
                    .append("ListingId", listingId.toString())
                    .append("@ClientNotes", "#NotesInput.Value"));
        }

        events.addEventBinding(CustomUIEventBindingType.Activating, "#BackBtn",
            EventData.of("Action", "Back"));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull ServiceEventData data) {
        if ("Back".equals(data.action)) {
            navigateBack(ref, store);
            return;
        }

        if ("Hire".equals(data.action)) {
            handleHire(data, ref, store);
            return;
        }

        sendUpdate();
    }

    private void handleHire(ServiceEventData data, Ref<EntityStore> ref, Store<EntityStore> store) {
        HC_ServicesPlugin plugin = HC_ServicesPlugin.getInstance();
        if (plugin == null) { sendUpdate(); return; }

        ServiceListing listing = plugin.getListingRepository().getListingById(listingId);
        if (listing == null || !listing.isActive()) {
            showError("This listing is no longer available!");
            return;
        }

        // Can't hire yourself
        if (listing.getProviderUuid().equals(playerRef.getUuid())) {
            showError("You cannot hire yourself!");
            return;
        }

        // Check provider's active contract limit
        ContractRepository contractRepo = plugin.getContractRepository();
        int activeContracts = contractRepo.countActiveContractsByProvider(listing.getProviderUuid());
        if (activeContracts >= MAX_ACTIVE_CONTRACTS) {
            showError("This provider has too many active contracts!");
            return;
        }

        // Create contract
        ServiceContract contract = new ServiceContract();
        contract.setId(UUID.randomUUID());
        contract.setListingId(listingId);
        contract.setProviderUuid(listing.getProviderUuid());
        contract.setProviderName(listing.getProviderName());
        contract.setClientUuid(playerRef.getUuid());
        contract.setClientName(playerRef.getUsername());
        contract.setStatus(ContractStatus.REQUESTED);
        contract.setPrice(listing.getPrice());
        contract.setClientNotes(data.clientNotes != null ? data.clientNotes.trim() : "");
        contractRepo.createContract(contract);

        // Notify provider
        plugin.getNotificationManager().notifyNewRequest(
            listing.getProviderUuid(), playerRef.getUsername(), listing.getTitle());

        playerRef.sendMessage(Message.raw("[Services] Contract request sent to " + listing.getProviderName() + "!").color(new Color(0x4A, 0x9E, 0x5C)));

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
        cmd.set("#ErrorLabel.TextSpans", Message.raw(message).color(Color.RED));
        sendUpdate(cmd);
    }
}
