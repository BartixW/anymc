/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.settings

import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.ic_image_unknown
import anymc.shared.generated.resources.ic_info
import anymc.shared.generated.resources.ic_java
import anymc.shared.generated.resources.ic_servers
import anymc.shared.generated.resources.ic_settings
import anymc.shared.generated.resources.ic_sync
import anymc.shared.generated.resources.ic_theme
import anymc.shared.generated.resources.ic_anymc_link
import anymc.shared.generated.resources.settings_link_description
import anymc.shared.generated.resources.settings_link_title
import anymc.shared.generated.resources.settings_section_about_desc
import anymc.shared.generated.resources.settings_section_about_title
import anymc.shared.generated.resources.settings_section_advanced_desc
import anymc.shared.generated.resources.settings_section_advanced_title
import anymc.shared.generated.resources.settings_section_appearance_title
import anymc.shared.generated.resources.settings_section_java_desc
import anymc.shared.generated.resources.settings_section_java_title
import anymc.shared.generated.resources.settings_sync_description
import anymc.shared.generated.resources.settings_sync_title
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

@Serializable
enum class SettingsSection(
    val title: StringResource,
    val description: StringResource?,
    val icon: DrawableResource
) {
    APPEARANCE(Res.string.settings_section_appearance_title, null, Res.drawable.ic_theme),
    JAVA_PERFORMANCE(Res.string.settings_section_java_title, Res.string.settings_section_java_desc, Res.drawable.ic_java),
    //SYNC(Res.string.settings_sync_title, Res.string.settings_sync_description, icon = Res.drawable.ic_sync),
    //SERVERS(Res.string.settings_section_servers_title, Res.string.settings_section_servers_desc, icon = Res.drawable.ic_servers),
    //LINK(Res.string.settings_link_title, Res.string.settings_link_description, icon = Res.drawable.ic_anymc_link),
    ADVANCED(Res.string.settings_section_advanced_title, Res.string.settings_section_advanced_desc, Res.drawable.ic_settings),
    ABOUT(Res.string.settings_section_about_title, Res.string.settings_section_about_desc, Res.drawable.ic_info)
}
