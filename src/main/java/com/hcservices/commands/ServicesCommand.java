package com.hcservices.commands;

import com.hcservices.pages.ServicesBoardPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class ServicesCommand extends AbstractPlayerCommand {

    public ServicesCommand() {
        super("services", "Open the services board");
        this.addAliases("sb");
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {
        // AbstractPlayerCommand runs off-world-thread, so wrap in world.execute()
        world.execute(() -> {
            Store<EntityStore> worldStore = world.getEntityStore().getStore();
            Ref<EntityStore> freshRef = worldStore.getExternalData().getRefFromUUID(playerRef.getUuid());
            if (freshRef == null || !freshRef.isValid()) return;

            Player player = worldStore.getComponent(freshRef, Player.getComponentType());
            if (player == null) return;

            ServicesBoardPage page = new ServicesBoardPage(playerRef);
            player.getPageManager().openCustomPage(freshRef, worldStore, (CustomUIPage) page);
        });
    }
}
