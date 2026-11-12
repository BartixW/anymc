/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ColorSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Color) {
        val argb = value.toArgb()
        encoder.encodeString("#" + argb.toUInt().toString(16).padStart(8, '0').uppercase())
    }
    override fun deserialize(decoder: Decoder): Color {
        val hex = decoder.decodeString().removePrefix("#")
        return if (hex.length == 6) {
            Color((hex.toLong(16) or 0xFF000000L).toInt())
        } else {
            Color(hex.toLong(16).toInt())
        }
    }
}

@Serializable
data class MinecraftColors(
    @Serializable(with = ColorSerializer::class) val primary: Color,
    @Serializable(with = ColorSerializer::class) val backgroundDirt: Color,
    @Serializable(with = ColorSerializer::class) val surfaceFrame: Color,
    @Serializable(with = ColorSerializer::class) val surfaceHighlight: Color,
    @Serializable(with = ColorSerializer::class) val surfaceShadow: Color,
    @Serializable(with = ColorSerializer::class) val textPrimary: Color,
    @Serializable(with = ColorSerializer::class) val textSecondary: Color,

    @Serializable(with = ColorSerializer::class) val controlDisabledBackground: Color,
    @Serializable(with = ColorSerializer::class) val controlBackground: Color,
    @Serializable(with = ColorSerializer::class) val controlBorder: Color,
    @Serializable(with = ColorSerializer::class) val controlHighlight: Color,
    @Serializable(with = ColorSerializer::class) val controlShadow: Color,

    @Serializable(with = ColorSerializer::class) val controlPrimaryDisabledBackground: Color,
    @Serializable(with = ColorSerializer::class) val controlPrimaryBackground: Color,
    @Serializable(with = ColorSerializer::class) val controlPrimaryBorder: Color,
    @Serializable(with = ColorSerializer::class) val controlPrimaryHighlight: Color,
    @Serializable(with = ColorSerializer::class) val controlPrimaryShadow: Color,

    @Serializable(with = ColorSerializer::class) val controlDestructiveDisabledBackground: Color,
    @Serializable(with = ColorSerializer::class) val controlDestructiveBackground: Color,
    @Serializable(with = ColorSerializer::class) val controlDestructiveHoveredBackground: Color,
    @Serializable(with = ColorSerializer::class) val controlDestructiveBorder: Color,
    @Serializable(with = ColorSerializer::class) val controlDestructiveHighlight: Color,
    @Serializable(with = ColorSerializer::class) val controlDestructiveShadow: Color,

    @Serializable(with = ColorSerializer::class) val v1inputBackground: Color,

    @Serializable(with = ColorSerializer::class) val buttonPrimaryBorder: Color,
    @Serializable(with = ColorSerializer::class) val buttonPrimaryBackground: Color,
    @Serializable(with = ColorSerializer::class) val buttonPrimaryShadow: Color,
    @Serializable(with = ColorSerializer::class) val buttonPrimaryOuterBorder: Color,
    @Serializable(with = ColorSerializer::class) val buttonPrimaryText: Color,

    @Serializable(with = ColorSerializer::class) val buttonPrimaryHoverBorder: Color,
    @Serializable(with = ColorSerializer::class) val buttonPrimaryHoverBackground: Color,
    @Serializable(with = ColorSerializer::class) val buttonPrimaryHoverShadow: Color,
    @Serializable(with = ColorSerializer::class) val buttonPrimaryHoverOuterBorder: Color,
    @Serializable(with = ColorSerializer::class) val buttonPrimaryHoverText: Color,

    @Serializable(with = ColorSerializer::class) val buttonPrimaryPressedBorder: Color,
    @Serializable(with = ColorSerializer::class) val buttonPrimaryPressedBackground: Color,
    @Serializable(with = ColorSerializer::class) val buttonPrimaryPressedOuterBorder: Color,
    @Serializable(with = ColorSerializer::class) val buttonPrimaryPressedText: Color,

    @Serializable(with = ColorSerializer::class) val buttonSecondaryBorder: Color,
    @Serializable(with = ColorSerializer::class) val buttonSecondaryBackground: Color,
    @Serializable(with = ColorSerializer::class) val buttonSecondaryShadow: Color,
    @Serializable(with = ColorSerializer::class) val buttonSecondaryOuterBorder: Color,
    @Serializable(with = ColorSerializer::class) val buttonSecondaryText: Color,

    @Serializable(with = ColorSerializer::class) val buttonSecondaryHoverBorder: Color,
    @Serializable(with = ColorSerializer::class) val buttonSecondaryHoverBackground: Color,
    @Serializable(with = ColorSerializer::class) val buttonSecondaryHoverShadow: Color,
    @Serializable(with = ColorSerializer::class) val buttonSecondaryHoverOuterBorder: Color,
    @Serializable(with = ColorSerializer::class) val buttonSecondaryHoverText: Color,

    @Serializable(with = ColorSerializer::class) val buttonSecondaryPressedBorder: Color,
    @Serializable(with = ColorSerializer::class) val buttonSecondaryPressedBackground: Color,
    @Serializable(with = ColorSerializer::class) val buttonSecondaryPressedOuterBorder: Color,
    @Serializable(with = ColorSerializer::class) val buttonSecondaryPressedText: Color,

    @Serializable(with = ColorSerializer::class) val buttonDarkBorder: Color,
    @Serializable(with = ColorSerializer::class) val buttonDarkBackground: Color,
    @Serializable(with = ColorSerializer::class) val buttonDarkShadow: Color,
    @Serializable(with = ColorSerializer::class) val buttonDarkOuterBorder: Color,
    @Serializable(with = ColorSerializer::class) val buttonDarkText: Color,

    @Serializable(with = ColorSerializer::class) val buttonDarkHoverBorder: Color,
    @Serializable(with = ColorSerializer::class) val buttonDarkHoverBackground: Color,
    @Serializable(with = ColorSerializer::class) val buttonDarkHoverShadow: Color,
    @Serializable(with = ColorSerializer::class) val buttonDarkHoverOuterBorder: Color,
    @Serializable(with = ColorSerializer::class) val buttonDarkHoverText: Color,

    @Serializable(with = ColorSerializer::class) val buttonDarkPressedBorder: Color,
    @Serializable(with = ColorSerializer::class) val buttonDarkPressedBackground: Color,
    @Serializable(with = ColorSerializer::class) val buttonDarkPressedOuterBorder: Color,
    @Serializable(with = ColorSerializer::class) val buttonDarkPressedText: Color,

    @Serializable(with = ColorSerializer::class) val buttonDestructiveBorder: Color,
    @Serializable(with = ColorSerializer::class) val buttonDestructiveBackground: Color,
    @Serializable(with = ColorSerializer::class) val buttonDestructiveShadow: Color,
    @Serializable(with = ColorSerializer::class) val buttonDestructiveOuterBorder: Color,
    @Serializable(with = ColorSerializer::class) val buttonDestructiveText: Color,

    @Serializable(with = ColorSerializer::class) val buttonDestructiveHoverBorder: Color,
    @Serializable(with = ColorSerializer::class) val buttonDestructiveHoverBackground: Color,
    @Serializable(with = ColorSerializer::class) val buttonDestructiveHoverShadow: Color,
    @Serializable(with = ColorSerializer::class) val buttonDestructiveHoverOuterBorder: Color,
    @Serializable(with = ColorSerializer::class) val buttonDestructiveHoverText: Color,

    @Serializable(with = ColorSerializer::class) val buttonDestructivePressedBorder: Color,
    @Serializable(with = ColorSerializer::class) val buttonDestructivePressedBackground: Color,
    @Serializable(with = ColorSerializer::class) val buttonDestructivePressedOuterBorder: Color,
    @Serializable(with = ColorSerializer::class) val buttonDestructivePressedText: Color,

    @Serializable(with = ColorSerializer::class) val buttonDisabledBorder: Color,
    @Serializable(with = ColorSerializer::class) val buttonDisabledBackground: Color,
    @Serializable(with = ColorSerializer::class) val buttonDisabledShadow: Color,
    @Serializable(with = ColorSerializer::class) val buttonDisabledOuterBorder: Color,
    @Serializable(with = ColorSerializer::class) val buttonDisabledText: Color,

    @Serializable(with = ColorSerializer::class) val sidebarItemText: Color,
    @Serializable(with = ColorSerializer::class) val sidebarItemIcon: Color,
    @Serializable(with = ColorSerializer::class) val sidebarItemSelectedBar: Color,
    @Serializable(with = ColorSerializer::class) val sidebarItemSelectedText: Color,
    @Serializable(with = ColorSerializer::class) val sidebarItemSelectedIcon: Color,

    @Serializable(with = ColorSerializer::class) val topAppBarBackground: Color,
    @Serializable(with = ColorSerializer::class) val topAppBarBorder: Color,
    @Serializable(with = ColorSerializer::class) val topAppBarText: Color,

    @Serializable(with = ColorSerializer::class) val dialogBackground: Color,
    @Serializable(with = ColorSerializer::class) val dialogBorder: Color,
    @Serializable(with = ColorSerializer::class) val dialogContainerBackground: Color,

    @Serializable(with = ColorSerializer::class) val listItemBackground: Color,
    @Serializable(with = ColorSerializer::class) val listItemHighlight: Color,
    @Serializable(with = ColorSerializer::class) val listItemShadow: Color,

    @Serializable(with = ColorSerializer::class) val dropdownBackground: Color,
    @Serializable(with = ColorSerializer::class) val dropdownBorder: Color,
    @Serializable(with = ColorSerializer::class) val dropdownText: Color,
    @Serializable(with = ColorSerializer::class) val dropdownInnerBorder: Color,

    @Serializable(with = ColorSerializer::class) val genericPrimary: Color,
    @Serializable(with = ColorSerializer::class) val genericDestructive: Color,

    @Serializable(with = ColorSerializer::class) val inputBackground: Color,
    @Serializable(with = ColorSerializer::class) val inputBorder: Color,
    @Serializable(with = ColorSerializer::class) val inputText: Color,
    @Serializable(with = ColorSerializer::class) val inputTextHint: Color,
    @Serializable(with = ColorSerializer::class) val inputShadow: Color,

    @Serializable(with = ColorSerializer::class) val checkboxBackground: Color,
    @Serializable(with = ColorSerializer::class) val checkboxBorder: Color,

    )

val ClassicMinecraftColors = MinecraftColors(
    primary = Color(0xFF55FF55),
    backgroundDirt = Color(0xFF2C2C2C),
    surfaceFrame = Color(0xFF555555),
    surfaceHighlight = Color(0xFF888888),
    surfaceShadow = Color(0xFF555555),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFBBBBBB),

    controlDisabledBackground = Color(0xFF444444),
    controlBackground = Color(0xFF707070),
    controlBorder = Color(0xFF000000),
    controlHighlight = Color(0xFFBBBBBB),
    controlShadow = Color(0xFF3C3C3C),

    controlPrimaryDisabledBackground = Color(0xFF115511),
    controlPrimaryBackground = Color(0xFF33AA33),
    controlPrimaryBorder = Color(0xFF000000),
    controlPrimaryHighlight = Color(0xFF55FF55),
    controlPrimaryShadow = Color(0xFF005500),

    controlDestructiveDisabledBackground = Color(0xFF883333),
    controlDestructiveBackground = Color(0xFFBA3333),
    controlDestructiveHoveredBackground = Color(0xFFE05555),
    controlDestructiveBorder = Color(0xFF000000),
    controlDestructiveHighlight = Color(0xFFFF8888),
    controlDestructiveShadow = Color(0xFF771111),

    v1inputBackground = Color(0xFF000000),

    buttonPrimaryText = Color(0xFFEEEEEE),
    buttonPrimaryBackground = Color(0xFF128698),
    buttonPrimaryBorder = Color(0xFF2195A7),
    buttonPrimaryOuterBorder = Color(0xFF1E1E1F),
    buttonPrimaryShadow = Color(0xFF004E5B),

    buttonPrimaryHoverText = Color(0xFFEEEEEE),
    buttonPrimaryHoverBackground = Color(0xFF2DA1B3),
    buttonPrimaryHoverBorder = Color(0xFF39ADBF),
    buttonPrimaryHoverOuterBorder = Color(0xFF1E1E1F),
    buttonPrimaryHoverShadow = Color(0xFF004E5B),

    buttonPrimaryPressedText = Color(0xFFEEEEEE),
    buttonPrimaryPressedBackground = Color(0xFF006878),
    buttonPrimaryPressedBorder = Color(0xFF1C90A2),
    buttonPrimaryPressedOuterBorder = Color(0xFF1E1E1F),

    buttonSecondaryText = Color(0xFF1E1E1F),
    buttonSecondaryBackground = Color(0xFFD0D1D4),
    buttonSecondaryBorder = Color(0xFFE3E3E5),
    buttonSecondaryOuterBorder = Color(0xFF1E1E1F),
    buttonSecondaryShadow = Color(0xFF58585A),

    buttonSecondaryHoverText = Color(0xFF1E1E1F),
    buttonSecondaryHoverBackground = Color(0xFFF4F6F9),
    buttonSecondaryHoverBorder = Color(0xFFFBFBFD),
    buttonSecondaryHoverOuterBorder = Color(0xFF1E1E1F),
    buttonSecondaryHoverShadow = Color(0xFF58585A),

    buttonSecondaryPressedText = Color(0xFF1E1E1F),
    buttonSecondaryPressedBackground = Color(0xFFB1B2B5),
    buttonSecondaryPressedBorder = Color(0xFFE0E0E1),
    buttonSecondaryPressedOuterBorder = Color(0xFF1E1E1F),

    buttonDarkText = Color(0xFFEEEEEE),
    buttonDarkBackground = Color(0xFF343536),
    buttonDarkBorder = Color(0xFF292A2B),
    buttonDarkOuterBorder = Color(0xFF1E1E1F),
    buttonDarkShadow = Color(0xFF242526),

    buttonDarkHoverText = Color(0xFFEEEEEE),
    buttonDarkHoverBackground = Color(0xFF444546),
    buttonDarkHoverBorder = Color(0xFF424344),
    buttonDarkHoverOuterBorder = Color(0xFF1E1E1F),
    buttonDarkHoverShadow = Color(0xFF393A3B),

    buttonDarkPressedText = Color(0xFFEEEEEE),
    buttonDarkPressedBackground = Color(0xFF2A2B2C),
    buttonDarkPressedBorder = Color(0xFF1B1C1D),
    buttonDarkPressedOuterBorder = Color(0xFF1E1E1F),

    buttonDestructiveText = Color(0xFFEEEEEE),
    buttonDestructiveBackground = Color(0xFFCA3636),
    buttonDestructiveBorder = Color(0xFFCF4A4A),
    buttonDestructiveOuterBorder = Color(0xFF1E1E1F),
    buttonDestructiveShadow = Color(0xFFAD1D1D),

    buttonDestructiveHoverText = Color(0xFFEEEEEE),
    buttonDestructiveHoverBackground = Color(0xFFFF8080),
    buttonDestructiveHoverBorder = Color(0xFFFF9999),
    buttonDestructiveHoverOuterBorder = Color(0xFF1E1E1F),
    buttonDestructiveHoverShadow = Color(0xFFAD1D1D),

    buttonDestructivePressedText = Color(0xFFEEEEEE),
    buttonDestructivePressedBackground = Color(0xFFC02D2D),
    buttonDestructivePressedBorder = Color(0xFFCD5757),
    buttonDestructivePressedOuterBorder = Color(0xFF1E1E1F),

    buttonDisabledText = Color(0xFF48494A),
    buttonDisabledBackground = Color(0xFFD0D1D4),
    buttonDisabledBorder = Color(0x00000000),
    buttonDisabledOuterBorder = Color(0xFF8C8D90),
    buttonDisabledShadow = Color(0xFFB1B2B5),

    sidebarItemText = Color(0xFFDDDDDD),
    sidebarItemIcon = Color(0xFFDDDDDD),
    sidebarItemSelectedBar = Color(0xFFFFFFFFF),
    sidebarItemSelectedText = Color(0xFFFFFFFFF),
    sidebarItemSelectedIcon = Color(0xFFFFFFFFF),

    topAppBarBackground = Color(0xFF48494A),
    topAppBarBorder = Color(0xFF6D6D6E),
    topAppBarText = Color(0xFFEEEEEE),

    dialogBackground = Color(0xFF333333),
    dialogBorder = Color(0xFF1E1E1F),
    dialogContainerBackground = Color(0xFF48494A),

    listItemBackground = Color(0xFF48494A),
    listItemHighlight = Color(0xFF5A5B5C),
    listItemShadow = Color(0xFF333334),

    dropdownBackground = Color(0xFF333333),
    dropdownBorder = Color(0xFF1E1E1F),
    dropdownText = Color(0xFFEEEEEE),
    dropdownInnerBorder = Color(0xFF454647),

    inputBackground = Color(0xFF333333),
    inputBorder = Color(0xFF1E1E1F),
    inputText = Color(0xFFEEEEEE),
    inputTextHint = Color(0xFF888888),
    inputShadow = Color(0xFF252525),

    checkboxBackground = Color(0xFF333333),
    checkboxBorder = Color(0xFF1E1E1F),

    genericPrimary = Color(0xFF027688),
    genericDestructive = Color(0xFFFF5555)
)

val CaveDarkColors = MinecraftColors(
    primary = Color(0xFF55FFFF),
    backgroundDirt = Color(0xFF141414),
    surfaceFrame = Color(0xFF3C3C3C),
    surfaceHighlight = Color(0xFF555555),
    surfaceShadow = Color(0xFF333333),
    textPrimary = Color(0xFFDDDDDD),
    textSecondary = Color(0xFFAAAAAA),

    controlDisabledBackground = Color(0xFF222222),
    controlBackground = Color(0xFF404040),
    controlBorder = Color(0xFF000000),
    controlHighlight = Color(0xFF555555),
    controlShadow = Color(0xFF2C2C2C),

    controlPrimaryDisabledBackground = Color(0xFF111155),
    controlPrimaryBackground = Color(0xFF333399),
    controlPrimaryBorder = Color(0xFF000000),
    controlPrimaryHighlight = Color(0xFF5555FF),
    controlPrimaryShadow = Color(0xFF000055),
    
    controlDestructiveDisabledBackground = Color(0xFF662222),
    controlDestructiveBackground = Color(0xFF993333),
    controlDestructiveHoveredBackground = Color(0xFFBB4444),
    controlDestructiveBorder = Color(0xFF000000),
    controlDestructiveHighlight = Color(0xFFFF6666),
    controlDestructiveShadow = Color(0xFF550000),
    
    v1inputBackground = Color(0xFF000000),

    buttonPrimaryText = Color(0xFFEEEEEE),
    buttonPrimaryBackground = Color(0xFF3A6DB1),
    buttonPrimaryBorder = Color(0xFF497CC0),
    buttonPrimaryOuterBorder = Color(0xFF002453),
    buttonPrimaryShadow = Color(0xFF0D4084),

    buttonPrimaryHoverText = Color(0xFFEEEEEE),
    buttonPrimaryHoverBackground = Color(0xFF5588CC),
    buttonPrimaryHoverBorder = Color(0xFF6EA1E5),
    buttonPrimaryHoverOuterBorder = Color(0xFF002453),
    buttonPrimaryHoverShadow = Color(0xFF0D4084),

    buttonPrimaryPressedText = Color(0xFFEEEEEE),
    buttonPrimaryPressedBackground = Color(0xFF205296),
    buttonPrimaryPressedBorder = Color(0xFF4477BB),
    buttonPrimaryPressedOuterBorder = Color(0xFF002453),

    buttonSecondaryText = Color(0xFFEEEEEE),
    buttonSecondaryBackground = Color(0xFF3A6DB1),
    buttonSecondaryBorder = Color(0xFF497CC0),
    buttonSecondaryOuterBorder = Color(0xFF002453),
    buttonSecondaryShadow = Color(0xFF0D4084),

    buttonSecondaryHoverText = Color(0xFFEEEEEE),
    buttonSecondaryHoverBackground = Color(0xFF5588CC),
    buttonSecondaryHoverBorder = Color(0xFF6EA1E5),
    buttonSecondaryHoverOuterBorder = Color(0xFF002453),
    buttonSecondaryHoverShadow = Color(0xFF0D4084),

    buttonSecondaryPressedText = Color(0xFFEEEEEE),
    buttonSecondaryPressedBackground = Color(0xFF205296),
    buttonSecondaryPressedBorder = Color(0xFF4477BB),
    buttonSecondaryPressedOuterBorder = Color(0xFF002453),

    buttonDarkText = Color(0xFFEEEEEE),
    buttonDarkBackground = Color(0xFF343536),
    buttonDarkBorder = Color(0xFF212223),
    buttonDarkOuterBorder = Color(0xFF1E1E1F),
    buttonDarkShadow = Color(0xFF313233),

    buttonDarkHoverText = Color(0xFFEEEEEE),
    buttonDarkHoverBackground = Color(0xFF444546),
    buttonDarkHoverBorder = Color(0xFF424344),
    buttonDarkHoverOuterBorder = Color(0xFF1E1E1F),
    buttonDarkHoverShadow = Color(0xFF393A3B),

    buttonDarkPressedText = Color(0xFFEEEEEE),
    buttonDarkPressedBackground = Color(0xFF2A2B2C),
    buttonDarkPressedBorder = Color(0xFF1B1C1D),
    buttonDarkPressedOuterBorder = Color(0xFF1E1E1F),

    buttonDestructiveText = Color(0xFFEEEEEE),
    buttonDestructiveBackground = Color(0xFF3A6DB1),
    buttonDestructiveBorder = Color(0xFF497CC0),
    buttonDestructiveOuterBorder = Color(0xFF002453),
    buttonDestructiveShadow = Color(0xFF0D4084),

    buttonDestructiveHoverText = Color(0xFFEEEEEE),
    buttonDestructiveHoverBackground = Color(0xFF5588CC),
    buttonDestructiveHoverBorder = Color(0xFF6EA1E5),
    buttonDestructiveHoverOuterBorder = Color(0xFF002453),
    buttonDestructiveHoverShadow = Color(0xFF0D4084),

    buttonDestructivePressedText = Color(0xFFEEEEEE),
    buttonDestructivePressedBackground = Color(0xFF205296),
    buttonDestructivePressedBorder = Color(0xFF4477BB),
    buttonDestructivePressedOuterBorder = Color(0xFF002453),

    buttonDisabledText = Color(0xFF48494A),
    buttonDisabledBackground = Color(0xFFD0D1D4),
    buttonDisabledBorder = Color(0x00000000),
    buttonDisabledOuterBorder = Color(0xFF8C8D90),
    buttonDisabledShadow = Color(0xFFB1B2B5),

    sidebarItemText = Color(0xFFDDDDDD),
    sidebarItemIcon = Color(0xFFDDDDDD),
    sidebarItemSelectedBar = Color(0xFFFFFFFFF),
    sidebarItemSelectedText = Color(0xFFFFFFFFF),
    sidebarItemSelectedIcon = Color(0xFFFFFFFFF),

    topAppBarBackground = Color(0xFF48494A),
    topAppBarBorder = Color(0xFF6D6D6E),
    topAppBarText = Color(0xFFEEEEEE),

    dialogBackground = Color(0xFF313233),
    dialogBorder = Color(0xFF1E1E1F),
    dialogContainerBackground = Color(0xFF48494A),

    listItemBackground = Color(0xFF48494A),
    listItemHighlight = Color(0xFF5A5B5C),
    listItemShadow = Color(0xFF333334),

    dropdownBackground = Color(0xFF313233),
    dropdownBorder = Color(0xFF1E1E1F),
    dropdownText = Color(0xFFEEEEEE),
    dropdownInnerBorder = Color(0xFF454647),

    inputBackground = Color(0xFF333333),
    inputBorder = Color(0xFF1E1E1F),
    inputText = Color(0xFFEEEEEE),
    inputTextHint = Color(0xFF888888),
    inputShadow = Color(0xFF454647),

    checkboxBackground = Color(0xFF333333),
    checkboxBorder = Color(0xFF1E1E1F),

    genericPrimary = Color(0xFF027688),
    genericDestructive = Color(0xFFFF5555)
)
