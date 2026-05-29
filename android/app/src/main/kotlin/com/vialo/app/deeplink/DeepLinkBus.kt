package com.vialo.app.deeplink

import android.net.Uri
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Single-process bus that the [com.vialo.app.MainActivity] uses to forward
 * incoming deep-link URIs to the Compose layer.
 *
 * Replay buffer of 1 so a link delivered before any collector is attached
 * (cold start) still reaches the eventual subscriber.
 */
object DeepLinkBus {
    private val _uris = MutableSharedFlow<Uri>(replay = 1, extraBufferCapacity = 4)
    val uris: SharedFlow<Uri> = _uris.asSharedFlow()

    fun publish(uri: Uri) {
        _uris.tryEmit(uri)
    }
}
