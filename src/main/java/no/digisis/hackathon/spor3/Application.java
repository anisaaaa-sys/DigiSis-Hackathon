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

        String soknadApiUrl = System.getenv("SOKNAD_API_URL");
        if (soknadApiUrl == null) {
            throw new IllegalStateException("Miljøvariabel SOKNAD_API_URL er ikke satt");
        }

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

        System.out.println("Server kjører på http://localhost:" + port);
        System.out.println("Henter søknader fra: " + soknadApiUrl);
    }
}