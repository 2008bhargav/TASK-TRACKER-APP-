package com.example.ui.calculator

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.tilt3D
import java.util.*
import kotlin.math.*

@Composable
fun CalculatorScreen(
    modifier: Modifier = Modifier
) {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }

    val onKeyboardClick: (String) -> Unit = { character ->
        when (character) {
            "C" -> {
                expression = ""
                result = ""
            }
            "⌫" -> {
                if (expression.isNotEmpty()) {
                    expression = expression.substring(0, expression.length - 1)
                }
            }
            "=" -> {
                if (expression.isNotBlank()) {
                    val eval = MathParser.evaluate(expression)
                    result = if (eval.isNaN()) {
                        "Error"
                    } else {
                        val formatted = String.format(Locale.getDefault(), "%.6f", eval)
                        // Trip trailing zeros
                        if (formatted.contains(".")) {
                            formatted.replace(Regex("0*$"), "").replace(Regex("\\.$"), "")
                        } else {
                            formatted
                        }
                    }
                }
            }
            "sin", "cos", "tan", "√" -> {
                expression += "$character("
            }
            else -> {
                expression += character
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "3D Calculator",
                    color = Color(0xFF001D35),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Super tactile advanced scientific parser",
                    color = Color(0xFF44474E),
                    fontSize = 12.sp
                )
            }
        }

        // Core Display LCD Panel (3D-like, inset)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 12.dp)
                .tilt3D(maxRotationX = 8f, maxRotationY = 8f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE0F7FA),
                            Color(0xFFB2EBF2)
                        )
                    )
                )
                .border(androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF00838F).copy(alpha = 0.25f)), RoundedCornerShape(24.dp))
                .padding(20.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Formula field
                Text(
                    text = expression.ifEmpty { "0" },
                    color = Color(0xFF006064),
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
                // Result field
                Text(
                    text = result.ifEmpty { "0" },
                    color = Color(0xFF004D40),
                    fontSize = 42.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Custom 3D Keyboard layout
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val rows = listOf(
                listOf("sin", "cos", "tan", "√"),
                listOf("C", "(", ")", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "-"),
                listOf("1", "2", "3", "+"),
                listOf("0", ".", "⌫", "=")
            )

            for (row in rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (char in row) {
                        val isOp = char in listOf("+", "-", "×", "÷", "=", "sin", "cos", "tan", "√", "(", ")")
                        val isAction = char in listOf("C", "⌫")
                        
                        val faceColor = when {
                            char == "=" -> Color(0xFFC2EFD3) // Sleek Green
                            isOp -> Color(0xFFD3E4FF) // Sleek soft blue
                            isAction -> Color(0xFFFFD8E4) // Sleek soft pink
                            else -> Color(0xFFFFFFFF) // Sleek pure white
                        }

                        val shadowColor = when {
                            char == "=" -> Color(0xFF8CD8A7)
                            isOp -> Color(0xFFB0CDE8)
                            isAction -> Color(0xFFF2B8B5)
                            else -> Color(0xFFCAC4D0).copy(alpha = 0.8f)
                        }

                        val textColor = when {
                            char == "=" -> Color(0xFF0A6B3A)
                            isOp -> Color(0xFF001D35)
                            isAction -> Color(0xFFBA1A1A)
                            else -> Color(0xFF1A1C1E)
                        }

                        Button3D(
                            text = char,
                            onClick = { onKeyboardClick(char) },
                            faceColor = faceColor,
                            shadowColor = shadowColor,
                            textColor = textColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Button3D(
    text: String,
    onClick: () -> Unit,
    faceColor: Color,
    shadowColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    // Surface elevation animations
    val currentElevation: Dp by animateDpAsState(
        targetValue = if (isPressed) 1.dp else 6.dp,
        animationSpec = tween(durationMillis = 60)
    )

    Box(
        modifier = modifier
            .height(58.dp)
            .pointerInput(text) {
                detectTapGestures(
                    onPress = {
                        try {
                            isPressed = true
                            awaitRelease()
                        } finally {
                            isPressed = false
                            onClick()
                        }
                    }
                )
            }
            .clip(RoundedCornerShape(12.dp))
            .background(shadowColor)
    ) {
        // Front plate (dynamic offset based on keypresses)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .offset(y = (-1).dp * currentElevation.value.coerceAtLeast(0f))
                .clip(RoundedCornerShape(12.dp))
                .background(faceColor)
                .border(
                    if (faceColor == Color(0xFFFFFFFF)) 
                        androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f))
                    else 
                        androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = if (text.length > 2) 13.sp else 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

object MathParser {
    fun evaluate(expression: String): Double {
        return try {
            val formatted = expression
                .replace("×", "*")
                .replace("÷", "/")
                .replace("sin", "s")
                .replace("cos", "c")
                .replace("tan", "t")
                .replace("√", "q")
            
            Parser(formatted).parse()
        } catch (e: Exception) {
            Double.NaN
        }
    }

    private class Parser(val str: String) {
        var pos = -1
        var ch = 0

        fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
        }

        fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < str.length) throw RuntimeException("Unexpected index")
            return x
        }

        fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                if (eat('+'.code)) x += parseTerm()
                else if (eat('-'.code)) x -= parseTerm()
                else break
            }
            return x
        }

        fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                if (eat('*'.code)) x *= parseFactor()
                else if (eat('/'.code)) x /= parseFactor()
                else if (eat('%'.code)) x %= parseFactor()
                else break
            }
            return x
        }

        fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = this.pos
            if (eat('('.code)) {
                x = parseExpression()
                eat(')'.code)
            } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                x = str.substring(startPos, this.pos).toDouble()
            } else if (eat('s'.code)) {
                x = parseFactor()
                x = sin(Math.toRadians(x))
            } else if (eat('c'.code)) {
                x = parseFactor()
                x = cos(Math.toRadians(x))
            } else if (eat('t'.code)) {
                x = parseFactor()
                x = tan(Math.toRadians(x))
            } else if (eat('q'.code)) {
                x = parseFactor()
                if (x < 0) throw RuntimeException("Square root of negative")
                x = sqrt(x)
            } else {
                throw RuntimeException("Unknown expression symbol")
            }

            if (eat('^'.code)) {
                x = x.pow(parseFactor())
            }

            return x
        }
    }
}
