package no.digisis.hackathon.spor3.test;

import no.digisis.hackathon.spor3.domain.model.*;
import no.digisis.hackathon.spor3.domain.service.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Foreldrepenger saksbehandling")
class ForeldrepengerTest {
    // Hjelpere
    private static Soknad lagSoknad(
            String id, boolean borger, String termin, int arsinntekt,
            List<Inntektsregistrering> historikk, int barn, String rett,
            int dekn) {
        return new Soknad(id, "test", "12345678901", borger,
                LocalDate.parse(termin), arsinntekt, historikk, barn,
                Rettsforhold.fraKode(rett), dekn);
    }

    private static List<Inntektsregistrering> inntekt12Mnd(int belop) {
        var list = new ArrayList<Inntektsregistrering>();
        YearMonth base = YearMonth.now().minusMonths(13);
        for (int i = 0; i < 12; i++) {
            list.add(new Inntektsregistrering(base.plusMonths(i), Inntektstype.ARBEID, belop));
        }
        return list;
    }

    private static List<Inntektsregistrering> inntektMaaneder(int antall, int belop) {
        var list = new ArrayList<Inntektsregistrering>();
        YearMonth base = YearMonth.now().minusMonths(antall + 1);
        for (int i = 0; i < antall; i++) {
            list.add(new Inntektsregistrering(base.plusMonths(i), Inntektstype.ARBEID, belop));
        }
        return list;
    }

    // Regel 1: Opptjeningsvurdering

    @Nested
    @DisplayName("Regel 1 - Opptjeningsvurdering")
    class OpptjeningTest {
        private final OpptjeningsvurdererService service = new OpptjeningsvurdererService();

        @Test
        @DisplayName("Avslag når søker ikke er norsk borger")
        void ikkeNorskBorger_girAvslag() {
            var soknad = lagSoknad("s1", false, "2026-08-15", 540000,
                    inntekt12Mnd(45000), 1, "begge", 100);
            var result = service.vurder(soknad);
            Assertions.assertInstanceOf(OpptjeningsvurdererService.Opptjeningsresultat.Avslag.class, result);
        }

        @Test
        @DisplayName("Oppfylt med 7 av 10 måneder med inntekt")
        void syv_av_ti_mnd_gir_oppfylt() {
            var soknad = lagSoknad("s2", true, "2026-08-15", 540000,
                    inntektMaaneder(7, 45000), 1, "begge", 100);
            var result = service.vurder(soknad);
            Assertions.assertInstanceOf(OpptjeningsvurdererService.Opptjeningsresultat.Oppfylt.class, result);
        }

        @Test
        @DisplayName("Engangsstønad-fallback med kun 3 måneder inntekt")
        void tre_mnd_gir_engangsstonad_fallback() {
            var soknad = lagSoknad("s3", true, "2026-08-15", 400000,
                    inntektMaaneder(3, 40000), 1, "kun-mor", 100);
            var result = service.vurder(soknad);
            Assertions.assertInstanceOf(OpptjeningsvurdererService.Opptjeningsresultat.EngangsstonadFallback.class, result);
        }

        @Test
        @DisplayName("STIPEND_LANEKASSEN teller ikke som opptjening")
        void stipend_teller_ikke() {
            var historikk = new ArrayList<Inntektsregistrering>();
            YearMonth base = YearMonth.now().minusMonths(11);
            for (int i = 0; i < 10; i++) {
                historikk.add(new Inntektsregistrering(base.plusMonths(i), Inntektstype.STIPEND_LANEKASSEN, 20000));
            }
            var soknad = lagSoknad("s4", true, "2026-08-15", 0,
                    historikk, 1, "begge", 100);
            var result = service.vurder(soknad);
            Assertions.assertInstanceOf(OpptjeningsvurdererService.Opptjeningsresultat.EngangsstonadFallback.class, result);
        }
    }

    // Regel 2: Engangsstønad

    @Nested
    @DisplayName("Regel 2 - Engangsstønad")
    class EngangsstonadTest {
        private final Saksbehandling saksbehandling = new Saksbehandling();

        @Test
        @DisplayName("Norsk borger uten opptjening får engangsstønad")
        void norskBorger_mangler_opptjening_gir_engangsstonad() {
            var soknad = lagSoknad("s5", true, "2026-08-15", 400000,
                    inntektMaaneder(3, 40000), 1, "kun-mor", 100);
            var vedtak = saksbehandling.fattVedtak(soknad);
            Assertions.assertInstanceOf(Vedtak.Engangsstonad.class, vedtak);
            Assertions.assertEquals(Penger.ENGANGSSTONAD, ((Vedtak.Engangsstonad) vedtak).belop());
        }

        @Test
        @DisplayName("Ikke-norsk statsborger får avslag selv uten opptjening")
        void ikkeBorger_gir_avslag_ikke_engangsstonad() {
            var soknad = lagSoknad("s6", false, "2026-08-15", 400000,
                    inntektMaaneder(3, 40000), 1, "kun-mor", 100);
            var vedtak = saksbehandling.fattVedtak(soknad);
            Assertions.assertInstanceOf(Vedtak.Avslag.class, vedtak);
        }
    }

    // Regel 3: Beregningsgrunnlag
}