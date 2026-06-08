package no.digisis.hackathon.spor3.domain.service;

import no.digisis.hackathon.spor3.domain.model.Beregningsgrunnlag;
import no.digisis.hackathon.spor3.domain.model.Inntektsregistrering;
import no.digisis.hackathon.spor3.domain.model.Penger;
import no.digisis.hackathon.spor3.domain.model.Soknad;

import java.time.YearMonth;
import java.util.List;

/**
 * Regel 3: Beregning av grunnlag for foreldrepenger (§ 14-7).
 *
 * Algoritme:
 *  1. Beregn månedlig snitt av de siste 3 hele kalendermånedene FØR termindato
 *  2. Annualiser: arssats = snitt × 12
 *  3. Avvikssjekk mot oppgittArsinntekt (hopp over hvis oppgittArsinntekt == 0)
 *  4. Kapp ved 6G
 */
public class BeregningsgrunnlagBeregner {
    private static final double AVVIKSTERSKEL = 0.25;
    private static final int ANTALL_MAAN_SNITT = 3;

    public Beregningsgrunnlag beregn(Soknad soknad) {
        YearMonth termindatoMaaned = YearMonth.from(soknad.termindato());
        List<YearMonth> siste3 = siste3HeleKalendermaneder(termindatoMaaned);

        int sumSiste3 = soknad.inntektshistorikk().stream()
                .filter(i -> siste3.contains(i.maned()))
                .filter(Inntektsregistrering::erGodkjentForOpptjening)
                .mapToInt(Inntektsregistrering::belop)
                .sum();

        int snittPrMaaned = sumSiste3 / ANTALL_MAAN_SNITT;
        int arssats = snittPrMaaned * 12;

        // Avvikssjekk — hopp over hvis oppgittArsinntekt er 0
        if (soknad.oppgittArsinntekt() > 0) {
            double avvik = Math.abs((double)(arssats - soknad.oppgittArsinntekt())) / soknad.oppgittArsinntekt();
            if (avvik > AVVIKSTERSKEL) {
                return new Beregningsgrunnlag.ManuellVurdering(
                        "For stort sprik mellom 3-måneders snitt (%d kr/år) og oppgitt årsinntekt (%d kr): %.0f%% avvik > 25%%"
                                .formatted(arssats, soknad.oppgittArsinntekt(), avvik * 100)
                );
            }
        }

        // Kapp ved 6G
        Penger grunnlag = Penger.av(arssats).min(Penger.SEKS_G);
        return new Beregningsgrunnlag.OK(grunnlag);
    }

    private List<YearMonth> siste3HeleKalendermaneder(YearMonth termindatoMaaned) {
        // "Siste 3 hele kalendermåneder FØR permisjonsstart" = månedene t-1, t-2, t-3
        YearMonth t1 = termindatoMaaned.minusMonths(1);
        YearMonth t2 = termindatoMaaned.minusMonths(2);
        YearMonth t3 = termindatoMaaned.minusMonths(3);
        return List.of(t1, t2, t3);
    }
}