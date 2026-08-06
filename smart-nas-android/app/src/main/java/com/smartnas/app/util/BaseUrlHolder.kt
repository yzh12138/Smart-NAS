package com.smartnas.app.util

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BaseUrlHolder @Inject constructor() {
    @Volatile
    var baseUrl: String = "http://10.0.2.2:8080"
        set(value) { field = value.trimEnd('/') }

    fun photoThumbUrl(photoId: Long): String = "$baseUrl/api/photo/$photoId/thumb"
    fun photoOriginalUrl(photoId: Long): String = "$baseUrl/api/photo/$photoId/original"
}
