package fr.luuuccasss.hygiene.mixin;

import fr.luuuccasss.hygiene.Dirty;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements Dirty {
    @Unique private float hygiene$dirt;

    @Override
    public float hygiene$getDirt() {
        return hygiene$dirt;
    }

    @Override
    public void hygiene$setDirt(float dirt) {
        hygiene$dirt = MathHelper.clamp(dirt, 0f, 100f);
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void hygiene$write(NbtCompound nbt, CallbackInfo ci) {
        nbt.putFloat("HygieneDirt", hygiene$dirt);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void hygiene$read(NbtCompound nbt, CallbackInfo ci) {
        hygiene$setDirt(nbt.getFloat("HygieneDirt"));
    }

    /** La crasse survit à la mort : on ne se lave pas en mourant. */
    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void hygiene$copyFrom(ServerPlayerEntity old, boolean alive, CallbackInfo ci) {
        hygiene$setDirt(((Dirty) old).hygiene$getDirt());
    }
}
