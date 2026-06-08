package no.digisis.hackathon.spor3.domain.model;

import java.time.YearMonth;

/**
 * Én måneds inntektsregistrering fra a-ordningen.
 * Value object - uforanderlig, identitet basert på innhold.
 */
public record Inntektsregistrering(YearMonth maned, Inntektstype type, int belop) {
    public Inntektsregistrering {
        if (belop < 0) throw new IllegalArgumentException("Beløp kan ikke være negativt");
    }

    public boolean erGodkjentForOpptjening() {
        return type.erGodkjentForOpptjening() && belop > 0;
    }
}