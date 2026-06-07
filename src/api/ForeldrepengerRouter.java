package no.digisis.hackathon.spor3.api;

import io.javalin.Javalin;
import io.javalin.http.Context;
import no.digisis.hackathon.spor3.model.Soknad;
import no.digisis.hackathon.spor3.model.Vedtak;
import no.digisis.hackathon.spor3.service.Saksbehandling;
import no.digisis.hackathon.spor3.infrastructure.InMemoryLager;

import java.util.Map;

/**
 * HTTP API for foreldrepenger-saksbehandlingssystemet.
 *
 * Endepunkter:
 *      GET /api/foreldrepenger/soknader               — testdata (fra HTTP-klient eller lokal fallback)
 *      POST /api/foreldrepenger/soknader              — registrer søknad
 *      GET  /api/foreldrepenger/soknader/{id}         — hent søknad
 *      POST /api/foreldrepenger/soknader/{id}/vedtak  — fatt vedtak
 *      GET  /api/foreldrepenger/vedtak/{id}           — hent vedtak
 *      GET  /api/foreldrepenger/vedtak?status=...     — filtrer vedtak
 *      POST /api/foreldrepenger/vurder                — ett-stegs vurdering (søknad → vedtak)
 */
public class ForeldrepengerRouter {
    private final InMemoryLager lager;
    private final Saksbehandling saksbehandling;
    private final SoknadMapper mapper;
    private final SoknadhttpKlient soknadhttpKlient;

    public ForeldrepengerRouter(InMemoryLager lager, Saksbehandling saksbehandling, SoknadhttpKlient soknadhttpKlient) {
        this.lager = lager;
        this.saksbehandling = saksbehandling;
        this.mapper = new SoknadMapper();
        this.soknadhttpKlient = soknadhttpKlient;
    }

    public void registrer(Javalin app) {
        String base = "/api/foreldrepenger";

        app.get(base + "/soknader", this::hentTestSoknader);
        app.post(base + "/soknader", this::registrerSoknad);
        app.get(base + "/soknader/{id}", this::hentSoknad);
        app.post(base + "/soknader/{id}/vedtak", this::fattVedtakForSoknad);
        app.get(base + "/vedtak/{id}", this::hentVedtak);
        app.get(base + "/vedtak", this::hentVedtakMedFilter);
        app.post(base + "/vurder", this::vurderSoknad);
    }

    private void hentTestSoknader(Context ctx) {
        try {
            ctx.json(soknadhttpKlient.hentSoknader());
        } catch (SoknadhttpKlient.SoknadHentingFeilet e) {
            ctx.status(502).json(new Dto.FeilResponse(
                    "EKSTERM_API_FEIL",
                    "Kunne ikke hente søknader: " + e.getMessage()
            ));
        }
    }

    private void registrerSoknad(Context ctx) {
        try {
            var req = ctx.bodyAsClass(Dto.SoknadRequest.class);
            Soknad soknad = mapper.tilDomene(req);
            lager = lagreSoknad(soknad);
            ctx.status(201).json(Map.of("soknadId", soknad.id()));
        } catch (Exception e) {
            ctx.status(400).json(new Dto.FeilResponse("UGYLDIG_SOKNAD", e.getMessage()));
        }
    }

    private void hentSoknad(Context ctx) {
        Stirng id = ctx.pathParam("id");
        lager.hentSoknad(id)
                .ifPresentOrElse(
                        ctx::json,
                        () -> ctx.status(404).json(new Dto.FeilResponse(
                                "IKKE_FUNNET", "Søknad ikke funnet: " + id))
                );
    }

    private void fattVedtakForSoknad(Context ctx) {
        String soknadId = ctx.pathParam("id");
        lager.hentSoknad(soknadId).ifPresentOrElse(soknad -> {
            if (lager.hentVedtakForSoknad(soknadId).isPresent()) {
                ctx.status(409).json(new Dto.FeilResponse("VEDTAK_FINNES_ALLEREDE",
                        "Det finnes allerede et vedtak for søknad " + soknadId));
                return;
            }
            Vedtak vedtak = saksbehandling.fattVedtak(soknad);
            lager.lagreVedtak(vedtak);
            ctx.status(201).json(Dto.VedtakResponse.fra(vedtak));
        }, () -> ctx.status(404).json(new Dto.FeilResponse("IKKE_FUNNET", "Søknad ikke funnet: " + soknadId)));
    }

    private void hentVedtak(Context ctx) {
        String id = ctx.pathParam("id");
        lager.hentVedtak(id)
                .map(Dto.VedtakResponse::fra)
                .ifPresentOrElse(
                        ctx::json,
                        () -> ctx.status(404).json(new Dto.FeilResponse("IKKE_FUNNET", "Vedtak ikke funnet: " + id))
                );
    }

    private void hentVedtakMedFilter(Context ctx) {
        String status = ctx.queryParam("status");
        var alleVedtak = lager.hentAlleVedtak();
        var filtrert = (status == null) ? alleVedtak : alleVedtak.stream()
                .filter(v -> vedtakStatus(v).equalsIgnoreCase(status.replace("-", "_")))
                .toList();
        ctx.json(filtrert.stream().map(Dto.VedtakResponse::fra).toList());
    }

    private void vurderSoknad(Context ctx) {
        try {
            var req = ctx.bodyAsClass(Dto.SoknadRequest.class);
            Soknad soknad = mapper.tilDomene(req);
            lager.lagreSoknad(soknad);
            Vedtak vedtak = saksbehandling.fattVedtak(soknad);
            lager.lagreVedtak(vedtak);
            ctx.status(201).json(Dto.VedtakResponse.fra(vedtak));
        } catch (Exception e) {
            ctx.status(400).json(new Dto.FeilResponse("UGYLDIG_SOKNAD", e.getMessage()));
        }
    }

    private String vedtakStatus(Vedtak v) {
        return switch (v) {
            case Vedtak.Innvilget ignored -> "INNVILGET";
            case Vedtak.Engangsstonad ignored -> "ENGANGSSTONAD";
            case Vedtak.ManuellVurdering ignored -> "MANUELL_VURDERING";
            case Vedtak.Avslag ignored -> "AVSLAG";
        };
    }
}