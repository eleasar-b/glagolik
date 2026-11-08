package io.github.eleasarb.glagolik.importer

data class Selection(
    /** The training set: the [coreSize] most frequent verbs that are not excluded. */
    val core: List<SourceVerb>,
    /** Aspect partners outside the core set, kept for the conjugation table. */
    val partners: List<SourceVerb>,
    /** Directed partner links between selected verbs (both directions are stored). */
    val links: List<Pair<VerbKey, VerbKey>>,
    /** Notes about partners that could not be resolved or were skipped. */
    val partnerNotes: List<String>,
)

fun select(source: List<SourceVerb>, overrides: Overrides, coreSize: Int): Selection {
    val candidates = source.filterNot { overrides.isExcluded(it.key) }
        .map(overrides::apply)
        .sortedBy { it.rank }
    val core = candidates.take(coreSize)
    require(core.size == coreSize) { "only ${core.size} verbs available, $coreSize requested" }

    val collector = PartnerCollector(
        coreKeys = core.map { it.key }.toSet(),
        byBare = candidates.groupBy { it.bare },
        overrides = overrides,
    )
    core.forEach { verb -> verb.partners.forEach { name -> collector.add(verb, name) } }
    return Selection(
        core = core,
        partners = collector.partners.values.sortedBy { it.rank },
        links = collector.links.toList(),
        partnerNotes = collector.notes,
    )
}

private class PartnerCollector(
    private val coreKeys: Set<VerbKey>,
    private val byBare: Map<String, List<SourceVerb>>,
    private val overrides: Overrides,
) {
    val partners = linkedMapOf<VerbKey, SourceVerb>()
    val links = linkedSetOf<Pair<VerbKey, VerbKey>>()
    val notes = mutableListOf<String>()

    fun add(verb: SourceVerb, name: String) {
        val partner = resolvePartner(verb, byBare[name].orEmpty())
        val inCore = partner != null && partner.key in coreKeys
        when {
            partner == null -> notes += "${verb.key}: partner '$name' not found"

            !inCore && !isValid(partner, overrides) ->
                notes += "${verb.key}: partner ${partner.key} skipped (fails validation)"

            else -> {
                if (!inCore) partners[partner.key] = partner
                links += verb.key to partner.key
                links += partner.key to verb.key
            }
        }
    }
}

/**
 * Partner names are bare infinitives and can be ambiguous (homographs). Prefer a row of
 * the other aspect that names this verb back, then any row of the other aspect, then the
 * most frequent row.
 */
private fun resolvePartner(verb: SourceVerb, candidates: List<SourceVerb>): SourceVerb? {
    val others = candidates.filter { it.key != verb.key }
    return others.firstOrNull { it.aspect != verb.aspect && verb.bare in it.partners }
        ?: others.firstOrNull { it.aspect != verb.aspect }
        ?: others.firstOrNull()
}

private fun isValid(verb: SourceVerb, overrides: Overrides): Boolean =
    validate(verb, overrides.allowedMissing(verb.key)).isEmpty()
