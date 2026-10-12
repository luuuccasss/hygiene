package fr.luuuccasss.hygiene;

import net.minecraft.util.Formatting;

/** Stades de saleté, de propre à infect. */
public enum Stage {
    PROPRE("propre", 0f, Formatting.AQUA, 0, 0, 0),
    ODEUR("odeur", 25f, Formatting.YELLOW, 0.20, 0, 5),
    PUANT("puant", 50f, Formatting.GOLD, 0.50, 8, 9),
    INFECT("infect", 75f, Formatting.DARK_GREEN, 0, 12, 13);

    public final String label;
    public final float threshold;
    public final Formatting color;
    /** Hausse des prix chez les villageois (l'infect se fait refuser tout échange). */
    public final double priceMalus;
    /** Rayon dans lequel les animaux fuient, 0 = pas de fuite. */
    public final int animalRadius;
    /** Rayon dans lequel les villageois réagissent. */
    public final int villagerRadius;

    Stage(String label, float threshold, Formatting color, double priceMalus, int animalRadius, int villagerRadius) {
        this.label = label;
        this.threshold = threshold;
        this.color = color;
        this.priceMalus = priceMalus;
        this.animalRadius = animalRadius;
        this.villagerRadius = villagerRadius;
    }

    public static Stage of(float dirt) {
        Stage result = PROPRE;
        for (Stage s : values()) if (dirt >= s.threshold) result = s;
        return result;
    }
}
