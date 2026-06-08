package no.digisis.hackathon.spor3.domain.model;

/**
 * Godkjente og ikke godkjente inntektstyper fra a-ordningen.
 * § 14-6: STIPEND_LANEKASSEN teller ikke som opptjening.
 */
public enum Inntektstype {
    ARBEID(true),

    SYKEPENGER(true),

    FORELDREPENGER(true),

    SVANGERSKAPSPENGER(true),

    DAPENGER(true),

    AAP(true),

    PLEIEPENGER(true),

    STIPEND_LANEKASSEN(false);

    private final boolean godkjentForOpptjening;

    Inntektstype(boolean godkjentForOpptjening) {
        this.godkjentForOpptjening = godkjentForOpptjening;
    }

    public boolean erGodkjentForOpptjening() {
        return godkjentForOpptjening;
    }
}
