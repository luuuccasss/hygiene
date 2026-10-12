package fr.luuuccasss.hygiene.client;

import fr.luuuccasss.hygiene.StinkPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public class HygieneClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(StinkPayload.ID,
            (payload, context) -> FlySwarmRenderer.update(payload.entityId(), payload.stage()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> FlySwarmRenderer.clear());
        WorldRenderEvents.AFTER_ENTITIES.register(FlySwarmRenderer::render);
    }
}
