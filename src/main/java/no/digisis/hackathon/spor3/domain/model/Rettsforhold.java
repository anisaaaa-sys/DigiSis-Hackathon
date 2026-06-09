package no.digisis.hackathon.spor3.domain.model;

public enum Rettsforhold {
    BEGGE("begge"),

    KUN_MOR("kun-mor"),

    KUN_FAR("kun-far");

    private final String kode;

    Rettsforhold(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }

    public static Rettsforhold fraKode(String kode) {
        for (Rettsforhold r : values()) {
            if (r.kode.equalsIgnoreCase(kode)) return r;
        }
        throw new IllegalArgumentException("Ukjent rettsforhold: " + kode);
    }
}