package fr.luuuccasss.hygiene;

import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Comment le monde réagit à un joueur qui pue. Appelé toutes les 10 ticks. */
public final class Reactions {
    private Reactions() {
    }

    public static void react(ServerWorld world, ServerPlayerEntity player, Stage stage) {
        if (stage.animalRadius > 0) animalsFlee(world, player, stage.animalRadius);
    }

    /** Les animaux s'enfuient, même ton chien. */
    private static void animalsFlee(ServerWorld world, ServerPlayerEntity player, int radius) {
        Box area = player.getBoundingBox().expand(radius);
        for (AnimalEntity animal : world.getEntitiesByClass(AnimalEntity.class, area, a -> a.isAlive() && !a.isLeashed() && !a.hasPassengers())) {
            if (animal instanceof TameableEntity tameable && tameable.isSitting()) continue;
            Vec3d away = NoPenaltyTargeting.findFrom(animal, 12, 6, player.getPos());
            if (away != null) animal.getNavigation().startMovingTo(away.x, away.y, away.z, 1.5);
        }
    }
}
