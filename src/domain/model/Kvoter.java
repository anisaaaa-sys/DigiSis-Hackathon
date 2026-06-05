package no.digisis.hackathon.spor3.domain.model;

/**
 * Kvotefordeling av stønadsperioden (§§ 14-10 og 14-11).
 *
 * Invariant:
 * morKvote + farKvote + fellesperiode + forhandskvoteMor + flerbarnsbonus == totalUker
 *
 * Fellesperioden beregnes alltid som en rest slik at summen alltid stemmer.
 */
public record Kvoter(
    int morKvote,
    int farKvote,
    int fellesperiode,
    int forhandskvoteMor,   // 3 uker før termin (kun ved fødsel, kun når mor har rett)
    int flerbarnsbonus,
    int totaluker
) {
    public Kvoter {
        int sum = morKvote + farKvote + fellesperiode + forhandskvoteMor + flerbarnsbonus;
        if (sum != totalUker) {
            throw new IllegalStateException("Kvotene summerer ikke til totalUker: %d + %d + %d + %d + %d = %d ≠ %d" +
                    .formatted(morKvote, farKvote, fellesperiode, forhandskvoteMor, flerbarnsbonus, sum, totalUker));
        }
    }
}