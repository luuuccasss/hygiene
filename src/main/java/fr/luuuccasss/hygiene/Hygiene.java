package fr.luuuccasss.hygiene;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

public class Hygiene implements ModInitializer {
    public static final String MOD_ID = "hygiene";

    /** Saleté gagnée par tick : 100 % en 50 minutes sans rien faire. */
    private static final float BASE_RATE = 1f / 600f;
    private static final float SPRINT_RATE = 1f / 300f;
    private static final float BLOCK_BREAK = 0.1f;
    private static final float ATTACK = 0.25f;
    private static final float WASH_WATER = 0.6f;
    private static final float WASH_SUBMERGED = 1.5f;
    private static final float WASH_RAIN = 0.04f;

    private static final Map<UUID, Stage> LAST_STAGE = new HashMap<>();

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(StinkPayload.ID, StinkPayload.CODEC);
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            for (ServerPlayerEntity player : world.getPlayers()) tickPlayer(world, player);
        });
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof Dirty dirty) dirty.hygiene$setDirt(dirty.hygiene$getDirt() + BLOCK_BREAK);
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> HygieneCommand.register(dispatcher));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> forget(handler.getPlayer()));
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (player instanceof Dirty dirty) dirty.hygiene$setDirt(dirty.hygiene$getDirt() + ATTACK);
            return ActionResult.PASS;
        });
    }

    public static void tickPlayer(ServerWorld world, ServerPlayerEntity player) {
        if (player.isSpectator() || player.isCreative() || !player.isAlive()) return;
        Dirty dirty = (Dirty) player;
        float dirt = dirty.hygiene$getDirt();
        if (player.isSubmergedInWater()) dirt -= WASH_SUBMERGED;
        else if (player.isTouchingWater()) dirt -= WASH_WATER;
        else if (player.isTouchingWaterOrRain()) dirt -= WASH_RAIN;
        else dirt += BASE_RATE + (player.isSprinting() ? SPRINT_RATE : 0f);
        dirty.hygiene$setDirt(dirt);
        Stage stage = dirty.hygiene$getStage();
        announceStage(world, player, stage);
        if (stage != Stage.PROPRE && (world.getTime() + player.getId()) % 5 == 0) Stench.show(world, player, stage);
        if (stage != Stage.PROPRE && (world.getTime() + player.getId()) % 10 == 0) Reactions.react(world, player, stage);
    }

    private static void announceStage(ServerWorld world, ServerPlayerEntity player, Stage stage) {
        Stage previous = LAST_STAGE.put(player.getUuid(), stage);
        if (previous == null || previous == stage) return;
        if (stage.ordinal() > previous.ordinal()) {
            String message = switch (stage) {
                case ODEUR -> "Tu commences à sentir un peu…";
                case PUANT -> "Tu pues. Les mouches t'adorent, les animaux te fuient.";
                case INFECT -> "Tu es une infection ambulante. Va te laver !";
                default -> null;
            };
            if (message != null) player.sendMessage(Text.literal(message).formatted(stage.color), true);
        } else if (stage == Stage.PROPRE) {
            player.sendMessage(Text.literal("Ahh… tout propre !").formatted(Stage.PROPRE.color), true);
            world.spawnParticles(ParticleTypes.BUBBLE_POP, player.getX(), player.getBodyY(0.6), player.getZ(),
                25, 0.4, 0.6, 0.4, 0.02);
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_SPLASH, SoundCategory.PLAYERS, 0.6f, 1.3f);
        } else {
            player.sendMessage(Text.literal("Tu sens déjà un peu moins.").formatted(stage.color), true);
        }
    }

    public static void forget(ServerPlayerEntity player) {
        LAST_STAGE.remove(player.getUuid());
        Reactions.forget(player);
    }
}
