package com.example.ui.games

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.MainViewModel
import com.example.ui.tilt3D
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

@Composable
fun GameScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val highScores by viewModel.highScores.collectAsState()
    var activeGameId by remember { mutableStateOf<String?>(null) } // "racing", "fruit", "archery", "cricket"

    LaunchedEffect(Unit) {
        viewModel.fetchHighScores()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        if (activeGameId == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Column {
                    Text(
                        text = "Retro Arcade",
                        color = Color(0xFF001D35),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Play offline physics-simulated casual games",
                        color = Color(0xFF44474E),
                        fontSize = 14.sp
                    )
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    item {
                        GameCard(
                            title = "Neon Highway",
                            badge = "2D Car Racing",
                            highScore = highScores["racing"] ?: 0,
                            icon = Icons.Filled.DirectionsCar,
                            tint = Color(0xFF0A6B3A),
                            onClick = { activeGameId = "racing" }
                        )
                    }
                    item {
                        GameCard(
                            title = "Fruit Slasher",
                            badge = "Slice Swipe",
                            highScore = highScores["fruit"] ?: 0,
                            icon = Icons.Filled.Restaurant,
                            tint = Color(0xFFBA1A1A),
                            onClick = { activeGameId = "fruit" }
                        )
                    }
                    item {
                        GameCard(
                            title = "Star Archer",
                            badge = "Bow Targets",
                            highScore = highScores["archery"] ?: 0,
                            icon = Icons.Filled.Send,
                            tint = Color(0xFFE28900),
                            onClick = { activeGameId = "archery" }
                        )
                    }
                    item {
                        GameCard(
                            title = "Hit Wicket",
                            badge = "Cricket Bowling",
                            highScore = highScores["cricket"] ?: 0,
                            icon = Icons.Filled.SportsCricket,
                            tint = Color(0xFF0061A4),
                            onClick = { activeGameId = "cricket" }
                        )
                    }
                }
            }
        } else {
            // Fullscreen tookover games viewport
            Box(modifier = Modifier.fillMaxSize()) {
                when (activeGameId) {
                    "racing" -> CarRacingGame(
                        highScore = highScores["racing"] ?: 0,
                        onClosed = { activeGameId = null },
                        onNewHighScore = { score -> viewModel.updateHighScore("racing", score) }
                    )
                    "fruit" -> FruitCutGame(
                        highScore = highScores["fruit"] ?: 0,
                        onClosed = { activeGameId = null },
                        onNewHighScore = { score -> viewModel.updateHighScore("fruit", score) }
                    )
                    "archery" -> ArcheryGame(
                        highScore = highScores["archery"] ?: 0,
                        onClosed = { activeGameId = null },
                        onNewHighScore = { score -> viewModel.updateHighScore("archery", score) }
                    )
                    "cricket" -> HitWicketGame(
                        highScore = highScores["cricket"] ?: 0,
                        onClosed = { activeGameId = null },
                        onNewHighScore = { score -> viewModel.updateHighScore("cricket", score) }
                    )
                }
            }
        }
    }
}

@Composable
fun GameCard(
    title: String,
    badge: String,
    highScore: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, tint.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .tilt3D(maxRotationX = 14f, maxRotationY = 14f)
            .clickable { onClick() }
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        tint.copy(alpha = 0.08f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(24.dp))
                }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(tint.copy(alpha = 0.11f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(badge, color = tint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column {
                Text(title, color = Color(0xFF001D35), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Color(0xFFE28900), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Top: $highScore", color = Color(0xFF44474E), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ======================== CAR RACING GAME ========================
@Composable
fun CarRacingGame(
    highScore: Int,
    onClosed: () -> Unit,
    onNewHighScore: (Int) -> Unit
) {
    var score by remember { mutableIntStateOf(0) }
    var gameActive by remember { mutableStateOf(true) }
    var gameOver by remember { mutableStateOf(false) }
    
    var playerX by remember { mutableFloatStateOf(0.5f) } // Road range: 0.15f to 0.85f
    
    // Obstacles
    val obstacles = remember { mutableStateListOf<Obstacle>() }
    
    var frameTick by remember { mutableStateOf(0L) }
    
    val speed = remember(score) {
        val baseSpeed = 0.012f
        baseSpeed + (score / 400.0f) // speed increases with score
    }

    LaunchedEffect(gameActive) {
        if (!gameActive) return@LaunchedEffect
        while (gameActive) {
            delay(24) // ~40 fps
            frameTick++

            // Spawner
            if (frameTick % 45 == 0L) {
                obstacles.add(
                    Obstacle(
                        x = Random.nextFloat() * 0.55f + 0.2f, // center spawn coordinates
                        y = 0f,
                        color = listOf(Color.Red, Color.Yellow, Color.Cyan, Color(0xFFF43F5E), Color(0xFFEC4899)).random()
                    )
                )
            }

            // Mover & Kollision
            val iterator = obstacles.iterator()
            while (iterator.hasNext()) {
                val obs = iterator.next()
                obs.y += speed
                if (obs.y >= 0.82f && obs.y <= 0.90f) {
                    // Collision check
                    if (abs(obs.x - playerX) < 0.14f) {
                        gameActive = false
                        gameOver = true
                        onNewHighScore(score)
                    }
                }
                if (obs.y > 1.1f) {
                    iterator.remove()
                    score += 50
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF151926))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClosed) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Close", tint = Color.White)
                }
                Text("NEON RACER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Score: $score", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            // Road Canvas Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Ground background
                    drawRect(color = Color(0xFF111317), size = size)

                    // Road background width
                    val roadWidth = w * 0.70f
                    val roadLeft = w * 0.15f
                    drawRect(
                        color = Color(0xFF1C1E24),
                        topLeft = Offset(roadLeft, 0f),
                        size = Size(roadWidth, h)
                    )

                    // Border markers (Yellow flashing lines)
                    val dividerYOffset = (frameTick * 12) % 100
                    for (y in -100 until h.toInt() step 100) {
                        drawLine(
                            color = Color.Yellow,
                            start = Offset(roadLeft, (y + dividerYOffset).toFloat()),
                            end = Offset(roadLeft, (y + dividerYOffset + 50).toFloat()),
                            strokeWidth = 4.dp.toPx()
                        )
                        drawLine(
                            color = Color.Yellow,
                            start = Offset(roadLeft + roadWidth, (y + dividerYOffset).toFloat()),
                            end = Offset(roadLeft + roadWidth, (y + dividerYOffset + 50).toFloat()),
                            strokeWidth = 4.dp.toPx()
                        )
                    }

                    // Middle road lanes (white ticking lines)
                    for (y in -100 until h.toInt() step 120) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.4f),
                            start = Offset(w / 2, (y + dividerYOffset).toFloat()),
                            end = Offset(w / 2, (y + dividerYOffset + 60).toFloat()),
                            strokeWidth = 3.dp.toPx()
                        )
                    }

                    // Render enemies
                    for (obs in obstacles) {
                        val obX = roadLeft + obs.x * roadWidth
                        val obY = obs.y * h

                        // Draw cyberpunk vehicle block
                        drawRoundRect(
                            color = obs.color,
                            topLeft = Offset(obX - 25.dp.toPx(), obY - 35.dp.toPx()),
                            size = Size(50.dp.toPx(), 70.dp.toPx()),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx())
                        )
                        // headlights
                        drawCircle(color = Color.Red, radius = 5.dp.toPx(), center = Offset(obX - 15.dp.toPx(), obY - 25.dp.toPx()))
                        drawCircle(color = Color.Red, radius = 5.dp.toPx(), center = Offset(obX + 15.dp.toPx(), obY - 25.dp.toPx()))
                    }

                    // Render player car at 85% depth
                    val pX = roadLeft + playerX * roadWidth
                    val pY = h * 0.85f

                    // Glowing cyan racing car
                    drawRoundRect(
                        color = Color(0xFF38BDF8),
                        topLeft = Offset(pX - 26.dp.toPx(), pY - 35.dp.toPx()),
                        size = Size(52.dp.toPx(), 72.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
                    )
                    // wind shield
                    drawRect(
                        color = Color.Black.copy(alpha = 0.6f),
                        topLeft = Offset(pX - 18.dp.toPx(), pY - 15.dp.toPx()),
                        size = Size(36.dp.toPx(), 20.dp.toPx())
                    )
                    // headlights (green)
                    drawCircle(color = Color.Green, radius = 6.dp.toPx(), center = Offset(pX - 16.dp.toPx(), pY - 30.dp.toPx()))
                    drawCircle(color = Color.Green, radius = 6.dp.toPx(), center = Offset(pX + 16.dp.toPx(), pY - 30.dp.toPx()))
                }
            }

            // Control panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF151926))
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { playerX = (playerX - 0.10f).coerceIn(0.08f, 0.92f) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.size(70.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "Left", tint = Color.White)
                }

                Button(
                    onClick = { playerX = (playerX + 0.10f).coerceIn(0.08f, 0.92f) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.size(70.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Filled.ArrowForwardIos, contentDescription = "Right", tint = Color.White)
                }
            }
        }

        if (gameOver) {
            Dialog(onDismissRequest = { gameOver = false }) {
                Card(
                    modifier = Modifier.padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2235)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("CRASH DAMAGE DETECTED", color = Color.Red, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Your performance record: $score points", color = Color.White)
                        
                        Button(
                            onClick = {
                                score = 0
                                obstacles.clear()
                                gameOver = false
                                gameActive = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Text("Restart Circuit")
                        }

                        OutlinedButton(onClick = onClosed) {
                            Text("Exit to Arcade", color = Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}

class Obstacle(var x: Float, var y: Float, val color: Color)

// ======================== FRUIT CUT GAME ========================
@Composable
fun FruitCutGame(
    highScore: Int,
    onClosed: () -> Unit,
    onNewHighScore: (Int) -> Unit
) {
    var score by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var gameActive by remember { mutableStateOf(true) }
    var gameOver by remember { mutableStateOf(false) }

    val fruits = remember { mutableStateListOf<ActiveFruit>() }

    // User swipe dragging lines coords
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragEnd by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(gameActive) {
        if (!gameActive) return@LaunchedEffect
        while (gameActive) {
            delay(30)

            // Spawn fruit periodically
            if (Random.nextInt(100) < 6 && fruits.size < 4) {
                fruits.add(
                    ActiveFruit(
                        x = Random.nextFloat() * 400 + 150,
                        y = 1100f,
                        vx = (Random.nextFloat() * 10 - 5),
                        vy = -(Random.nextFloat() * 14 + 18), // toss high
                        radius = 45f,
                        isSliced = false,
                        isBomb = Random.nextInt(100) < 18,
                        color = listOf(Color.Red, Color.Green, Color.Yellow, Color(0xFFF43F5E)).random()
                    )
                )
            }

            // Apply gravity equations
            val iterator = fruits.iterator()
            while (iterator.hasNext()) {
                val f = iterator.next()
                f.x += f.vx
                f.y += f.vy
                f.vy += 0.5f // gravity pull

                if (f.y > 1300f) {
                    // Falls off screen missed
                    if (!f.isSliced && !f.isBomb) {
                        lives--
                        if (lives <= 0) {
                            gameActive = false
                            gameOver = true
                            onNewHighScore(score)
                        }
                    }
                    iterator.remove()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0E14))
    ) {
        // Stats header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E2235))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClosed) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Close", tint = Color.White)
            }
            Text("Lives: $lives", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("FRUIT BLADE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Score: $score", color = Color(0xFFF43F5E), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        // Swipe canvas viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(gameActive) {
                    if (!gameActive) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragStart = offset
                        },
                        onDragEnd = {
                            dragStart = null
                            dragEnd = null
                        },
                        onDragCancel = {
                            dragStart = null
                            dragEnd = null
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val curStart = dragStart ?: return@detectDragGestures
                            val curEnd = curStart + dragAmount
                            dragStart = curEnd
                            dragEnd = curEnd

                            // Check collision of swipe coordinate with fruits
                            for (f in fruits) {
                                if (!f.isSliced) {
                                    val dist = sqrt((f.x - curEnd.x).pow(2) + (f.y - curEnd.y).pow(2))
                                    if (dist < f.radius + 15) {
                                        f.isSliced = true
                                        if (f.isBomb) {
                                            gameActive = false
                                            gameOver = true
                                            onNewHighScore(score)
                                        } else {
                                            score += 10
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background ambient glow
                drawRect(color = Color(0xFF0F111A))

                // Render active fruits
                for (f in fruits) {
                    if (f.isBomb) {
                        // Drawing retro spiked bomb visual
                        drawCircle(color = Color.DarkGray, radius = f.radius, center = Offset(f.x, f.y))
                        drawCircle(color = Color.Red, radius = 10f, center = Offset(f.x, f.y))
                        // fuse lines
                        drawLine(color = Color.Yellow, start = Offset(f.x, f.y), end = Offset(f.x + 20, f.y - 20), strokeWidth = 3f)
                    } else if (f.isSliced) {
                        // Draw two separate halves drifting apart
                        drawArc(
                            color = f.color,
                            startAngle = 45f,
                            sweepAngle = 180f,
                            useCenter = true,
                            topLeft = Offset(f.x - f.radius - 12, f.y - f.radius),
                            size = Size(f.radius * 2, f.radius * 2)
                        )
                        drawArc(
                            color = f.color,
                            startAngle = 225f,
                            sweepAngle = 180f,
                            useCenter = true,
                            topLeft = Offset(f.x - f.radius + 12, f.y - f.radius),
                            size = Size(f.radius * 2, f.radius * 2)
                        )
                    } else {
                        // Uncut circular glowing fruit
                        drawCircle(
                            color = f.color,
                            radius = f.radius,
                            center = Offset(f.x, f.y)
                        )
                        // core accent
                        drawCircle(
                            color = Color.White.copy(alpha = 0.3f),
                            radius = f.radius / 2.5f,
                            center = Offset(f.x - f.radius / 3f, f.y - f.radius / 3f)
                        )
                    }
                }

                // Render active drag slash blade trace
                val s = dragStart
                val e = dragEnd
                if (s != null && e != null) {
                    drawLine(
                        color = Color.Cyan,
                        start = s,
                        end = e,
                        strokeWidth = 6.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White,
                        start = s,
                        end = e,
                        strokeWidth = 3.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
        }
    }

    if (gameOver) {
        Dialog(onDismissRequest = { gameOver = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2235)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("GAME OVER DETECTED", color = Color.Red, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Total Sliced Capacity: $score", color = Color.White)
                    
                    Button(
                        onClick = {
                            score = 0
                            lives = 3
                            fruits.clear()
                            gameOver = false
                            gameActive = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Slice Again")
                    }

                    OutlinedButton(onClick = onClosed) {
                        Text("Exit to Arcade", color = Color.LightGray)
                    }
                }
            }
        }
    }
}

class ActiveFruit(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val radius: Float,
    var isSliced: Boolean,
    val isBomb: Boolean,
    val color: Color
)

// ======================== ARCHERY GAME ========================
@Composable
fun ArcheryGame(
    highScore: Int,
    onClosed: () -> Unit,
    onNewHighScore: (Int) -> Unit
) {
    var score by remember { mutableIntStateOf(0) }
    var arrowsLeft by remember { mutableIntStateOf(5) }
    var gameActive by remember { mutableStateOf(true) }
    var gameOver by remember { mutableStateOf(false) }

    // Bow mechanics coordinates
    var dragPullOffset by remember { mutableStateOf(Offset.Zero) }
    var isDraggingStr by remember { mutableStateOf(false) }

    // Bullet active Arrow vector
    var arrowFired by remember { mutableStateOf(false) }
    var arrowPos by remember { mutableStateOf(Offset.Zero) }
    var arrowVel by remember { mutableStateOf(Offset.Zero) }

    // Moving Bullseye target
    var targetY by remember { mutableFloatStateOf(300f) }
    var targetDirection by remember { mutableIntStateOf(1) }

    LaunchedEffect(gameActive) {
        if (!gameActive) return@LaunchedEffect
        while (gameActive) {
            delay(24)

            // Target slider animator
            targetY += targetDirection * 6f
            if (targetY > 900f || targetY < 200f) {
                targetDirection *= -1
            }

            // Arrow flight calculations
            if (arrowFired) {
                arrowPos += arrowVel
                arrowVel = Offset(arrowVel.x, arrowVel.y + 0.3f) // wind pull / gravity effect

                // Collision limits checks
                if (arrowPos.x > 950f) {
                    // Check hitting scoring boundaries
                    val dy = abs(arrowPos.y - targetY)
                    if (dy < 130f) {
                        // Hit target
                        val reward = when {
                            dy < 20f -> 100 // Bullseye!
                            dy < 60f -> 50
                            else -> 10
                        }
                        score += reward
                    }
                    // Reset
                    arrowFired = false
                    arrowsLeft--
                    if (arrowsLeft <= 0) {
                        gameActive = false
                        gameOver = true
                        onNewHighScore(score)
                    }
                } else if (arrowPos.y > 1400f || arrowPos.x < 0f) {
                    // Out of bounds miss
                    arrowFired = false
                    arrowsLeft--
                    if (arrowsLeft <= 0) {
                        gameActive = false
                        gameOver = true
                        onNewHighScore(score)
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F111A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E2235))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClosed) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Close", tint = Color.White)
            }
            Text("Arrows: $arrowsLeft", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("STAR ARCHER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Score: $score", color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(arrowFired, gameActive) {
                    if (arrowFired || !gameActive) return@pointerInput
                    detectDragGestures(
                        onDragStart = { isDraggingStr = true },
                        onDragEnd = {
                            isDraggingStr = false
                            // Fire Arrow with velocity proportionate to pull length
                            val dist = dragPullOffset.getDistance()
                            if (dist > 25f) {
                                arrowFired = true
                                arrowPos = Offset(140f, 500f)
                                // Velocity computation
                                val scale = 0.22f
                                arrowVel = Offset(-dragPullOffset.x * scale, -dragPullOffset.y * scale)
                            }
                            dragPullOffset = Offset.Zero
                        },
                        onDragCancel = {
                            isDraggingStr = false
                            dragPullOffset = Offset.Zero
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragPullOffset += dragAmount
                            // Clamp pull limit
                            if (dragPullOffset.getDistance() > 140f) {
                                val angle = atan2(dragPullOffset.y, dragPullOffset.x)
                                dragPullOffset = Offset(cos(angle) * 140f, sin(angle) * 140f)
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Render Bow target path
                drawLine(
                    color = Color(0x22FFFFFF),
                    start = Offset(140f, 0f),
                    end = Offset(140f, h),
                    strokeWidth = 1.dp.toPx()
                )

                // Render target moving block (red white target circle)
                val targetXPos = w * 0.88f
                drawCircle(color = Color.White, radius = 64f, center = Offset(targetXPos, targetY))
                drawCircle(color = Color.Red, radius = 46f, center = Offset(targetXPos, targetY), style = Stroke(width = 10f))
                drawCircle(color = Color.White, radius = 30f, center = Offset(targetXPos, targetY))
                drawCircle(color = Color.Yellow, radius = 14f, center = Offset(targetXPos, targetY))

                // Render Archery Bow base string curves
                val bowCenter = Offset(140f, 500f)
                drawArc(
                    color = Color(0xFFA5B4FC),
                    startAngle = -90f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(bowCenter.x - 70f, bowCenter.y - 120f),
                    size = Size(140f, 240f),
                    style = Stroke(width = 8f)
                )

                // Bow flexible rubber chord lines
                val pullX = bowCenter.x + dragPullOffset.x
                val pullY = bowCenter.y + dragPullOffset.y
                drawLine(color = Color.White, start = Offset(bowCenter.x, bowCenter.y - 120f), end = Offset(pullX, pullY), strokeWidth = 3f)
                drawLine(color = Color.White, start = Offset(bowCenter.x, bowCenter.y + 120f), end = Offset(pullX, pullY), strokeWidth = 3f)

                // Arrow visual rendering
                if (arrowFired) {
                    val angle = atan2(arrowVel.y, arrowVel.x)
                    val tipX = arrowPos.x + cos(angle) * 50f
                    val tipY = arrowPos.y + sin(angle) * 50f

                    drawLine(
                        color = Color(0xFFF59E0B),
                        start = arrowPos,
                        end = Offset(tipX, tipY),
                        strokeWidth = 6f
                    )
                    // point arrowhead
                    drawCircle(color = Color.Red, radius = 6f, center = Offset(tipX, tipY))
                } else if (isDraggingStr) {
                    // pre-fire aim line pointer vectors
                    val angle = atan2(dragPullOffset.y, dragPullOffset.x)
                    val arrowStart = Offset(pullX, pullY)
                    val arrowEnd = Offset(pullX - cos(angle) * 55f, pullY - sin(angle) * 55f)
                    
                    drawLine(
                        color = Color.White,
                        start = arrowStart,
                        end = arrowEnd,
                        strokeWidth = 6f
                    )
                    drawCircle(color = Color.Red, radius = 6f, center = arrowEnd)
                }
            }

            // Small pull hint text instructions overlays
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DRAG BACK String to tension bow, RELEASE finger to launch arrow",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (gameOver) {
        Dialog(onDismissRequest = { gameOver = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2235)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("ROUND FINISHED", color = Color(0xFFFBBF24), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Total hit points reward: $score points", color = Color.White)
                    
                    Button(
                        onClick = {
                            score = 0
                            arrowsLeft = 5
                            gameOver = false
                            gameActive = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Re-string Bow")
                    }

                    OutlinedButton(onClick = onClosed) {
                        Text("Exit to Arcade", color = Color.LightGray)
                    }
                }
            }
        }
    }
}

// ======================== HIT WICKET GAME ========================
@Composable
fun HitWicketGame(
    highScore: Int,
    onClosed: () -> Unit,
    onNewHighScore: (Int) -> Unit
) {
    var score by remember { mutableIntStateOf(0) }
    var bowlsCount by remember { mutableIntStateOf(6) } // 6 balls = 1 over limit
    var gameActive by remember { mutableStateOf(true) }
    var gameOver by remember { mutableStateOf(false) }

    // Ball physics trajectory
    var ballPos by remember { mutableStateOf(Offset(450f, 960f)) }
    var ballVel by remember { mutableStateOf(Offset.Zero) }
    var ballThrown by remember { mutableStateOf(false) }
    
    // Stumps (Wickets configuration)
    // Left Stump: X = 400, Mid: X = 450, Right: X = 500. Stumps height: Y goes from 200f to 320f.
    var stumpsHit by remember { mutableStateOf(false) }
    var bailsYOffset by remember { mutableFloatStateOf(0f) }
    var showHitPrompt by remember { mutableStateOf(false) }

    LaunchedEffect(gameActive) {
        if (!gameActive) return@LaunchedEffect
        while (gameActive) {
            delay(24)

            if (ballThrown) {
                ballPos += ballVel
                // bounce effect halfway up screen depth
                if (ballPos.y in 480f..530f && ballVel.y < 0) {
                    ballVel = Offset(ballVel.x, ballVel.y * 0.95f) // slow down slightly on bounce pitch
                }

                // Check intersection hitting stumps block coordinates
                if (ballPos.y <= 280f) {
                    if (ballPos.y >= 200f && ballPos.x in 370f..530f && !stumpsHit) {
                        stumpsHit = true
                        showHitPrompt = true
                        bailsYOffset = -25f // bails fly off upwards
                        score += 1
                    }

                    // Reset when ball passes boundary
                    if (ballPos.y < 50f) {
                        delay(1200)
                        ballPos = Offset(450f, 960f)
                        ballVel = Offset.Zero
                        ballThrown = false
                        stumpsHit = false
                        showHitPrompt = false
                        bailsYOffset = 0f
                        
                        bowlsCount--
                        if (bowlsCount <= 0) {
                            gameActive = false
                            gameOver = true
                            onNewHighScore(score)
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F111A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E2235))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClosed) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Close", tint = Color.White)
            }
            Text("Over limit: $bowlsCount balls left", color = Color(0xFFA5B4FC), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("HIT WICKET CRICKET", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Matches Hit: $score", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(ballThrown, gameActive) {
                    if (ballThrown || !gameActive) return@pointerInput
                    detectDragGestures(
                        onDragStart = {},
                        onDragEnd = {},
                        onDragCancel = {},
                        onDrag = { change, dragAmount ->
                            change.consume()
                            // If user flicks fast upwards
                            if (dragAmount.y < -15f) {
                                ballThrown = true
                                ballVel = Offset(dragAmount.x * 0.42f, dragAmount.y * 0.65f) // ball throw vectors
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Pitch Lane outlines
                val path = Path().apply {
                    moveTo(w * 0.30f, h)
                    lineTo(w * 0.42f, 200f)
                    lineTo(w * 0.58f, 200f)
                    lineTo(w * 0.70f, h)
                    close()
                }
                drawPath(path = path, color = Color(0xFF192015)) // dark green grass pitch lane

                // crease white lines
                drawLine(color = Color.White, start = Offset(w * 0.35f, 320f), end = Offset(w * 0.65f, 320f), strokeWidth = 4f)

                // Stumps details
                // Left stump
                drawRoundRect(
                    color = Color(0xFF92400E),
                    topLeft = Offset(w / 2 - 40f, 200f),
                    size = Size(10f, 130f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
                )
                // Middle stump
                drawRoundRect(
                    color = Color(0xFF92400E),
                    topLeft = Offset(w / 2 - 5f, 200f),
                    size = Size(10f, 130f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
                )
                // Right stump
                drawRoundRect(
                    color = Color(0xFF92400E),
                    topLeft = Offset(w / 2 + 30f, 200f),
                    size = Size(10f, 130f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
                )

                // Horizontal Bails on top
                val bailY = 192f + bailsYOffset
                // Bail 1
                drawRoundRect(
                    color = Color(0xFFF43F5E),
                    topLeft = Offset(w / 2 - 45f, bailY),
                    size = Size(40f, 8f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f)
                )
                // Bail 2
                drawRoundRect(
                    color = Color(0xFFF43F5E),
                    topLeft = Offset(w / 2 + 5f, bailY),
                    size = Size(40f, 8f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f)
                )

                // Render current Cricket ball (red circular vector)
                val ballScale = if (ballThrown) {
                    // shrink ball slightly as it travels back into screen space depth
                    max(12f, 32f * (ballPos.y / h))
                } else {
                    32f
                }
                
                drawCircle(
                    color = Color(0xFFEF4444),
                    radius = ballScale,
                    center = if (ballThrown) ballPos else Offset(w / 2, h - 80f)
                )
                // seam line
                drawCircle(
                    color = Color.White.copy(alpha = 0.6f),
                    radius = ballScale,
                    center = if (ballThrown) ballPos else Offset(w / 2, h - 80f),
                    style = Stroke(width = 2f)
                )
            }

            if (showHitPrompt) {
                Text(
                    text = "STUMP HIT! WICKET CRASH!",
                    color = Color.Green,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }

            Text(
                text = "FLICK OR SWIPE BALL UPWARD to bowl and smash the wickets stumps!",
                color = Color.LightGray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
            )
        }
    }

    if (gameOver) {
        Dialog(onDismissRequest = { gameOver = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2235)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("OVER FINISHED", color = Color(0xFF10B981), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("System hit count score: $score successful wickets matches", color = Color.White, textAlign = TextAlign.Center)
                    
                    Button(
                        onClick = {
                            score = 0
                            bowlsCount = 6
                            gameOver = false
                            gameActive = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Bowl Next Over")
                    }

                    OutlinedButton(onClick = onClosed) {
                        Text("Exit to Arcade", color = Color.LightGray)
                    }
                }
            }
        }
    }
}
