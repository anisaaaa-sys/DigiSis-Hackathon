package no.digisis.hackathon.spor3.domain.service;

import no.digisis.hackathon.spor3.domain.model.Kvoter;
import no.digisis.hackathon.spor3.domain.model.Rettsforhold;
import no.digisis.hackathon.spor3.domain.model.Soknad;

/**
 * Regel 5: Kvotefordeling (§§ 14-10 og 14-11).
 *
 * Forhandskvote (3 uker til mor FØR termin) gjelder ved fødsel
 * når mor er rettshaver.
 *
 * Designprinsipp: fellesperioden beregnes alltid som rest slik at
 * Kvoter-invarianten (sum == totalUker) aldri kan brytes.
 */
public class KvoteFordelingsService {

    // Faste kvoter per dekningsgrad
    private static final int MODREKVOTE_100 = 15;
    private static final int MODREKVOTE_80 = 19;
    private static final int FEDREKVOTE_100 = 15;
    private static final int FEDREKVOTE_80 = 19;
    private static final int FORHANDSKVOTE_MOR = 3; // alltid 3 uker

    // Flerbarnsbonuser for 1 / 2 / 3+ barn (100%)
    private static final int[] FLERBARNSBONUS_100 = {0, 0, 17, 46}; // index = antallBarn, 3+ = index 3
    private static final int[] FLERBARNSBONUS_80 = {0, 0, 21, 57};

    public Kvoter fordel(Soknad soknad, int totalUker) {
        return fordel(soknad.rettsforhold(), soknad.antallBarn(), soknad.dekningsgrad(), totalUker);
    }

    public Kvoter fordel(Rettsforhold rettsforhold, int antallBarn, int dekningsgrad, int totalUker) {
        int barnIdx = Math.min(antallBarn, 3);
        int flerbarnsbonus = dekningsgrad == 100
                ? FLERBARNSBONUS_100[barnIdx]
                : FLERBARNSBONUS_80[barnIdx];

        return switch (rettsforhold) {
            case BEGGE -> fordelBegge(dekningsgrad, flerbarnsbonus, totalUker);
            case KUN_MOR -> fordelKunMor(flerbarnsbonus, totalUker);
            case KUN_FAR -> fordelKunFar(flerbarnsbonus, totalUker);
        };
    }

    private Kvoter fordelBegge(int dekningsgrad, int flerbarnsbonus, int totalUker) {
        int morKvote   = dekningsgrad == 100 ? MODREKVOTE_100 : MODREKVOTE_80;
        int farKvote   = dekningsgrad == 100 ? FEDREKVOTE_100 : FEDREKVOTE_80;
        int forhand    = FORHANDSKVOTE_MOR;
        // Fellesperiode = rest (garanterer at summen alltid stemmer)
        int felles = totalUker - morKvote - farKvote - forhand - flerbarnsbonus;
        return new Kvoter(morKvote, farKvote, felles, forhand, flerbarnsbonus, totalUker);
    }

    private Kvoter fordelKunMor(int flerbarnsbonus, int totalUker) {
        // Alt til mor (minus forhåndskvote) — ingen far, ingen fellesperiode
        int forhand = FORHANDSKVOTE_MOR;
        int morKvote = totalUker - forhand - flerbarnsbonus;
        return new Kvoter(morKvote, 0, 0, forhand, flerbarnsbonus, totalUker);
    }

    private Kvoter fordelKunFar(int flerbarnsbonus, int totalUker) {
        // Alt til far — ingen forhåndskvote, ingen fellesperiode
        int farKvote = totalUker - flerbarnsbonus;
        return new Kvoter(0, farKvote, 0, 0, flerbarnsbonus, totalUker);
    }
}