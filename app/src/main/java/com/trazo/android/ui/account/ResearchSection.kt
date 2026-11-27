package com.trazo.android.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.trazo.android.R
import com.trazo.android.data.net.ApiError
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.repo.ResearchConsentRepository
import com.trazo.android.diagnostics.launchGuarded
import com.trazo.android.ui.common.*
import com.trazo.android.ui.theme.TrazoPalette

/** The research consent: a switch that stays disabled until the server has said how it is. */
@Composable
fun ResearchSection(repository: ResearchConsentRepository) {
    var consent by remember { mutableStateOf<Boolean?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<ApiError?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        scope.launchGuarded(R.string.feature_account) {
            when (val result = repository.read()) {
                is ApiResult.Success -> consent = result.value
                is ApiResult.Failure -> error = result.error
            }
        }
    }

    fun change(wanted: Boolean) {
        busy = true
        error = null
        scope.launchGuarded(R.string.feature_account, onFailure = { busy = false }) {
            when (val result = repository.set(wanted)) {
                is ApiResult.Success -> consent = wanted
                is ApiResult.Failure -> error = result.error
            }
            busy = false
        }
    }

    Row(
        Modifier.fillMaxWidth().heightIn(min = MinTouch)
            .toggleable(consent == true, enabled = consent != null && !busy, role = Role.Switch, onValueChange = ::change),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f)) { SectionTitle(stringResource(R.string.research_title)) }
        Switch(checked = consent == true, onCheckedChange = null, enabled = consent != null && !busy)
    }
    Text(stringResource(R.string.research_text), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
    error?.let { InlineError(it) }
}
