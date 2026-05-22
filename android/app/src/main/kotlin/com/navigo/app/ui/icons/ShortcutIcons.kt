package com.navigo.app.ui.icons

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Duotone
import com.adamglin.phosphoricons.duotone.Airplane
import com.adamglin.phosphoricons.duotone.Bank
import com.adamglin.phosphoricons.duotone.Barbell
import com.adamglin.phosphoricons.duotone.Briefcase
import com.adamglin.phosphoricons.duotone.Bus
import com.adamglin.phosphoricons.duotone.Church
import com.adamglin.phosphoricons.duotone.Coffee
import com.adamglin.phosphoricons.duotone.FirstAidKit
import com.adamglin.phosphoricons.duotone.ForkKnife
import com.adamglin.phosphoricons.duotone.GasPump
import com.adamglin.phosphoricons.duotone.GraduationCap
import com.adamglin.phosphoricons.duotone.Hospital
import com.adamglin.phosphoricons.duotone.House
import com.adamglin.phosphoricons.duotone.MapPin
import com.adamglin.phosphoricons.duotone.MoonStars
import com.adamglin.phosphoricons.duotone.Mosque
import com.adamglin.phosphoricons.duotone.Park
import com.adamglin.phosphoricons.duotone.ShoppingCart
import com.adamglin.phosphoricons.duotone.Sparkle
import com.adamglin.phosphoricons.duotone.Stethoscope
import com.adamglin.phosphoricons.duotone.Storefront
import com.adamglin.phosphoricons.duotone.Train
import com.adamglin.phosphoricons.duotone.Tree
import com.adamglin.phosphoricons.duotone.User
import com.adamglin.phosphoricons.duotone.UsersThree

/**
 * Maps the 24 stable shortcut icon keys (carried over from the Flutter build
 * so deep-link payloads stay decodable) to Phosphor duotone ImageVectors.
 *
 * Keep this list and [ShortcutIconCatalog.entries] in sync — the icon picker
 * renders directly from the catalog.
 *
 * Notes:
 *   - "temple" falls back to Sparkle (Phosphor has no Hindu-temple glyph).
 *     Replace during the design pass if a better stand-in surfaces.
 */
object ShortcutIconCatalog {

    data class IconEntry(val key: String, val label: String, val image: ImageVector)

    val entries: List<IconEntry> = listOf(
        IconEntry("home", "Home", PhosphorIcons.Duotone.House),
        IconEntry("hospital", "Hospital", PhosphorIcons.Duotone.Hospital),
        IconEntry("bank", "Bank", PhosphorIcons.Duotone.Bank),
        IconEntry("grocery", "Grocery", PhosphorIcons.Duotone.ShoppingCart),
        IconEntry("temple", "Temple", PhosphorIcons.Duotone.Sparkle),
        IconEntry("mosque", "Mosque", PhosphorIcons.Duotone.Mosque),
        IconEntry("church", "Church", PhosphorIcons.Duotone.Church),
        IconEntry("pharmacy", "Pharmacy", PhosphorIcons.Duotone.FirstAidKit),
        IconEntry("restaurant", "Restaurant", PhosphorIcons.Duotone.ForkKnife),
        IconEntry("cafe", "Cafe", PhosphorIcons.Duotone.Coffee),
        IconEntry("park", "Park", PhosphorIcons.Duotone.Park),
        IconEntry("office", "Office", PhosphorIcons.Duotone.Briefcase),
        IconEntry("school", "School", PhosphorIcons.Duotone.GraduationCap),
        IconEntry("gym", "Gym", PhosphorIcons.Duotone.Barbell),
        IconEntry("doctor", "Doctor", PhosphorIcons.Duotone.Stethoscope),
        IconEntry("airport", "Airport", PhosphorIcons.Duotone.Airplane),
        IconEntry("train", "Train", PhosphorIcons.Duotone.Train),
        IconEntry("bus", "Bus", PhosphorIcons.Duotone.Bus),
        IconEntry("petrol", "Petrol", PhosphorIcons.Duotone.GasPump),
        IconEntry("market", "Market", PhosphorIcons.Duotone.Storefront),
        IconEntry("friend", "Friend", PhosphorIcons.Duotone.User),
        IconEntry("family", "Family", PhosphorIcons.Duotone.UsersThree),
        IconEntry("place", "Other", PhosphorIcons.Duotone.MapPin),
    )

    private val byKey = entries.associateBy { it.key }

    fun forKey(key: String): ImageVector =
        byKey[key]?.image ?: PhosphorIcons.Duotone.MapPin

    fun labelFor(key: String): String =
        byKey[key]?.label ?: "Other"
}

/**
 * Best-guess icon key from a free-text label. Mirrors the Flutter
 * `autoDetectIcon` heuristic so users get the same suggested icon when adding
 * a new place by search.
 */
fun autoDetectIconKey(label: String): String {
    val lower = label.lowercase()
    for ((key, keywords) in AUTODETECT_PATTERNS) {
        if (keywords.any { it in lower }) return key
    }
    return "place"
}

private val AUTODETECT_PATTERNS: List<Pair<String, List<String>>> = listOf(
    "hospital" to listOf("hospital", "medical center", "clinic", "health center", "emergency"),
    "doctor" to listOf("doctor", "dr.", "physician", "dentist", "dental"),
    "pharmacy" to listOf("pharmacy", "chemist", "drugstore", "medical store", "medicine"),
    "bank" to listOf("bank", "atm", "credit union", "finance"),
    "grocery" to listOf("grocery", "supermarket", "mart", "provision", "kirana"),
    "restaurant" to listOf("restaurant", "bistro", "diner", "eatery", "dhaba", "food"),
    "cafe" to listOf("cafe", "coffee", "starbucks", "bakery"),
    "temple" to listOf("temple", "mandir", "hindu", "gurudwara"),
    "mosque" to listOf("mosque", "masjid", "islamic"),
    "church" to listOf("church", "cathedral", "chapel", "christian"),
    "school" to listOf("school", "college", "university", "academy", "institute", "education"),
    "park" to listOf("park", "garden", "playground", "nature"),
    "gym" to listOf("gym", "fitness", "yoga", "workout", "sports"),
    "airport" to listOf("airport", "terminal", "aviation"),
    "train" to listOf("train", "railway", "station", "metro"),
    "bus" to listOf("bus stop", "bus stand", "bus station", "bus depot"),
    "petrol" to listOf("petrol", "gas station", "fuel", "diesel", "petroleum"),
    "market" to listOf("market", "bazaar", "mall", "shopping", "store", "shop"),
    "office" to listOf("office", "corporate", "workspace", "coworking"),
    "home" to listOf("home", "house", "apartment", "residence", "flat"),
)

/**
 * Renders a Phosphor duotone icon as an [Image] (not [androidx.compose.material3.Icon])
 * so the duotone fill is preserved instead of being flattened to a single tint.
 *
 * The Phosphor primary path uses Color.Black and the secondary path uses 20%
 * alpha black — readable on light surfaces. Dark-theme styling is a design-pass
 * concern; revisit before shipping a polished UI.
 */
@Composable
fun ShortcutIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = rememberVectorPainter(image = imageVector),
        contentDescription = contentDescription,
        modifier = modifier,
    )
}
