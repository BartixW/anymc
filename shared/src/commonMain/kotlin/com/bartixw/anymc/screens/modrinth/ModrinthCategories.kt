/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.modrinth

import anymc.shared.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

data class ModrinthCategory(
    val name: StringResource,
    val slug: String,
    val icon: DrawableResource
)

data class ModrinthCategoryGroup(
    val name: StringResource,
    val categories: List<ModrinthCategory>
)

object ModrinthCategories {
    fun getGroupsFor(projectType: String): List<ModrinthCategoryGroup> {
        val catGroup = Res.string.modrinth_cat_group_category
        val featGroup = Res.string.modrinth_cat_group_features
        val resGroup = Res.string.modrinth_cat_group_resolution
        val perfGroup = Res.string.modrinth_cat_group_performance

        return when (projectType) {
            "mod" -> listOf(
                ModrinthCategoryGroup(catGroup, listOf(
                    ModrinthCategory(Res.string.modrinth_cat_library, "library", Res.drawable.ic_book_closed),
                    ModrinthCategory(Res.string.modrinth_cat_cursed, "cursed", Res.drawable.ic_bug),
                    ModrinthCategory(Res.string.modrinth_cat_decoration, "decoration", Res.drawable.ic_home),
                    ModrinthCategory(Res.string.modrinth_cat_economy, "economy", Res.drawable.ic_money),
                    ModrinthCategory(Res.string.modrinth_cat_world_gen, "world-generation", Res.drawable.ic_globe),
                    ModrinthCategory(Res.string.modrinth_cat_food, "food", Res.drawable.ic_food),
                    ModrinthCategory(Res.string.modrinth_cat_storage, "storage", Res.drawable.ic_box),
                    ModrinthCategory(Res.string.modrinth_cat_magic, "magic", Res.drawable.ic_wand),
                    ModrinthCategory(Res.string.modrinth_cat_game_mechanics, "game-mechanics", Res.drawable.ic_settings),
                    ModrinthCategory(Res.string.modrinth_cat_minigame, "minigame", Res.drawable.ic_medal),
                    ModrinthCategory(Res.string.modrinth_cat_mobs, "mobs", Res.drawable.ic_creeper),
                    ModrinthCategory(Res.string.modrinth_cat_utility, "utility", Res.drawable.ic_suitcase),
                    ModrinthCategory(Res.string.modrinth_cat_optimization, "optimization", Res.drawable.ic_spark),
                    ModrinthCategory(Res.string.modrinth_cat_adventure, "adventure", Res.drawable.ic_compass),
                    ModrinthCategory(Res.string.modrinth_cat_social, "social", Res.drawable.ic_chat_bubble),
                    ModrinthCategory(Res.string.modrinth_cat_technology, "technology", Res.drawable.ic_server),
                    ModrinthCategory(Res.string.modrinth_cat_transportation, "transportation", Res.drawable.ic_bus),
                    ModrinthCategory(Res.string.modrinth_cat_equipment, "equipment", Res.drawable.ic_sword),
                    ModrinthCategory(Res.string.modrinth_cat_management, "management", Res.drawable.ic_servers)
                ))
            )
            "modpack" -> listOf(
                ModrinthCategoryGroup(catGroup, listOf(
                    ModrinthCategory(Res.string.modrinth_cat_lightweight, "lightweight", Res.drawable.ic_leaf),
                    ModrinthCategory(Res.string.modrinth_cat_magic, "magic", Res.drawable.ic_wand),
                    ModrinthCategory(Res.string.modrinth_cat_optimization, "optimization", Res.drawable.ic_spark),
                    ModrinthCategory(Res.string.modrinth_cat_adventure, "adventure", Res.drawable.ic_compass),
                    ModrinthCategory(Res.string.modrinth_cat_technology, "tech", Res.drawable.ic_server),
                    ModrinthCategory(Res.string.modrinth_cat_multiplayer, "multiplayer", Res.drawable.ic_profile),
                    ModrinthCategory(Res.string.modrinth_cat_combat, "combat", Res.drawable.ic_sword),
                    ModrinthCategory(Res.string.modrinth_cat_challenge, "challenging", Res.drawable.ic_skull),
                    ModrinthCategory(Res.string.modrinth_cat_quest, "quest", Res.drawable.ic_check),
                    ModrinthCategory(Res.string.modrinth_cat_kitchen_sink, "kitchen-sink", Res.drawable.ic_box)
                ))
            )
            "resourcepack" -> listOf(
                ModrinthCategoryGroup(catGroup, listOf(
                    ModrinthCategory(Res.string.modrinth_cat_cursed, "cursed", Res.drawable.ic_bug),
                    ModrinthCategory(Res.string.modrinth_cat_decoration, "decorations", Res.drawable.ic_home),
                    ModrinthCategory(Res.string.modrinth_cat_modded, "modded", Res.drawable.ic_plugin),
                    ModrinthCategory(Res.string.modrinth_cat_tweaks, "tweaks", Res.drawable.ic_edit),
                    ModrinthCategory(Res.string.modrinth_cat_vanilla_like, "vanilla-like", Res.drawable.ic_ice_cream),
                    ModrinthCategory(Res.string.modrinth_cat_utility, "utility", Res.drawable.ic_suitcase),
                    ModrinthCategory(Res.string.modrinth_cat_simple, "simple", Res.drawable.ic_maximize),
                    ModrinthCategory(Res.string.modrinth_cat_realistic, "realistic", Res.drawable.ic_camera),
                    ModrinthCategory(Res.string.modrinth_cat_themed, "themed", Res.drawable.ic_theme),
                    ModrinthCategory(Res.string.modrinth_cat_combat, "combat", Res.drawable.ic_sword),
                )),
                ModrinthCategoryGroup(featGroup, listOf(
                    ModrinthCategory(Res.string.modrinth_cat_blocks, "blocks", Res.drawable.ic_block),
                    ModrinthCategory(Res.string.modrinth_cat_entities, "entities", Res.drawable.ic_creeper),
                    ModrinthCategory(Res.string.modrinth_cat_core_shaders, "core-shaders", Res.drawable.ic_cpu),
                    ModrinthCategory(Res.string.modrinth_cat_fonts, "fonts", Res.drawable.ic_text),
                    ModrinthCategory(Res.string.modrinth_cat_audio, "audio", Res.drawable.ic_headphones),
                    ModrinthCategory(Res.string.modrinth_cat_gui, "gui", Res.drawable.ic_gui),
                    ModrinthCategory(Res.string.modrinth_cat_models, "models", Res.drawable.ic_stack),
                    ModrinthCategory(Res.string.modrinth_cat_items, "items", Res.drawable.ic_book_closed),
                    ModrinthCategory(Res.string.modrinth_cat_environment, "environment", Res.drawable.ic_terrain),
                    ModrinthCategory(Res.string.modrinth_cat_translations, "locale", Res.drawable.ic_globe),
                    ModrinthCategory(Res.string.modrinth_cat_equipment, "equipment", Res.drawable.ic_sword),
                )),
                ModrinthCategoryGroup(resGroup, listOf(
                    ModrinthCategory(Res.string.modrinth_cat_16x, "16x", Res.drawable.ic_block),
                    ModrinthCategory(Res.string.modrinth_cat_32x, "32x", Res.drawable.ic_block),
                    ModrinthCategory(Res.string.modrinth_cat_64x, "64x", Res.drawable.ic_block),
                    ModrinthCategory(Res.string.modrinth_cat_128x, "128x", Res.drawable.ic_block),
                    ModrinthCategory(Res.string.modrinth_cat_256x, "256x", Res.drawable.ic_block),
                    ModrinthCategory(Res.string.modrinth_cat_512x, "512x-or-higher", Res.drawable.ic_block)
                ))
            )
            "shader" -> listOf(
                ModrinthCategoryGroup(catGroup, listOf(
                    ModrinthCategory(Res.string.modrinth_cat_cursed, "cursed", Res.drawable.ic_bug),
                    ModrinthCategory(Res.string.modrinth_cat_fantasy, "fantasy", Res.drawable.ic_wand),
                    ModrinthCategory(Res.string.modrinth_cat_vanilla_like, "vanilla-like", Res.drawable.ic_ice_cream),
                    ModrinthCategory(Res.string.modrinth_cat_cartoon, "cartoon", Res.drawable.ic_theme),
                    ModrinthCategory(Res.string.modrinth_cat_realistic, "realistic", Res.drawable.ic_camera),
                    ModrinthCategory(Res.string.modrinth_cat_semi_realistic, "semi-realistic", Res.drawable.ic_film)
                )),
                ModrinthCategoryGroup(featGroup, listOf(
                    ModrinthCategory(Res.string.modrinth_cat_atmosphere, "atmosphere", Res.drawable.ic_weather),
                    ModrinthCategory(Res.string.modrinth_cat_bloom, "bloom", Res.drawable.ic_lamp),
                    ModrinthCategory(Res.string.modrinth_cat_shadows, "shadows", Res.drawable.ic_terrain),
                    ModrinthCategory(Res.string.modrinth_cat_lighting, "colored-lightning", Res.drawable.ic_color_mix),
                    ModrinthCategory(Res.string.modrinth_cat_leaves, "leaves", Res.drawable.ic_leaf),
                    ModrinthCategory(Res.string.modrinth_cat_reflections, "reflections", Res.drawable.ic_reflection),
                    ModrinthCategory(Res.string.modrinth_cat_pbr, "pbr", Res.drawable.ic_light_bulb),
                    ModrinthCategory(Res.string.modrinth_cat_path_tracing, "path-tracing", Res.drawable.ic_bounce),

                )),
                ModrinthCategoryGroup(perfGroup, listOf(
                    ModrinthCategory(Res.string.modrinth_cat_potato, "potato", Res.drawable.ic_potato),
                    ModrinthCategory(Res.string.modrinth_cat_low, "low", Res.drawable.ic_ping_high),
                    ModrinthCategory(Res.string.modrinth_cat_medium, "medium", Res.drawable.ic_ping_med),
                    ModrinthCategory(Res.string.modrinth_cat_high, "high", Res.drawable.ic_ping_low),
                    ModrinthCategory(Res.string.modrinth_cat_screenshot, "screenshot", Res.drawable.ic_image)
                ))
            )
            else -> emptyList()
        }
    }
}
