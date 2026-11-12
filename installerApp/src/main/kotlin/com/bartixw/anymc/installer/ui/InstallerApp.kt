/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.installer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.ic_close
import anymc.shared.generated.resources.ic_minimize
import anymc.shared.generated.resources.installer_browse
import anymc.shared.generated.resources.installer_browse_dialog_title
import anymc.shared.generated.resources.installer_btn_back
import anymc.shared.generated.resources.installer_btn_next
import anymc.shared.generated.resources.installer_cancel
import anymc.shared.generated.resources.installer_close
import anymc.shared.generated.resources.installer_error_title
import anymc.shared.generated.resources.installer_fetching_releases
import anymc.shared.generated.resources.installer_header_title
import anymc.shared.generated.resources.installer_install_button
import anymc.shared.generated.resources.installer_install_dir
import anymc.shared.generated.resources.installer_install_from_zip
import anymc.shared.generated.resources.installer_installing_button
import anymc.shared.generated.resources.installer_launch_app
import anymc.shared.generated.resources.installer_no_releases
import anymc.shared.generated.resources.installer_retry
import anymc.shared.generated.resources.installer_select_version
import anymc.shared.generated.resources.installer_select_zip_dialog_title
import anymc.shared.generated.resources.installer_selected_zip_file
import anymc.shared.generated.resources.installer_shortcut_desktop
import anymc.shared.generated.resources.installer_shortcut_start_menu
import anymc.shared.generated.resources.installer_status_downloading
import anymc.shared.generated.resources.installer_status_downloading_simple
import anymc.shared.generated.resources.installer_status_extracting
import anymc.shared.generated.resources.installer_status_preparing
import anymc.shared.generated.resources.installer_status_registering_system
import anymc.shared.generated.resources.installer_status_registering_uri
import anymc.shared.generated.resources.installer_step_indicator
import anymc.shared.generated.resources.installer_step_location_desc
import anymc.shared.generated.resources.installer_step_location_title
import anymc.shared.generated.resources.installer_step_options_desc
import anymc.shared.generated.resources.installer_step_options_title
import anymc.shared.generated.resources.installer_step_welcome_desc
import anymc.shared.generated.resources.installer_step_welcome_title
import anymc.shared.generated.resources.installer_success_message
import anymc.shared.generated.resources.installer_success_title
import anymc.shared.generated.resources.installer_title
import anymc.shared.generated.resources.installer_uri_scheme
import anymc.shared.generated.resources.installer_uri_scheme_name
import anymc.shared.generated.resources.installer_use_github_releases
import com.bartixw.anymc.core.utils.PlatformUtils
import com.bartixw.anymc.installer.engine.InstallationManager
import com.bartixw.anymc.installer.model.GitHubRelease
import com.bartixw.anymc.installer.model.InstallationState
import com.bartixw.anymc.installer.model.InstallerConfig
import com.bartixw.anymc.ui.components.v2.Button
import com.bartixw.anymc.ui.components.v2.ButtonType
import com.bartixw.anymc.ui.components.v2.Checkbox
import com.bartixw.anymc.ui.components.v2.Dropdown
import com.bartixw.anymc.ui.components.v2.Icon
import com.bartixw.anymc.ui.components.v2.Input
import com.bartixw.anymc.ui.components.v2.LoadingIndicator
import com.bartixw.anymc.ui.components.v2.Text
import com.bartixw.anymc.ui.theme.MinecraftTheme
import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import java.io.File

enum class InstallerWizardStep(val stepNumber: Int) {
    WELCOME(1),
    LOCATION(2),
    OPTIONS(3),
    INSTALLING(4),
    COMPLETED(5)
}

@Composable
fun InstallerApp(
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    windowDraggableArea: @Composable (Modifier, @Composable () -> Unit) -> Unit,
    apiHost: String
) {
    val installationManager = remember { InstallationManager(apiHost) }
    val installationState by installationManager.state.collectAsState()
    val scope = rememberCoroutineScope()

    var currentStep by remember { mutableStateOf(InstallerWizardStep.WELCOME) }

    var repoOwner by remember { mutableStateOf("BartixW") }
    var repoName by remember { mutableStateOf("AnyMC") }
    var releases by remember { mutableStateOf<List<GitHubRelease>>(emptyList()) }
    var selectedRelease by remember { mutableStateOf<GitHubRelease?>(null) }
    var selectedLocalZipFile by remember { mutableStateOf<File?>(null) }

    var installDirText by remember {
        mutableStateOf(PlatformUtils.userProgramsDirectory.absolutePath)
    }
    var createDesktopShortcut by remember { mutableStateOf(true) }
    var createStartMenuShortcut by remember { mutableStateOf(true) }
    var registerUriScheme by remember { mutableStateOf(true) }
    var uriSchemeText by remember { mutableStateOf("anymc") }

    LaunchedEffect(Unit) {
        val loadedReleases = installationManager.loadReleases(repoOwner, repoName)
        releases = loadedReleases
        selectedRelease = loadedReleases.firstOrNull()
    }

    LaunchedEffect(installationState) {
        when (installationState) {
            is InstallationState.Downloading, is InstallationState.Extracting,
            is InstallationState.RegisteringSystem, is InstallationState.RegisteringUriScheme -> {
                currentStep = InstallerWizardStep.INSTALLING
            }
            is InstallationState.Completed -> {
                currentStep = InstallerWizardStep.COMPLETED
            }
            else -> {}
        }
    }

    MinecraftTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MinecraftTheme.colors.dialogBackground)
        ) {
            // Title Bar
            InstallerHeader(
                title = stringResource(Res.string.installer_title),
                onMinimize = onMinimize,
                onClose = onClose,
                windowDraggableArea = windowDraggableArea
            )

            // Content Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.installer_header_title).uppercase(),
                        style = MinecraftTheme.typographyAnyMC.copy(fontSize = 28.sp)
                    )

                    if (currentStep.stepNumber in 1..3) {
                        Text(
                            text = stringResource(Res.string.installer_step_indicator, currentStep.stepNumber, 3),
                            style = MinecraftTheme.typographyPixel.copy(
                                color = MinecraftTheme.colors.textSecondary,
                                fontSize = 14.sp
                            )
                        )
                    }
                }

                when (val state = installationState) {
                    is InstallationState.Error -> {
                        ErrorSection(
                            errorMessage = state.message,
                            onRetry = {
                                scope.launch {
                                    installationManager.loadReleases(repoOwner, repoName)
                                    currentStep = InstallerWizardStep.WELCOME
                                }
                            }
                        )
                    }
                    else -> {
                        when (currentStep) {
                            InstallerWizardStep.WELCOME -> {
                                WelcomeStepContent(
                                    releases = releases,
                                    selectedRelease = selectedRelease,
                                    onReleaseSelected = { selectedRelease = it },
                                    selectedLocalZipFile = selectedLocalZipFile,
                                    onSelectLocalZipFile = { selectedLocalZipFile = it },
                                    isLoadingReleases = installationState is InstallationState.FetchingReleases
                                )
                            }
                            InstallerWizardStep.LOCATION -> {
                                LocationStepContent(
                                    installDirText = installDirText,
                                    onInstallDirChange = { installDirText = it }
                                )
                            }
                            InstallerWizardStep.OPTIONS -> {
                                OptionsStepContent(
                                    createDesktopShortcut = createDesktopShortcut,
                                    onDesktopShortcutChange = { createDesktopShortcut = it },
                                    createStartMenuShortcut = createStartMenuShortcut,
                                    onStartMenuShortcutChange = { createStartMenuShortcut = it },
                                    registerUriScheme = registerUriScheme,
                                    onRegisterUriSchemeChange = { registerUriScheme = it },
                                    uriSchemeText = uriSchemeText,
                                    onUriSchemeTextChange = { uriSchemeText = it }
                                )
                            }
                            InstallerWizardStep.INSTALLING -> {
                                ProgressSection(state = state)
                            }
                            InstallerWizardStep.COMPLETED -> {
                                if (state is InstallationState.Completed) {
                                    CompletedSection(
                                        installedPath = state.installedPath,
                                        executablePath = state.executablePath,
                                        onClose = onClose
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Footer Navigation
            if (currentStep != InstallerWizardStep.COMPLETED && installationState !is InstallationState.Error) {
                WizardFooter(
                    currentStep = currentStep,
                    isInstalling = currentStep == InstallerWizardStep.INSTALLING,
                    canProceed = (selectedLocalZipFile != null || selectedRelease != null) && (currentStep != InstallerWizardStep.LOCATION || installDirText.isNotBlank()),
                    onCancel = onClose,
                    onBack = {
                        when (currentStep) {
                            InstallerWizardStep.LOCATION -> currentStep = InstallerWizardStep.WELCOME
                            InstallerWizardStep.OPTIONS -> currentStep = InstallerWizardStep.LOCATION
                            else -> {}
                        }
                    },
                    onNext = {
                        when (currentStep) {
                            InstallerWizardStep.WELCOME -> currentStep = InstallerWizardStep.LOCATION
                            InstallerWizardStep.LOCATION -> currentStep = InstallerWizardStep.OPTIONS
                            else -> {}
                        }
                    },
                    onInstall = {
                        val config = InstallerConfig(
                            repoOwner = repoOwner,
                            repoName = repoName,
                            installDirectory = File(installDirText),
                            createDesktopShortcut = createDesktopShortcut,
                            createStartMenuShortcut = createStartMenuShortcut,
                            registerUriScheme = registerUriScheme,
                            uriScheme = uriSchemeText,
                            selectedRelease = selectedRelease,
                            localZipFile = selectedLocalZipFile
                        )
                        scope.launch {
                            installationManager.install(config)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun InstallerHeader(
    title: String,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    windowDraggableArea: @Composable (Modifier, @Composable () -> Unit) -> Unit
) {
    windowDraggableArea(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(MinecraftTheme.colors.dialogBackground)
                .border(1.dp, MinecraftTheme.colors.dialogBorder)
                .padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MinecraftTheme.typographyPixel.copy(
                    color = MinecraftTheme.colors.textPrimary,
                    fontSize = 14.sp
                )
            )

            Row {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = onMinimize),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Res.drawable.ic_minimize, contentDescription = "Minimize")
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Res.drawable.ic_close, contentDescription = "Close")
                }
            }
        }
    }
}

@Composable
private fun WelcomeStepContent(
    releases: List<GitHubRelease>,
    selectedRelease: GitHubRelease?,
    onReleaseSelected: (GitHubRelease) -> Unit,
    selectedLocalZipFile: File?,
    onSelectLocalZipFile: (File?) -> Unit,
    isLoadingReleases: Boolean
) {
    val zipDialogTitle = stringResource(Res.string.installer_select_zip_dialog_title)
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(Res.string.installer_step_welcome_title),
            style = MinecraftTheme.typographyPixel.copy(fontSize = 18.sp, color = MinecraftTheme.colors.textPrimary)
        )

        Text(
            text = stringResource(Res.string.installer_step_welcome_desc),
            style = MinecraftTheme.typographyPixel.copy(color = MinecraftTheme.colors.textSecondary)
        )

        Spacer(Modifier.height(8.dp))

        if (selectedLocalZipFile != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(Res.string.installer_selected_zip_file, selectedLocalZipFile.name),
                    style = MinecraftTheme.typographyPixel.copy(color = MinecraftTheme.colors.buttonPrimaryBackground)
                )

                Text(
                    text = stringResource(Res.string.installer_use_github_releases),
                    style = MinecraftTheme.typographyPixel.copy(
                        color = MinecraftTheme.colors.primary,
                        textDecoration = TextDecoration.Underline
                    ),
                    modifier = Modifier.clickable {
                        onSelectLocalZipFile(null)
                    }
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(Res.string.installer_select_version),
                    style = MinecraftTheme.typographyPixel
                )

                if (isLoadingReleases) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LoadingIndicator(Modifier.size(20.dp))
                        Text(
                            text = stringResource(Res.string.installer_fetching_releases),
                            style = MinecraftTheme.typographyPixel
                        )
                    }
                } else if (releases.isNotEmpty()) {
                    Dropdown(
                        options = releases,
                        selectedOption = selectedRelease ?: releases.first(),
                        onOptionSelected = onReleaseSelected,
                        labelProvider = { it.name ?: it.tagName }
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.installer_no_releases),
                        style = MinecraftTheme.typographyPixel.copy(color = MinecraftTheme.colors.buttonDisabledText)
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = stringResource(Res.string.installer_install_from_zip),
                    style = MinecraftTheme.typographyPixel.copy(
                        color = MinecraftTheme.colors.primary,
                        textDecoration = TextDecoration.Underline
                    ),
                    modifier = Modifier.clickable {
                        scope.launch {
                            val file = FileKit.pickFile(
                                type = PickerType.File(extensions = listOf("zip", "gz", "tgz")),
                                mode = PickerMode.Single,
                                title = zipDialogTitle
                            )
                            file?.path?.let { onSelectLocalZipFile(File(it)) }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun LocationStepContent(
    installDirText: String,
    onInstallDirChange: (String) -> Unit
) {
    val browseDialogTitle = stringResource(Res.string.installer_browse_dialog_title)
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(Res.string.installer_step_location_title),
            style = MinecraftTheme.typographyPixel.copy(fontSize = 18.sp, color = MinecraftTheme.colors.textPrimary)
        )

        Text(
            text = stringResource(Res.string.installer_step_location_desc),
            style = MinecraftTheme.typographyPixel.copy(color = MinecraftTheme.colors.textSecondary)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.installer_install_dir),
            style = MinecraftTheme.typographyPixel
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Input(
                value = installDirText,
                onValueChange = onInstallDirChange,
                modifier = Modifier.weight(1f)
            )

            Button(
                content = { Text(stringResource(Res.string.installer_browse)) },
                type = ButtonType.Secondary,
                onClick = {
                    scope.launch {
                        val folder = FileKit.pickDirectory(title = browseDialogTitle)
                        folder?.path?.let { onInstallDirChange(it) }
                    }
                }
            )
        }
    }
}

@Composable
private fun OptionsStepContent(
    createDesktopShortcut: Boolean,
    onDesktopShortcutChange: (Boolean) -> Unit,
    createStartMenuShortcut: Boolean,
    onStartMenuShortcutChange: (Boolean) -> Unit,
    registerUriScheme: Boolean,
    onRegisterUriSchemeChange: (Boolean) -> Unit,
    uriSchemeText: String,
    onUriSchemeTextChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(Res.string.installer_step_options_title),
            style = MinecraftTheme.typographyPixel.copy(fontSize = 18.sp, color = MinecraftTheme.colors.textPrimary)
        )

        Text(
            text = stringResource(Res.string.installer_step_options_desc),
            style = MinecraftTheme.typographyPixel.copy(color = MinecraftTheme.colors.textSecondary)
        )

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = createDesktopShortcut, onCheckedChange = onDesktopShortcutChange)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.installer_shortcut_desktop), style = MinecraftTheme.typographyPixel)
        }
    }
}

@Composable
private fun ProgressSection(state: InstallationState) {
    val statusText = when (state) {
        is InstallationState.Downloading -> {
            val mbDownloaded = state.bytesDownloaded / (1024 * 1024f)
            val mbTotal = state.totalBytes / (1024 * 1024f)
            if (state.totalBytes > 0) {
                stringResource(Res.string.installer_status_downloading, state.assetName, mbDownloaded, mbTotal)
            } else {
                stringResource(Res.string.installer_status_downloading_simple, state.assetName)
            }
        }
        is InstallationState.Extracting -> stringResource(Res.string.installer_status_extracting, state.fileName)
        is InstallationState.RegisteringSystem -> stringResource(Res.string.installer_status_registering_system)
        is InstallationState.RegisteringUriScheme -> stringResource(Res.string.installer_status_registering_uri)
        else -> stringResource(Res.string.installer_status_preparing)
    }

    val progress = when (state) {
        is InstallationState.Downloading -> state.progress
        is InstallationState.Extracting -> 0.8f
        is InstallationState.RegisteringSystem -> 0.9f
        is InstallationState.RegisteringUriScheme -> 0.95f
        else -> 0.1f
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = statusText, style = MinecraftTheme.typographyPixel)

        // Minecraft Style Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(MinecraftTheme.colors.controlDisabledBackground)
                .border(2.dp, MinecraftTheme.colors.dialogBorder)
        ) {
            Box(
                modifier = Modifier
                    .height(24.dp)
                    .fillMaxWidth(fraction = progress.coerceIn(0.05f, 1f))
                    .background(MinecraftTheme.colors.buttonPrimaryBackground)
            )
        }
    }
}

@Composable
private fun CompletedSection(
    installedPath: File,
    executablePath: File?,
    onClose: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        Text(
            text = stringResource(Res.string.installer_success_title).uppercase(),
            style = MinecraftTheme.typographyAnyMC.copy(
                fontSize = 24.sp
            )
        )

        Text(
            text = stringResource(Res.string.installer_success_message, installedPath.absolutePath),
            style = MinecraftTheme.typographyPixel
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            executablePath?.let { exe ->
                Button(
                    content = { Text(stringResource(Res.string.installer_launch_app).uppercase()) },
                    type = ButtonType.Hero,
                    onClick = {
                        try {
                            ProcessBuilder(exe.absolutePath).start()
                            onClose()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                )
            }

            Button(
                content = { Text(stringResource(Res.string.installer_close)) },
                type = ButtonType.Secondary,
                onClick = onClose
            )
        }
    }
}

@Composable
private fun ErrorSection(errorMessage: String, onRetry: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF442222))
            .border(2.dp, Color(0xFF883333))
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(Res.string.installer_error_title),
            style = MinecraftTheme.typographyPixel.copy(color = Color(0xFFFF8888))
        )
        Text(
            text = errorMessage,
            style = MinecraftTheme.typographyPixel.copy(color = Color.White)
        )

        Button(
            content = { Text(stringResource(Res.string.installer_retry)) },
            type = ButtonType.Destructive,
            onClick = onRetry
        )
    }
}

@Composable
private fun WizardFooter(
    currentStep: InstallerWizardStep,
    isInstalling: Boolean,
    canProceed: Boolean,
    onCancel: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onInstall: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MinecraftTheme.colors.dialogBackground)
            .border(1.dp, MinecraftTheme.colors.dialogBorder)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            content = { Text(stringResource(Res.string.installer_cancel)) },
            type = ButtonType.Secondary,
            disabled = isInstalling,
            onClick = onCancel
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (currentStep in listOf(InstallerWizardStep.LOCATION, InstallerWizardStep.OPTIONS) && !isInstalling) {
                Button(
                    content = { Text(stringResource(Res.string.installer_btn_back)) },
                    type = ButtonType.Secondary,
                    onClick = onBack
                )
            }

            if (currentStep in listOf(InstallerWizardStep.WELCOME, InstallerWizardStep.LOCATION)) {
                Button(
                    content = { Text(stringResource(Res.string.installer_btn_next)) },
                    type = ButtonType.Primary,
                    disabled = !canProceed,
                    onClick = onNext
                )
            } else if (currentStep == InstallerWizardStep.OPTIONS || currentStep == InstallerWizardStep.INSTALLING) {
                Button(
                    content = {
                        if (isInstalling) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LoadingIndicator(Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(Res.string.installer_installing_button).uppercase())
                            }
                        } else {
                            Text(stringResource(Res.string.installer_install_button).uppercase())
                        }
                    },
                    type = ButtonType.Hero,
                    disabled = !canProceed || isInstalling,
                    onClick = onInstall
                )
            }
        }
    }
}
