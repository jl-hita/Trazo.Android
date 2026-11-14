package com.trazo.android.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Palette taken from the TRAZO administration web app, so both clients look the same. */
object TrazoPalette {
    val Background = Color(0xFF0D1119)
    val Card = Color(0xFF131B27)
    val Sidebar = Color(0xFF111722)
    val Border = Color(0xFF293444)
    val InputBorder = Color(0xFF34465B)
    val Focus = Color(0xFF6D8EB5)
    val Text = Color(0xFFE8EDF5)
    val TextSecondary = Color(0xFFAAB6C7)
    val Muted = Color(0xFF7F8DA0)
    val Eyebrow = Color(0xFF8291A6)
    val Link = Color(0xFF8FB0D6)
    val PrimaryButton = Color(0xFF3B6EA8)
    val PrimaryButtonBorder = Color(0xFF6D9BD2)
    val SecondaryButton = Color(0xFF1B2A3E)
    val SecondaryButtonBorder = Color(0xFF4A6585)
    val SecondaryButtonText = Color(0xFFE4EDF8)
    val Error = Color(0xFFF0A3A3)
    val Ok = Color(0xFF7FD3A0)
    val WarningBackground = Color(0xFF2A2412)
    val WarningBorder = Color(0xFF6B5A2E)
    val WarningText = Color(0xFFE6D39B)
    val Teal = Color(0xFF00D4B4)
}

private val TrazoColorScheme = darkColorScheme(
    primary = TrazoPalette.PrimaryButton,
    onPrimary = Color.White,
    secondary = TrazoPalette.SecondaryButton,
    onSecondary = TrazoPalette.SecondaryButtonText,
    background = TrazoPalette.Background,
    onBackground = TrazoPalette.Text,
    surface = TrazoPalette.Card,
    onSurface = TrazoPalette.Text,
    surfaceVariant = TrazoPalette.SecondaryButton,
    onSurfaceVariant = TrazoPalette.TextSecondary,
    outline = TrazoPalette.InputBorder,
    outlineVariant = TrazoPalette.Border,
    error = TrazoPalette.Error,
    onError = TrazoPalette.Background
)

@Composable
fun TrazoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TrazoColorScheme,
        shapes = Shapes(
            small = RoundedCornerShape(6.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(10.dp)
        ),
        content = content
    )
}

/** Small uppercase label with wide tracking, shown above titles (the "eyebrow" of the web app). */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        color = TrazoPalette.Eyebrow,
        style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
    )
}

@Composable
fun TrazoField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
    minLines: Int = 1
) = OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    label = label,
    keyboardOptions = keyboardOptions,
    visualTransformation = visualTransformation,
    singleLine = singleLine,
    minLines = if (singleLine) 1 else minLines,
    maxLines = if (singleLine) 1 else 6,
    shape = RoundedCornerShape(6.dp),
    colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = TrazoPalette.Focus,
        unfocusedBorderColor = TrazoPalette.InputBorder,
        focusedContainerColor = TrazoPalette.Background,
        unfocusedContainerColor = TrazoPalette.Background,
        focusedTextColor = TrazoPalette.Text,
        unfocusedTextColor = TrazoPalette.Text,
        focusedLabelColor = TrazoPalette.TextSecondary,
        unfocusedLabelColor = TrazoPalette.TextSecondary,
        cursorColor = TrazoPalette.Link
    )
)

@Composable
fun TrazoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    secondary: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val container = if (secondary) TrazoPalette.SecondaryButton else TrazoPalette.PrimaryButton
    val border = if (secondary) TrazoPalette.SecondaryButtonBorder else TrazoPalette.PrimaryButtonBorder
    val text = if (secondary) TrazoPalette.SecondaryButtonText else Color.White
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, if (enabled) border else border.copy(alpha = 0.45f)),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = text,
            disabledContainerColor = container.copy(alpha = 0.45f),
            disabledContentColor = text.copy(alpha = 0.6f)
        ),
        content = content
    )
}

@Composable
fun TrazoLink(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) = TextButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    colors = ButtonDefaults.textButtonColors(
        contentColor = TrazoPalette.Link,
        disabledContentColor = TrazoPalette.Muted
    ),
    content = content
)
