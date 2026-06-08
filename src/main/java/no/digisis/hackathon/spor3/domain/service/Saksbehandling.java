package no.digisis.hackathon.spor3.domain.service;

import no.digisis.hackathon.spor3.domain.model.Beregningsgrunlag;
import no.digisis.hackathon.spor3.domain.model.Kvoter;
import no.digisis.hackathon.spor3.domain.model.Soknad;
import no.digisis.hackathon.spor3.domain.model.Vedtak;

/**
 * Soknadsbehandling - orkestrerer de fem reglene fra Søknad til Vedtak.
 *
 * Sekvens:
 *    1. Opptjeningsvurdering (regel 1 + fallback regel 2)
 *    2. Beregningsgrunnlag (regel 3) - kun hvis opptjening OK
 *    3. Stønadsperiode-oppsøag (regel 4) - kun hvis grunnlag OK
 *    4. Kvotefordeling (regel 5) - kun hvis stønadsperiode satt
 *
 * Ingen exceptions for forretningsutfall - alt modelleres som Vedtak-varianter.
 */
public class Saksbehandling {

    private final OpptjeningsvurdererService opptjeningsvurderer;
    private final BeregningsgrunnlagBeregner grunnlagBeregner;
    private final StonadsperiodeOppslag stonadsperiodeOppslag;
    private final KvoteFordelingsService kvoteFordelingsService;

    public Saksbehandling() {
        this.opptjeningsvurderer = new OpptjeningsvurdererService();
        this.grunnlagBeregner = ew BeregningsgrunnlagBeregner();
        this.stonadsperiodeOppslag = new StonadsperiodeOppslag();
        this.kvoteFordelingsService = new KvoteFordelingsService();
    }

    /** For testing med injiserte avhengigheter. */
    public Saksbehandling(
            OpptjeningsvurdererService opptjeningsvurderer,
            BeregningsgrunnlagBeregner grunnlagBeregner,
            StonadsperiodeOppslag stonadsperiodeOppslag,
            KvoteFordelingsService kvoteFordelingsService) {
        this.opptjeningsvurderer = opptjeningsvurderer;
        this.grunnlagBeregner = grunnlagBeregner;
        this.stonadsperiodeOppslag = stonadsperiodeOppslag;
        this.kvoteFordelingsService = kvoteFordelingsService;
    }

    /**
     * Fatter vedtak for en søknad.
     * Returnerer alltid en konkret Vedtak-instans - aldri null.
     */
    public Vedtak fattVedtak(Soknad soknad) {
        // Steg 1: Opptjeningsvurdering (regler 1 & 2)
        var opptjening = opptjeningsvurderer.vurder(soknad);

        return switch (opptjening) {
            case OpptjeningsvurdererService.Opptjeningsresultat.Avslag avslag ->
                Vedtak.avslag(soknad.id(), avslag.begrunnelse());

            case OpptjeningsvurdererService.Opptjeningsresultat.EngangsstonadFallback fallback ->
                Vedtak.engangsstonad(soknad.id());

            case OpptjeningsvurdererService.Opptjeningsresultat.Oppfylt ignored ->
                // Steg 2: Beregningsgrunnlag (regel 3)
            fortsettMedGrunnlag(soknad);
        };
    }

    private Vedtak fortsettMedGrunnlag(Soknad soknad) {
        Beregningsgrunnlag grunnlag = grunnlagBeregner.beregn(soknad);

        return switch (grunnlag) {
            case Beregningsgrunnlag.ManuellVurdering mv -> Vedtak.manuellVurdering(soknad.id(), mv.begrunnelse());

            case Beregningsgrunnlag.OK ok -> {
                // Steg 3: Stønadsperiode (regel 4)
                int totalUker = stonadsperiodeOppslag.hentTotalUker(soknad);

                // Steg 4: Kvotefordeling (regel 5)
                Kvoter kvoter = kvoteFordelingsService.fordel(soknad, totalUker);

                yield Vedtak.innvilget(soknad.id(), ok.belop(), totalUker, kvoter, soknad.dekningsgrad());
            }
        };
    }
}