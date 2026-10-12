package fr.luuuccasss.hygiene;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Envoyé aux clients qui ont le mod : tel joueur est à tel stade de saleté (pour dessiner les mouches en 3D). */
public record StinkPayload(int entityId, int stage) implements CustomPayload {
    public static final CustomPayload.Id<StinkPayload> ID = new CustomPayload.Id<>(Identifier.of(Hygiene.MOD_ID, "stink"));
    public static final PacketCodec<RegistryByteBuf, StinkPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.VAR_INT, StinkPayload::entityId,
        PacketCodecs.VAR_INT, StinkPayload::stage,
        StinkPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
