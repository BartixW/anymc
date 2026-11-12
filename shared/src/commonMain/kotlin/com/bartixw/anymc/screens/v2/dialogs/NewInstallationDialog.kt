/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.v2.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.common_back
import anymc.shared.generated.resources.common_danger
import anymc.shared.generated.resources.common_error
import anymc.shared.generated.resources.common_next
import anymc.shared.generated.resources.common_retry
import anymc.shared.generated.resources.ic_download
import anymc.shared.generated.resources.ic_reload
import anymc.shared.generated.resources.nbt_cancel
import anymc.shared.generated.resources.versions_create_install_button
import anymc.shared.generated.resources.versions_create_loader_version_label
import anymc.shared.generated.resources.versions_create_mc_version_label
import anymc.shared.generated.resources.versions_create_modpack
import anymc.shared.generated.resources.versions_create_name_label
import anymc.shared.generated.resources.versions_create_name_placeholder
import anymc.shared.generated.resources.versions_create_no_loaders
import anymc.shared.generated.resources.versions_create_show_snapshots
import anymc.shared.generated.resources.versions_create_type_title
import anymc.shared.generated.resources.versions_import_danger
import anymc.shared.generated.resources.versions_import_drag_drop
import anymc.shared.generated.resources.versions_import_select_file
import anymc.shared.generated.resources.versions_import_type
import anymc.shared.generated.resources.versions_new_installation
import com.bartixw.anymc.managers.ConfigManager
import com.bartixw.anymc.managers.MinecraftManager
import com.bartixw.anymc.minecraft.data.FabricLoaderVersion
import com.bartixw.anymc.minecraft.data.InstallationType
import com.bartixw.anymc.minecraft.data.Version
import com.bartixw.anymc.minecraft.data.VersionManifest
import com.bartixw.anymc.screens.versions.VersionsViewModel
import com.bartixw.anymc.screens.versions.create.VersionsCreateChooseVersionViewModel
import com.bartixw.anymc.ui.components.McButton
import com.bartixw.anymc.ui.components.McCheckbox
import com.bartixw.anymc.ui.components.McDropdown
import com.bartixw.anymc.ui.components.McLoadingIndicator
import com.bartixw.anymc.ui.components.McText
import com.bartixw.anymc.ui.components.McTextField
import com.bartixw.anymc.ui.components.v2.Button
import com.bartixw.anymc.ui.components.v2.ButtonType
import com.bartixw.anymc.ui.components.v2.Checkbox
import com.bartixw.anymc.ui.components.v2.Dialog
import com.bartixw.anymc.ui.components.v2.Dropdown
import com.bartixw.anymc.ui.components.v2.Input
import com.bartixw.anymc.ui.components.v2.LoadingIndicator
import com.bartixw.anymc.ui.components.v2.Text
import com.bartixw.anymc.ui.theme.MinecraftTheme
import com.bartixw.anymc.utils.onModExternalDrag
import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import java.io.File

class VersionsCreateChooseVersionViewModel(
    val type: InstallationType
) : ViewModel() {

    sealed interface UiState {
        object Loading : UiState
        data class Loaded(
            val manifest: VersionManifest,
            val filteredVersions: List<Version>,
            val showSnapshots: Boolean,
            val fabricLoaders: List<FabricLoaderVersion> = emptyList(),
            val isFabricLoading: Boolean = false
        ) : UiState
        data class Error(val message: String?) : UiState
    }

    var uiState by mutableStateOf<UiState>(UiState.Loading)
        private set

    init {
        getVersionsManifest()
    }

    fun getVersionsManifest() {
        if (type == InstallationType.IMPORT) {
            uiState = UiState.Loaded(
                manifest = VersionManifest(latest = com.bartixw.anymc.minecraft.data.LatestVersion("", ""), versions = emptyList()),
                filteredVersions = emptyList(),
                showSnapshots = false
            )
            return
        }
        uiState = UiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fetchedManifest = MinecraftManager.getVersionManifest()
                val showSnapshots = ConfigManager.data.value.showSnapshotsInList

                val supportedVersions = when (type) {
                    InstallationType.FABRICMC -> MinecraftManager.fabricMetadataService.getSupportedMinecraftVersions()
                    InstallationType.LEGACYFABRIC -> MinecraftManager.legacyFabricMetadataService.getSupportedMinecraftVersions()
                    InstallationType.FORGE -> MinecraftManager.forgeMetadataService.getSupportedMinecraftVersions()
                    InstallationType.NEOFORGE -> MinecraftManager.neoforgeMetadataService.getSupportedMinecraftVersions()
                    else -> null
                }

                val filtered = fetchedManifest.versions.filter { version ->
                    val isSupported = supportedVersions == null || version.id in supportedVersions
                    val isSnapshotMatch = showSnapshots || version.type == "release"
                    isSupported && isSnapshotMatch
                }

                uiState = UiState.Loaded(
                    manifest = fetchedManifest,
                    filteredVersions = filtered,
                    showSnapshots = showSnapshots
                )

                if ((type == InstallationType.FABRICMC || type == InstallationType.LEGACYFABRIC || type == InstallationType.FORGE || type == InstallationType.NEOFORGE) && filtered.isNotEmpty()) {
                    getFabricLoadersForVersion(filtered.first().id)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                uiState = UiState.Error(e.toString())
            }
        }
    }

    fun toggleSnapshots() {
        val currentState = uiState as? UiState.Loaded ?: return
        val newShowSnapshots = !currentState.showSnapshots

        ConfigManager.update { it.copy(showSnapshotsInList = newShowSnapshots) }

        viewModelScope.launch(Dispatchers.Default) {
            val supportedVersions = when (type) {
                InstallationType.FABRICMC -> MinecraftManager.fabricMetadataService.getSupportedMinecraftVersions()
                InstallationType.LEGACYFABRIC -> MinecraftManager.legacyFabricMetadataService.getSupportedMinecraftVersions()
                InstallationType.FORGE -> MinecraftManager.forgeMetadataService.getSupportedMinecraftVersions()
                InstallationType.NEOFORGE -> MinecraftManager.neoforgeMetadataService.getSupportedMinecraftVersions()
                else -> null
            }

            val filtered = currentState.manifest.versions.filter { version ->
                val isSupported = supportedVersions == null || version.id in supportedVersions
                val isSnapshotMatch = newShowSnapshots || version.type == "release"
                isSupported && isSnapshotMatch
            }

            uiState = currentState.copy(
                showSnapshots = newShowSnapshots,
                filteredVersions = filtered
            )
        }
    }

    fun getFabricLoadersForVersion(mcVersion: String) {
        val currentState = uiState as? UiState.Loaded ?: return

        uiState = currentState.copy(isFabricLoading = true)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = when (type) {
                    InstallationType.FORGE -> {
                        MinecraftManager.forgeMetadataService.getVersionsForGame(mcVersion).map {
                            FabricLoaderVersion(version = it.version, stable = it.type == "recommended", maven = "", build = 0)
                        }
                    }
                    InstallationType.NEOFORGE -> {
                        MinecraftManager.neoforgeMetadataService.getVersionsForGame(mcVersion).map {
                            FabricLoaderVersion(version = it.version, stable = it.type == "recommended", maven = "", build = 0)
                        }
                    }
                    InstallationType.LEGACYFABRIC -> {
                        MinecraftManager.legacyFabricMetadataService.getLoaderVersionsForGame(mcVersion)
                    }
                    else -> {
                        MinecraftManager.fabricMetadataService.getLoaderVersionsForGame(mcVersion)
                    }
                }

                uiState = currentState.copy(
                    fabricLoaders = response,
                    isFabricLoading = false
                )
            } catch (e: Exception) {
                e.printStackTrace()
                uiState = currentState.copy(
                    fabricLoaders = emptyList(),
                    isFabricLoading = false
                )
            }
        }
    }

    fun installVanilla(url: Url, name: String) {
        MinecraftManager.startBackgroundVanillaInstall(url, name)
    }

    fun installFabric(mcVersionUrl: Url, loaderVersion: String, name: String, isLegacy: Boolean = false) {
        MinecraftManager.startBackgroundFabricInstall(mcVersionUrl, loaderVersion, name, isLegacy)
    }
}

@Composable
fun NewInstallationDialog(
    onDismissRequest: () -> Unit,
    navController: NavController
) {
    var secondStage by remember { mutableStateOf(false) }
    var secondStageError by remember { mutableStateOf(false) }
    var installationType by remember { mutableStateOf(InstallationType.VANILLA) }
    var selectedOption by remember { mutableStateOf("") }
    var selectedLoader by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var importZipFile by remember { mutableStateOf<File?>(null) }
    
    val versionsViewModel: VersionsViewModel = viewModel()
    val scope = rememberCoroutineScope()

    val viewModel: VersionsCreateChooseVersionViewModel = viewModel(key = installationType.toString()) {
        VersionsCreateChooseVersionViewModel(type = installationType)
    }

    val placeholder = stringResource(Res.string.versions_create_name_placeholder)
    val modpackLabel = stringResource(Res.string.versions_create_modpack)
    val importLabel = stringResource(Res.string.versions_import_type)

    val installationTypes = remember(modpackLabel, importLabel) {
        mapOf(
            InstallationType.VANILLA to "Vanilla",
            InstallationType.FABRICMC to "Fabric",
            InstallationType.FORGE to "Forge",
            InstallationType.LEGACYFABRIC to "Legacy Fabric",
            InstallationType.MODRINTH_MODPACK to modpackLabel,
            InstallationType.IMPORT to importLabel
        )
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(Res.string.versions_new_installation)) },
        buttons = {
            if (secondStage) {
                Button(
                    content = { Text(stringResource(Res.string.common_back)) },
                    onClick = { secondStage = false }
                )
                if (secondStageError) Button(
                    content = { Text(stringResource(Res.string.common_retry)) },
                    type = ButtonType.Primary,
                    onClick = { viewModel.getVersionsManifest() }
                ) else Button(
                    content = { Text(stringResource(Res.string.versions_create_install_button)) },
                    disabled = installationType == InstallationType.IMPORT && importZipFile == null,
                    onClick = {
                        if (installationType == InstallationType.IMPORT) {
                            importZipFile?.let { file ->
                                versionsViewModel.importInstallation(
                                    file = file,
                                    newName = name.ifEmpty { null },
                                    onComplete = { success ->
                                        if (success) onDismissRequest()
                                    }
                                )
                            }
                            return@Button
                        }

                        when (val state = viewModel.uiState) {
                            is VersionsCreateChooseVersionViewModel.UiState.Loaded -> {
                                val finalName = name.ifEmpty { placeholder }
                                val selectedMcVersion = state.manifest.versions.find { it.id == selectedOption }!!

                                when (installationType) {
                                    InstallationType.FABRICMC, InstallationType.LEGACYFABRIC -> {
                                        viewModel.installFabric(
                                            mcVersionUrl = Url(selectedMcVersion.url),
                                            loaderVersion = selectedLoader,
                                            name = finalName,
                                            isLegacy = installationType == InstallationType.LEGACYFABRIC
                                        )
                                    }
                                    InstallationType.FORGE, InstallationType.NEOFORGE -> {
                                        MinecraftManager.startBackgroundMultiMCInstall(
                                            vUrl = Url(selectedMcVersion.url),
                                            lt = if (installationType == InstallationType.FORGE) "forge" else "neoforge",
                                            lv = selectedLoader,
                                            n = finalName
                                        )
                                    }
                                    else -> {
                                        viewModel.installVanilla(
                                            url = Url(selectedMcVersion.url),
                                            name = finalName
                                        )
                                    }
                                }

                                onDismissRequest()
                            }
                            else -> {}
                        }
                    },
                    type = ButtonType.Primary
                )
            } else {
                Button(
                    content = { Text(stringResource(Res.string.nbt_cancel)) },
                    onClick = onDismissRequest
                )
                Button(
                    content = { Text(stringResource(Res.string.common_next)) },
                    onClick = {
                        if (installationType == InstallationType.MODRINTH_MODPACK)
                            navController.navigate("versions/create/modpack")
                        else secondStage = true
                    },
                    type = ButtonType.Primary
                )
            }
        }
    ) {
        if (secondStage) {
            if (installationType == InstallationType.IMPORT) {
                Column(
                    modifier = Modifier
                        .border(2.dp, Color.Red)
                        .background(Color.Red.copy(alpha = 0.2f))
                        .padding(8.dp)
                ) {
                    Text(stringResource(Res.string.common_danger), style = TextStyle(fontWeight = FontWeight.Bold))
                    Spacer(Modifier.size(4.dp))
                    Text(stringResource(Res.string.versions_import_danger))
                }

                Spacer(Modifier.size(16.dp))

                Text(stringResource(Res.string.versions_create_name_label))
                Input(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.size(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .border(1.dp, MinecraftTheme.colors.dialogBorder, RoundedCornerShape(4.dp))
                        .background(MinecraftTheme.colors.dialogBackground.copy(alpha = 0.5f))
                        .onModExternalDrag(
                            onDragStart = {},
                            onDragExit = {},
                            onDrop = { uris ->
                                val uri = uris.firstOrNull()
                                if (uri != null) {
                                    try {
                                        importZipFile = if (uri.startsWith("file:")) {
                                            File(java.net.URI(uri))
                                        } else {
                                            File(uri)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (importZipFile != null) {
                            Text(importZipFile!!.name)
                            Spacer(Modifier.height(8.dp))
                            Button(
                                content = { Text(stringResource(Res.string.versions_import_select_file)) },
                                onClick = {
                                    scope.launch {
                                        val file = FileKit.pickFile(
                                            type = PickerType.File(extensions = listOf("zip")),
                                            mode = PickerMode.Single
                                        )
                                        file?.path?.let { importZipFile = File(it) }
                                    }
                                }
                            )
                        } else {
                            Text(stringResource(Res.string.versions_import_drag_drop))
                            Spacer(Modifier.height(8.dp))
                            Text("or")
                            Spacer(Modifier.height(8.dp))
                            Button(
                                content = { Text(stringResource(Res.string.versions_import_select_file)) },
                                onClick = {
                                    scope.launch {
                                        val file = FileKit.pickFile(
                                            type = PickerType.File(extensions = listOf("zip")),
                                            mode = PickerMode.Single
                                        )
                                        file?.path?.let { importZipFile = File(it) }
                                    }
                                }
                            )
                        }
                    }
                }
            } else when (val state = viewModel.uiState) {
                is VersionsCreateChooseVersionViewModel.UiState.Loading -> {
                    LoadingIndicator()
                }
                is VersionsCreateChooseVersionViewModel.UiState.Loaded -> {
                    LaunchedEffect(state.filteredVersions) {
                        if (selectedOption.isEmpty() || state.filteredVersions.none { it.id == selectedOption }) {
                            selectedOption = state.filteredVersions.firstOrNull()?.id ?: ""
                        }
                    }

                    LaunchedEffect(selectedOption) {
                        if (selectedOption.isNotEmpty() && (installationType == InstallationType.FABRICMC || installationType == InstallationType.LEGACYFABRIC || installationType == InstallationType.FORGE || installationType == InstallationType.NEOFORGE)) {
                            viewModel.getFabricLoadersForVersion(selectedOption)
                        }
                    }

                    LaunchedEffect(state.fabricLoaders) {
                        selectedLoader = if (state.fabricLoaders.isNotEmpty()) {
                            state.fabricLoaders.firstOrNull { it.stable }?.version
                                ?: state.fabricLoaders.first().version
                        } else {
                            ""
                        }
                    }

                    Text(stringResource(Res.string.versions_create_name_label))
                    Input(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.size(16.dp))

                    Text(stringResource(Res.string.versions_create_mc_version_label))
                    Dropdown(
                        options = state.filteredVersions.map { it.id },
                        selectedOption = selectedOption,
                        onOptionSelected = { selectedOption = it },
                        modifier = Modifier.widthIn(128.dp)
                    )

                    Spacer(Modifier.size(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Checkbox(
                            checked = state.showSnapshots,
                            onCheckedChange = { viewModel.toggleSnapshots() }
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(Res.string.versions_create_show_snapshots))
                    }

                    when (installationType) {
                        InstallationType.FABRICMC, InstallationType.LEGACYFABRIC, InstallationType.FORGE, InstallationType.NEOFORGE -> {
                            val loaderTitle = when (installationType) {
                                InstallationType.LEGACYFABRIC -> "Legacy Fabric Loader"
                                InstallationType.FORGE -> "Forge"
                                InstallationType.NEOFORGE -> "NeoForge"
                                else -> "Fabric Loader"
                            }
                            Spacer(Modifier.size(16.dp))
                            Text(stringResource(Res.string.versions_create_loader_version_label, loaderTitle))
                            if (state.isFabricLoading) {
                                LoadingIndicator()
                            } else if (state.fabricLoaders.isNotEmpty()) {
                                Dropdown(
                                    options = state.fabricLoaders.map { it.version },
                                    selectedOption = selectedLoader,
                                    onOptionSelected = { selectedLoader = it }
                                )
                            } else {
                                Text(stringResource(Res.string.versions_create_no_loaders))
                            }
                        }
                        else -> {}
                    }
                }
                is VersionsCreateChooseVersionViewModel.UiState.Error -> {
                    McText(stringResource(Res.string.common_error, state.message ?: ""))

                    DisposableEffect(Unit) {
                        secondStageError = true

                        onDispose {
                            secondStageError = false
                        }
                    }
                }
            }
        } else {
            Text(stringResource(Res.string.versions_create_type_title))
            Dropdown(
                options = installationTypes.keys.toList(),
                selectedOption = installationType,
                onOptionSelected = { installationType = it },
                labelProvider = { installationTypes[it]!! }
            )
        }
    }
}