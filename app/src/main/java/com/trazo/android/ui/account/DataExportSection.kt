package com.trazo.android.ui.account

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trazo.android.R
import com.trazo.android.ui.common.InlineError
import com.trazo.android.ui.common.MinTouch
import com.trazo.android.ui.common.SectionTitle
import com.trazo.android.ui.theme.TrazoButton
import com.trazo.android.ui.theme.TrazoPalette

/** "Download my data": the file of the server, saved wherever the person chooses. */
@Composable
fun DataExportSection(viewModel: DataExportViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        viewModel.save(uri?.let { chosen -> { context.contentResolver.openOutputStream(chosen, "wt") } })
    }
    LaunchedEffect(state.readyToSave) {
        if (state.readyToSave) picker.launch("trazo-${java.time.LocalDate.now()}.json")
    }

    SectionTitle(stringResource(R.string.export_title))
    Text(stringResource(R.string.export_note), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
    state.error?.let { InlineError(it) }
    if (state.saveFailed) Text(stringResource(R.string.export_save_failed), color = MaterialTheme.colorScheme.error)
    if (state.saved) Text(stringResource(R.string.export_saved), color = TrazoPalette.Ok)
    TrazoButton(
        onClick = viewModel::download, enabled = !state.downloading && !state.readyToSave, secondary = true,
        modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)
    ) { Text(stringResource(R.string.export_button)) }
}
