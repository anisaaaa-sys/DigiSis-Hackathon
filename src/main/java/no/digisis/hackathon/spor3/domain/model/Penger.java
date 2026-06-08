package no.digisis.hackathon.spor3.domain.model;

/**
 * Pengebeløp i hele kroner.
 * Value object - uforanderlig, ingen negativ verdi.
 */
public record Penger(int kroner) implements Comparable<Penger> {
    // Grunnbeløp-konstanter for 2026
    public static final Penger HALV_G = new Penger(68_274);
    public static final Penger SEKS_G = new Penger(819_294);
    public static final Penger ENGANGSSTONAD = new Penger(92_648);

    public Penger {
        if (kroner < 0) throw new IllegalArgumentException("Pengebeløp kan ikke være negativt");
    }

    public static Penger av(int kroner) {
        return new Penger(kroner);
    }

    public Penger min(Penger other) {
        return this.kroner <= other.kroner ? this : other;
    }

    public boolean erMindreEnn(Penger other) {
        return this.kroner < other.kroner;
    }

    @Override
    public int compareTo(Penger other) {
        return Integer.compare(this.kroner, other.kroner);
    }

    @Override
    public String toString() {
        return kroner + " kr";
    }
}