@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package dolphin.desktop.apps.onitranslator.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_send_request
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_sensor_glossary_ok
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_sensor_glossary_warn
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_sensor_tags_ok
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_sensor_tags_warn
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_show_prompt
import dolphin.desktop.apps.onitranslator.generated.resources.button_cancel
import dolphin.desktop.apps.onitranslator.generated.resources.button_close
import dolphin.desktop.apps.onitranslator.generated.resources.ic_auto_awesome
import dolphin.desktop.apps.onitranslator.generated.resources.ic_close
import dolphin.desktop.apps.onitranslator.generated.resources.ic_done
import dolphin.desktop.apps.onitranslator.generated.resources.ic_report
import dolphin.desktop.apps.onitranslator.generated.resources.ic_visibility
import dolphin.desktop.apps.onitranslator.generated.resources.ic_visibility_off
import dolphin.desktop.apps.onitranslator.model.BrigetteDeskState
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
) {
    AnimatedVisibility(
        visible = state.visible,
        enter = slideInHorizontally { it } + fadeIn(),
        exit = slideOutHorizontally { it } + fadeOut(),
        modifier = modifier.fillMaxSize().padding(start = 20.dp),
    ) {
        var showGlossaryDetails by remember(state.entryKey) { mutableStateOf(false) }
        var showPromptInResult by remember(state.entryKey) { mutableStateOf(false) }
        var previewIndices by remember(state.entryKey, state.result) { mutableStateOf(initialPreviewIndices) }
        val lastSentOptions = remember(state.entryKey, state.startedAtMs, state.result) {
            state.contextOptions
        }
        val optionsDirty = state.result != null && state.contextOptions != lastSentOptions
        val activeModel = state.result?.usage?.modelName?.ifBlank { configuredModel }
            ?: configuredModel.ifBlank { "—" }

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

                // Main Content (Loading / Error / Success / Ready Editable Prompt Preview)
                if (!state.isLoading && state.error == null && state.result == null) {
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
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        when {
                            state.isLoading -> {
                                BrigetteLoadingView(
                                    startedAtMs = state.startedAtMs,
                                    onCancel = { onEvent(AppEvent.Brigette.Cancel) },
                                )
                            }

                            state.error != null -> {
                                BrigetteErrorView(
                                    error = state.error,
                                    onOpenSettings = { onEvent(AppEvent.Ui.ShowConfig) },
                                    onRetry = { onSendRequest(state.contextOptions, state.editablePrompt) },
                                )
                            }

                            state.result != null -> {
                                val payload = state.result.payload
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
                                    val isSelected = index == state.selectedOptionIndex
                                    val isRecommended = index == payload.recommendedIndex
                                    val isPreviewVisible = index in previewIndices

                                    ScoredOptionCard(
                                        option = option,
                                        sourceText = sourceText,
                                        glossaryTerms = glossaryTerms,
                                        isSelected = isSelected,
                                        isRecommended = isRecommended,
                                        previewVisible = isPreviewVisible,
                                        onSelect = { onEvent(AppEvent.Brigette.SelectOption(index)) },
                                        onTogglePreview = {
                                            previewIndices = if (isPreviewVisible) {
                                                previewIndices - index
                                            } else {
                                                previewIndices + index
                                            }
                                        },
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
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Sticky Footer: Usage Telemetry & Primary Actions
                val selectedOption = state.result?.payload?.options?.getOrNull(state.selectedOptionIndex)
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
                        if (state.result != null) {
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
                                Text(stringResource(Res.string.brigette_apply_option))
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
                    Text(stringResource(Res.string.brigette_retry))
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
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
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
