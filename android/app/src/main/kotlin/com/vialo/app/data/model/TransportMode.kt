package com.vialo.app.data.model

/**
 * How tapping a shortcut should launch.
 *
 * Stored as the enum name (`"DRIVE"`, `"TRANSIT"`, `"UBER"`) in both the
 * Room column and the [com.vialo.app.data.pairing.RemoteShortcut] wire
 * model so paired devices share the same constant. Add new modes by
 * appending here — never reorder or rename existing values, because old
 * snapshots on the backend carry these literals.
 */
enum class TransportMode {
    DRIVE,
    TRANSIT,
    UBER;

    companion object {
        /** Parse a persisted name, falling back to [DRIVE] for unknown values
         *  so an older client reading a future snapshot degrades to a sane
         *  default rather than crashing. */
        fun fromName(name: String?): TransportMode =
            entries.firstOrNull { it.name == name } ?: DRIVE
    }
}
