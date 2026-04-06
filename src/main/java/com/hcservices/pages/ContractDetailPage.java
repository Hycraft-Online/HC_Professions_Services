package com.hcservices.pages;

import com.hcservices.HC_ServicesPlugin;
import com.hcservices.database.ContractRepository;
import com.hcservices.database.ListingRepository;
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

public class ContractDetailPage extends InteractiveCustomUIPage<ServiceEventData> {

    private static final HytaleLogger LOGGER = HytaleLogger.getLogger().getSubLogger("HC_Services-Contract");

    private final PlayerRef playerRef;
    private final UUID contractId;

    public ContractDetailPage(@Nonnull PlayerRef playerRef, @Nonnull UUID contractId) {
        super(playerRef, CustomPageLifetime.CanDismiss, ServiceEventData.CODEC);
        this.playerRef = playerRef;
        this.contractId = contractId;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/Services/ContractDetail.ui");

        HC_ServicesPlugin plugin = HC_ServicesPlugin.getInstance();
        if (plugin == null) { return; }

        ContractRepository contractRepo = plugin.getContractRepository();
        ServiceContract contract = contractRepo.getContractById(contractId);
        if (contract == null) {
            cmd.set("#StatusLabel.TextSpans", Message.raw("Contract Not Found").color(Color.RED));
            events.addEventBinding(CustomUIEventBindingType.Activating, "#BackBtn",
                EventData.of("Action", "Back"));
            return;
        }

        // Get listing title
        String serviceTitle = "Unknown Service";
        ServiceListing listing = plugin.getListingRepository().getListingById(contract.getListingId());
        if (listing != null) {
            serviceTitle = listing.getTitle();
        }

        // Display info
        cmd.set("#StatusLabel.TextSpans", Message.raw(formatStatus(contract.getStatus())).color(getStatusColor(contract.getStatus())));
        cmd.set("#ServiceTitle.TextSpans", Message.raw(serviceTitle));
        cmd.set("#ProviderLabel.TextSpans", Message.raw(contract.getProviderName()));
        cmd.set("#ClientLabel.TextSpans", Message.raw(contract.getClientName()));
        cmd.set("#PriceLabel.TextSpans", Message.raw(contract.getPrice() + " coins").color(new Color(0xFF, 0xD7, 0x00)));
        cmd.set("#NotesLabel.TextSpans", Message.raw(
            contract.getClientNotes() != null && !contract.getClientNotes().isEmpty()
                ? contract.getClientNotes() : "None"));

        // Determine role
        boolean isProvider = contract.getProviderUuid().equals(playerRef.getUuid());
        boolean isClient = contract.getClientUuid().equals(playerRef.getUuid());
        ContractStatus status = contract.getStatus();

        // Show action buttons based on role + status
        if (isProvider && status == ContractStatus.REQUESTED) {
            cmd.set("#AcceptBtn.Visible", true);
            cmd.set("#DeclineBtn.Visible", true);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#AcceptBtn",
                EventData.of("Action", "Accept").append("ContractId", contractId.toString()));
            events.addEventBinding(CustomUIEventBindingType.Activating, "#DeclineBtn",
                EventData.of("Action", "Decline").append("ContractId", contractId.toString()));
        }

        if (isProvider && status == ContractStatus.ACCEPTED) {
            cmd.set("#StartBtn.Visible", true);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#StartBtn",
                EventData.of("Action", "Start").append("ContractId", contractId.toString()));
        }

        if (isProvider && status == ContractStatus.IN_PROGRESS) {
            cmd.set("#CompleteBtn.Visible", true);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#CompleteBtn",
                EventData.of("Action", "Complete").append("ContractId", contractId.toString()));
        }

        // Both parties can cancel non-terminal contracts
        if ((isProvider || isClient) && !status.isTerminal()) {
            cmd.set("#CancelContractBtn.Visible", true);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#CancelContractBtn",
                EventData.of("Action", "CancelContract").append("ContractId", contractId.toString()));
        }

        events.addEventBinding(CustomUIEventBindingType.Activating, "#BackBtn",
            EventData.of("Action", "Back"));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull ServiceEventData data) {
        if (data.action == null) { sendUpdate(); return; }

        switch (data.action) {
            case "Back" -> navigateBack(ref, store);
            case "Accept" -> updateContractStatus(ContractStatus.ACCEPTED, ref, store);
            case "Decline" -> updateContractStatus(ContractStatus.DECLINED, ref, store);
            case "Start" -> updateContractStatus(ContractStatus.IN_PROGRESS, ref, store);
            case "Complete" -> updateContractStatus(ContractStatus.COMPLETED, ref, store);
            case "CancelContract" -> updateContractStatus(ContractStatus.CANCELLED, ref, store);
            default -> sendUpdate();
        }
    }

    private void updateContractStatus(ContractStatus newStatus, Ref<EntityStore> ref, Store<EntityStore> store) {
        HC_ServicesPlugin plugin = HC_ServicesPlugin.getInstance();
        if (plugin == null) { sendUpdate(); return; }

        ContractRepository contractRepo = plugin.getContractRepository();
        ServiceContract contract = contractRepo.getContractById(contractId);
        if (contract == null || contract.getStatus().isTerminal()) {
            sendUpdate();
            return;
        }

        // Verify permissions
        boolean isProvider = contract.getProviderUuid().equals(playerRef.getUuid());
        boolean isClient = contract.getClientUuid().equals(playerRef.getUuid());

        // Provider-only actions
        if ((newStatus == ContractStatus.ACCEPTED || newStatus == ContractStatus.DECLINED
                || newStatus == ContractStatus.IN_PROGRESS || newStatus == ContractStatus.COMPLETED)
                && !isProvider) {
            sendUpdate();
            return;
        }

        // Cancel is allowed by either party
        if (newStatus == ContractStatus.CANCELLED && !isProvider && !isClient) {
            sendUpdate();
            return;
        }

        contractRepo.updateStatus(contractId, newStatus);

        // Get listing title for notification
        String serviceTitle = "a service";
        ServiceListing listing = plugin.getListingRepository().getListingById(contract.getListingId());
        if (listing != null) {
            serviceTitle = listing.getTitle();
        }

        // Notify the other party
        UUID targetUuid = isProvider ? contract.getClientUuid() : contract.getProviderUuid();
        String otherName = isProvider ? contract.getProviderName() : contract.getClientName();
        plugin.getNotificationManager().notifyStatusChange(targetUuid, playerRef.getUsername(), serviceTitle, formatStatus(newStatus));

        // Rebuild to show updated state
        rebuild();
    }

    private void navigateBack(Ref<EntityStore> ref, Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            ServicesBoardPage boardPage = new ServicesBoardPage(playerRef);
            player.getPageManager().openCustomPage(ref, store, (CustomUIPage) boardPage);
        }
    }

    private static String formatStatus(ContractStatus status) {
        if (status == null) return "Unknown";
        return switch (status) {
            case REQUESTED -> "Requested";
            case ACCEPTED -> "Accepted";
            case IN_PROGRESS -> "In Progress";
            case COMPLETED -> "Completed";
            case CANCELLED -> "Cancelled";
            case DECLINED -> "Declined";
        };
    }

    private static Color getStatusColor(ContractStatus status) {
        if (status == null) return Color.GRAY;
        return switch (status) {
            case REQUESTED -> new Color(0xFF, 0xD7, 0x00);
            case ACCEPTED -> new Color(0x4A, 0x9E, 0x5C);
            case IN_PROGRESS -> new Color(0x32, 0x78, 0xC8);
            case COMPLETED -> new Color(0x55, 0xFF, 0x55);
            case CANCELLED -> Color.GRAY;
            case DECLINED -> new Color(0xC8, 0x32, 0x32);
        };
    }
}
