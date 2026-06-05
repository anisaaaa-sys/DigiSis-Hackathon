package no.digisis.hackathon.spor3.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import no.digisis.hackathon.spor3.domain.model.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API-kontrakt: Request og response DTOs.
 * Records gir automatisk JSON-serialisering via Jackson.
 */
public final class Dto {
    // --- Request ---

    public record SoknadRequest(
            String id,
            String beskrivelse,
            String fnr,
            boolean erNorskBorger,
            String termindato,
            int oppgittArsinntekt,
            List<InntektsregistreringDto> inntektshistorikk,
            int antallBarn,
            String rettsforhold,
            int dekningsgrad
    ) {}

    public record InntektsregistreringDto(String maned, String type, int belop) {}

    // --- Response ---
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record VedtakResponse(
            String vedtakId,
            String soknadId,
            String status,
            LocalDateTime fattetTidspunkt,
            // Innvilget-felter
            Integer beregningsgrunnlagKroner,
            Integer totalUker,
            Integer dekningsgrad,
            KvoterDto kvoter,
            // Engangsstønad-felter
            Integer engangsstonadBelop,
            // Alle utfall unntatt innvilget
            String begrunnelse
    ) {
        public static VedtakResponse fra(Vedtak vedtak) {
            return switch (vedtak) {
                case Vedtak.Innvilget v -> new VedtakResponse(
                        v.vedtakId(), v.soknadId(), "INNVILGET", v.fattetTidspunkt(),
                        v.beregningsgrunnlag().kroner(), v.totalUker(), v.dekningsgrad(),
                        KvoterDto.fra(v.kvoter()),
                        null, null
                );
                case Vedtak.Engangsstonad v -> new VedtakResponse(
                        v.vedtakId(), v.soknadId(), "ENGANGSSTONAD", v.fattetTidspunkt(),
                        null, null, null, null,
                        v.belop().kroner(), v.begrunnelse()
                );
                case Vedtak.ManuellVurdering v -> new VedtakResponse(
                        v.vedtakId(), v.soknadId(), "MANUELL_VURDERING", v.fattetTidspunkt(),
                        null, null, null, null, null, v.begrunnelse()
                );
                case Vedtak.Avslag v -> new VedtakResponse(
                        v.vedtakId(), v.soknadId(), "AVSLAG", v.fattetTidspunkt(),
                        null, null, null, null, null, v.begrunnelse()
                );
            };
        }
    }

    public record KvoterDto(
            (
            int morKvote,
            int farKvote,
            int fellesperiode,
            int forhandskvoteMor,
            int flerbarnsbonus,
            int totalUker
    ) {
        public static KvoterDto fra(Kvoter k) {
            return new KvoterDto(
                    k.morKvote(), k.farKvote(), k.fellesperiode(),
                    k.forhandskvoteMor(), k.flerbarnsbonus(), k.totalUker()
            );
        }
    }

    public record FeilResponse(String feil, String melding) {}
}