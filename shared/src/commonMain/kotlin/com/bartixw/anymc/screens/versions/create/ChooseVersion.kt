/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.versions.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import anymc.shared.generated.resources.*
import com.bartixw.anymc.minecraft.data.FabricLoaderVersion
import com.bartixw.anymc.minecraft.data.Version
import com.bartixw.anymc.minecraft.data.VersionManifest
import com.bartixw.anymc.minecraft.data.InstallationType
import com.bartixw.anymc.managers.ConfigManager
import com.bartixw.anymc.managers.MinecraftManager
import com.bartixw.anymc.ui.components.McButton
import com.bartixw.anymc.ui.components.McCheckbox
import com.bartixw.anymc.ui.components.McDropdown
import com.bartixw.anymc.ui.components.McLoadingIndicator
import com.bartixw.anymc.ui.components.McText
import com.bartixw.anymc.ui.components.McTextField
import com.bartixw.anymc.ui.components.McTopAppBar
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource

@Serializable
data class ChooseVersionRoute(
    val type: String = InstallationType.VANILLA.toString()
)

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
fun VersionsCreateChooseVersionScreen(
    navController: NavController,
    type: String,
) {
    val placeholder = stringResource(Res.string.versions_create_name_placeholder)

    val installationType = remember(type) {
        try {
            InstallationType.valueOf(type.uppercase())
        } catch (e: Exception) {
            InstallationType.VANILLA
        }
    }

    val viewModel: VersionsCreateChooseVersionViewModel = viewModel(key = type) {
        VersionsCreateChooseVersionViewModel(type = installationType)
    }

    Box {
        McTopAppBar(
            navController = navController,
            title = stringResource(Res.string.versions_create_version_title),
            actions = listOf()
        )
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            when (val state = viewModel.uiState) {
                is VersionsCreateChooseVersionViewModel.UiState.Loading -> {
                    McLoadingIndicator()
                }
                is VersionsCreateChooseVersionViewModel.UiState.Loaded -> {
                    var selectedOption by remember { mutableStateOf("") }
                    var selectedLoader by remember { mutableStateOf("") }
                    var name by remember { mutableStateOf("") }

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

                    McText(stringResource(Res.string.versions_create_name_label))
                    Spacer(Modifier.size(16.dp))
                    McTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = placeholder
                    )
                    Spacer(Modifier.size(32.dp))

                    McText(stringResource(Res.string.versions_create_mc_version_label))
                    Spacer(Modifier.size(16.dp))
                    McDropdown(
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
                        McCheckbox(
                            checked = state.showSnapshots,
                            onCheckedChange = { viewModel.toggleSnapshots() }
                        )
                        Spacer(Modifier.size(8.dp))
                        McText(stringResource(Res.string.versions_create_show_snapshots))
                    }

                    Spacer(Modifier.size(32.dp))

                    when (installationType) {
                        InstallationType.FABRICMC, InstallationType.LEGACYFABRIC, InstallationType.FORGE, InstallationType.NEOFORGE -> {
                            val loaderTitle = when (installationType) {
                                InstallationType.LEGACYFABRIC -> "Legacy Fabric Loader"
                                InstallationType.FORGE -> "Forge"
                                InstallationType.NEOFORGE -> "NeoForge"
                                else -> "Fabric Loader"
                            }
                            McText(stringResource(Res.string.versions_create_loader_version_label, loaderTitle))
                            Spacer(Modifier.size(16.dp))
                            if (state.isFabricLoading) {
                                McLoadingIndicator()
                            } else if (state.fabricLoaders.isNotEmpty()) {
                                McDropdown(
                                    options = state.fabricLoaders.map { it.version },
                                    selectedOption = selectedLoader,
                                    onOptionSelected = { selectedLoader = it }
                                )
                            } else {
                                McText(stringResource(Res.string.versions_create_no_loaders))
                            }
                            Spacer(Modifier.size(32.dp))
                        }
                        else -> {}
                    }

                    McButton(
                        text = stringResource(Res.string.versions_create_install_button),
                        icon = Res.drawable.ic_download,
                        disabled = selectedOption.isEmpty() || !((installationType != InstallationType.FABRICMC && installationType != InstallationType.LEGACYFABRIC && installationType != InstallationType.FORGE && installationType != InstallationType.NEOFORGE) || selectedLoader.isNotEmpty()),
                        onClick = {
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

                            navController.navigate("main") {
                                popUpTo(0) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    )
                }
                is VersionsCreateChooseVersionViewModel.UiState.Error -> {
                    McText(stringResource(Res.string.common_error, state.message ?: ""))
                    Spacer(Modifier.size(16.dp))
                    McButton(
                        text = stringResource(Res.string.common_retry),
                        icon = Res.drawable.ic_reload,
                        onClick = { viewModel.getVersionsManifest() }
                    )
                }
            }
        }
    }
}
