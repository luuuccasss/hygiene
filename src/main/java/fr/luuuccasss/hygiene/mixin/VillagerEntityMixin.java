package fr.luuuccasss.hygiene.mixin;

import fr.luuuccasss.hygiene.Dirty;
import fr.luuuccasss.hygiene.Stage;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.village.TradeOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin {
    /** Un joueur infect se fait refuser tout échange. */
    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void hygiene$refuse(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (!(player instanceof Dirty dirty) || dirty.hygiene$getStage() != Stage.INFECT) return;
        VillagerEntity self = (VillagerEntity) (Object) this;
        if (self.isBaby()) return;
        self.setHeadRollingTimeLeft(40);
        self.getWorld().playSound(null, self.getBlockPos(), SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.NEUTRAL, 1f, 0.8f);
        player.sendMessage(Text.literal("« Va te laver avant de revenir ! »").formatted(Stage.INFECT.color), true);
        cir.setReturnValue(ActionResult.SUCCESS);
    }

    /** Plus le joueur pue, plus le villageois demande cher pour qu'il s'en aille vite. */
    @Inject(method = "prepareOffersFor", at = @At("TAIL"))
    private void hygiene$smellyPrices(PlayerEntity player, CallbackInfo ci) {
        if (!(player instanceof Dirty dirty)) return;
        double malus = dirty.hygiene$getStage().priceMalus;
        if (malus <= 0) return;
        for (TradeOffer offer : ((VillagerEntity) (Object) this).getOffers()) {
            int base = offer.getOriginalFirstBuyItem().getCount();
            offer.increaseSpecialPrice(Math.max(1, (int) Math.ceil(base * malus)));
        }
    }
}
