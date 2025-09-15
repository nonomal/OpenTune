package com.arturo254.opentune.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score

val DefaultThemeColor = Color(0xFF4285F4)

@Composable
fun OpenTuneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    albumBitmap: Bitmap? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    // Decide el color base
    val baseColor = remember(albumBitmap, themeColor) {
        albumBitmap?.extractThemeColor() ?: themeColor
    }

    val useSystemDynamicColor = (baseColor == DefaultThemeColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

    // Genera el ColorScheme
    val baseColorScheme = if (useSystemDynamicColor) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        rememberDynamicColorScheme(
            seedColor = baseColor,
            isDark = darkTheme,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            style = PaletteStyle.TonalSpot
        )
    }

    val colorScheme = remember(baseColorScheme, pureBlack, darkTheme) {
        if (darkTheme && pureBlack) baseColorScheme.pureBlack(true) else baseColorScheme
    }

    // ✅ Usamos MaterialTheme estable
    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        shapes = androidx.compose.material3.Shapes(),
        content = content
    )
}

// Extrae el color dominante de un Bitmap
fun Bitmap.extractThemeColor(): Color {
    val colorsToPopulation = Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .swatches
        .associate { it.rgb to it.population }
    val rankedColors = Score.score(colorsToPopulation)
    return if (rankedColors.isNotEmpty()) Color(rankedColors.first()) else DefaultThemeColor
}

// Extrae colores para gradientes desde un Bitmap
fun Bitmap.extractGradientColors(): List<Color> {
    val extractedColors = Palette.from(this)
        .maximumColorCount(64)
        .generate()
        .swatches
        .associate { it.rgb to it.population }

    val orderedColors = Score.score(extractedColors, 2, DefaultThemeColor.toArgb(), true)
        .sortedByDescending { Color(it).luminance() }

    return if (orderedColors.size >= 2)
        listOf(Color(orderedColors[0]), Color(orderedColors[1]))
    else
        listOf(Color(0xFF595959), Color(0xFF0D0D0D))
}

// Extensión para aplicar pureBlack
fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

// Saver para Color
val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
