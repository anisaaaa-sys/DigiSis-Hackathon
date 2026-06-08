package no.digisis.hackathon.spor3.domain.service;

import no.digisis.hackathon.spor3.domain.model.Inntektsregistrering;
import no.digisis.hackathon.spor3.domain.model.Penger;
import no.digisis.hackathon.spor3.domain.model.Soknad;

import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Regel 1 & 2: Opptjeningsvurdering (§§ 2-1 og 14-6) og engangsstønad-fallback (§ 14-7).
 *
 * Returnerer et opptjeningsresultat som beskriver utfallet uten å kaste exceptions.
 * Utfall: OPPFYLT | ENGANGSSTONAD_FALLBACK | AVSLAG
 */
public class OpptjeningsvurdererService {
    private static final int ANTALL_MAANEDER_VINDU = 10;
    private static final int KRAV_MANEDER_MED_INNTEKT = 6;

    public sealed interface Oppteningsresultat
        permits Opptjeningsresultat.Oppfylt,
            Opptjeningsresultat.EngangsstonadFallback,
            Opptjeningsresultat.Avslag {

        record Oppfylt() implements Opptjeningsresultat {}

        record EngangsstonadFallback(String begrunnelse) implements Opptjeningsresultat {}

        record Avslag(String begrunnelse) implements Opptjeningsresultat {}
    }

    public Opptjeningsresultat vurder(Soknad soknad) {
        // § 2-1 — forenkling til boolean
        if (!soknad.erNorskBorger()) {
            return new Opptjeningsresultat.Avslag(
                    "Søker er ikke norsk borger og har ikke rett til verken foreldrepenger eller engangsstønad (§ 2-1 / § 14-17)"
            );
        }

        // Finn de 10 siste kalendermånedene FØR termindato (eksklusive termindato-måneden)
        YearMonth termindatoMaaned = YearMonth.from(soknad.termindato());
        List<YearMonth> vindu = vinduMaaneder(termindatoMaaned);

        Set<YearMonth> maanederMedGodkjentInntekt = soknad.inntektshistorikk().stream()
                .filter(Inntektsregistrering::erGodkjentForOpptjening)
                .filter(i -> vindu.contains(i.maned()))
                .map(Inntektsregistrering::maned)
                .collect(Collectors.toSet());

        if (maanederMedGodkjentInntekt.size() < KRAV_MAANEDER_MED_INNTEKT) {
            return new Opptjeningsresultat.EngangsstonadFallback(
                    "Kun %d av %d måneder med godkjent inntekt (krever %d)"
                            .formatted(maanederMedGodkjentInntekt.size(), ANTALL_MAANEDER_VINDU, KRAV_MAANEDER_MED_INNTEKT)
            );
        }

        // Inntektskrav: sum av alle godkjente inntekter i vinduet × 12/10 ≥ ½G
        int sumInntektIVindu = soknad.inntektshistorikk().stream()
                .filter(Inntektsregistrering::erGodkjentForOpptjening)
                .filter(i -> vindu.contains(i.maned()))
                .mapToInt(Inntektsregistrering::belop)
                .sum();

        // Omregn til årsats: snitt av vinduet (10 mnd) × 12
        int omregnetArsinntekt = sumInntektIVindu * 12 / ANTALL_MAANEDER_VINDU;

        if (Penger.av(omregnetArsinntekt).erMindreEnn(Penger.HALV_G)) {
            return new Opptjeningsresultat.EngangsstonadFallback(
                    "Omregnet årsinntekt %d kr er under ½G (%d kr) (§ 14-6)"
                            .formatted(omregnetArsinntekt, Penger.HALV_G.kroner())
            );
        }

        return new Opptjeningsresultat.Oppfylt();
    }

    private List<YearMonth> vinduMaaneder(YearMonth termindatoMaaned) {
        // Én måned bak termindato (§ 14-6: "månedene FØR termin") og 10 måneder tilbake
        YearMonth start = termindatoMaaned.minusMonths(ANTALL_MAANEDER_VINDU);
        return start.datesUntil(termindatoMaaned)
                .map(YearMonth::from)
                .collect(java.util.stream.Collectors.toList());
    }
}