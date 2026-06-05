package no.digisis.hackathon.spor3.domain.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Søknad om foreldrepenger - domeneobjekt med identitet (fnr + id).
 *
 * Uforanderlig etter opprettelse (record). Inneholder rå søknadsdata
 * slik de kom inn; domenereglene appliseres av service-laget.
 */
public record Soknad(
        String id,
        String beskrivelse,
        String fnr,
        boolean erNorskBorger,
        LocalDate termindato,
        int oppgittArsinntekt,
        List<Inntektsregistrering> inntektshistorikk,
        int antallBarn,
        Rettsforhold rettsforhold,
        int dekningsgrad
) {
    public Soknad {
        if (fnr == null || fnr.length != 11)
            throw new IllegalArgumentException("FNR må ha 11 siffer");
        if (dekningsgrad != 100 && dekningsgrad != 80)
            throw new IllegalArgumentException("Dekningsgrad må være 100 eller 80");
        if (antallBarn < 1)
            throw new IllegalArgumentException("Antall barn må være >= 1");
        inntektshistorikk = List.copyOf(inntektshistorikk); // defensiv kopi
    }
}