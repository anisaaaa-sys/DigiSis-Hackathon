package no.digisis.hackathon.spor3.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.digisis.hackathon.spor3.api.Dto;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * HTTP-klient for å hente søknader fra et eksternt API.
 *
 * Brukes i stedet for hardkodet testdata. Base-URL konfigureres via
 * miljøvariabelen SOKNAD_API_URL (standard: http://localhost:7070).
 *
 * Eksempel:
 *   SOKNAD_API_URL=http://nav-testdata-api.intern/api henter søknader fra
 *   http://nav-testdata-api.intern/api/foreldrepenger/soknader
 */
public class Soknadhttpklient {
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public Soknadhttpklient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Henter alle tilgjengelige søknader fra GET {baseUrl}/api/foreldrepenger/soknader.
     *
     * @throws SoknadHentingFeilet hvis HTTP-kallet feiler eller svaret ikke kan parses
     */
    public List<Dto.SoknadRequest> hentSoknader() {
        String url = baseUrl + "/api/foreldrepenger/soknader";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new SoknadHentingFeilet(
                        "Uventet HTTP-statuskode %d fra %s".formatted(response.statusCode(), url));
            }

            return objectMapper.readValue(response.body(), new TypeReference<List<Dto.SoknadRequest>>() {
            });

        } catch (SoknadHentingFeilet e) {
            throw e;
        } catch (Exception e) {
            throw new SoknadHentingFeilet("Kunne ikke hente søknader fra " + url + ": " + e.getMessage(), e);
        }
    }

    /**
     * Henter én enkelt søknad fra GET {baseUrl}/api/foreldrepenger/soknader/{id}.
     *
     * @throws SoknadHentingFeilet hvis søknaden ikke finnes eller kallet feiler
     */
    public Dto.SoknadRequest hentSoknad(String id) {
        String url = baseUrl + "/api/foreldrepenger/soknader/" + id;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                throw new SoknadHentingFeilet("Søknad ikke funnet: " + id);
            }
            if (response.statusCode() != 200) {
                throw new SoknadHentingFeilet(
                        "Uventet HTTP-statuskode %d fra %s".formatted(response.statusCode(), url));
            }

            return objectMapper.readValue(response.body(), Dto.SoknadRequest.class);

        } catch (SoknadHentingFeilet e) {
            throw e;
        } catch (Exception e) {
            throw new SoknadHentingFeilet("Kunne ikke hente søknad " + id + " fra " + url + ": " + e.getMessage(), e);
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /** Kastes ved feil under HTTP-henting — ikke en forretningsfeil. */
    public static class SoknadHentingFeilet extends RuntimeException {
        public SoknadHentingFeilet(String message) { super(message); }
        public SoknadHentingFeilet(String message, Throwable cause) { super(message, cause); }
    }
}