package com.navigo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.navigo.app.service.search.PlaceResult
import kotlinx.coroutines.delay

/**
 * Forward-search field for OpenStreetMap Nominatim with a 400 ms debounce —
 * keeps us under the 1 req/sec policy. OSM attribution is shown below the
 * dropdown whenever results are visible (policy requirement).
 *
 * The search callback is suspend, so the caller owns the Ktor client (via
 * the [Graph]) and we don't pull a heavy dep into this Composable.
 */
@Composable
fun PlaceSearchField(
    onPlaceSelected: (PlaceResult) -> Unit,
    onSearch: suspend (String) -> List<PlaceResult>,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<PlaceResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        if (query.length < 3) {
            results = emptyList()
            errorText = null
            return@LaunchedEffect
        }
        isLoading = true
        errorText = null
        delay(DEBOUNCE_MS)
        try {
            results = onSearch(query)
        } catch (_: Exception) {
            errorText = "Could not search. Check your internet connection and try again."
            results = emptyList()
        } finally {
            isLoading = false
        }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search for a place...") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                when {
                    isLoading -> CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    query.isNotEmpty() -> IconButton(onClick = {
                        query = ""
                        results = emptyList()
                        errorText = null
                    }) {
                        Icon(Icons.Outlined.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        errorText?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp),
            )
        }

        if (results.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .heightIn(max = DROPDOWN_MAX_HEIGHT_DP.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 2.dp,
            ) {
                LazyColumn {
                    items(results, key = { it.placeId.ifBlank { it.displayName } }) { result ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    query = result.displayName
                                    results = emptyList()
                                    errorText = null
                                    onPlaceSelected(result)
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = result.displayName,
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .fillMaxWidth(),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    "© OpenStreetMap contributors",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                )
            }
        }
    }
}

private const val DEBOUNCE_MS = 400L
private const val DROPDOWN_MAX_HEIGHT_DP = 300
