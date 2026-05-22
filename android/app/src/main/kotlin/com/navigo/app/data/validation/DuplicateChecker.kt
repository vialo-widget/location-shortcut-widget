package com.navigo.app.data.validation

import com.navigo.app.data.model.Shortcut
import kotlin.math.abs

/**
 * Outcome of a save-time duplicate check. Ported from the Flutter build's
 * AddShortcutScreen / ConfirmAddScreen / EditShortcutScreen so the rewrite
 * keeps the same behaviour around "one shortcut per place, one label per
 * shortcut, don't leave dead rows around".
 */
sealed interface DuplicateCheck {
    /** No conflict — proceed with the save. */
    data object Clear : DuplicateCheck

    /** Same coordinates *and* same label as an existing shortcut. Block. */
    data class ExactDuplicate(val matched: Shortcut) : DuplicateCheck

    /**
     * Same coordinates but a different label. The UI should prompt the user
     * to either replace the existing shortcut in-place (preserving its id
     * and sortOrder) or cancel.
     */
    data class SameCoordsDifferentLabel(val matched: Shortcut) : DuplicateCheck

    /**
     * Different coordinates but the label is already taken by an existing
     * shortcut. Add / Edit treat this as a block ("pick a different name");
     * Confirm-Add auto-renames via [nextAvailableLabel] instead.
     */
    data object SameLabelDifferentCoords : DuplicateCheck
}

object DuplicateChecker {
    /** ~11 m at the equator — what Flutter's screens used for "same place". */
    const val COORD_EPSILON: Double = 0.0001

    fun check(
        existing: List<Shortcut>,
        latitude: Double,
        longitude: Double,
        label: String,
        excludeId: String? = null,
    ): DuplicateCheck {
        val others = if (excludeId == null) existing else existing.filter { it.id != excludeId }

        val coordMatch = others.firstOrNull {
            abs(it.latitude - latitude) < COORD_EPSILON &&
                abs(it.longitude - longitude) < COORD_EPSILON
        }
        if (coordMatch != null) {
            return if (coordMatch.label == label) {
                DuplicateCheck.ExactDuplicate(coordMatch)
            } else {
                DuplicateCheck.SameCoordsDifferentLabel(coordMatch)
            }
        }
        if (others.any { it.label == label }) {
            return DuplicateCheck.SameLabelDifferentCoords
        }
        return DuplicateCheck.Clear
    }

    /**
     * Returns the smallest `"$base $n"` (n ≥ 2) that isn't already a label
     * in [existing]. Returns [base] unchanged if it's free.
     */
    fun nextAvailableLabel(
        existing: List<Shortcut>,
        base: String,
        excludeId: String? = null,
    ): String {
        val others = if (excludeId == null) existing else existing.filter { it.id != excludeId }
        if (others.none { it.label == base }) return base
        var suffix = 2
        while (others.any { it.label == "$base $suffix" }) suffix++
        return "$base $suffix"
    }
}

/**
 * UI-friendly projection of [DuplicateCheck] — the cases that need to
 * surface as a dialog before a save can proceed. Null means "no blocker".
 */
sealed interface SaveBlocker {
    data class ExactDuplicate(val matched: Shortcut) : SaveBlocker
    data class ReplacePrompt(val matched: Shortcut) : SaveBlocker
    data object LabelTaken : SaveBlocker
}

fun DuplicateCheck.toBlocker(): SaveBlocker? = when (this) {
    DuplicateCheck.Clear -> null
    is DuplicateCheck.ExactDuplicate -> SaveBlocker.ExactDuplicate(matched)
    is DuplicateCheck.SameCoordsDifferentLabel -> SaveBlocker.ReplacePrompt(matched)
    DuplicateCheck.SameLabelDifferentCoords -> SaveBlocker.LabelTaken
}
