package no.digisis.hackathon.spor3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import no.digisis.hackathon.spor3.api.ForeldrepengerRouter;
import no.digisis.hackathon.spor3.api.Soknadhttpklient;
import no.digisis.hackathon.spor3.domain.service.Saksbehandling;
import no.digisis.hackathon.spor3.infrastructure.InMemoryLager;

/**
 * Miljøvariabler:
 *   PORT           — HTTP-port (standard: 7070)
 *   SOKNAD_API_URL — Base-URL til eksternt søknad-API (påkrevd)
 *                    Eksempel: http://nav-testdata.intern/api
 */
public class Application {

    public static void main(String[] args) {
        int port = System.getenv("PORT") != null
                ? Integer.parseInt(System.getenv("PORT"))
                : 7070;

        String soknadApiUrl = System.getenv("SOKNAD_API_URL") != null
                ? System.getenv("SOKNAD_API_URL")
                : "http://localhost:" + port;


        InMemoryLager lager = new InMemoryLager();
        Saksbehandling saksbehandling = new Saksbehandling();
        Soknadhttpklient soknadhttpklient = new Soknadhttpklient(soknadApiUrl);
        ForeldrepengerRouter router = new ForeldrepengerRouter(lager, saksbehandling, soknadhttpklient);

        Javalin app = Javalin.create(config -> {
                    config.jsonMapper(new JavalinJackson().updateMapper(mapper -> {
                        mapper.registerModule(new JavaTimeModule());
                        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                    }));
                    config.bundledPlugins.enableCors(cors -> cors.addRule(rule -> rule.anyHost()));
                });

        router.registrer(app);
        app.start(port);

        // Selvtest: henter alle søknader fra case-serveren ved oppstart og behandler hver enkelt.
        // Skriver søknad-ID og vedtak-type til konsollen.
        try {
            var soknader = soknadhttpklient.hentSoknader();
            var soknadMapper = new no.digisis.hackathon.spor3.api.SoknadMapper();
            System.out.println("\n=== Selvtest: alle søknader ===");
            for (var req : soknader) {
                var soknad = soknadMapper.tilDomene(req);
                var vedtak = saksbehandling.fattVedtak(soknad);
                System.out.println("%-30s → %s".formatted(soknad.id(), vedtak.getClass().getSimpleName()));
            }
            System.out.println("================================\n");
        } catch (Exception e) {
            System.out.println("[ADVARSEL] Selvtest feilet: " + e.getMessage());
        }
    }
}