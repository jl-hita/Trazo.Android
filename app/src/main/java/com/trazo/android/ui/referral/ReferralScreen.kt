package com.trazo.android.ui.referral

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trazo.android.R
import com.trazo.android.data.referral.PremiumGrant
import com.trazo.android.data.referral.ReferralInvitation
import com.trazo.android.data.referral.invitationMessage
import com.trazo.android.ui.common.*
import com.trazo.android.ui.plan.formatDate
import com.trazo.android.ui.theme.TrazoButton
import com.trazo.android.ui.theme.TrazoLink
import com.trazo.android.ui.theme.TrazoPalette

/** Invite a friend: personal code and link, how the programme works, statistics, invitations and the Premium time earned. */
@Composable
fun ReferralScreen(viewModel: ReferralViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TrazoTopBar(stringResource(R.string.referral_title), onBack = onBack)
        val me = state.me
        when {
            state.loading -> LoadingBox()
            me == null -> ErrorBox(state.error ?: com.trazo.android.data.net.ApiError(com.trazo.android.data.net.ErrorKind.Unknown), onRetry = { viewModel.load() })
            !me.enabled || me.code == null || me.link == null ->
                EmptyBox(stringResource(R.string.referral_off))
            else -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TrazoCard {
                    Text(stringResource(R.string.referral_headline, me.rewardDays), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.referral_how), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                }

                SectionTitle(stringResource(R.string.referral_code_label))
                TrazoCard {
                    Text(me.code, fontFamily = FontFamily.Monospace, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 4.sp)
                    Text(me.link, color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        TrazoButton(
                            onClick = { clipboard.setText(AnnotatedString(me.code)); copied = true },
                            secondary = true, modifier = Modifier.weight(1f).heightIn(min = MinTouch)
                        ) { Text(stringResource(if (copied) R.string.referral_copied else R.string.referral_copy)) }
                        TrazoButton(
                            onClick = {
                                val text = invitationMessage(context.getString(R.string.referral_share_message, me.code), me.link)
                                val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                                    .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.referral_share_subject))
                                    .putExtra(Intent.EXTRA_TEXT, text)
                                context.startActivity(Intent.createChooser(send, context.getString(R.string.referral_share)))
                                viewModel.shared()
                            },
                            modifier = Modifier.weight(1f).heightIn(min = MinTouch)
                        ) { Text(stringResource(R.string.referral_share)) }
                    }
                }

                SectionTitle(stringResource(R.string.referral_stats))
                val stats = me.stats
                if (stats != null) TrazoCard {
                    StatRow(stringResource(R.string.referral_stat_invited), stats.invited.toString())
                    StatRow(stringResource(R.string.referral_stat_waiting), stats.pending.toString())
                    StatRow(stringResource(R.string.referral_stat_converted), stats.converted.toString())
                    StatRow(stringResource(R.string.referral_stat_rewards), stats.rewards.toString())
                    StatRow(stringResource(R.string.referral_stat_days), pluralStringResource(R.plurals.referral_days, stats.daysEarned, stats.daysEarned), highlight = true)
                    if (stats.daysPending > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text(pluralStringResource(R.plurals.referral_days_pending, stats.daysPending, stats.daysPending), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }

                state.listError?.let { Notice(stringResource(R.string.referral_list_error)); TrazoLink(onClick = { viewModel.load(refresh = true) }, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.retry)) } }

                SectionTitle(stringResource(R.string.referral_invitations))
                if (state.invitations.isEmpty()) TrazoCard { Text(stringResource(R.string.referral_none), color = TrazoPalette.TextSecondary) }
                else state.invitations.forEach { InvitationCard(it) }

                val earned = state.grants.filter { it.kind != "admin_grant" || it.status != "voided" }
                if (earned.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.referral_history))
                    earned.forEach { GrantCard(it) }
                }

                TrazoButton(onClick = { viewModel.load(refresh = true) }, enabled = !state.refreshing, secondary = true, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)) {
                    Text(stringResource(if (state.refreshing) R.string.loading else R.string.referral_refresh))
                }
                Text(stringResource(R.string.referral_fine_print), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, highlight: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TrazoPalette.TextSecondary)
        Text(value, color = if (highlight) TrazoPalette.Ok else TrazoPalette.Text, fontWeight = FontWeight.SemiBold)
    }
}

/** An invitation shows only its state: who it is stays private, on both sides. */
@Composable
private fun InvitationCard(item: ReferralInvitation) {
    TrazoCard {
        Text(
            stringResource(
                when (item.status) {
                    "rewarded" -> R.string.referral_status_rewarded
                    "voided" -> R.string.referral_status_voided
                    "skipped" -> R.string.referral_status_skipped
                    else -> R.string.referral_status_registered
                }
            ),
            color = if (item.status == "rewarded") TrazoPalette.Ok else TrazoPalette.Text, fontWeight = FontWeight.SemiBold
        )
        Text(stringResource(R.string.referral_registered_on, formatDate(item.registeredAt.take(10))), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
        if (item.status == "rewarded") {
            val plan = when (item.plan) { "annual" -> stringResource(R.string.referral_plan_annual); "monthly" -> stringResource(R.string.referral_plan_monthly); else -> null }
            item.rewardDays?.let { Text(pluralStringResource(R.plurals.referral_reward_days, it, it) + (plan?.let { p -> " · $p" } ?: ""), style = MaterialTheme.typography.bodySmall) }
            rewardStateText(item.rewardState)?.let { Text(it, color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun rewardStateText(state: String?): String? = when (state) {
    "deferred" -> stringResource(R.string.referral_reward_deferred)
    "pending" -> stringResource(R.string.referral_reward_pending)
    "active" -> stringResource(R.string.referral_reward_active)
    else -> null
}

@Composable
private fun GrantCard(g: PremiumGrant) {
    TrazoCard {
        val kind = when (g.kind) {
            "signup_trial" -> R.string.referral_kind_welcome
            "referral_reward" -> R.string.referral_kind_reward
            else -> R.string.referral_kind_other
        }
        Text(stringResource(kind) + " · " + pluralStringResource(R.plurals.referral_days, g.days, g.days), fontWeight = FontWeight.SemiBold)
        val detail = when {
            g.status == "voided" -> stringResource(R.string.referral_grant_voided)
            g.status == "deferred" && g.googleExpiryAfter != null -> stringResource(R.string.referral_grant_deferred, formatDate(g.googleExpiryAfter.take(10)))
            g.status == "pending" -> stringResource(R.string.referral_reward_pending)
            g.startsAt != null && g.endsAt != null -> stringResource(R.string.referral_grant_window, formatDate(g.startsAt.take(10)), formatDate(g.endsAt.take(10)))
            else -> null
        }
        detail?.let { Text(it, color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall) }
    }
}
