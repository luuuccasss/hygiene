package fr.luuuccasss.hygiene;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3f;

/**
 * Ce que tout le monde voit et entend autour d'un joueur sale.
 * Les clients qui ont le mod reçoivent un {@link StinkPayload} et dessinent eux-mêmes des mouches en 3D ;
 * les autres voient des particules. Sans le mod, le joueur concerné ne voit que quelques mouches
 * tenues à distance de sa caméra, pour que les particules ne lui bouchent pas la vue.
 */
public final class Stench {
    private static final DustParticleEffect FLY = new DustParticleEffect(new Vector3f(0.05f, 0.05f, 0.05f), 0.6f);
    private static final DustParticleEffect SMELL = new DustParticleEffect(new Vector3f(0.45f, 0.62f, 0.12f), 1.3f);
    private static final double VIEW_DISTANCE = 32;

    private Stench() {
    }

    /** Appelé toutes les 5 ticks. */
    public static void show(ServerWorld world, ServerPlayerEntity player, Stage stage) {
        Random random = world.random;
        List<ServerPlayerEntity> nearby = world.getPlayers(p -> p.squaredDistanceTo(player) < VIEW_DISTANCE * VIEW_DISTANCE);
        StinkPayload payload = new StinkPayload(player.getId(), stage.ordinal());
        List<ServerPlayerEntity> others = new ArrayList<>();
        boolean selfModded = false;
        for (ServerPlayerEntity viewer : nearby) {
            if (ServerPlayNetworking.canSend(viewer, StinkPayload.ID)) {
                ServerPlayNetworking.send(viewer, payload);
                if (viewer == player) selfModded = true;
            } else if (viewer != player) {
                others.add(viewer);
            }
        }
        int flies = switch (stage) {
            case ODEUR -> 1;
            case PUANT -> 4;
            case INFECT -> 9;
            default -> 0;
        };
        // mouches qui tournent autour de la tête et du corps
        for (int i = 0; i < flies; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = 0.4 + random.nextDouble() * 0.5;
            send(world, others, FLY, player.getX() + Math.cos(angle) * radius,
                player.getY() + 0.6 + random.nextDouble() * 1.4, player.getZ() + Math.sin(angle) * radius, 1, 0.02, 0);
        }
        // pour le joueur sale sans le mod : quelques mouches à bonne distance, sous le niveau des yeux
        for (int i = 0; i < (selfModded ? 0 : (flies + 2) / 3); i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = 1.2 + random.nextDouble() * 0.6;
            world.spawnParticles(player, FLY, false, player.getX() + Math.cos(angle) * radius,
                player.getY() + 0.3 + random.nextDouble() * 1.1, player.getZ() + Math.sin(angle) * radius, 1, 0.02, 0.02, 0.02, 0);
        }
        // nuage d'odeur qui monte (le joueur sale ne le voit qu'à ses pieds)
        if (stage.ordinal() >= Stage.PUANT.ordinal()) {
            int count = stage == Stage.INFECT ? 4 : 2;
            send(world, others, SMELL, player.getX(), player.getY() + 0.3, player.getZ(), count, 0.35, 0.5);
            world.spawnParticles(player, SMELL, false, player.getX(), player.getY() + 0.05, player.getZ(), 1, 0.5, 0.02, 0.5, 0);
        }
        // bourdonnement
        if (stage.ordinal() >= Stage.PUANT.ordinal() && random.nextInt(stage == Stage.INFECT ? 6 : 12) == 0) {
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_BEE_LOOP, SoundCategory.PLAYERS,
                stage == Stage.INFECT ? 0.5f : 0.3f, 1.8f + random.nextFloat() * 0.2f);
        }
    }

    private static void send(ServerWorld world, List<ServerPlayerEntity> viewers, ParticleEffect effect,
                             double x, double y, double z, int count, double spreadXZ, double spreadY) {
        for (ServerPlayerEntity viewer : viewers) {
            world.spawnParticles(viewer, effect, false, x, y, z, count, spreadXZ, spreadY, spreadXZ, 0.01);
        }
    }
}
