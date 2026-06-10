# Spor 3 — Erfaren · Java

Dette repoet er **bevisst tomt**. Spor 3 handler om at *du* velger
tech-stack, arkitektur og design selv.

## Hva du har

- Standard Maven-mappestruktur (`src/main/java`, `src/main/resources`,
  `src/test/java`, `src/test/resources`).
- En `Application.java` klasse med `main` metode som skriver "Hello, world!"
- En `pom.xml` med JDK 26 støtte

## Kjøre

```
mvn -q compile exec:java
```

Serveren statrer på `http://localhost:7070` og henter søknader fra 
`https://api.digisis.org`.

For å overstyre URL:

```bash
SOKNAD_API_URL=https://annen-url.no mvn -q compile exec:java
```

## Caset

Beskrivelse av problemet, kontrakten og vurderingskriteriene får du
utdelt på hackathon-dagen. Frem til da: kom forberedt på din egen
favoritt-stack.

## Test alle søknader manuelt

Når serveren kjører:

```bash
curl -s https://api.digisis.org/api/foreldrepenger/soknader \
  | jq -c '.[]' \
  | while read soknad; do
      echo "--- Sender: $(echo $soknad | jq -r '.id') ---"
      echo $soknad | curl -s -X POST http://localhost:7070/api/foreldrepenger/vurder \
        -H "Content-Type: application/json" -d @- | jq '{status: .status, vedtakId: .vedtakId}'
    done
```

## Endepunkter
| Method | Path | Beskrivelse | 
|-------- | ------ | ------------- | 
| `GET`  | `/api/foreldrepenger/soknader`                         | Henter søknader fra case-serveren |
| `POST` | `/api/foreldrepenger/soknader`                         | Registrer søknad |
| `GET`  | `/api/foreldrepenger/soknader/{id}`                    | Hent søknad |
| `POST` | `/api/foreldrepenger/soknader/{id}/vedtak`             | Fatt vedtak |
| `GET`  | `/api/foreldrepenger/vedtak/{id}`                      | Hent vedtak |
| `GET`  | `/api/foreldrepenger/vedtak?status=MANUELL_VURDERING`  | Filtrer vedtak |
| `POST` | `/api/foreldrepenger/vurder`                           | Ett-stegs vurdering (søknad → vedtak) |
