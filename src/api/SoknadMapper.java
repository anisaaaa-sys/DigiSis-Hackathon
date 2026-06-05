package no.digisis.hackathon.spor3.api;

import no.digisis.hackathon.spor3.domain.model.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Mapper mellom API-DTOer og domeneobjekter.
 * Isolerer API-laget fra domenemodellen.
 */
public class SoknadMapper {
    public Soknad tilDomene(Dto.SoknadRequest req) {
        String id = (req.id() != null && !req.id().isBlank())
                ? req.id()
                : "fp-" + UUID.randomUUID().toString().substring(0, 8);

        List<Inntektsregistrering> historikk = req.inntektshistorikk().stream()
                .map(dto -> new Inntektsregistrering(
                        YearMonth.parse(dto.maned()),
                        Inntektstype.valueOf(dto.type()),
                        dto.belop()
                ))
                .toList();

        return new Soknad(
                id,
                req.beskrivelse(),
                req.fnr(),
                req.erNorskBorger(),
                LocalDate.parse(req.termindato()),
                req.oppgittArsinntekt(),
                historikk,
                req.antallBarn(),
                Rettsforhold.fraKode(req.rettsforhold()),
                req.dekningsgrad()
        );
    }
}