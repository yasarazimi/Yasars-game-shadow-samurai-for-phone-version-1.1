package com.example

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.AIDifficulty
import com.example.game.FighterState
import com.example.game.GameEngine
import com.example.game.GameMode
import com.example.game.GameRenderer
import com.example.game.GameTouchButton
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val gameEngine = GameEngine()
    private val gameRenderer = GameRenderer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Force landscape orientation for true fighting game experience
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF0F172A) // Dark void slate
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        GameScreen(gameEngine, gameRenderer)
                    }
                }
            }
        }
    }
}

@Composable
fun GameScreen(engine: GameEngine, renderer: GameRenderer) {
    var showMenu by remember { mutableStateOf(true) }
    var runningMode by remember { mutableStateOf(GameMode.PLAYER_VS_AI) }
    var selectedDifficulty by remember { mutableStateOf(AIDifficulty.MEDIUM) }
    
    val scope = rememberCoroutineScope()
    
    // 60 FPS Core Game loop clock activation
    LaunchedEffect(showMenu, engine.isGameOver) {
        if (!showMenu && !engine.isGameOver) {
            engine.startTimer(this)
            while (!showMenu && !engine.isGameOver) {
                engine.updateFrame()
                delay(16) // roughly 60 fps
            }
        }
    }

    if (showMenu) {
        MainMenu(
            runningMode = runningMode,
            difficulty = selectedDifficulty,
            onModeChange = { runningMode = it },
            onDifficultyChange = { selectedDifficulty = it },
            onStartGame = {
                engine.startGame(runningMode, selectedDifficulty)
                showMenu = false
            }
        )
    } else {
        ArenaLayout(
            engine = engine,
            renderer = renderer,
            onBackToMenu = {
                showMenu = true
                engine.resetGame()
            }
        )
    }
}

@Composable
fun MainMenu(
    runningMode: GameMode,
    difficulty: AIDifficulty,
    onModeChange: (GameMode) -> Unit,
    onDifficultyChange: (AIDifficulty) -> Unit,
    onStartGame: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1E1B4B), Color(0xFF03001C)),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 840.dp)
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title Header with retro neon styling
            Text(
                text = "SAMURAI SHOWDOWN",
                fontSize = 44.sp,
                color = Color(0xFFF43F5E),
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .border(4.dp, Color.White)
                    .background(Color.Black)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "RETRO 2D FIGHTING BRAWLER • PORTED TO ANDROID NATIVE",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Game Mode Card Choice - 60% Width
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x661E1E38)),
                    modifier = Modifier
                        .weight(1.3f)
                        .border(1.dp, Color(0xFF334155)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "SELECT BATTLE STYLE",
                            color = Color(0xFFF1F5F9),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Modes Rows
                        GameModeRow(
                            title = "PLAYER vs CPU AI",
                            desc = "Fight against local training bots to hone your blades",
                            isSelected = runningMode == GameMode.PLAYER_VS_AI,
                            onClick = { onModeChange(GameMode.PLAYER_VS_AI) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        GameModeRow(
                            title = "LOCAL 2-PLAYER VS SEC",
                            desc = "Split touchscreen overlay controls for dual couch pvp duels",
                            isSelected = runningMode == GameMode.LOCAL_VS,
                            onClick = { onModeChange(GameMode.LOCAL_VS) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        GameModeRow(
                            title = "AI SPECTATOR MODE",
                            desc = "Sit back & watch Samurai Mack battle Kenji in real time",
                            isSelected = runningMode == GameMode.AI_VS_AI,
                            onClick = { onModeChange(GameMode.AI_VS_AI) }
                        )
                    }
                }

                // AI Setup Parameters - 40% Width
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x661E1E38)),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, Color(0xFF334155)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "AI TRAINING LEVEL",
                            color = Color(0xFFF1F5F9),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        DifficultyOption(
                            levelName = "EASY (RECRU)",
                            desc = "Slow attack patterns, generous dodge windows",
                            isSelected = difficulty == AIDifficulty.EASY,
                            onClick = { onDifficultyChange(AIDifficulty.EASY) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DifficultyOption(
                            levelName = "MEDIUM (RONIN)",
                            desc = "Intermediate active tracking and dodging rates",
                            isSelected = difficulty == AIDifficulty.MEDIUM,
                            onClick = { onDifficultyChange(AIDifficulty.MEDIUM) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DifficultyOption(
                            levelName = "HARD (SHOGUN)",
                            desc = "Ultra-fast reaction, predictive sword blocks",
                            isSelected = difficulty == AIDifficulty.HARD,
                            onClick = { onDifficultyChange(AIDifficulty.HARD) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Massive Retro Start Battle Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(320.dp)
                    .height(64.dp)
                    .background(Color(0xFFE11D48)) // Vibrant Red Rose 600
                    .border(4.dp, Color.White)
                    .clickable { onStartGame() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "START SHOWDOWN",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun GameModeRow(
    title: String,
    desc: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) Color(0xFF1E1E38) else Color.Transparent)
            .border(2.dp, if (isSelected) Color(0xFFF43F5E) else Color(0x3364748B))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pixel radio indicator
        Box(
            modifier = Modifier
                .size(16.dp)
                .border(2.dp, if (isSelected) Color(0xFFF43F5E) else Color(0xFF64748B))
                .background(if (isSelected) Color(0xFFF43F5E) else Color.Transparent)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                color = if (isSelected) Color(0xFFF43F5E) else Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                color = Color(0xFF64748B),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun DifficultyOption(
    levelName: String,
    desc: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, if (isSelected) Color(0xFFF43F5E) else Color(0x2264748B))
            .background(if (isSelected) Color(0x33F43F5E) else Color.Transparent)
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Text(
            text = levelName,
            color = if (isSelected) Color(0xFFF43F5E) else Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = desc,
            color = Color(0xFF64748B),
            fontSize = 8.sp,
            lineHeight = 10.sp
        )
    }
}

@Composable
fun ArenaLayout(
    engine: GameEngine,
    renderer: GameRenderer,
    onBackToMenu: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    // Smooth animated health floats
    val p1HealthAnim by animateFloatAsState(targetValue = engine.player1.health.toFloat() / 100f, label = "p1Health")
    val p2HealthAnim by animateFloatAsState(targetValue = engine.player2.health.toFloat() / 100f, label = "p2Health")

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020208)),
        contentAlignment = Alignment.Center
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // HUD Row - Directly mirroring the HTML/CSS Double White-Border structure
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                
                // Player 1 Health Bar Wrapper (Mirrors Right-to-Left depletion)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .background(Color(0xFFEF4444)) // Depleted damage background (Red)
                        .border(4.dp, Color.White)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(p1HealthAnim) // Depletes smoothly
                            .background(Color(0xFF818CF8)) // Active health overlay blue-purple
                            .align(Alignment.CenterEnd) // Deplates from right-to-left
                    )
                    
                    // Small character label left-attached
                    Text(
                        text = "MACK (P1)",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 12.dp)
                    )
                }

                // Double Bordered Timer Block in Center
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .size(width = 100.dp, height = 50.dp)
                        .background(Color.Black)
                        .border(4.dp, Color.White)
                ) {
                    Text(
                        text = "${engine.timer}",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Player 2 Health Bar Wrapper (Mirrors Left-to-Right depletion)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .background(Color(0xFFEF4444)) // Depleted damage background (Red)
                        .border(4.dp, Color.White)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(p2HealthAnim) // Depletes smoothly
                            .background(Color(0xFF818CF8)) // Active health overlay blue-purple
                            .align(Alignment.CenterStart) // Deplates left-to-right
                    )
                    
                    // Small character label right-attached
                    Text(
                        text = "KENJI (P2)",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp)
                    )
                }
            }

            // CORE FIGHTING CANVAS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp)
                    .border(4.dp, Color.White)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(1024f / 576f)
                ) {
                    renderer.drawGame(this, engine, size.width, size.height)
                }

                // Victory/Death State Banner overlay
                MatchCompletionOverlay(
                    isGameOver = engine.isGameOver,
                    winnerText = engine.winnerText,
                    onReset = { engine.resetGame() },
                    onBackToMenu = onBackToMenu
                )
            }

            // CONTROLLER ACTIONS DOCK - Adapts to chosen Game Mode dynamically
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp, vertical = 16.dp)
            ) {
                if (engine.isGameOver) {
                    // Match Over actions displayed simply
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FIGHT FINISHED. SELECT AN ACTION ABOVE",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    when (engine.mode) {
                        GameMode.PLAYER_VS_AI -> {
                            // Player 1 controller active, AI controls enemy.
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // P1 Movements Buttons (Left-hand side)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    GameTouchButton(
                                        text = "A",
                                        accentColor = Color(0xFFFF8A8A),
                                        onDown = {
                                            engine.p1LeftPressed = true
                                            engine.player1.lastKey = "a"
                                        },
                                        onUp = { engine.p1LeftPressed = false }
                                    )
                                    GameTouchButton(
                                        text = "D",
                                        accentColor = Color(0xFFFF8A8A),
                                        onDown = {
                                            engine.p1RightPressed = true
                                            engine.player1.lastKey = "d"
                                        },
                                        onUp = { engine.p1RightPressed = false }
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .border(1.dp, Color(0xFF334155))
                                            .background(Color(0xFF0F172A))
                                            .clickable { engine.resetGame() }
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Text("RESTART", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .border(1.dp, Color(0xFF334155))
                                            .background(Color(0xFF0F172A))
                                            .clickable { onBackToMenu() }
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Text("MENU", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                }

                                // P1 Action triggers (Right-hand side)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // JUMP (W)
                                    GameTouchButton(
                                        text = "JUMP",
                                        accentColor = Color(0xFFFFC107),
                                        onDown = {
                                            if (engine.player1.velocityY == 0f) {
                                                engine.player1.velocityY = -20f
                                            }
                                        },
                                        onUp = {}
                                    )
                                    // ATTACK (SPACE)
                                    GameTouchButton(
                                        text = "ATTACK",
                                        accentColor = Color(0xFFEF4444),
                                        onDown = { engine.player1.attack() },
                                        onUp = {}
                                    )
                                }
                            }
                        }

                        GameMode.LOCAL_VS -> {
                            // Two dynamic set Controllers: P1 Left side, P2 Right side
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // P1 FULL PANEL (L,R, J, A)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    GameTouchButton(
                                        text = "A",
                                        onDown = {
                                            engine.p1LeftPressed = true
                                            engine.player1.lastKey = "a"
                                        },
                                        onUp = { engine.p1LeftPressed = false }
                                    )
                                    GameTouchButton(
                                        text = "D",
                                        onDown = {
                                            engine.p1RightPressed = true
                                            engine.player1.lastKey = "d"
                                        },
                                        onUp = { engine.p1RightPressed = false }
                                    )
                                    GameTouchButton(
                                        text = "JMP",
                                        accentColor = Color(0xFFFFC107),
                                        onDown = {
                                            if (engine.player1.velocityY == 0f) {
                                                engine.player1.velocityY = -20f
                                            }
                                        },
                                        onUp = {}
                                    )
                                    GameTouchButton(
                                        text = "ATC",
                                        accentColor = Color(0xFFEF4444),
                                        onDown = { engine.player1.attack() },
                                        onUp = {}
                                    )
                                }

                                // Quick Exit helper buttons
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .border(1.dp, Color(0xFF334155))
                                            .background(Color(0xFF0F172A))
                                            .clickable { engine.resetGame() }
                                            .padding(6.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .border(1.dp, Color(0xFF334155))
                                            .background(Color(0xFF0F172A))
                                            .clickable { onBackToMenu() }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("MENU", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                }

                                // P2 FULL PANEL (◀, ▶, ▲, ⚔)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    GameTouchButton(
                                        text = "◀",
                                        accentColor = Color(0xFF818CF8),
                                        onDown = {
                                            engine.p2LeftPressed = true
                                            engine.player2.lastKey = "ArrowLeft"
                                        },
                                        onUp = { engine.p2LeftPressed = false }
                                    )
                                    GameTouchButton(
                                        text = "▶",
                                        accentColor = Color(0xFF818CF8),
                                        onDown = {
                                            engine.p2RightPressed = true
                                            engine.player2.lastKey = "ArrowRight"
                                        },
                                        onUp = { engine.p2RightPressed = false }
                                    )
                                    GameTouchButton(
                                        text = "JMP",
                                        accentColor = Color(0xFFFFC107),
                                        onDown = {
                                            if (engine.player2.velocityY == 0f) {
                                                engine.player2.velocityY = -20f
                                            }
                                        },
                                        onUp = {}
                                    )
                                    GameTouchButton(
                                        text = "ATC",
                                        accentColor = Color(0xFF3B82F6),
                                        onDown = { engine.player2.attack() },
                                        onUp = {}
                                    )
                                }
                            }
                        }

                        GameMode.AI_VS_AI -> {
                            // Empty control overlays running - display spectating details banner
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(2.dp, Color(0xFF334155))
                                    .background(Color(0xFF0F172A))
                                    .padding(vertical = 12.dp, horizontal = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "• SPECTATING SHOGUN SIMULATION BATTLE ACTIVE",
                                        color = Color(0xFFEF4444),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .border(1.dp, Color(0xFF475569))
                                                .background(Color(0xFF1E293B))
                                                .clickable { engine.resetGame() }
                                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Text("RESTART", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .border(1.dp, Color(0xFF475569))
                                                .background(Color(0xFF1E293B))
                                                .clickable { onBackToMenu() }
                                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Text("MAIN MENU", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatchCompletionOverlay(
    isGameOver: Boolean,
    winnerText: String,
    onReset: () -> Unit,
    onBackToMenu: () -> Unit
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = isGameOver,
        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
    ) {
        Column(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.85f))
                .border(6.dp, Color.White)
                .padding(horizontal = 48.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "SHOWDOWN COMPLETED",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = winnerText.uppercase(),
                color = Color(0xFFEF4444),
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Retry Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .border(2.dp, Color.White)
                        .background(Color(0xFF1E293B))
                        .clickable { onReset() }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "REMATCH",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                // Main Menu Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .border(2.dp, Color.White)
                        .background(Color(0xFF991B1B))
                        .clickable { onBackToMenu() }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "MAIN MENU",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
