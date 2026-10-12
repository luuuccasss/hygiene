package fr.luuuccasss.hygiene;

/** Interface ajoutée à ServerPlayerEntity par mixin. */
public interface Dirty {
    float hygiene$getDirt();

    void hygiene$setDirt(float dirt);

    default Stage hygiene$getStage() {
        return Stage.of(hygiene$getDirt());
    }
}
