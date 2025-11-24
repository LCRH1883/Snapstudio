package com.snapswipe.app.data

import android.net.Uri

data class PhotoItem(
    val id: Long,
    val uri: Uri,
    val dateTaken: Long?
)
