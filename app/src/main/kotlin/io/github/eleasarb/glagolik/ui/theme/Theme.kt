package io.github.eleasarb.glagolik.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Burgundy = Color(0xFF8C1D2F)
private val BurgundyLight = Color(0xFFFFB3B6)
private val Slate = Color(0xFF3F4A5A)
private val SlateLight = Color(0xFFBCC7DB)

/** Accent for new and learning forms, clearly apart from burgundy. */
private val Amber = Color(0xFF8A5100)
private val AmberLight = Color(0xFFFFB870)

private val LightColors = lightColorScheme(
    primary = Burgundy,
    secondary = Slate,
    tertiary = Amber,
)

private val DarkColors = darkColorScheme(
    primary = BurgundyLight,
    secondary = SlateLight,
    tertiary = AmberLight,
)

@Composable
fun GlagolikTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors

        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
