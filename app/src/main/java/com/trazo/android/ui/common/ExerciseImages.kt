package com.trazo.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.trazo.android.R
import com.trazo.android.ui.theme.TrazoPalette

fun exerciseImageUrl(exerciseId: String, slot: Int = 0): String = "${com.trazo.android.BuildConfig.API_BASE_URL}api/v1/exercises/$exerciseId/images/$slot"

/** One exercise image. If the server has none (404) the empty box stays; nothing breaks. */
@Composable
fun ExerciseThumb(url: String, name: String, size: Dp = 56.dp, modifier: Modifier = Modifier) {
    var failed by remember(url) { mutableStateOf(false) }
    if (failed) return
    AsyncImage(
        model = url,
        contentDescription = stringResource(R.string.exercise_image_of, name),
        contentScale = ContentScale.Crop,
        onError = { failed = true },
        modifier = modifier.size(size).clip(RoundedCornerShape(8.dp)).background(TrazoPalette.SecondaryButton)
    )
}

/** Gallery of the (up to four) images of an exercise, with the attribution the API asks for. */
@Composable
fun ExerciseGallery(urls: List<String>, name: String, attribution: String?, modifier: Modifier = Modifier) {
    if (urls.isEmpty()) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(urls) { i, url ->
                AsyncImage(
                    model = url,
                    contentDescription = stringResource(R.string.exercise_image_n, name, i + 1),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(220.dp).widthIn(min = 160.dp).clip(RoundedCornerShape(10.dp)).background(TrazoPalette.SecondaryButton)
                )
            }
        }
        attribution?.let { Text(it, color = TrazoPalette.Muted, style = MaterialTheme.typography.bodySmall) }
    }
}
