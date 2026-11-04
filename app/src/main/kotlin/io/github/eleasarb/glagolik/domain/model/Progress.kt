package io.github.eleasarb.glagolik.domain.model

import io.github.eleasarb.glagolik.domain.srs.SrsCard

/** Identifies one trainable form: stable verb id plus grammatical slot. */
data class FormRef(val verbId: Int, val form: FormKey)

/** A form with its current scheduling state. */
data class ScheduledCard(val ref: FormRef, val card: SrsCard)
