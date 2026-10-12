package fr.luuuccasss.hygiene;

import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3f;

/** Ce que tout le monde voit et entend autour d'un joueur sale. */
public final class Stench {
    private static final DustParticleEffect FLY = new DustParticleEffect(new Vector3f(0.05f, 0.05f, 0.05f), 0.6f);
    private static final DustParticleEffect SMELL = new DustParticleEffect(new Vector3f(0.45f, 0.62f, 0.12f), 1.3f);

    private Stench() {
    }

    /** Appelé toutes les 5 ticks. */
    public static void show(ServerWorld world, ServerPlayerEntity player, Stage stage) {
        Random random = world.random;
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
            world.spawnParticles(FLY, player.getX() + Math.cos(angle) * radius,
                player.getY() + 0.6 + random.nextDouble() * 1.4, player.getZ() + Math.sin(angle) * radius,
                1, 0.02, 0.02, 0.02, 0);
        }
        // nuage d'odeur qui monte
        if (stage.ordinal() >= Stage.PUANT.ordinal()) {
            world.spawnParticles(SMELL, player.getX(), player.getY() + 0.3, player.getZ(),
                stage == Stage.INFECT ? 4 : 2, 0.35, 0.5, 0.35, 0.01);
        }
        // bourdonnement
        if (stage.ordinal() >= Stage.PUANT.ordinal() && random.nextInt(stage == Stage.INFECT ? 6 : 12) == 0) {
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_BEE_LOOP, SoundCategory.PLAYERS,
                stage == Stage.INFECT ? 0.5f : 0.3f, 1.8f + random.nextFloat() * 0.2f);
        }
    }
}
