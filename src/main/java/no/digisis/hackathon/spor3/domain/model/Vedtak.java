package no.digisis.hackathon.spor3.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Vedtal - utfallet av saksbehandlingen.
 *
 * Sealed hierarki representerer de fire mulige vedtakene som et lukket sett.
 * Dette er den sentrale sum-typen i domenet: pattern-matching på Vedtak er
 * uttømmende og uten behov for null-sjekk eller exception-throwing.
 *
 * Hvert vedtak bærer med seg sin søknads-id og tidspunkt for sporing.
 */
public sealed interface Vedtak permits
        Vedtak.Innvilget,
        Vedtak.Engangsstonad,
        Vedtak.ManuellVurdering,
        Vedtak.Avslag {

    String vedtakId();
    String soknadId();
    LocalDateTime fattetTidspunkt();

    /**
     * Foreldrepenger innvilget med beregnet grunnlag, stønadsperiode og kvoter.
     */
    record Innvilget(
            String vedtakId,
            String soknadId,
            LocalDateTime fattetTidspunkt,
            Penger beregningsgrunnlag,
            int totalUker,
            Kvoter kvoter,
            int dekningsgrad
    ) implements Vedtak {}

    /**
     * Engangsstønad: søker er norsk borger men mangler opptjening.
     * Fast beløp uavhengig av inntekt (§ 14-17).
     */
    record Engangsstonad(
            String vedtakId,
            String soknadId,
            LocalDateTime fattetTidspunkt,
            Penger belop,
            String begrunnelse
    ) implements Vedtak {}

    /**
     * Manuell vurdering nødvendig - saksbehandler må se på saken.
     * Typisk grunn: for stort avvik mellom 3-mnd snitt og oppgitt inntekt (§ 14-7).
     */
    record ManuellVurdering(
            String vedtakId,
            String soknadId,
            LocalDateTime fattetTidspunkt,
            String begrunnelse
    ) implements Vedtak {}

    /**
     * Avslag: søker er ikke norsk borger og har ikke rett til engangsstønad.
     */
    record Avslag(
            String vedtakId,
            String soknadId,
            LocalDateTime fattetTidspunkt,
            String begrunnelse
    ) implements Vedtak {}

    // Factory-metoder for ren konstruksjon

    static Vedtak innvilget(String soknadId, Penger grunnlag, int totalUker, Kvoter kvoter, int dekningsgrad) {
        return new Innvilget(nyId(), soknadId, LocalDateTime.now(), grunnlag, totalUker, kvoter, dekningsgrad);
    }

    static Vedtak engangsstonad(String soknadId) {
        return new Engangsstonad(nyId(), soknadId, LocalDateTime.now(),
                Penger.ENGANGSSTONAD, "Ikke opptjening, men norsk borger — rett til engangsstønad § 14-17");
    }

    static Vedtak manuellVurdering(String soknadId, String begrunnelse) {
        return new ManuellVurdering(nyId(), soknadId, LocalDateTime.now(), begrunnelse);
    }

    static Vedtak avslag(String soknadId, String begrunnelse) {
        return new Avslag(nyId(), soknadId, LocalDateTime.now(), begrunnelse);
    }

    private static String nyId() {
        return "vedtak-" + UUID.randomUUID().toString().substring(0, 8);
    }
}