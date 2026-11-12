/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.modrinth

import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.*
import com.bartixw.anymc.managers.ModDownloadManager
import com.bartixw.anymc.minecraft.data.modrinth.ModrinthDependency
import com.bartixw.anymc.minecraft.data.modrinth.ModrinthProject
import com.bartixw.anymc.minecraft.data.modrinth.ModrinthTeamMember
import com.bartixw.anymc.minecraft.data.modrinth.ModrinthVersion
import com.bartixw.anymc.ui.components.v2.Icon
import com.bartixw.anymc.ui.components.v2.ListItem
import com.bartixw.anymc.ui.components.v2.LoadingIndicator
import com.bartixw.anymc.ui.components.v2.Text
import com.bartixw.anymc.ui.components.v2.TopAppBar
import com.bartixw.anymc.ui.theme.MinecraftTheme
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.mikepenz.markdown.model.DefaultMarkdownTypography
import com.mikepenz.markdown.model.ImageData
import com.mikepenz.markdown.model.ImageTransformer
import com.mikepenz.markdown.model.markdownAnimations
import com.mikepenz.markdown.model.markdownAnnotator
import com.mikepenz.markdown.model.markdownDimens
import com.mikepenz.markdown.model.markdownExtendedSpans
import com.mikepenz.markdown.model.markdownInlineContent
import com.mikepenz.markdown.model.markdownPadding
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.stringResource

@Serializable
data class ModrinthDetailRoute(
    val projectId: String,
    val installationId: String,
    val mcVersion: String,
    val loader: String,
    val projectType: String = "mod"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModrinthDetailScreen(
    navController: NavController,
    projectId: String,
    installationId: String,
    mcVersion: String,
    loader: String,
    projectType: String
) {
    val viewModel = viewModel {
        ModrinthViewModel(installationId, mcVersion, loader, projectType)
    }

    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
    }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(
        stringResource(Res.string.modrinth_tab_description),
        stringResource(Res.string.modrinth_tab_changelog),
        stringResource(Res.string.modrinth_tab_versions)
    )

    Column(modifier = Modifier.fillMaxSize().background(MinecraftTheme.colors.backgroundDirt)) {
        TopAppBar(
            navController = navController,
            title = { Text(viewModel.selectedProject?.title ?: stringResource(Res.string.common_loading)) },
            actions = {}
        )

        if (viewModel.isLoadingProject) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
        } else if (viewModel.selectedProject != null) {
            val project = viewModel.selectedProject!!

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = project.iconUrl?.let { url ->
                        LaunchedEffect(url) {
                            viewModel.loadImage(url)
                        }
                        viewModel.imageCache[url]
                    }

                    if (icon != null) {
                        Image(
                            bitmap = icon,
                            contentDescription = null,
                            modifier = Modifier.size(96.dp),
                            filterQuality = FilterQuality.None
                        )
                    } else {
                        Box(
                            Modifier.size(96.dp).background(MinecraftTheme.colors.dialogBackground).border(2.dp, MinecraftTheme.colors.dialogBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(resource = Res.drawable.ic_image_unknown, contentDescription = null, modifier = Modifier.size(48.dp))
                        }
                    }

                    Spacer(Modifier.size(24.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            text = project.title,
                            style = MinecraftTheme.typographyPixel.copy(fontSize = 24.sp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = project.description,
                            style = MinecraftTheme.typographyV2.copy(fontSize = 14.sp, color = MinecraftTheme.colors.textSecondary)
                        )

                        Spacer(Modifier.size(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(resource = Res.drawable.ic_download, contentDescription = null, modifier = Modifier.size(16.dp), tint = MinecraftTheme.colors.textSecondary)
                            Spacer(Modifier.size(4.dp))
                            Text(
                                text = stringResource(Res.string.modrinth_downloads, project.downloads),
                                style = MinecraftTheme.typographyPixel.copy(fontSize = 12.sp, color = MinecraftTheme.colors.textSecondary)
                            )
                            Spacer(Modifier.size(16.dp))
                            Icon(resource = Res.drawable.ic_plus, contentDescription = null, modifier = Modifier.size(16.dp), tint = MinecraftTheme.colors.textSecondary)
                            Spacer(Modifier.size(4.dp))
                            Text(
                                text = stringResource(Res.string.modrinth_followers, project.followers),
                                style = MinecraftTheme.typographyPixel.copy(fontSize = 12.sp, color = MinecraftTheme.colors.textSecondary)
                            )
                        }

                        Spacer(Modifier.size(8.dp))

                        Row {
                            project.categories.take(3).forEach { category ->
                                CategoryTag(category)
                                Spacer(Modifier.size(8.dp))
                            }
                        }
                    }

                    Spacer(Modifier.size(16.dp))

                    DownloadButton(
                        viewModel = viewModel,
                        projectId = project.id,
                        onOpenVersions = { selectedTab = 2 },
                        primary = viewModel.selectedVersion == null
                    )
                }

                // Tabs
                Row(Modifier.padding(16.dp)) {
                    tabs.forEachIndexed { index, title ->
                        Box(
                            modifier = Modifier
                                .background(if (selectedTab == index) MinecraftTheme.colors.controlPrimaryBackground else MinecraftTheme.colors.dialogBackground)
                                .border(2.dp, if (selectedTab == index) MinecraftTheme.colors.controlPrimaryBorder else MinecraftTheme.colors.dialogBorder)
                                .clickable { selectedTab = index }
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = title,
                                style = MinecraftTheme.typographyPixel
                            )
                        }
                        Spacer(Modifier.size(8.dp))
                    }
                }

                if (viewModel.selectedVersion != null && selectedTab == 2) {
                    VersionDetailContent(
                        project = project,
                        version = viewModel.selectedVersion!!,
                        viewModel = viewModel,
                        onBack = { viewModel.selectVersion(null) }
                    )
                } else if (selectedTab == 0) {
                    val scrollState = rememberScrollState()

                    // Description + Sidebar
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                                .padding(horizontal = 16.dp)
                                .padding(end = 8.dp)
                        ) {
                            // Main Content
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(MinecraftTheme.colors.dialogBackground)
                                    .border(2.dp, MinecraftTheme.colors.dialogBorder)
                                    .padding(16.dp)
                            ) {
                                MarkdownContent(project.body ?: stringResource(Res.string.modrinth_no_description), viewModel)
                            }

                            Spacer(Modifier.size(16.dp))

                            // Sidebar
                            Column(modifier = Modifier.width(300.dp)) {
                                SidebarSection(title = stringResource(Res.string.modrinth_sidebar_compatibility)) {
                                    Text("Minecraft: Java Edition", style = MinecraftTheme.typographyPixel.copy(fontSize = 12.sp))
                                    Spacer(Modifier.size(8.dp))
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        project.gameVersions.take(12).forEach { version ->
                                            Tag(version)
                                        }
                                    }
                                }

                                SidebarSection(title = stringResource(Res.string.modrinth_sidebar_platforms)) {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        project.loaders.forEach { loader ->
                                            Tag(loader.replaceFirstChar { c -> c.uppercase() })
                                        }
                                    }
                                }

                                SidebarSection(title = stringResource(Res.string.modrinth_sidebar_environments)) {
                                    project.clientSide?.let {
                                        Text("${stringResource(Res.string.modrinth_env_client)}: ${it.translateEnvironment()}", style = MinecraftTheme.typographyPixel.copy(fontSize = 12.sp))
                                    }
                                    project.serverSide?.let {
                                        Text("${stringResource(Res.string.modrinth_env_server)}: ${it.translateEnvironment()}", style = MinecraftTheme.typographyPixel.copy(fontSize = 12.sp))
                                    }
                                }

                                val links = listOfNotNull(
                                    project.sourceUrl?.let { (stringResource(Res.string.modrinth_link_source) to Res.drawable.ic_github) to it },
                                    project.issuesUrl?.let { (stringResource(Res.string.modrinth_link_issues) to Res.drawable.ic_warning) to it },
                                    project.wikiUrl?.let { (stringResource(Res.string.modrinth_link_wiki) to Res.drawable.ic_book) to it },
                                    project.discordUrl?.let { (stringResource(Res.string.modrinth_link_discord) to Res.drawable.ic_discord) to it }
                                )

                                if (links.isNotEmpty()) {
                                    SidebarSection(title = stringResource(Res.string.modrinth_sidebar_links)) {
                                        links.forEach { (label, url) ->
                                            ExternalLink(label.first, url, label.second)
                                            Spacer(Modifier.size(4.dp))
                                        }
                                    }
                                }

                                if (project.categories.isNotEmpty()) {
                                    SidebarSection(title = stringResource(Res.string.modrinth_tags)) {
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            project.categories.forEach { Tag(it) }
                                        }
                                    }
                                }

                                if (viewModel.teamMembers.isNotEmpty()) {
                                    SidebarSection(title = stringResource(Res.string.modrinth_sidebar_authors)) {
                                        viewModel.teamMembers.forEach { member ->
                                            TeamMemberItem(member, viewModel)
                                            Spacer(Modifier.size(8.dp))
                                        }
                                    }
                                }

                                SidebarSection(title = stringResource(Res.string.modrinth_sidebar_details)) {
                                    project.license?.let {
                                        DetailItem(Res.drawable.ic_info, stringResource(Res.string.modrinth_detail_license, it.name.ifEmpty { "Kliknij" }), it.url)
                                    }
                                    DetailItem(Res.drawable.ic_bread, stringResource(Res.string.modrinth_detail_published, project.published.formatModrinthDate()))
                                    DetailItem(Res.drawable.ic_reload, stringResource(Res.string.modrinth_detail_updated, project.updated.formatModrinthDate()))
                                }
                            }
                        }
                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(scrollState),
                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                        )
                    }
                } else if (selectedTab == 1) {
                    // Changelog Tab
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (viewModel.isLoadingVersions) {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                LoadingIndicator()
                            }
                        } else {
                            val listState = rememberLazyListState()

                            Box(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .padding(end = 8.dp)
                                ) {
                                    items(viewModel.modVersions) { version ->
                                        ChangelogItem(version) {
                                            viewModel.selectVersion(version)
                                            selectedTab = 2
                                        }
                                    }
                                }

                                VerticalScrollbar(
                                    adapter = rememberScrollbarAdapter(listState),
                                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                                )
                            }
                        }
                    }
                } else {
                    // Versions Tab
                    if (viewModel.isLoadingVersions) {
                        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                            LoadingIndicator()
                        }
                    } else {
                        val listState = rememberLazyListState()

                        Box(Modifier.fillMaxSize()) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(end = 8.dp)
                            ) {
                                items(viewModel.modVersions) { version ->
                                    ListItem(
                                        content = {
                                            Column {
                                                Text(
                                                    version.versionNumber,
                                                    style = MinecraftTheme.typographyPixel
                                                )
                                                Text(
                                                    version.name,
                                                    style = TextStyle(color = MinecraftTheme.colors.textSecondary)
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        baseModifier = Modifier.clickable {
                                            viewModel.selectVersion(version)
                                        }
                                    )
                                }
                            }
                            VerticalScrollbar(
                                adapter = rememberScrollbarAdapter(listState),
                                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChangelogItem(version: ModrinthVersion, onClick: () -> Unit) {
    ListItem(
        baseModifier = Modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = version.versionNumber,
                    style = MinecraftTheme.typographyPixel.copy(fontSize = 18.sp)
                )
                Spacer(Modifier.size(12.dp))
                VersionTypeTag(version.versionType)
                Spacer(Modifier.weight(1f))
                Text(
                    text = version.datePublished.formatModrinthDate(),
                    style = MinecraftTheme.typographyPixel.copy(fontSize = 12.sp, color = MinecraftTheme.colors.textSecondary)
                )
            }

            if (!version.changelog.isNullOrBlank()) {
                Spacer(Modifier.size(8.dp))
                Text(
                    text = version.changelog!!,
                    style = MinecraftTheme.typographyV2.copy(fontSize = 12.sp),
                    maxLines = 3
                )
            }
        }
    }
}

@Composable
fun VersionTypeTag(type: String) {
    val color = when (type) {
        "release" -> Color(0xFF2ecc71)
        "beta" -> Color(0xFFf1c40f)
        "alpha" -> Color(0xFFe67e22)
        else -> MinecraftTheme.colors.textSecondary
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.2f))
            .border(1.dp, color)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = type.replaceFirstChar { it.uppercase() },
            style = MinecraftTheme.typographyPixel.copy(fontSize = 10.sp, color = color)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VersionDetailContent(
    project: ModrinthProject,
    version: ModrinthVersion,
    viewModel: ModrinthViewModel,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().verticalScroll(scrollState).padding(horizontal = 16.dp).padding(end = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onBack() }) {
                Icon(resource = Res.drawable.ic_chevron_left, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Cyan)
                Spacer(Modifier.size(4.dp))
                Text(stringResource(Res.string.modrinth_all_versions), style = MinecraftTheme.typographyPixel.copy(color = Color.Cyan))
            }

            Spacer(Modifier.size(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = version.versionNumber,
                    style = MinecraftTheme.typographyPixel.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.size(12.dp))
                VersionTypeTag(version.versionType)

                Spacer(Modifier.weight(1f))

                DownloadButton(
                    viewModel = viewModel,
                    projectId = project.id
                )
            }

            Spacer(Modifier.size(8.dp))

            Row (modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = version.name,
                    style = MinecraftTheme.typographyPixel.copy(fontSize = 14.sp, color = MinecraftTheme.colors.textSecondary),
                    modifier = Modifier.weight(1f)
                )
                Row (verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.weight(1f)) {
                    Icon(
                        resource = Res.drawable.ic_clock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MinecraftTheme.colors.textSecondary
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = version.datePublished.formatModrinthDate(),
                        style = MinecraftTheme.typographyV2.copy(fontSize = 14.sp, color = MinecraftTheme.colors.textSecondary)
                    )
                }
                Row (verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End, modifier = Modifier.weight(1f)) {
                    Icon(
                        resource = Res.drawable.ic_download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MinecraftTheme.colors.textSecondary
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = version.downloads.toString(),
                        style = MinecraftTheme.typographyV2.copy(fontSize = 14.sp, color = MinecraftTheme.colors.textSecondary)
                    )
                }
            }

            Spacer(Modifier.size(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                SidebarSection(title = stringResource(Res.string.modrinth_sidebar_compatibility), modifier = Modifier.weight(1f)) {
                    Text("Minecraft: Java Edition", style = MinecraftTheme.typographyPixel.copy(fontSize = 12.sp))
                    Spacer(Modifier.size(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        version.gameVersions.forEach { Tag(it) }
                    }
                }
                Spacer(Modifier.size(16.dp))
                SidebarSection(title = stringResource(Res.string.modrinth_sidebar_platforms), modifier = Modifier.weight(1f)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        version.loaders.forEach { loader -> Tag(loader.replaceFirstChar { c -> c.uppercase() }) }
                    }
                }
                Spacer(Modifier.size(16.dp))
                SidebarSection(title = stringResource(Res.string.modrinth_sidebar_environments), modifier = Modifier.weight(1f)) {
                    project.clientSide?.let { Text("${stringResource(Res.string.modrinth_env_client)}: ${it.translateEnvironment()}", style = MinecraftTheme.typographyPixel.copy(fontSize = 12.sp)) }
                    project.serverSide?.let { Text("${stringResource(Res.string.modrinth_env_server)}: ${it.translateEnvironment()}", style = MinecraftTheme.typographyPixel.copy(fontSize = 12.sp)) }
                }
            }

            Spacer(Modifier.size(24.dp))

            Text(stringResource(Res.string.modrinth_tab_changelog), style = MinecraftTheme.typographyPixel.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold))
            Spacer(Modifier.size(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MinecraftTheme.colors.dialogBackground)
                    .border(2.dp, MinecraftTheme.colors.dialogBorder)
                    .padding(16.dp)
            ) {
                MarkdownContent(version.changelog ?: stringResource(Res.string.modrinth_no_changelog), viewModel)
            }

            if (version.dependencies.isNotEmpty()) {
                Spacer(Modifier.size(24.dp))
                Text(stringResource(Res.string.modrinth_dependencies), style = MinecraftTheme.typographyPixel.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold))
                Spacer(Modifier.size(8.dp))
                DependencyTable(version.dependencies, viewModel)
            }
        }

        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
        )
    }
}

@Composable
fun DependencyTable(dependencies: List<ModrinthDependency>, viewModel: ModrinthViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MinecraftTheme.colors.dialogBackground)
            .border(2.dp, MinecraftTheme.colors.dialogBorder)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(MinecraftTheme.colors.surfaceShadow).padding(12.dp)
        ) {
            Text(stringResource(Res.string.modrinth_dependency_name), modifier = Modifier.weight(1f), style = MinecraftTheme.typographyV2.copy(fontWeight = FontWeight.Bold))
            Text(stringResource(Res.string.modrinth_dependency_version), modifier = Modifier.width(150.dp), style = MinecraftTheme.typographyV2.copy(fontWeight = FontWeight.Bold))
        }

        dependencies.forEach { dep ->
            val depProject = viewModel.versionContentProjects[dep.projectId]
            Row(
                modifier = Modifier.fillMaxWidth().border(1.dp, MinecraftTheme.colors.dialogBorder).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon = depProject?.iconUrl?.let { viewModel.imageCache[it] }
                Image(bitmap = icon ?: imageResource(Res.drawable.ic_image_unknown), contentDescription = null, modifier = Modifier.size(32.dp))
                Spacer(Modifier.size(12.dp))
                Text(text = depProject?.title ?: (dep.fileName ?: "Nieznany mod"), modifier = Modifier.weight(1f))
                Text(
                    text = dep.formatReadableVersion(),
                    modifier = Modifier.width(150.dp),
                    style = MinecraftTheme.typographyV2.copy(fontSize = 12.sp, color = MinecraftTheme.colors.textSecondary)
                )
            }
        }
    }
}

@Composable
fun TeamMemberItem(member: ModrinthTeamMember, viewModel: ModrinthViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val avatar = member.user.avatarUrl?.let { viewModel.imageCache[it] }
        if (avatar != null) {
            Image(bitmap = avatar, contentDescription = null, modifier = Modifier.size(32.dp))
        } else {
            Box(Modifier.size(32.dp).background(MinecraftTheme.colors.dialogBackground).border(1.dp, MinecraftTheme.colors.dialogBorder))
        }
        Spacer(Modifier.size(12.dp))
        Column {
            Text(text = member.user.username, style = MinecraftTheme.typographyPixel.copy(fontSize = 13.sp))
            Text(text = member.role, style = MinecraftTheme.typographyPixel.copy(fontSize = 11.sp, color = MinecraftTheme.colors.textSecondary))
        }
    }
}

@Composable
fun MarkdownContent(body: String, viewModel: ModrinthViewModel) {
    val colors = MinecraftTheme.colors
    val typography = MinecraftTheme.typographyV2

    val mdColors = DefaultMarkdownColors(
        text = colors.textPrimary,
        codeBackground = colors.v1inputBackground,
        inlineCodeBackground = colors.v1inputBackground,
        dividerColor = colors.surfaceShadow,
        tableBackground = colors.controlBackground
    )

    val mdTypography = DefaultMarkdownTypography(
        text = typography,
        code = typography,
        inlineCode = typography,
        h1 = typography.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
        h2 = typography.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold),
        h3 = typography.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
        h4 = typography.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
        h5 = typography.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
        h6 = typography.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
        quote = typography,
        paragraph = typography,
        ordered = typography,
        bullet = typography,
        list = typography,
        textLink = TextLinkStyles(style = SpanStyle(color = Color.Cyan)),
        table = typography
    )

    Markdown(
        content = body,
        colors = mdColors,
        typography = mdTypography,
        padding = markdownPadding(),
        dimens = markdownDimens(),
        imageTransformer = object : ImageTransformer {
            @Composable
            override fun transform(link: String): ImageData? {
                val bitmap = viewModel.imageCache[link]
                if (bitmap == null) {
                    LaunchedEffect(link) {
                        viewModel.loadImage(link)
                    }
                }
                return bitmap?.let {
                    ImageData(painter = BitmapPainter(it))
                }
            }
        },
        annotator = markdownAnnotator(),
        extendedSpans = markdownExtendedSpans(),
        inlineContent = markdownInlineContent(),
        components = markdownComponents(),
        animations = markdownAnimations()
    )
}

@Composable
fun DownloadButton(
    viewModel: ModrinthViewModel,
    projectId: String,
    onOpenVersions: (() -> Unit)? = null,
    primary: Boolean = true
) {
    val downloadTasks by ModDownloadManager.tasks.collectAsState()
    val taskId = "${viewModel.installationId}-$projectId"
    val task = downloadTasks[taskId]

    Row(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .border(2.dp, if (primary) MinecraftTheme.colors.controlPrimaryBorder else MinecraftTheme.colors.dialogBorder)
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (primary)
                        if (task?.status == ModDownloadManager.Status.COMPLETED)
                            MinecraftTheme.colors.controlPrimaryDisabledBackground
                        else MinecraftTheme.colors.controlPrimaryBackground
                    else if (task?.status == ModDownloadManager.Status.COMPLETED)
                        MinecraftTheme.colors.controlDisabledBackground
                    else MinecraftTheme.colors.dialogBackground
                )
                .clickable(enabled = task == null) {
                    viewModel.downloadLatest()
                }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    resource = Res.drawable.ic_download,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.size(8.dp))
                val text = when (task?.status) {
                    ModDownloadManager.Status.DOWNLOADING -> "${(task.progress * 100).toInt()}%"
                    ModDownloadManager.Status.COMPLETED -> stringResource(Res.string.modrinth_btn_downloaded)
                    ModDownloadManager.Status.FAILED -> stringResource(Res.string.modrinth_btn_error)
                    else -> stringResource(Res.string.modrinth_btn_download)
                }
                Text(text = text, style = MinecraftTheme.typographyPixel)
            }
        }

        if (onOpenVersions != null) Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(if (primary) MinecraftTheme.colors.controlPrimaryBorder else MinecraftTheme.colors.dialogBorder)
        )

        if (onOpenVersions != null) Box(
            modifier = Modifier
                .background(
                    if (primary)
                        if (task?.status == ModDownloadManager.Status.COMPLETED)
                            MinecraftTheme.colors.controlPrimaryDisabledBackground
                        else MinecraftTheme.colors.controlPrimaryBackground
                    else if (task?.status == ModDownloadManager.Status.COMPLETED)
                        MinecraftTheme.colors.controlDisabledBackground
                    else MinecraftTheme.colors.dialogBackground
                )
                .clickable { onOpenVersions() }
                .padding(horizontal = 8.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                resource = Res.drawable.ic_chevron_down,
                contentDescription = stringResource(Res.string.modrinth_btn_other_versions),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
