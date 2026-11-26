@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package dolphin.desktop.apps.onitranslator.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dolphin.desktop.apps.onitranslator.app.AppEvent
import dolphin.desktop.apps.onitranslator.generated.resources.NisbetThinking
import dolphin.desktop.apps.onitranslator.generated.resources.Res
import dolphin.desktop.apps.onitranslator.generated.resources.bottom_bar_ai_stats
import dolphin.desktop.apps.onitranslator.generated.resources.bottom_bar_ai_stats_detail
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_apply_option
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_apply_option_from_round
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_copy_handoff_tooltip
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_ctx_glossary
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_ctx_recent
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_ctx_siblings
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_desk_title
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_error_cancelled
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_error_missing_key
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_error_missing_model
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_error_rate_daily
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_error_rate_generic
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_error_rate_minute_delay
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_error_unauthorized
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_followup_placeholder
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_followup_send
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_loading_elapsed
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_open_settings
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_preview_hide
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_preview_show
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_prompt_char_count
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_prompt_editor_label
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_prompt_reset
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_recommended
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_resend
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_retry
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_retry_followup
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_round_collapse
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_round_expand
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_round_followup
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_round_initial
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_round_selected_badge
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_send_request
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_sensor_glossary_ok
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_sensor_glossary_warn
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_sensor_newlines_ok
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_sensor_newlines_warn
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_sensor_tags_ok
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_sensor_tags_warn
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_show_prompt
import dolphin.desktop.apps.onitranslator.generated.resources.button_cancel
import dolphin.desktop.apps.onitranslator.generated.resources.button_close
import dolphin.desktop.apps.onitranslator.generated.resources.ic_auto_awesome
import dolphin.desktop.apps.onitranslator.generated.resources.ic_close
import dolphin.desktop.apps.onitranslator.generated.resources.ic_content_copy
import dolphin.desktop.apps.onitranslator.generated.resources.ic_done
import dolphin.desktop.apps.onitranslator.generated.resources.ic_report
import dolphin.desktop.apps.onitranslator.generated.resources.ic_visibility
import dolphin.desktop.apps.onitranslator.generated.resources.ic_visibility_off
import dolphin.desktop.apps.onitranslator.model.BrigetteDeskState
import dolphin.desktop.apps.onitranslator.model.BrigetteRound
import dolphin.desktop.apps.onitranslator.model.GeminiError
import dolphin.desktop.apps.onitranslator.model.GeminiOption
import dolphin.desktop.apps.onitranslator.model.GeminiPayload
import dolphin.desktop.apps.onitranslator.model.GeminiSessionStats
import dolphin.desktop.apps.onitranslator.model.GeminiTranslationResult
import dolphin.desktop.apps.onitranslator.model.GeminiUsage
import dolphin.desktop.apps.onitranslator.model.GlossarySensor
import dolphin.desktop.apps.onitranslator.model.GlossaryTerm
import dolphin.desktop.apps.onitranslator.model.PromptContextOptions
import dolphin.desktop.apps.onitranslator.model.PromptContextSnapshot
import dolphin.desktop.apps.onitranslator.model.SiblingContext
import dolphin.desktop.apps.onitranslator.model.TagSensor
import dolphin.desktop.apps.onitranslator.theme.OniTranslatorTheme
import dolphin.desktop.apps.onitranslator.widget.TooltipIconButton
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.util.Locale

/**
 * BrigetteDeskDrawer: Slide-in AI localization review overlay layered over the right-hand editor.
 * Supports Stage-then-Send editable prompt preview before dispatching to Gemini, followed by
 * scored Brigette translation options with live Tag/Glossary sensor checks and inline
 * [NisbetPeekCard] in-game previews before applying.
 */
@Composable
fun BrigetteDeskDrawer(
    state: BrigetteDeskState,
    sourceText: String,
    glossaryTerms: List<GlossaryTerm>,
    configuredModel: String,
    onEvent: (AppEvent) -> Unit,
    onToggleContextOptions: (PromptContextOptions) -> Unit,
    onSendRequest: (PromptContextOptions, String) -> Unit,
    onApplyTranslation: (String) -> Unit,
    modifier: Modifier = Modifier,
    sessionStats: GeminiSessionStats = GeminiSessionStats(),
    initialPreviewIndices: Set<Int> = emptySet(),
    initialExpandedHistoryRounds: Set<Int> = emptySet(),
    initialFollowUpDraft: String = "",
) {
    AnimatedVisibility(
        visible = state.visible,
        enter = slideInHorizontally { it } + fadeIn(),
        exit = slideOutHorizontally { it } + fadeOut(),
        modifier = modifier.fillMaxSize().padding(start = 32.dp),
    ) {
        var showGlossaryDetails by remember(state.entryKey) { mutableStateOf(false) }
        var showPromptInResult by remember(state.entryKey) { mutableStateOf(false) }
        var followUpText by remember(state.entryKey) { mutableStateOf(initialFollowUpDraft) }
        var expandedHistoryRounds by remember(state.entryKey, state.rounds.size) {
            mutableStateOf(initialExpandedHistoryRounds)
        }
        var previewPairs by remember(state.entryKey, state.rounds.size) {
            val latestIdx = state.rounds.lastIndex.coerceAtLeast(0)
            mutableStateOf(initialPreviewIndices.map { latestIdx to it }.toSet())
        }
        val lastSentOptions = remember(state.entryKey, state.startedAtMs, state.rounds.size) {
            state.contextOptions
        }
        val hasRounds = state.rounds.isNotEmpty()
        val optionsDirty = hasRounds && state.contextOptions != lastSentOptions
        val activeModel = state.result?.usage?.modelName?.ifBlank { configuredModel }
            ?: configuredModel.ifBlank { "—" }

        fun submitFollowUp() {
            val trimmed = followUpText.trim()
            if (trimmed.isBlank() || state.isLoading) return
            followUpText = ""
            onEvent(AppEvent.Brigette.FollowUp(trimmed))
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_auto_awesome),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(Res.string.brigette_desk_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            text = activeModel,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (hasRounds) {
                        TooltipIconButton(
                            painter = painterResource(Res.drawable.ic_content_copy),
                            tooltip = stringResource(Res.string.brigette_copy_handoff_tooltip),
                            modifier = Modifier.size(28.dp),
                            onClick = { onEvent(AppEvent.Brigette.CopyHandoff(draftFollowUp = followUpText)) },
                        )
                    }
                    IconButton(
                        onClick = { onEvent(AppEvent.Brigette.Dismiss) },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_close),
                            contentDescription = stringResource(Res.string.button_close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Dynamic Context Controls
                ContextControlBar(
                    state = state,
                    showGlossaryDetails = showGlossaryDetails,
                    onToggleGlossaryDetails = { showGlossaryDetails = !showGlossaryDetails },
                    showPromptInResult = showPromptInResult,
                    onTogglePromptInResult = { showPromptInResult = !showPromptInResult },
                    optionsDirty = optionsDirty,
                    onEvent = onEvent,
                    onToggleContextOptions = onToggleContextOptions,
                    onSendRequest = onSendRequest,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Main Content (Multi-turn Conversation / Initial Loading / Initial Error / Ready Editable Prompt Preview)
                when {
                    hasRounds -> {
                        val conversationScrollState = rememberScrollState()
                        LaunchedEffect(state.rounds.size, state.isLoading, state.error) {
                            if (state.rounds.size > 1 || state.isLoading || state.error != null) {
                                conversationScrollState.animateScrollTo(conversationScrollState.maxValue)
                            }
                        }
                        DeskScrollContainer(
                            modifier = Modifier.weight(1f),
                            scrollState = conversationScrollState,
                        ) {
                            state.rounds.forEachIndexed { roundIndex, round ->
                                val isLatest = roundIndex == state.rounds.lastIndex
                                val isExpanded = isLatest || roundIndex in expandedHistoryRounds
                                val hasSelectedInRound = roundIndex == state.selectedRoundIndex

                                if (isExpanded) {
                                    RoundDetailSection(
                                        roundIndex = roundIndex,
                                        totalRounds = state.rounds.size,
                                        round = round,
                                        isLatest = isLatest,
                                        hasSelectedInRound = hasSelectedInRound,
                                        selectedOptionIndex = state.selectedOptionIndex,
                                        sourceText = sourceText,
                                        glossaryTerms = glossaryTerms,
                                        previewPairs = previewPairs,
                                        onCollapse = { expandedHistoryRounds = expandedHistoryRounds - roundIndex },
                                        onSelectOption = { optIndex ->
                                            onEvent(AppEvent.Brigette.SelectOption(optIndex, roundIndex))
                                        },
                                        onTogglePreview = { optIndex ->
                                            val pair = roundIndex to optIndex
                                            previewPairs = if (pair in previewPairs) {
                                                previewPairs - pair
                                            } else {
                                                previewPairs + pair
                                            }
                                        },
                                    )
                                } else {
                                    HistoryRoundSummaryCard(
                                        roundIndex = roundIndex,
                                        round = round,
                                        hasSelectedInRound = hasSelectedInRound,
                                        selectedOptionIndex = state.selectedOptionIndex,
                                        onExpand = { expandedHistoryRounds = expandedHistoryRounds + roundIndex },
                                    )
                                }
                            }

                            if (state.isLoading) {
                                if (state.pendingFollowUp.isNotBlank()) {
                                    FollowUpPromptBanner(prompt = state.pendingFollowUp)
                                }
                                BrigetteLoadingView(
                                    startedAtMs = state.startedAtMs,
                                    onCancel = { onEvent(AppEvent.Brigette.Cancel) },
                                )
                            } else if (state.error != null) {
                                if (state.pendingFollowUp.isNotBlank()) {
                                    FollowUpPromptBanner(prompt = state.pendingFollowUp)
                                }
                                BrigetteErrorView(
                                    error = state.error,
                                    onOpenSettings = { onEvent(AppEvent.Ui.ShowConfig) },
                                    onRetry = {
                                        val msg = state.pendingFollowUp.ifBlank { followUpText.trim() }
                                        if (msg.isNotBlank()) {
                                            onEvent(AppEvent.Brigette.FollowUp(msg))
                                        } else {
                                            onSendRequest(state.contextOptions, state.editablePrompt)
                                        }
                                    },
                                    retryLabel = if (state.pendingFollowUp.isNotBlank()) {
                                        stringResource(Res.string.brigette_retry_followup)
                                    } else {
                                        null
                                    },
                                )
                            }
                        }
                    }

                    state.isLoading -> {
                        DeskScrollContainer(modifier = Modifier.weight(1f)) {
                            BrigetteLoadingView(
                                startedAtMs = state.startedAtMs,
                                onCancel = { onEvent(AppEvent.Brigette.Cancel) },
                            )
                        }
                    }

                    state.error != null -> {
                        DeskScrollContainer(modifier = Modifier.weight(1f)) {
                            BrigetteErrorView(
                                error = state.error,
                                onOpenSettings = { onEvent(AppEvent.Ui.ShowConfig) },
                                onRetry = { onSendRequest(state.contextOptions, state.editablePrompt) },
                            )
                        }
                    }

                    else -> {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) {
                            EditablePromptCard(
                                promptText = state.editablePrompt,
                                isEdited = state.isPromptEdited,
                                onPromptChange = { onEvent(AppEvent.Brigette.UpdatePrompt(it)) },
                                onReset = { onEvent(AppEvent.Brigette.ResetPrompt) },
                                fillHeight = true,
                            )
                        }
                    }
                }

                // Option A: Permanent single-line follow-up input bar whenever at least 1 round exists
                if (hasRounds) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    FollowUpInputBar(
                        text = followUpText,
                        enabled = !state.isLoading,
                        onTextChange = { followUpText = it },
                        onSend = ::submitFollowUp,
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Sticky Footer: Usage Telemetry & Primary Actions
                val selectedOption = state.selectedOption
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.result?.usage?.let { usage ->
                        val secText = String.format(Locale.US, "%.1f", usage.elapsedMs / 1000.0)
                        val thoughtsSuffix = if (usage.thoughtsTokens > 0) " / Think ${usage.thoughtsTokens}" else ""
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "⏱ ${secText}s · In ${usage.promptTokens} / Out ${usage.candidateTokens}$thoughtsSuffix · Total ${usage.totalTokens} tok",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            SessionStatsFooterText(sessionStats)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (hasRounds) {
                            val isPickingEarlierRound = state.rounds.size > 1 && state.selectedRoundIndex != state.rounds.lastIndex
                            OutlinedButton(
                                onClick = { onSendRequest(state.contextOptions, state.editablePrompt) },
                                enabled = !state.isLoading,
                            ) {
                                Text(stringResource(Res.string.brigette_resend))
                            }
                            Spacer(Modifier.weight(1f))
                            Button(
                                onClick = {
                                    selectedOption?.let { onApplyTranslation(it.text) }
                                },
                                enabled = !state.isLoading && selectedOption != null && selectedOption.text.isNotEmpty(),
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_done),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (isPickingEarlierRound) {
                                        stringResource(Res.string.brigette_apply_option_from_round, state.selectedRoundIndex + 1)
                                    } else {
                                        stringResource(Res.string.brigette_apply_option)
                                    }
                                )
                            }
                        } else {
                            SessionStatsFooterText(sessionStats)
                            Spacer(Modifier.weight(1f))
                            Button(
                                onClick = { onSendRequest(state.contextOptions, state.editablePrompt) },
                                enabled = !state.isLoading && state.editablePrompt.isNotBlank(),
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_auto_awesome),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(Res.string.brigette_send_request))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeskScrollContainer(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun HistoryRoundSummaryCard(
    roundIndex: Int,
    round: BrigetteRound,
    hasSelectedInRound: Boolean,
    selectedOptionIndex: Int,
    onExpand: () -> Unit,
) {
    val payload = round.result.payload
    val previewOption = if (hasSelectedInRound) {
        payload.options.getOrNull(selectedOptionIndex)
    } else {
        payload.options.getOrNull(payload.recommendedIndex) ?: payload.options.firstOrNull()
    }
    val roundLabel = if (round.isFollowUp) {
        stringResource(Res.string.brigette_round_followup, roundIndex + 1)
    } else {
        stringResource(Res.string.brigette_round_initial, roundIndex + 1)
    }

    Surface(
        onClick = onExpand,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        border = BorderStroke(
            width = if (hasSelectedInRound) 1.5.dp else 1.dp,
            color = if (hasSelectedInRound) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            },
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = roundLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (hasSelectedInRound) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = stringResource(Res.string.brigette_round_selected_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(Res.string.brigette_round_expand),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (round.isFollowUp && round.userPrompt.isNotBlank()) {
                Text(
                    text = round.userPrompt,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            previewOption?.let { opt ->
                Text(
                    text = opt.text,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun RoundDetailSection(
    roundIndex: Int,
    totalRounds: Int,
    round: BrigetteRound,
    isLatest: Boolean,
    hasSelectedInRound: Boolean,
    selectedOptionIndex: Int,
    sourceText: String,
    glossaryTerms: List<GlossaryTerm>,
    previewPairs: Set<Pair<Int, Int>>,
    onCollapse: () -> Unit,
    onSelectOption: (Int) -> Unit,
    onTogglePreview: (Int) -> Unit,
) {
    val payload = round.result.payload
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (totalRounds > 1) {
            val roundLabel = if (round.isFollowUp) {
                stringResource(Res.string.brigette_round_followup, roundIndex + 1)
            } else {
                stringResource(Res.string.brigette_round_initial, roundIndex + 1)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = roundLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (hasSelectedInRound && !isLatest) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = stringResource(Res.string.brigette_round_selected_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                if (!isLatest) {
                    TextButton(
                        onClick = onCollapse,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.brigette_round_collapse),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }

        if (round.isFollowUp && round.userPrompt.isNotBlank()) {
            FollowUpPromptBanner(prompt = round.userPrompt)
        }

        if (payload.analysis.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = payload.analysis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(10.dp),
                )
            }
        }

        payload.options.forEachIndexed { index, option ->
            val isSelected = hasSelectedInRound && index == selectedOptionIndex
            val isRecommended = index == payload.recommendedIndex
            val isPreviewVisible = (roundIndex to index) in previewPairs

            ScoredOptionCard(
                option = option,
                sourceText = sourceText,
                glossaryTerms = glossaryTerms,
                isSelected = isSelected,
                isRecommended = isRecommended,
                previewVisible = isPreviewVisible,
                onSelect = { onSelectOption(index) },
                onTogglePreview = { onTogglePreview(index) },
            )
        }

        if (payload.conclusion.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = payload.conclusion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(10.dp),
                )
            }
        }
    }
}

@Composable
private fun FollowUpPromptBanner(prompt: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = prompt,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun FollowUpInputBar(
    text: String,
    enabled: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            enabled = enabled,
            minLines = 1,
            maxLines = 3,
            placeholder = {
                Text(
                    text = stringResource(Res.string.brigette_followup_placeholder),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            textStyle = MaterialTheme.typography.bodySmall,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .weight(1f)
                .onPreviewKeyEvent { event ->
                    // Only submit on Ctrl+Enter; never intercept plain Enter so Windows Zhuyin IME candidate confirmation works safely
                    if (event.type == KeyEventType.KeyDown &&
                        event.isCtrlPressed &&
                        (event.key == Key.Enter || event.key == Key.NumPadEnter)
                    ) {
                        onSend()
                        true
                    } else {
                        false
                    }
                },
        )
        FilledTonalButton(
            onClick = onSend,
            enabled = enabled && text.isNotBlank(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_auto_awesome),
                contentDescription = null,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(Res.string.brigette_followup_send),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun ContextControlBar(
    state: BrigetteDeskState,
    showGlossaryDetails: Boolean,
    onToggleGlossaryDetails: () -> Unit,
    showPromptInResult: Boolean,
    onTogglePromptInResult: () -> Unit,
    optionsDirty: Boolean,
    onEvent: (AppEvent) -> Unit,
    onToggleContextOptions: (PromptContextOptions) -> Unit,
    onSendRequest: (PromptContextOptions, String) -> Unit,
) {
    val snapshot = state.contextSnapshot
    val isReadyPreview = !state.isLoading && state.result == null && state.error == null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            DeskContextChip(
                label = stringResource(Res.string.brigette_ctx_glossary, snapshot?.matchedTerms?.size ?: 0),
                selected = showGlossaryDetails,
                showCheckmark = false,
                onClick = onToggleGlossaryDetails,
            )
            DeskContextChip(
                label = stringResource(Res.string.brigette_ctx_siblings, snapshot?.siblings?.size ?: 0),
                selected = state.contextOptions.includeSiblings,
                enabled = !state.isLoading,
                onClick = {
                    onToggleContextOptions(
                        state.contextOptions.copy(includeSiblings = !state.contextOptions.includeSiblings)
                    )
                },
            )
            DeskContextChip(
                label = stringResource(Res.string.brigette_ctx_recent, snapshot?.recent?.size ?: 0),
                selected = state.contextOptions.includeRecent,
                enabled = !state.isLoading,
                onClick = {
                    onToggleContextOptions(
                        state.contextOptions.copy(includeRecent = !state.contextOptions.includeRecent)
                    )
                },
            )
            if (!isReadyPreview && state.editablePrompt.isNotBlank()) {
                DeskContextChip(
                    label = stringResource(Res.string.brigette_show_prompt),
                    selected = showPromptInResult,
                    showCheckmark = false,
                    onClick = onTogglePromptInResult,
                )
            }
            if ((optionsDirty || state.isPromptEdited) && state.result != null && !state.isLoading) {
                FilledTonalButton(
                    onClick = { onSendRequest(state.contextOptions, state.editablePrompt) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                ) {
                    Text(stringResource(Res.string.brigette_resend), style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (showGlossaryDetails && snapshot != null && snapshot.matchedTerms.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp),
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    snapshot.matchedTerms.forEach { term ->
                        Text(
                            text = "${term.english} => ${term.canonicalCht}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (!isReadyPreview && showPromptInResult && state.editablePrompt.isNotBlank()) {
            EditablePromptCard(
                promptText = state.editablePrompt,
                isEdited = state.isPromptEdited,
                onPromptChange = { onEvent(AppEvent.Brigette.UpdatePrompt(it)) },
                onReset = { onEvent(AppEvent.Brigette.ResetPrompt) },
                fillHeight = false,
                minLines = 5,
                maxLines = 8,
            )
        }
    }
}

@Composable
private fun DeskContextChip(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    showCheckmark: Boolean = true,
    onClick: () -> Unit,
) {
    val chipShape = RoundedCornerShape(8.dp)
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    }

    Surface(
        shape = chipShape,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier
                .clip(chipShape)
                .clickable(enabled = enabled, onClick = onClick)
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (selected && showCheckmark) {
                Icon(
                    painter = painterResource(Res.drawable.ic_done),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = contentColor,
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun EditablePromptCard(
    promptText: String,
    isEdited: Boolean,
    onPromptChange: (String) -> Unit,
    onReset: () -> Unit,
    fillHeight: Boolean = false,
    minLines: Int = 8,
    maxLines: Int = Int.MAX_VALUE,
) {
    Column(
        modifier = if (fillHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.brigette_prompt_editor_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(Res.string.brigette_prompt_char_count, promptText.length),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isEdited) {
                TextButton(
                    onClick = onReset,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.brigette_prompt_reset),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        OutlinedTextField(
            value = promptText,
            onValueChange = onPromptChange,
            minLines = minLines,
            maxLines = maxLines,
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            shape = MaterialTheme.shapes.medium,
            modifier = if (fillHeight) Modifier.fillMaxWidth().weight(1f) else Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun BrigetteLoadingView(
    startedAtMs: Long,
    onCancel: () -> Unit,
) {
    var elapsedMs by remember(startedAtMs) {
        mutableLongStateOf(
            if (startedAtMs > 0L) (System.currentTimeMillis() - startedAtMs).coerceAtLeast(0L) else 0L
        )
    }
    LaunchedEffect(startedAtMs) {
        while (true) {
            elapsedMs = if (startedAtMs > 0L) {
                (System.currentTimeMillis() - startedAtMs).coerceAtLeast(0L)
            } else {
                elapsedMs + 100L
            }
            delay(100L)
        }
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(Res.drawable.NisbetThinking),
                contentDescription = null,
                modifier = Modifier.size(84.dp),
            )
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            val elapsedSec = String.format(Locale.US, "%.1f", elapsedMs / 1000.0)
            Text(
                text = stringResource(Res.string.brigette_loading_elapsed, elapsedSec),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onCancel) {
                Text(stringResource(Res.string.button_cancel))
            }
        }
    }
}

@Composable
private fun BrigetteErrorView(
    error: GeminiError,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    retryLabel: String? = null,
) {
    val guidance = when (error) {
        is GeminiError.MissingApiKey -> stringResource(Res.string.brigette_error_missing_key)
        is GeminiError.ModelNotFound -> stringResource(Res.string.brigette_error_missing_model)
        is GeminiError.Unauthorized -> stringResource(Res.string.brigette_error_unauthorized, error.httpStatus ?: 401)
        is GeminiError.RateLimited -> when {
            error.isDailyQuota -> stringResource(Res.string.brigette_error_rate_daily)
            error.retryDelaySeconds != null -> stringResource(
                Res.string.brigette_error_rate_minute_delay,
                error.retryDelaySeconds,
            )
            else -> stringResource(Res.string.brigette_error_rate_generic)
        }
        is GeminiError.Cancelled -> stringResource(Res.string.brigette_error_cancelled)
        else -> error.message
    }
    val showSettingsButton = error is GeminiError.MissingApiKey ||
        error is GeminiError.ModelNotFound ||
        error is GeminiError.Unauthorized ||
        (error is GeminiError.RateLimited && error.isDailyQuota)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_report),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = guidance,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (guidance != error.message && error.message.isNotBlank()) {
                Text(
                    text = error.message,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showSettingsButton) {
                    TextButton(onClick = onOpenSettings) {
                        Text(stringResource(Res.string.brigette_open_settings))
                    }
                    Spacer(Modifier.width(8.dp))
                }
                OutlinedButton(onClick = onRetry) {
                    Text(retryLabel ?: stringResource(Res.string.brigette_retry))
                }
            }
        }
    }
}

@Composable
private fun ScoredOptionCard(
    option: GeminiOption,
    sourceText: String,
    glossaryTerms: List<GlossaryTerm>,
    isSelected: Boolean,
    isRecommended: Boolean,
    previewVisible: Boolean,
    onSelect: () -> Unit,
    onTogglePreview: () -> Unit,
) {
    val tagDiag = remember(option.text, sourceText) {
        TagSensor.diagnose(sourceText, option.text)
    }
    val glossDiag = remember(option.text, sourceText, glossaryTerms) {
        if (glossaryTerms.isNotEmpty()) GlossarySensor.diagnose(sourceText, option.text, glossaryTerms) else null
    }

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            },
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = option.label.ifBlank { "Option" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (isRecommended) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                    ) {
                        Text(
                            text = stringResource(Res.string.brigette_recommended),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (option.score >= 9) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                ) {
                    Text(
                        text = "${option.score}/10",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (option.score >= 9) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = option.text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(10.dp),
                )
            }

            if (option.rationale.isNotBlank()) {
                Text(
                    text = option.rationale,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SensorPill(
                    text = if (tagDiag.hasIssue) {
                        stringResource(Res.string.brigette_sensor_tags_warn)
                    } else {
                        stringResource(Res.string.brigette_sensor_tags_ok)
                    },
                    isWarning = tagDiag.hasIssue,
                )

                if (glossDiag != null) {
                    SensorPill(
                        text = if (glossDiag.hasConflict) {
                            stringResource(Res.string.brigette_sensor_glossary_warn, glossDiag.conflicts.size)
                        } else {
                            stringResource(Res.string.brigette_sensor_glossary_ok)
                        },
                        isWarning = glossDiag.hasConflict,
                    )
                }

                val sourceNewlines = tagDiag.sourceTags[TagSensor.NEWLINE_TOKEN] ?: 0
                val targetNewlines = tagDiag.targetTags[TagSensor.NEWLINE_TOKEN] ?: 0
                if (sourceNewlines > 0 || targetNewlines > 0) {
                    val newlineMismatch = sourceNewlines != targetNewlines
                    SensorPill(
                        text = if (newlineMismatch) {
                            stringResource(Res.string.brigette_sensor_newlines_warn, sourceNewlines, targetNewlines)
                        } else {
                            stringResource(Res.string.brigette_sensor_newlines_ok, targetNewlines)
                        },
                        isWarning = newlineMismatch,
                    )
                }

                Spacer(Modifier.weight(1f))

                TextButton(
                    onClick = onTogglePreview,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Icon(
                        painter = painterResource(
                            if (previewVisible) Res.drawable.ic_visibility_off else Res.drawable.ic_visibility
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(
                            if (previewVisible) Res.string.brigette_preview_hide else Res.string.brigette_preview_show
                        ),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            if (previewVisible) {
                NisbetPeekCard(text = option.text)
            }
        }
    }
}

@Composable
private fun SessionStatsFooterText(stats: GeminiSessionStats) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = {
            Surface(
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                tonalElevation = 4.dp,
                shape = MaterialTheme.shapes.extraSmall,
            ) {
                val totalSeconds = String.format(Locale.US, "%.1f", stats.totalElapsedMs / 1000.0)
                Text(
                    text = stringResource(
                        Res.string.bottom_bar_ai_stats_detail,
                        stats.requestCount,
                        stats.totalPromptTokens,
                        stats.totalCandidateTokens,
                        stats.totalThoughtsTokens,
                        stats.totalTokens,
                        totalSeconds,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        },
        state = rememberTooltipState(),
    ) {
        Text(
            text = stringResource(Res.string.bottom_bar_ai_stats, stats.requestCount, stats.totalTokens),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = if (stats.requestCount > 0) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun SensorPill(
    text: String,
    isWarning: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isWarning) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f)
        } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (isWarning) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            },
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
        )
    }
}

@Preview
@Composable
private fun BrigetteDeskReadyPreview() {
    val terms = listOf(
        GlossaryTerm("ELEMENTS", "Oxygen", "氧氣", "STRINGS.ELEMENTS.OXYGEN.NAME"),
    )
    val promptSample = """
        ## Target Entry
        ```po
        #. STRINGS.BUILDINGS.PREFABS.GASCONDUIT.DESC
        msgctxt "STRINGS.BUILDINGS.PREFABS.GASCONDUIT.DESC"
        msgid "Carries <link=\"OXYGEN\">Oxygen</link>."
        msgstr ""
        ```

        ## Locked Glossary Terms
        - Oxygen => 氧氣 (STRINGS.ELEMENTS.OXYGEN.NAME)
    """.trimIndent()
    val deskState = BrigetteDeskState(
        visible = true,
        entryKey = "STRINGS.BUILDINGS.PREFABS.GASCONDUIT.DESC",
        contextSnapshot = PromptContextSnapshot(matchedTerms = terms),
        builtPromptPreview = promptSample,
        editablePrompt = promptSample,
    )

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        arrayOf(false, true).forEach { dark ->
            OniTranslatorTheme(darkTheme = dark) {
                Surface(modifier = Modifier.width(760.dp).height(540.dp)) {
                    BrigetteDeskDrawer(
                        state = deskState,
                        sourceText = "Carries <link=\"OXYGEN\">Oxygen</link>.",
                        glossaryTerms = terms,
                        configuredModel = "gemini-3.6-flash",
                        sessionStats = GeminiSessionStats(
                            requestCount = 3,
                            totalPromptTokens = 1620,
                            totalCandidateTokens = 640,
                            totalThoughtsTokens = 150,
                            totalTokens = 2410,
                            totalElapsedMs = 6400L,
                        ),
                        onEvent = {},
                        onToggleContextOptions = {},
                        onSendRequest = { _, _ -> },
                        onApplyTranslation = {},
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun BrigetteDeskSuccessPreview() {
    val terms = listOf(
        GlossaryTerm("ELEMENTS", "Oxygen", "氧氣", "STRINGS.ELEMENTS.OXYGEN.NAME"),
        GlossaryTerm("BUILDINGS.PREFABS", "Gas Pipe", "氣體管道", "STRINGS.BUILDINGS.PREFABS.GASCONDUIT.NAME"),
    )
    val source = "Carries <link=\"OXYGEN\">Oxygen</link> between <style=\"KKeyword\">Gas Pipe</style> vents."
    val deskState = BrigetteDeskState(
        visible = true,
        entryKey = "STRINGS.BUILDINGS.PREFABS.GASCONDUIT.DESC",
        contextSnapshot = PromptContextSnapshot(
            matchedTerms = terms,
            siblings = listOf(
                SiblingContext(
                    key = "STRINGS.BUILDINGS.PREFABS.GASCONDUIT.NAME",
                    suffix = "NAME",
                    sourceText = "Gas Pipe",
                    currentTranslation = "氣體管道",
                )
            ),
        ),
        builtPromptPreview = "## Target Entry\nmsgid \"$source\"",
        editablePrompt = "## Target Entry\nmsgid \"$source\"",
        rounds = listOf(
            BrigetteRound(
                userPrompt = "## Target Entry\nmsgid \"$source\"",
                isFollowUp = false,
                result = GeminiTranslationResult(
                    payload = GeminiPayload(
                        matchedTerms = listOf("Oxygen => 氧氣", "Gas Pipe => 氣體管道"),
                        analysis = "建築物功能說明，需保留 <link> 與 <style> 標籤並鎖定既有術語。",
                        options = listOf(
                            GeminiOption(
                                label = "選項 A（鎖定術語）",
                                score = 10,
                                text = "在<style=\"KKeyword\">氣體管道</style>通風口之間輸送<link=\"OXYGEN\">氧氣</link>。",
                                rationale = "術語與遊戲內標籤完全對齊，句型簡潔自然。",
                            ),
                            GeminiOption(
                                label = "選項 B（口語流暢）",
                                score = 8,
                                text = "負責將<link=\"OXYGEN\">氧氣</link>輸送至各個<style=\"KKeyword\">氣體管道</style>通風口。",
                                rationale = "較為口語，但略偏離原文結構。",
                            ),
                        ),
                        recommendedIndex = 0,
                        conclusion = "建議採用選項 A，術語鎖定與標籤對稱皆已達標。",
                    ),
                    usage = GeminiUsage(
                        promptTokens = 540,
                        candidateTokens = 210,
                        thoughtsTokens = 64,
                        totalTokens = 814,
                        elapsedMs = 2150L,
                        modelName = "gemini-3.6-flash",
                    ),
                    rawJson = "{}",
                ),
            )
        ),
        selectedRoundIndex = 0,
        selectedOptionIndex = 0,
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        arrayOf(false, true).forEach { dark ->
            OniTranslatorTheme(darkTheme = dark) {
                Surface(modifier = Modifier.width(760.dp).height(720.dp)) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        BrigetteDeskDrawer(
                            state = deskState,
                            sourceText = source,
                            glossaryTerms = terms,
                            configuredModel = "gemini-3.6-flash",
                            onEvent = {},
                            onToggleContextOptions = {},
                            onSendRequest = { _, _ -> },
                            onApplyTranslation = {},
                            initialPreviewIndices = setOf(0),
                            modifier = Modifier.align(Alignment.CenterEnd),
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun BrigetteDeskLoadingAndErrorPreview() {
    val loadingState = BrigetteDeskState(
        visible = true,
        entryKey = "STRINGS.ELEMENTS.OXYGEN.DESC",
        isLoading = true,
        startedAtMs = System.currentTimeMillis() - 1500L,
    )
    val errorState = BrigetteDeskState(
        visible = true,
        entryKey = "STRINGS.ELEMENTS.OXYGEN.DESC",
        error = GeminiError.RateLimited(
            message = "RESOURCE_EXHAUSTED: Quota exceeded for GenerateRequestsPerMinute",
            retryDelaySeconds = 18,
            quotaId = "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
        ),
    )

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OniTranslatorTheme(darkTheme = false) {
            Surface(modifier = Modifier.width(760.dp).height(480.dp)) {
                BrigetteDeskDrawer(
                    state = loadingState,
                    sourceText = "Oxygen",
                    glossaryTerms = emptyList(),
                    configuredModel = "gemini-3.6-flash",
                    onEvent = {},
                    onToggleContextOptions = {},
                    onSendRequest = { _, _ -> },
                    onApplyTranslation = {},
                )
            }
        }
        OniTranslatorTheme(darkTheme = true) {
            Surface(modifier = Modifier.width(760.dp).height(480.dp)) {
                BrigetteDeskDrawer(
                    state = errorState,
                    sourceText = "Oxygen",
                    glossaryTerms = emptyList(),
                    configuredModel = "gemini-3.6-flash",
                    onEvent = {},
                    onToggleContextOptions = {},
                    onSendRequest = { _, _ -> },
                    onApplyTranslation = {},
                )
            }
        }
    }
}

private fun sampleMultiRoundFixtures(): Pair<List<GlossaryTerm>, List<BrigetteRound>> {
    val terms = listOf(
        GlossaryTerm("ELEMENTS", "Oxygen", "氧氣", "STRINGS.ELEMENTS.OXYGEN.NAME"),
        GlossaryTerm("BUILDINGS.PREFABS", "Gas Pipe", "氣體管道", "STRINGS.BUILDINGS.PREFABS.GASCONDUIT.NAME"),
    )
    val round1 = BrigetteRound(
        userPrompt = "## Target Entry\nmsgid \"Carries <link=\\\"OXYGEN\\\">Oxygen</link> cleanly.\"",
        isFollowUp = false,
        result = GeminiTranslationResult(
            payload = GeminiPayload(
                analysis = "建築物功能說明，需保留 <link> 標籤與鎖定術語。",
                options = listOf(
                    GeminiOption(
                        label = "選項 A（標準）",
                        score = 9,
                        text = "穩定輸送<link=\"OXYGEN\">氧氣</link>。",
                        rationale = "精準簡潔，完整保留連結標籤。",
                    ),
                    GeminiOption(
                        label = "選項 B（口語）",
                        score = 8,
                        text = "用來乾淨輸送<link=\"OXYGEN\">氧氣</link>的管線。",
                        rationale = "語氣較自然，略帶「管線」補充。",
                    ),
                ),
                recommendedIndex = 0,
                conclusion = "建議採用選項 A，術語與標籤皆對齊。",
            ),
            usage = GeminiUsage(520, 180, 40, 740, 1800L, "gemini-3.6-flash"),
            rawJson = "{}",
        ),
    )
    val round2 = BrigetteRound(
        userPrompt = "選項 B 的「乾淨」好像有點怪，有沒有更適合 UI 的講法？",
        isFollowUp = true,
        result = GeminiTranslationResult(
            payload = GeminiPayload(
                analysis = "「cleanly」在建築說明裡多指順暢無阻，避免翻成表面字義的「乾淨」。",
                options = listOf(
                    GeminiOption(
                        label = "選項 A2（順暢）",
                        score = 10,
                        text = "順暢輸送<link=\"OXYGEN\">氧氣</link>。",
                        rationale = "保留動作感又不失 UI 簡潔度。",
                    ),
                    GeminiOption(
                        label = "選項 B2（緊湊）",
                        score = 9,
                        text = "純淨輸送<link=\"OXYGEN\">氧氣</link>。",
                        rationale = "貼近原句重心，字數精簡。",
                    ),
                ),
                recommendedIndex = 0,
                conclusion = "建議改用「順暢輸送」作為第 2 輪定案。",
            ),
            usage = GeminiUsage(920, 195, 55, 1170, 2100L, "gemini-3.6-flash"),
            rawJson = "{}",
        ),
    )
    return terms to listOf(round1, round2)
}

@Preview
@Composable
private fun BrigetteDeskFollowUpConversationPreview() {
    val (terms, rounds) = sampleMultiRoundFixtures()
    val deskState = BrigetteDeskState(
        visible = true,
        entryKey = "STRINGS.BUILDINGS.PREFABS.GASCONDUIT.DESC",
        contextSnapshot = PromptContextSnapshot(matchedTerms = terms),
        builtPromptPreview = rounds.first().userPrompt,
        editablePrompt = rounds.first().userPrompt,
        rounds = rounds,
        selectedRoundIndex = 1,
        selectedOptionIndex = 0,
    )

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        arrayOf(false, true).forEach { dark ->
            OniTranslatorTheme(darkTheme = dark) {
                Surface(modifier = Modifier.width(760.dp).height(760.dp)) {
                    BrigetteDeskDrawer(
                        state = deskState,
                        sourceText = "Carries <link=\"OXYGEN\">Oxygen</link> cleanly.",
                        glossaryTerms = terms,
                        configuredModel = "gemini-3.6-flash",
                        sessionStats = GeminiSessionStats(2, 1440, 375, 95, 1910, 3900L),
                        initialFollowUpDraft = "如果想改成四字詞綴會不會更好？",
                        onEvent = {},
                        onToggleContextOptions = {},
                        onSendRequest = { _, _ -> },
                        onApplyTranslation = {},
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun BrigetteDeskFollowUpExpandedHistoryPreview() {
    val (terms, rounds) = sampleMultiRoundFixtures()
    val deskState = BrigetteDeskState(
        visible = true,
        entryKey = "STRINGS.BUILDINGS.PREFABS.GASCONDUIT.DESC",
        contextSnapshot = PromptContextSnapshot(matchedTerms = terms),
        builtPromptPreview = rounds.first().userPrompt,
        editablePrompt = rounds.first().userPrompt,
        rounds = rounds,
        selectedRoundIndex = 0,
        selectedOptionIndex = 1,
    )

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        arrayOf(false, true).forEach { dark ->
            OniTranslatorTheme(darkTheme = dark) {
                Surface(modifier = Modifier.width(760.dp).height(820.dp)) {
                    BrigetteDeskDrawer(
                        state = deskState,
                        sourceText = "Carries <link=\"OXYGEN\">Oxygen</link> cleanly.",
                        glossaryTerms = terms,
                        configuredModel = "gemini-3.6-flash",
                        initialExpandedHistoryRounds = setOf(0),
                        onEvent = {},
                        onToggleContextOptions = {},
                        onSendRequest = { _, _ -> },
                        onApplyTranslation = {},
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun BrigetteDeskFollowUpLoadingAndErrorPreview() {
    val (terms, rounds) = sampleMultiRoundFixtures()
    val round1Only = listOf(rounds.first())
    val loadingState = BrigetteDeskState(
        visible = true,
        entryKey = "STRINGS.BUILDINGS.PREFABS.GASCONDUIT.DESC",
        contextSnapshot = PromptContextSnapshot(matchedTerms = terms),
        builtPromptPreview = round1Only.first().userPrompt,
        editablePrompt = round1Only.first().userPrompt,
        rounds = round1Only,
        selectedRoundIndex = 0,
        selectedOptionIndex = 0,
        isLoading = true,
        startedAtMs = System.currentTimeMillis() - 1800L,
        pendingFollowUp = "請幫我把語氣調得更簡短精煉一點",
    )
    val errorState = BrigetteDeskState(
        visible = true,
        entryKey = "STRINGS.BUILDINGS.PREFABS.GASCONDUIT.DESC",
        contextSnapshot = PromptContextSnapshot(matchedTerms = terms),
        builtPromptPreview = round1Only.first().userPrompt,
        editablePrompt = round1Only.first().userPrompt,
        rounds = round1Only,
        selectedRoundIndex = 0,
        selectedOptionIndex = 0,
        pendingFollowUp = "請幫我把語氣調得更簡短精煉一點",
        error = GeminiError.RateLimited(
            message = "RESOURCE_EXHAUSTED: Quota exceeded for GenerateRequestsPerMinute",
            retryDelaySeconds = 12,
            quotaId = "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
        ),
    )

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OniTranslatorTheme(darkTheme = false) {
            Surface(modifier = Modifier.width(760.dp).height(760.dp)) {
                BrigetteDeskDrawer(
                    state = loadingState,
                    sourceText = "Carries <link=\"OXYGEN\">Oxygen</link> cleanly.",
                    glossaryTerms = terms,
                    configuredModel = "gemini-3.6-flash",
                    onEvent = {},
                    onToggleContextOptions = {},
                    onSendRequest = { _, _ -> },
                    onApplyTranslation = {},
                )
            }
        }
        OniTranslatorTheme(darkTheme = true) {
            Surface(modifier = Modifier.width(760.dp).height(760.dp)) {
                BrigetteDeskDrawer(
                    state = errorState,
                    sourceText = "Carries <link=\"OXYGEN\">Oxygen</link> cleanly.",
                    glossaryTerms = terms,
                    configuredModel = "gemini-3.6-flash",
                    onEvent = {},
                    onToggleContextOptions = {},
                    onSendRequest = { _, _ -> },
                    onApplyTranslation = {},
                )
            }
        }
    }
}
