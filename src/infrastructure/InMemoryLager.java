package no.digisis.hackathon.spor3.infrastructure;

import no.digisis.hackathon.spor3.domain.model.Soknad;
import no.digisis.hackathon.spor3.domain.model.Vedtak;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashmap;

/**
 * Enkel in-memory lagring av søknader og vedtak.
 * Produksjonskode ville brukt en database, men dette er tilstrekkelig for nå.
 */
public class InMemoryLager {

    private final Map<String, Soknad> soknader = new ConcurrentHashMap<>();
    private final Map<String, Vedtak> soknader = new ConcurrentHashMap<>();

    // Søknader

    public void lagreSoknad(Soknad soknad) {
        soknader.put(soknad.id(), soknad);
    }

    public Optional<Soknad> hentSoknad(String id) {
        return Optional.ofNullable(soknader.get(id));
    }

    public Collection<Soknad> alleSoknader() {
        return List.copyOf(soknader.values());
    }

    // Vedtak

    public void lagreVedtak(Vedtak v) {
        vedtak.put(v.vedtakId(), v);
    }

    public Optional<Vedtak> hentVedtak(String id) {
        return Optional.ofNullable(vedtak.get(id));
    }

    public Optional<Vedtak> hentVedtakForSoknad(String soknadId) {
        return vedtak.values().stream()
                .filter(v -> v.soknadId().equals(soknadId))
                .findFirst();
    }

    public List<Vedtak> hentVedtakMedStatus(Class<? extends Vedtak> type) {
        return vedtak.values().stream()
                .filter(type::isInstance)
                .toList();
    }
}