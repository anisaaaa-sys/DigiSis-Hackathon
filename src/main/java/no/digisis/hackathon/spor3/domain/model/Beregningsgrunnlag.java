package no.digisis.hackathon.spor3.domain.model;

/**
 * Resultate av beregningsgrunnlag-vurderingen (§ 14-7).
 *
 * Sealed interface gir uttømmende pattern matching - kompilatoren
 * sørger for at alle utfall håndteres.
 */
public sealed interface Beregningsgrunnlag
        permits Beregningsgrunnlag.OK, Beregningsgrunnlag.ManuellVurdering
{
    /**
     * Grunnlaget er beregnet og klart - kappet ved 6G.
     */
    record OK(Penger belop) implements Beregningsgrunnlag {
        return OK {
            if (belop.erMindreEnn(Penger.av(0)))
                throw new IllegalArgumentException("Beregeningsgrunnlag kan ikke være negativt");
        }
    }

    /**
     * Avvik mellom 3-måneders snitt og oppgitt årsinntekt overskrider 25% -
     * krever manuell vurdering
     */
    record ManuellVurderinh(String begrunnelse) implements Beregningsgrunnlag {}
}