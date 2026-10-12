package fr.luuuccasss.hygiene;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/** Comment le monde réagit à un joueur qui pue. Appelé toutes les 10 ticks. */
public final class Reactions {
    private Reactions() {
    }

    public static void react(ServerWorld world, ServerPlayerEntity player, Stage stage) {
        if (stage.animalRadius > 0) animalsFlee(world, player, stage.animalRadius);
        if (stage.villagerRadius > 0) villagersReact(world, player, stage);
        if (stage == Stage.INFECT) nauseateNeighbours(world, player);
    }

    /** Les joueurs collés à un joueur infect ont la nausée. */
    private static void nauseateNeighbours(ServerWorld world, ServerPlayerEntity stinky) {
        for (ServerPlayerEntity other : world.getPlayers(p -> p != stinky && p.isAlive() && !p.isSpectator()
            && p.squaredDistanceTo(stinky) < 9)) {
            if (!other.hasStatusEffect(StatusEffects.NAUSEA)) {
                other.sendMessage(Text.literal(stinky.getName().getString() + " pue atrocement…").formatted(Stage.INFECT.color), true);
            }
            other.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 140, 0, false, false));
        }
    }

    /**
     * Odeur : les villageois secouent la tête.
     * Puant : ils s'écartent en se bouchant le nez.
     * Infect : ils rentrent chez eux en courant.
     */
    private static void villagersReact(ServerWorld world, ServerPlayerEntity player, Stage stage) {
        Box area = player.getBoundingBox().expand(stage.villagerRadius);
        List<VillagerEntity> villagers = world.getEntitiesByClass(VillagerEntity.class, area,
            v -> v.isAlive() && !v.isSleeping() && !v.hasCustomer());
        if (villagers.isEmpty()) return;
        Random random = world.random;
        for (VillagerEntity villager : villagers) {
            if (villager.getHeadRollingTimeLeft() <= 0 && random.nextInt(3) == 0) villager.setHeadRollingTimeLeft(25);
            if (random.nextInt(stage == Stage.ODEUR ? 12 : 5) == 0) {
                world.playSound(null, villager.getBlockPos(), SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.NEUTRAL,
                    0.8f, 0.9f + random.nextFloat() * 0.2f);
            }
            if (stage == Stage.ODEUR) continue;
            world.spawnParticles(ParticleTypes.SNEEZE, villager.getX(), villager.getEyeY(), villager.getZ(), 2, 0.2, 0.1, 0.2, 0.01);
            Vec3d target = stage == Stage.INFECT ? home(world, villager) : null;
            if (target == null) target = NoPenaltyTargeting.findFrom(villager, 14, 6, player.getPos());
            if (target != null) {
                villager.getBrain().remember(MemoryModuleType.WALK_TARGET,
                    new WalkTarget(target, stage == Stage.INFECT ? 0.9f : 0.75f, 1));
            }
        }
        comment(player, stage, random);
    }

    private static Vec3d home(ServerWorld world, VillagerEntity villager) {
        return villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.HOME)
            .filter(pos -> pos.dimension() == world.getRegistryKey())
            .map(pos -> Vec3d.ofBottomCenter(pos.pos()))
            .orElse(null);
    }

    private static final Map<UUID, Long> LAST_COMMENT = new HashMap<>();
    private static final String[][] COMMENTS = {
        {},
        {"Un villageois fronce le nez.", "« Hrmm… tu connais l'eau ? »", "Un villageois te regarde de travers."},
        {"Les villageois s'écartent sur ton passage.", "« HRMMM ! » Un villageois s'enfuit en se bouchant le nez.",
            "Un villageois retient sa respiration en te croisant."},
        {"Les villageois rentrent chez eux en courant.", "Quelqu'un vient de claquer sa porte.",
            "« HRRRMMM !! » Le village entier se barricade."},
    };

    /** Commentaire dans la barre d'action, au plus toutes les 5 secondes. */
    private static void comment(ServerPlayerEntity player, Stage stage, Random random) {
        long now = player.getServerWorld().getTime();
        Long last = LAST_COMMENT.get(player.getUuid());
        if (last != null && now - last < 100) return;
        LAST_COMMENT.put(player.getUuid(), now);
        String[] lines = COMMENTS[stage.ordinal()];
        player.sendMessage(Text.literal(lines[random.nextInt(lines.length)]).formatted(stage.color), true);
    }

    public static void forget(ServerPlayerEntity player) {
        LAST_COMMENT.remove(player.getUuid());
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
