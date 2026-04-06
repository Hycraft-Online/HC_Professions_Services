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
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class ServicesBoardPage extends InteractiveCustomUIPage<ServiceEventData> {

    private static final HytaleLogger LOGGER = HytaleLogger.getLogger().getSubLogger("HC_Services-Board");
    private static final int ITEMS_PER_PAGE = 8;

    private static final String[] PROFESSIONS = {
        "WEAPONSMITH", "ARMORSMITH", "ALCHEMIST", "COOK",
        "LEATHERWORKER", "CARPENTER", "TAILOR", "ENCHANTER", "BUILDER"
    };
    private static final String[] PROFESSION_DISPLAY = {
        "Weaponsmith", "Armorsmith", "Alchemist", "Cook",
        "Leatherworker", "Carpenter", "Tailor", "Enchanter", "Builder"
    };
    private static final String[] PROFESSION_COLORS = {
        "#C83232", "#3278C8", "#8B5CF6", "#E89040",
        "#8B6914", "#B47832", "#C864C8", "#6432C8", "#4A9E5C"
    };

    private final PlayerRef playerRef;
    private String activeTab = "browse";
    private String professionFilter = null;
    private int currentPage = 0;

    public ServicesBoardPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, ServiceEventData.CODEC);
        this.playerRef = playerRef;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/Services/ServicesBoardMain.ui");

        HC_ServicesPlugin plugin = HC_ServicesPlugin.getInstance();
        if (plugin == null) {
            cmd.set("#EmptyLabel.Visible", true);
            cmd.set("#EmptyLabel.TextSpans", Message.raw("Services plugin not available"));
            return;
        }

        ListingRepository listingRepo = plugin.getListingRepository();
        ContractRepository contractRepo = plugin.getContractRepository();
        UUID myUuid = playerRef.getUuid();

        // Tab event bindings
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabBrowse",
            EventData.of("Action", "SwitchTab").append("Filter", "browse"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabMyListings",
            EventData.of("Action", "SwitchTab").append("Filter", "myListings"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabMyContracts",
            EventData.of("Action", "SwitchTab").append("Filter", "myContracts"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseBtn",
            EventData.of("Action", "Close"));

        switch (activeTab) {
            case "browse" -> buildBrowseTab(cmd, events, listingRepo);
            case "myListings" -> buildMyListingsTab(cmd, events, listingRepo, myUuid);
            case "myContracts" -> buildMyContractsTab(cmd, events, contractRepo, myUuid);
        }
    }

    private void buildBrowseTab(UICommandBuilder cmd, UIEventBuilder events, ListingRepository listingRepo) {
        cmd.set("#FilterRow.Visible", true);

        // Setup filter buttons
        events.addEventBinding(CustomUIEventBindingType.Activating, "#FilterAll",
            EventData.of("Action", "FilterProfession").append("Filter", "ALL"));

        for (int i = 0; i < PROFESSIONS.length; i++) {
            String filterId = "#Filter" + i;
            cmd.set(filterId + ".Visible", true);
            cmd.set(filterId + ".Text", PROFESSION_DISPLAY[i]);
            events.addEventBinding(CustomUIEventBindingType.Activating, filterId,
                EventData.of("Action", "FilterProfession").append("Filter", PROFESSIONS[i]));
        }

        // Get listings
        int totalCount;
        List<ServiceListing> listings;
        int offset = currentPage * ITEMS_PER_PAGE;

        if (professionFilter != null) {
            totalCount = listingRepo.countActiveListingsByProfession(professionFilter);
            listings = listingRepo.getActiveListingsByProfession(professionFilter, offset, ITEMS_PER_PAGE);
        } else {
            totalCount = listingRepo.countActiveListings();
            listings = listingRepo.getActiveListings(offset, ITEMS_PER_PAGE);
        }

        int totalPages = Math.max(1, (totalCount + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);

        if (listings.isEmpty()) {
            cmd.set("#EmptyLabel.Visible", true);
            cmd.set("#EmptyLabel.TextSpans", Message.raw("No services listed yet. Be the first!"));
        } else {
            cmd.set("#EmptyLabel.Visible", false);
            for (ServiceListing listing : listings) {
                appendListingRow(cmd, events, listing);
            }
        }

        buildPagination(cmd, events, totalPages);
        cmd.set("#CreateBtn.Visible", false);
    }

    private void buildMyListingsTab(UICommandBuilder cmd, UIEventBuilder events, ListingRepository listingRepo, UUID myUuid) {
        cmd.set("#FilterRow.Visible", false);
        cmd.set("#CreateBtn.Visible", true);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CreateBtn",
            EventData.of("Action", "CreateListing"));

        List<ServiceListing> myListings = listingRepo.getListingsByProvider(myUuid);

        if (myListings.isEmpty()) {
            cmd.set("#EmptyLabel.Visible", true);
            cmd.set("#EmptyLabel.TextSpans", Message.raw("You have no service listings. Create one!"));
        } else {
            cmd.set("#EmptyLabel.Visible", false);
            for (ServiceListing listing : myListings) {
                appendMyListingRow(cmd, events, listing);
            }
        }

        cmd.set("#PageInfo.TextSpans", Message.raw(myListings.size() + " listing(s)"));
    }

    private void buildMyContractsTab(UICommandBuilder cmd, UIEventBuilder events, ContractRepository contractRepo, UUID myUuid) {
        cmd.set("#FilterRow.Visible", false);
        cmd.set("#CreateBtn.Visible", false);

        List<ServiceContract> asClient = contractRepo.getContractsByClient(myUuid);
        List<ServiceContract> asProvider = contractRepo.getContractsByProvider(myUuid);

        boolean hasAny = !asClient.isEmpty() || !asProvider.isEmpty();

        if (!hasAny) {
            cmd.set("#EmptyLabel.Visible", true);
            cmd.set("#EmptyLabel.TextSpans", Message.raw("No contracts yet. Browse services and hire someone!"));
        } else {
            cmd.set("#EmptyLabel.Visible", false);

            if (!asClient.isEmpty()) {
                cmd.appendInline("#ContentArea",
                    "Label { Text: \"As Client\"; Anchor: (Height: 28); Style: (FontSize: 14, TextColor: #4A9E5C, RenderBold: true); }");
                for (ServiceContract contract : asClient) {
                    appendContractRow(cmd, events, contract, "client");
                }
            }

            if (!asProvider.isEmpty()) {
                cmd.appendInline("#ContentArea",
                    "Label { Text: \"As Provider\"; Anchor: (Height: 28, Top: 8); Style: (FontSize: 14, TextColor: #3278C8, RenderBold: true); }");
                for (ServiceContract contract : asProvider) {
                    appendContractRow(cmd, events, contract, "provider");
                }
            }
        }

        cmd.set("#PageInfo.TextSpans", Message.raw((asClient.size() + asProvider.size()) + " contract(s)"));
    }

    private void appendListingRow(UICommandBuilder cmd, UIEventBuilder events, ServiceListing listing) {
        String id = listing.getId().toString();
        String profColor = getProfessionColor(listing.getProfession());
        String btnId = "#VL" + id.substring(0, 8);
        String displayText = escapeText(listing.getTitle()) + " - " + escapeText(listing.getProviderName())
            + " (" + getProfessionDisplay(listing.getProfession()) + ") - " + listing.getPrice() + " coins";

        String row = String.format("Group #R%s {\n"
            + "    Background: (Color: %s);\n"
            + "    LayoutMode: Left;\n"
            + "    Anchor: (Bottom: 4, Height: 44);\n"
            + "    Padding: (Horizontal: 10);\n"
            + "\n"
            + "    Label {\n"
            + "        Style: (FontSize: 13, TextColor: #ffffff, RenderBold: true, VerticalAlignment: Center);\n"
            + "        FlexWeight: 1;\n"
            + "        Text: \"%s\";\n"
            + "    }\n"
            + "\n"
            + "    Label {\n"
            + "        Style: (FontSize: 12, TextColor: #FFD700, RenderBold: true, VerticalAlignment: Center);\n"
            + "        Anchor: (Width: 80);\n"
            + "        Text: \"%d coins\";\n"
            + "    }\n"
            + "\n"
            + "    TextButton %s {\n"
            + "        Background: (Color: #4A9E5C);\n"
            + "        Anchor: (Width: 55, Height: 28);\n"
            + "        Text: \"View\";\n"
            + "    }\n"
            + "}\n",
            id.substring(0, 8), "#1a2332", escapeText(listing.getTitle()), listing.getPrice(), btnId);

        cmd.appendInline("#ContentArea", row);
        events.addEventBinding(CustomUIEventBindingType.Activating, btnId,
            EventData.of("Action", "ViewListing").append("ListingId", id));
    }

    private void appendMyListingRow(UICommandBuilder cmd, UIEventBuilder events, ServiceListing listing) {
        String id = listing.getId().toString();
        String short8 = id.substring(0, 8);
        String statusText = listing.isActive() ? "Active" : "Paused";
        String statusColor = listing.isActive() ? "#4A9E5C" : "#888888";
        String editBtnId = "#EL" + short8;
        String pauseBtnId = "#PL" + short8;
        String deleteBtnId = "#DL" + short8;

        String row = String.format("Group #M%s {\n"
            + "    Background: (Color: #1a2332);\n"
            + "    LayoutMode: Left;\n"
            + "    Anchor: (Bottom: 4, Height: 44);\n"
            + "    Padding: (Horizontal: 10);\n"
            + "\n"
            + "    Label {\n"
            + "        Style: (FontSize: 13, TextColor: #ffffff, RenderBold: true, VerticalAlignment: Center);\n"
            + "        FlexWeight: 1;\n"
            + "        Text: \"%s\";\n"
            + "    }\n"
            + "\n"
            + "    Label {\n"
            + "        Style: (FontSize: 11, TextColor: %s, VerticalAlignment: Center);\n"
            + "        Anchor: (Width: 70, Right: 8);\n"
            + "        Text: \"%s\";\n"
            + "    }\n"
            + "\n"
            + "    TextButton %s {\n"
            + "        Background: (Color: #4A9E5C);\n"
            + "        Anchor: (Width: 48, Height: 26, Right: 4);\n"
            + "        Text: \"Edit\";\n"
            + "    }\n"
            + "\n"
            + "    TextButton %s {\n"
            + "        Background: (Color: #2a2a3e);\n"
            + "        Anchor: (Width: 60, Height: 26, Right: 4);\n"
            + "        Text: \"%s\";\n"
            + "    }\n"
            + "\n"
            + "    TextButton %s {\n"
            + "        Background: (Color: #882222);\n"
            + "        Anchor: (Width: 40, Height: 26);\n"
            + "        Text: \"Del\";\n"
            + "    }\n"
            + "}\n",
            short8, escapeText(listing.getTitle()), statusColor, statusText,
            editBtnId, pauseBtnId, listing.isActive() ? "Pause" : "Resume", deleteBtnId);

        cmd.appendInline("#ContentArea", row);

        events.addEventBinding(CustomUIEventBindingType.Activating, editBtnId,
            EventData.of("Action", "EditListing").append("ListingId", id));
        events.addEventBinding(CustomUIEventBindingType.Activating, pauseBtnId,
            EventData.of("Action", "PauseListing").append("ListingId", id));
        events.addEventBinding(CustomUIEventBindingType.Activating, deleteBtnId,
            EventData.of("Action", "DeleteListing").append("ListingId", id));
    }

    private void appendContractRow(UICommandBuilder cmd, UIEventBuilder events, ServiceContract contract, String role) {
        String id = contract.getId().toString();
        String short8 = id.substring(0, 8);
        String btnId = "#VC" + short8;
        String otherName = "client".equals(role) ? contract.getProviderName() : contract.getClientName();
        String statusColor = getStatusColor(contract.getStatus());

        String row = String.format("Group #C%s {\n"
            + "    Background: (Color: #1a2332);\n"
            + "    LayoutMode: Left;\n"
            + "    Anchor: (Bottom: 4, Height: 38);\n"
            + "    Padding: (Horizontal: 10);\n"
            + "\n"
            + "    Label {\n"
            + "        Style: (FontSize: 12, TextColor: #ffffff, VerticalAlignment: Center);\n"
            + "        FlexWeight: 1;\n"
            + "        Text: \"%s - %d coins\";\n"
            + "    }\n"
            + "\n"
            + "    Label {\n"
            + "        Style: (FontSize: 11, TextColor: %s, RenderBold: true, VerticalAlignment: Center);\n"
            + "        Anchor: (Width: 80, Right: 8);\n"
            + "        Text: \"%s\";\n"
            + "    }\n"
            + "\n"
            + "    TextButton %s {\n"
            + "        Background: (Color: #4A9E5C);\n"
            + "        Anchor: (Width: 55, Height: 26);\n"
            + "        Text: \"View\";\n"
            + "    }\n"
            + "}\n",
            short8, escapeText(otherName), contract.getPrice(), statusColor,
            contract.getStatus().getDbValue(), btnId);

        cmd.appendInline("#ContentArea", row);
        events.addEventBinding(CustomUIEventBindingType.Activating, btnId,
            EventData.of("Action", "ViewContract").append("ContractId", id));
    }

    private void buildPagination(UICommandBuilder cmd, UIEventBuilder events, int totalPages) {
        cmd.set("#PageInfo.TextSpans", Message.raw("Page " + (currentPage + 1) + " / " + totalPages));

        if (currentPage > 0) {
            cmd.set("#PrevPageBtn.Visible", true);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#PrevPageBtn",
                EventData.of("Action", "PrevPage"));
        }

        if (currentPage < totalPages - 1) {
            cmd.set("#NextPageBtn.Visible", true);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#NextPageBtn",
                EventData.of("Action", "NextPage"));
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull ServiceEventData data) {
        if (data.action == null || data.action.isEmpty()) {
            sendUpdate();
            return;
        }

        switch (data.action) {
            case "SwitchTab" -> {
                activeTab = data.filter != null ? data.filter : "browse";
                currentPage = 0;
                professionFilter = null;
                rebuild();
            }
            case "FilterProfession" -> {
                if ("ALL".equals(data.filter)) {
                    professionFilter = null;
                } else {
                    professionFilter = data.filter;
                }
                currentPage = 0;
                rebuild();
            }
            case "PrevPage" -> {
                if (currentPage > 0) currentPage--;
                rebuild();
            }
            case "NextPage" -> {
                currentPage++;
                rebuild();
            }
            case "ViewListing" -> navigateToListingDetail(data.listingId, ref, store);
            case "ViewContract" -> navigateToContractDetail(data.contractId, ref, store);
            case "CreateListing" -> navigateToCreateListing(null, ref, store);
            case "EditListing" -> navigateToCreateListing(data.listingId, ref, store);
            case "PauseListing" -> handlePauseListing(data.listingId);
            case "DeleteListing" -> handleDeleteListing(data.listingId);
            case "Close" -> close();
            default -> sendUpdate();
        }
    }

    private void navigateToListingDetail(String listingIdStr, Ref<EntityStore> ref, Store<EntityStore> store) {
        if (listingIdStr == null) { sendUpdate(); return; }
        try {
            UUID listingId = UUID.fromString(listingIdStr);
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                ListingDetailPage page = new ListingDetailPage(playerRef, listingId);
                player.getPageManager().openCustomPage(ref, store, (CustomUIPage) page);
            }
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Failed to navigate to listing: " + e.getMessage());
            sendUpdate();
        }
    }

    private void navigateToContractDetail(String contractIdStr, Ref<EntityStore> ref, Store<EntityStore> store) {
        if (contractIdStr == null) { sendUpdate(); return; }
        try {
            UUID contractId = UUID.fromString(contractIdStr);
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                ContractDetailPage page = new ContractDetailPage(playerRef, contractId);
                player.getPageManager().openCustomPage(ref, store, (CustomUIPage) page);
            }
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Failed to navigate to contract: " + e.getMessage());
            sendUpdate();
        }
    }

    private void navigateToCreateListing(String editListingIdStr, Ref<EntityStore> ref, Store<EntityStore> store) {
        try {
            UUID editId = editListingIdStr != null ? UUID.fromString(editListingIdStr) : null;
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                CreateListingPage page = new CreateListingPage(playerRef, editId);
                player.getPageManager().openCustomPage(ref, store, (CustomUIPage) page);
            }
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Failed to navigate to create listing: " + e.getMessage());
            sendUpdate();
        }
    }

    private void handlePauseListing(String listingIdStr) {
        if (listingIdStr == null) { sendUpdate(); return; }
        try {
            HC_ServicesPlugin plugin = HC_ServicesPlugin.getInstance();
            if (plugin == null) { sendUpdate(); return; }
            UUID listingId = UUID.fromString(listingIdStr);
            ServiceListing listing = plugin.getListingRepository().getListingById(listingId);
            if (listing != null && listing.getProviderUuid().equals(playerRef.getUuid())) {
                listing.setStatus(listing.isActive() ? "paused" : "active");
                plugin.getListingRepository().updateListing(listing);
            }
            rebuild();
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Failed to pause listing: " + e.getMessage());
            sendUpdate();
        }
    }

    private void handleDeleteListing(String listingIdStr) {
        if (listingIdStr == null) { sendUpdate(); return; }
        try {
            HC_ServicesPlugin plugin = HC_ServicesPlugin.getInstance();
            if (plugin == null) { sendUpdate(); return; }
            UUID listingId = UUID.fromString(listingIdStr);
            ServiceListing listing = plugin.getListingRepository().getListingById(listingId);
            if (listing != null && listing.getProviderUuid().equals(playerRef.getUuid())) {
                plugin.getListingRepository().deleteListing(listingId);
                playerRef.sendMessage(Message.raw("Listing deleted.").color(Color.GRAY));
            }
            rebuild();
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Failed to delete listing: " + e.getMessage());
            sendUpdate();
        }
    }

    private static String getProfessionColor(String profession) {
        if (profession == null) return "#888888";
        for (int i = 0; i < PROFESSIONS.length; i++) {
            if (PROFESSIONS[i].equalsIgnoreCase(profession)) return PROFESSION_COLORS[i];
        }
        return "#888888";
    }

    private static String getProfessionDisplay(String profession) {
        if (profession == null) return "Unknown";
        for (int i = 0; i < PROFESSIONS.length; i++) {
            if (PROFESSIONS[i].equalsIgnoreCase(profession)) return PROFESSION_DISPLAY[i];
        }
        return profession;
    }

    private static String getStatusColor(ContractStatus status) {
        if (status == null) return "#888888";
        return switch (status) {
            case REQUESTED -> "#FFD700";
            case ACCEPTED -> "#4A9E5C";
            case IN_PROGRESS -> "#3278C8";
            case COMPLETED -> "#55FF55";
            case CANCELLED -> "#888888";
            case DECLINED -> "#C83232";
        };
    }

    private static String escapeText(String text) {
        if (text == null) return "";
        return text.replace("\"", "'").replace("<", "").replace(">", "");
    }
}
