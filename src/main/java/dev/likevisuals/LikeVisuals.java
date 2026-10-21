package dev.likevisuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.util.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LikeVisuals implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("LikeVisuals");

    @Override
    public void onInitializeClient() {
        Modules.init();
        ClientTickEvents.END_CLIENT_TICK.register(Modules::tick);
        HudRenderCallback.EVENT.register(Modules::hud);
        WorldRenderEvents.LAST.register(ctx -> Modules.world(ctx.matrixStack(), ctx.camera().getPos(), ctx.tickCounter().getTickDelta(false)));

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient()) {
                if (Friends.blocks(entity)) { U.bar("\u00a7aЭто твой друг: удар отменён"); return ActionResult.FAIL; }
                Modules.attack(entity);
            }
            return ActionResult.PASS;
        });

        ClientReceiveMessageEvents.GAME.register((msg, overlay) -> { if (!overlay) Utils.onGameMessage(msg); });
        ClientReceiveMessageEvents.MODIFY_GAME.register((msg, overlay) -> overlay ? msg : Utils.modifyMessage(msg));
        ClientSendMessageEvents.ALLOW_COMMAND.register(Combat::allowCommand);
        ClientSendMessageEvents.ALLOW_CHAT.register(msg -> !Tools.onChat(msg));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> Tools.commands(dispatcher));

        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof HandledScreen<?> hs)
                ScreenEvents.afterRender(screen).register((s, ctx, mx, my, td) -> Visuals2.highlightSlots(hs, ctx));
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> Modules.save());
        LOG.info("LikeVisuals loaded: {} modules", Modules.ALL.size());
    }
}
