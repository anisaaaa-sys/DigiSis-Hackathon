package no.digisis.hackathon.spor3.domain.service;

import no.digisis.hackathon.spor3.domain.model.Rettsforhold;
import no.digisis.hackathon.spor3.domain.model.Soknad;

/**
 * Regel 4: Stønadsperiode-oppslag (§ 14-9).
 *
 * Oppslagstabell over totalt antall uker basert på
 * (rettsforhold, antallBarn, dekningsgrad).
 */
public class StonadsperiodeOppslag {

    /**
     * Returnerer total antall søknadsuker.
     */
    public int hentTotalUker(Soknad soknad) {
        return hentTotalUker(soknad.rettsforhold(), soknad.antallBarn(), soknad.dekningsgrad());
    }

    public int hentTotalUker(Rettsforhold rettsforhold, int antallBarn, int dekningsgrad) {
        int barnKategori = Math.min(antallBarn, 3); // "3+" = kategori 3
        return switch (rettsforhold) {
            case BEGGE, KUN_MOR -> switch (barnKategori) {
                case 1 -> dekningsgrad == 100 ? 49 : 61;
                case 2 -> dekningsgrad == 100 ? 66 : 82;
                default -> dekningsgrad == 100 ? 95 : 118;
            };
            case KUN_FAR -> switch (barnKategori) {
                case 1 -> dekningsgrad == 100 ? 40 : 52;
                case 2 -> dekningsgrad == 100 ? 57 : 73;
                default -> dekningsgrad == 100 ? 86 : 109;
            };
        };
    }
}