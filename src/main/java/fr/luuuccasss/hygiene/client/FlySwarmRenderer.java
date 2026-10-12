package fr.luuuccasss.hygiene.client;

import fr.luuuccasss.hygiene.Hygiene;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

/** Essaim de mouches en 3D autour des joueurs sales, entièrement côté client. */
public final class FlySwarmRenderer {
    private static final Identifier TEXTURE = Identifier.of(Hygiene.MOD_ID, "textures/entity/mouche.png");
    private static final int[] FLIES_PER_STAGE = {0, 2, 6, 14};
    private static final float SCALE = 0.55f;
    private static final long EXPIRY_MS = 1500;

    private static final Map<Integer, Swarm> SWARMS = new HashMap<>();
    private static ModelPart model;

    private record Swarm(int stage, long expiresAt) {
    }

    private FlySwarmRenderer() {
    }

    public static void update(int entityId, int stage) {
        SWARMS.put(entityId, new Swarm(stage, System.currentTimeMillis() + EXPIRY_MS));
    }

    public static void clear() {
        SWARMS.clear();
    }

    private static ModelPart model() {
        if (model == null) {
            ModelData data = new ModelData();
            ModelPartData root = data.getRoot();
            root.addChild("body", ModelPartBuilder.create().uv(0, 0).cuboid(-1, -1, -1.5f, 2, 2, 3), ModelTransform.NONE);
            root.addChild("head", ModelPartBuilder.create().uv(0, 6).cuboid(-1, -1, -2.5f, 2, 2, 1), ModelTransform.NONE);
            root.addChild("left_wing", ModelPartBuilder.create().uv(0, 10).cuboid(0, 0, -1.5f, 3, 0, 3), ModelTransform.pivot(0.5f, -1, 0.5f));
            root.addChild("right_wing", ModelPartBuilder.create().uv(0, 10).mirrored().cuboid(-3, 0, -1.5f, 3, 0, 3), ModelTransform.pivot(-0.5f, -1, 0.5f));
            model = TexturedModelData.of(data, 16, 16).createModel();
        }
        return model;
    }

    public static void render(WorldRenderContext context) {
        if (SWARMS.isEmpty() || context.consumers() == null || context.matrixStack() == null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = context.world();
        float tickDelta = context.tickCounter().getTickDelta(true);
        double time = (world.getTime() + tickDelta) / 20.0;
        Vec3d camera = context.camera().getPos();
        MatrixStack matrices = context.matrixStack();
        VertexConsumer consumer = context.consumers().getBuffer(RenderLayer.getEntityTranslucent(TEXTURE));
        ModelPart root = model();
        ModelPart leftWing = root.getChild("left_wing");
        ModelPart rightWing = root.getChild("right_wing");
        long now = System.currentTimeMillis();

        for (Iterator<Map.Entry<Integer, Swarm>> it = SWARMS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Swarm> entry = it.next();
            if (entry.getValue().expiresAt() < now) {
                it.remove();
                continue;
            }
            Entity entity = world.getEntityById(entry.getKey());
            if (entity == null || entity.isInvisible()) continue;
            boolean firstPersonSelf = entity == client.player && client.options.getPerspective() == Perspective.FIRST_PERSON;
            Vec3d base = entity.getLerpedPos(tickDelta);
            int count = FLIES_PER_STAGE[Math.min(entry.getValue().stage(), FLIES_PER_STAGE.length - 1)];
            if (firstPersonSelf) count = (count + 1) / 2;

            for (int i = 0; i < count; i++) {
                Random random = new Random(entity.getId() * 1000L + i);
                double radius = 0.45 + random.nextDouble() * 0.45 + (firstPersonSelf ? 0.8 : 0);
                double height = 0.4 + random.nextDouble() * 1.5;
                double speed = (1.8 + random.nextDouble() * 2.5) * (random.nextBoolean() ? 1 : -1);
                double phase = random.nextDouble() * Math.PI * 2;
                // trajectoire en zigzag : orbite + rayon et hauteur qui tremblent
                double angle = phase + speed * time + 0.6 * Math.sin(time * 3.1 + phase);
                double r = radius * (1 + 0.25 * Math.sin(time * 4.3 + phase * 2));
                double y = height + 0.25 * Math.sin(time * 5.7 + phase) + 0.08 * Math.sin(time * 23 + phase);
                Vec3d pos = base.add(Math.cos(angle) * r, y, Math.sin(angle) * r);
                if (firstPersonSelf && pos.squaredDistanceTo(camera) < 1.3 * 1.3) continue;
                // direction de vol = tangente à l'orbite
                float yaw = (float) (Math.atan2(-Math.sin(angle) * speed, Math.cos(angle) * speed));

                matrices.push();
                matrices.translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotation(-yaw));
                matrices.scale(-SCALE, -SCALE, SCALE);
                float flap = (float) Math.sin(time * 90 + i) * 0.7f;
                leftWing.roll = 0.35f + flap;
                rightWing.roll = -0.35f - flap;
                int light = WorldRenderer.getLightmapCoordinates(world, BlockPos.ofFloored(pos));
                root.render(matrices, consumer, light, OverlayTexture.DEFAULT_UV);
                matrices.pop();
            }
        }
    }
}
