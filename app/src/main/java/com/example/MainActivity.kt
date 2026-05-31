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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import android.app.Activity
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

    override fun onResume() {
        super.onResume()
        // Resume koto ambient music throughout brawler menus and combat
        com.example.game.SoundManager.startAmbientMusic()
    }

    override fun onPause() {
        super.onPause()
        // Free and stop static synthetic sound loops
        com.example.game.SoundManager.stopAmbientMusic()
    }
}

enum class ScreenState {
    INTRO,
    GATE_MENU,
    SELECTION_MENU,
    PLAYING
}

@Composable
fun GameScreen(engine: GameEngine, renderer: GameRenderer) {
    var screenState by remember { mutableStateOf(ScreenState.INTRO) }
    var runningMode by remember { mutableStateOf(GameMode.PLAYER_VS_AI) }
    var selectedDifficulty by remember { mutableStateOf(AIDifficulty.MEDIUM) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // 60 FPS Core Game loop clock activation
    LaunchedEffect(screenState, engine.isGameOver) {
        if (screenState == ScreenState.PLAYING && !engine.isGameOver) {
            engine.startTimer(this)
            while (screenState == ScreenState.PLAYING && !engine.isGameOver) {
                engine.updateFrame()
                delay(16) // roughly 60 fps
            }
        }
    }

    when (screenState) {
        ScreenState.INTRO -> {
            IntroVideoScreen(onFinished = { 
                screenState = ScreenState.GATE_MENU
                com.example.game.SoundManager.startAmbientMusic()
            })
        }
        ScreenState.GATE_MENU -> {
            GateMenuScreen(
                onStart = {
                    screenState = ScreenState.SELECTION_MENU
                },
                onQuit = {
                    (context as? Activity)?.finish()
                }
            )
        }
        ScreenState.SELECTION_MENU -> {
            MainMenu(
                runningMode = runningMode,
                difficulty = selectedDifficulty,
                onModeChange = { runningMode = it },
                onDifficultyChange = { selectedDifficulty = it },
                onStartGame = {
                    engine.startGame(runningMode, selectedDifficulty)
                    screenState = ScreenState.PLAYING
                },
                onBackPressed = {
                    screenState = ScreenState.GATE_MENU
                }
            )
        }
        ScreenState.PLAYING -> {
            ArenaLayout(
                engine = engine,
                renderer = renderer,
                onBackToMenu = {
                    screenState = ScreenState.GATE_MENU
                    engine.resetGame()
                }
            )
        }
    }
}

@Composable
fun MainMenu(
    runningMode: GameMode,
    difficulty: AIDifficulty,
    onModeChange: (GameMode) -> Unit,
    onDifficultyChange: (AIDifficulty) -> Unit,
    onStartGame: () -> Unit,
    onBackPressed: () -> Unit
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

            Spacer(modifier = Modifier.height(16.dp))

            // Back to main menu door option
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(320.dp)
                    .height(48.dp)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .border(2.dp, Color(0xFF64748B))
                    .clickable { onBackPressed() }
            ) {
                Text(
                    text = "« BACK TO TITLE MENU",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )
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
                                                com.example.game.SoundManager.playJump()
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
                                                com.example.game.SoundManager.playJump()
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
                                                com.example.game.SoundManager.playJump()
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

@Composable
fun IntroVideoScreen(onFinished: () -> Unit) {
    var textOverlay by remember { mutableStateOf("THE BLADES AWAKEN...") }
    var elapsedMs by remember { mutableStateOf(0) }
    var playedClashSound by remember { mutableStateOf(false) }

    // Particle state for intro
    val sakuraPetals = remember {
        List(25) {
            SakuraPetal(
                x = (0..1024).random().toFloat(),
                y = (0..576).random().toFloat(),
                speedX = -1f - (0..3).random().toFloat(),
                speedY = 1f + (0..2).random().toFloat(),
                size = 6f + (0..8).random().toFloat(),
                rotation = (0..360).random().toFloat(),
                rotSpeed = 0.5f + (0..2).random().toFloat()
            )
        }
    }
    
    val sparkParticles = remember { androidx.compose.runtime.mutableStateListOf<IntroSpark>() }
    var screenShakeOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(Unit) {
        while (elapsedMs < 5200) {
            delay(16)
            elapsedMs += 16

            // Update subtitles based on exact timing from requested frames
            if (elapsedMs in 1200..2699) {
                textOverlay = "BEGIN"
            } else if (elapsedMs >= 2700) {
                textOverlay = "ONLY ONE WILL WIN."
            }

            // Update Sakura petals
            sakuraPetals.forEach { p ->
                p.x += p.speedX
                p.y += p.speedY
                p.rotation += p.rotSpeed
                if (p.x < -20f) p.x = 1044f
                if (p.y > 596f) p.y = -20f
            }

            // At 3000ms, the epic clash occurs!
            if (elapsedMs >= 3000) {
                if (!playedClashSound) {
                    playedClashSound = true
                    com.example.game.SoundManager.playIntroClash()
                    // Spawn 60 massive beautiful sparks in center
                    for (i in 0 until 60) {
                        val angle = (0..360).random() * Math.PI / 180.0
                        val speed = 2f + (0..12).random().toFloat()
                        sparkParticles.add(
                            IntroSpark(
                                x = 512f,
                                y = 250f,
                                vx = (Math.cos(angle) * speed).toFloat(),
                                vy = (Math.sin(angle) * speed - 1.5f).toFloat(),
                                size = 4f + (0..6).random().toFloat(),
                                maxLife = 20 + (0..30).random()
                            )
                        )
                    }
                }

                // Update Sparks
                val it = sparkParticles.iterator()
                while (it.hasNext()) {
                    val sp = it.next()
                    sp.currentLife++
                    if (sp.currentLife >= sp.maxLife) {
                        it.remove()
                    } else {
                        sp.x += sp.vx
                        sp.y += sp.vy
                        sp.vy += 0.2f // gravity pull
                    }
                }

                // Screenshake decay after clash
                val shakeIntensity = maxOf(0f, 25f - (elapsedMs - 3000) / 15f)
                if (shakeIntensity > 0f) {
                    screenShakeOffset = Offset(
                        (Math.random().toFloat() * 2f - 1f) * shakeIntensity,
                        (Math.random().toFloat() * 2f - 1f) * shakeIntensity
                    )
                } else {
                    screenShakeOffset = Offset.Zero
                }
            }
        }
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0E1E)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(1024f / 576f)
        ) {
            val canvasScale = minOf(size.width / 1024f, size.height / 576f)
            val dx = (size.width - 1024f * canvasScale) / 2f
            val dy = (size.height - 576f * canvasScale) / 2f

            withTransform({
                translate(left = dx + screenShakeOffset.x, top = dy + screenShakeOffset.y)
                scale(scaleX = canvasScale, scaleY = canvasScale, pivot = Offset.Zero)
                clipRect(left = 0f, top = 0f, right = 1024f, bottom = 576f)
            }) {
                // Background Sunrise/sunset Gradient
                val skyBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Deep indigo
                        Color(0xFF311042), // Cosmic purple
                        Color(0xFF991B1B)  // Crimson sky bottom
                    ),
                    startY = 0f,
                    endY = 576f
                )
                drawRect(brush = skyBrush, topLeft = Offset.Zero, size = Size(1024f, 576f))

                // Giant Samurai Red Sun
                drawCircle(
                    color = Color(0xFFEF4444).copy(alpha = 0.95f),
                    radius = 110f,
                    center = Offset(250f, 220f)
                )

                // Draw mountains peaks in silhouette
                val mountains = Path().apply {
                    moveTo(0f, 576f)
                    lineTo(0f, 430f)
                    lineTo(180f, 320f)
                    lineTo(350f, 460f)
                    lineTo(500f, 290f)
                    lineTo(680f, 440f)
                    lineTo(850f, 310f)
                    lineTo(1024f, 480f)
                    lineTo(1024f, 576f)
                    close()
                }
                drawPath(path = mountains, color = Color(0xFF070B19))

                // Draw Japanese Pagoda Temple outline (detailed oriental silhouette)
                drawRect(Color(0xFF02040A), Offset(790f, 350f), Size(100f, 130f)) // main tower body
                // Roof 1
                val roof1 = Path().apply {
                    moveTo(760f, 360f)
                    quadraticTo(780f, 350f, 840f, 350f)
                    quadraticTo(900f, 350f, 920f, 360f)
                    lineTo(900f, 335f)
                    lineTo(780f, 335f)
                    close()
                }
                drawPath(roof1, Color(0xFF02040A))
                // Roof 2 (second tier)
                drawRect(Color(0xFF02040A), Offset(810f, 250f), Size(60f, 85f))
                val roof2 = Path().apply {
                    moveTo(780f, 260f)
                    quadraticTo(810f, 250f, 840f, 250f)
                    quadraticTo(870f, 250f, 900f, 260f)
                    lineTo(880f, 240f)
                    lineTo(800f, 240f)
                    close()
                }
                drawPath(roof2, Color(0xFF02040A))
                // Top spire
                drawLine(
                    color = Color(0xFF02040A),
                    start = Offset(840f, 240f),
                    end = Offset(840f, 180f),
                    strokeWidth = 4f
                )

                // Ground plate
                drawRect(color = Color(0xFF02040A), topLeft = Offset(0f, 480f), size = Size(1024f, 96f))

                // Calculate Silhouette Samurai Choreography
                // Left Samurai starts 180f, moves at 1500ms to center. Clashes at 3000ms.
                val s1X = if (elapsedMs < 1500) {
                    180f
                } else if (elapsedMs < 3000) {
                    val progress = (elapsedMs - 1500f) / 1500f // 0 to 1
                    180f + progress * (512f - 80f - 180f)
                } else {
                    512f - 90f
                }

                val s1Y = if (elapsedMs < 1500) {
                    480f - 150f
                } else if (elapsedMs < 3000) {
                    val progress = (elapsedMs - 1500f) / 1500f // 0 to 1
                    (480f - 150f) - kotlin.math.sin(progress * Math.PI).toFloat() * 180f
                } else {
                    480f - 150f - 40f
                }

                // Right Samurai starts 844f, moves at 1500ms to center. Clashes at 3000ms.
                val s2X = if (elapsedMs < 1500) {
                    844f - 50f
                } else if (elapsedMs < 3000) {
                    val progress = (elapsedMs - 1500f) / 1500f
                    (844f - 50f) - progress * ((844f - 50f) - (512f + 30f))
                } else {
                    512f + 40f
                }

                val s2Y = if (elapsedMs < 1500) {
                    480f - 150f
                } else if (elapsedMs < 3000) {
                    val progress = (elapsedMs - 1500f) / 1500f
                    (480f - 150f) - kotlin.math.sin(progress * Math.PI).toFloat() * 180f
                } else {
                    480f - 150f - 40f
                }

                // Draw Left Samurai
                drawSamuraiSilhouette(this, s1X, s1Y, xRight = true, neonColor = Color(0xFF6366F1), isLeftSlam = (elapsedMs >= 1500 && elapsedMs < 3000))

                // Draw Right Samurai
                drawSamuraiSilhouette(this, s2X, s2Y, xRight = false, neonColor = Color(0xFFEF4444), isLeftSlam = (elapsedMs >= 1500 && elapsedMs < 3000))

                // Draw Sparks on Clash
                if (elapsedMs >= 3000) {
                    sparkParticles.forEach { sp ->
                        drawCircle(
                            color = Color(0xFFFFD700).copy(alpha = 1.0f - (sp.currentLife.toFloat() / sp.maxLife.toFloat())),
                            radius = sp.size,
                            center = Offset(sp.x, sp.y)
                        )
                    }
                    if (elapsedMs < 3120) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.8f),
                            radius = 60f,
                            center = Offset(512f, 250f)
                        )
                    }
                }

                // Falling Sakura Petals
                sakuraPetals.forEach { petal ->
                    withTransform({
                        translate(petal.x, petal.y)
                        rotate(petal.rotation, pivot = Offset.Zero)
                    }) {
                        val leaf = Path().apply {
                            moveTo(0f, 0f)
                            quadraticTo(petal.size / 2f, -petal.size / 2f, petal.size, 0f)
                            quadraticTo(petal.size / 2f, petal.size / 2f, 0f, 0f)
                            close()
                        }
                        drawPath(path = leaf, color = Color(0xFFFDA4AF))
                    }
                }
            }
        }

        // Vignette Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        // Subtitles and Skip Button Layer
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SAMURAI SHOWDOWN • CINEMATIC RE-FIGHT",
                color = Color(0xFFF43F5E).copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                    .border(2.dp, if (textOverlay == "BEGIN") Color(0xFFEF4444) else Color.White, RoundedCornerShape(4.dp))
                    .padding(horizontal = 30.dp, vertical = 14.dp)
            ) {
                Text(
                    text = textOverlay.uppercase(),
                    color = if (textOverlay == "BEGIN") Color(0xFFEF4444) else Color.White,
                    fontSize = if (textOverlay == "ONLY ONE WILL WIN.") 22.sp else 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .border(2.dp, Color.White, RoundedCornerShape(2.dp))
                    .background(Color(0xFFE11D48))
                    .clickable { onFinished() }
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "SKIP TO SELECTION »",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

class SakuraPetal(
    var x: Float,
    var y: Float,
    val speedX: Float,
    val speedY: Float,
    val size: Float,
    var rotation: Float,
    val rotSpeed: Float
)

class IntroSpark(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val size: Float,
    val maxLife: Int,
    var currentLife: Int = 0
)

fun drawSamuraiSilhouette(
    scope: androidx.compose.ui.graphics.drawscope.DrawScope,
    x: Float,
    y: Float,
    xRight: Boolean,
    neonColor: Color,
    isLeftSlam: Boolean
) {
    val torsoColor = Color(0xFF0C1324)
    val headColor = Color(0xFF030712)
    val dir = if (xRight) 1f else -1f

    // Head
    scope.drawCircle(
        color = headColor,
        radius = 16f,
        center = Offset(x + 25f, y + 20f)
    )

    // Samurai helmet (Kabuto spikes)
    val kabuto = Path().apply {
        moveTo(x + 25f - 16f, y + 16f)
        lineTo(x + 25f + 16f, y + 16f)
        lineTo(x + 25f + 4f * dir, y + 4f)
        close()
    }
    scope.drawPath(kabuto, torsoColor)

    // Body/Kimono
    val body = Path().apply {
        moveTo(x + 25f - 20f * dir, y + 36f)
        lineTo(x + 25f + 20f * dir, y + 36f)
        lineTo(x + 25f + 25f * dir, y + 150f)
        lineTo(x + 25f - 25f * dir, y + 150f)
        close()
    }
    scope.drawPath(body, torsoColor)

    // Glowing Neon Katana sword in dynamic sword stance or leap positions
    val swordStart: Offset
    val swordEnd: Offset

    if (isLeftSlam) {
        swordStart = Offset(x + 25f + 12f * dir, y + 45f)
        swordEnd = Offset(x + 25f + 70f * dir, y + 90f)
    } else {
        swordStart = Offset(x + 25f + 15f * dir, y + 70f)
        swordEnd = Offset(x + 25f + 65f * dir, y + 10f)
    }

    scope.drawLine(
        color = neonColor.copy(alpha = 0.35f),
        start = swordStart,
        end = swordEnd,
        strokeWidth = 14f,
        cap = StrokeCap.Round
    )
    scope.drawLine(
        color = neonColor.copy(alpha = 0.7f),
        start = swordStart,
        end = swordEnd,
        strokeWidth = 8f,
        cap = StrokeCap.Round
    )
    scope.drawLine(
        color = Color.White,
        start = swordStart,
        end = swordEnd,
        strokeWidth = 4f,
        cap = StrokeCap.Round
    )
}

@Composable
fun GateMenuScreen(
    onStart: () -> Unit,
    onQuit: () -> Unit
) {
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }

    // Read current volumes from SoundManager
    var masterVolume by remember { mutableStateOf(com.example.game.SoundManager.soundVolume) }
    var ambientVolume by remember { mutableStateOf(com.example.game.SoundManager.musicVolume) }
    var keystepsVolume by remember { mutableStateOf(com.example.game.SoundManager.keyMovementVolume) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0C1021),
                        Color(0xFF03001C)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Red sun overlay details
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFFEF4444).copy(alpha = 0.08f),
                radius = size.minDimension * 0.35f,
                center = Offset(size.width * 0.5f, size.height * 0.45f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Giant retro title with red/neon accents
            Text(
                text = "SAMURAI SHOWDOWN",
                fontSize = 40.sp,
                color = Color(0xFFF43F5E),
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .border(3.dp, Color.White)
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "THE ULTIMATE SYNTHETIC SWORD DUEL",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Start option button
            GateButton(
                text = "START SHOWDOWN",
                accentColor = Color(0xFF10B981) // Emerald accent for start
            ) {
                onStart()
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Settings option button
            GateButton(
                text = "SETTINGS / VOLUME",
                accentColor = Color(0xFFEF4444) // Vibrant red for settings
            ) {
                showSettings = true
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quit option button
            GateButton(
                text = "QUIT GAME",
                accentColor = Color(0xFF64748B) // Slate grey for quit
            ) {
                onQuit()
            }
        }

        // Overlay dialog/popup for Settings
        if (showSettings) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable { /* absorb clicks */ },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111322)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth(0.9f)
                        .border(3.dp, Color(0xFFF43F5E), RoundedCornerShape(8.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "AUDIO CONFIGURATION",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Slider 1: Master volume
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "MASTER SFX VOLUME",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${(masterVolume * 100).toInt()}%",
                                color = Color(0xFFF43F5E),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = masterVolume,
                            onValueChange = { newValue ->
                                masterVolume = newValue
                                com.example.game.SoundManager.soundVolume = newValue
                                // Play testing slash preview on adjusting
                                com.example.game.SoundManager.playSlash()
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFF43F5E),
                                activeTrackColor = Color(0xFFF43F5E),
                                inactiveTrackColor = Color(0xFF1E293B)
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Slider 2: Traditional Music level
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "TRADITIONAL MUSIC",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${(ambientVolume * 100).toInt()}%",
                                color = Color(0xFFEF4444),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = ambientVolume,
                            onValueChange = { newValue ->
                                ambientVolume = newValue
                                com.example.game.SoundManager.musicVolume = newValue
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFEF4444),
                                activeTrackColor = Color(0xFFEF4444),
                                inactiveTrackColor = Color(0xFF1E293B)
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Slider 3: Sound of keys and movements
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "KEYS & MOVEMENTS",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${(keystepsVolume * 100).toInt()}%",
                                color = Color(0xFF10B981),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = keystepsVolume,
                            onValueChange = { newValue ->
                                keystepsVolume = newValue
                                com.example.game.SoundManager.keyMovementVolume = newValue
                                // Play movement test feed back
                                com.example.game.SoundManager.playMove()
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF10B981),
                                activeTrackColor = Color(0xFF10B981),
                                inactiveTrackColor = Color(0xFF1E293B)
                            )
                        )

                        Spacer(modifier = Modifier.height(30.dp))

                        // Save & Close button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(Color(0xFFE11D48), RoundedCornerShape(4.dp))
                                .border(2.dp, Color.White, RoundedCornerShape(4.dp))
                                .clickable {
                                    showSettings = false
                                }
                        ) {
                            Text(
                                text = "APPLY SETTINGS",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GateButton(
    text: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(300.dp)
            .border(3.dp, accentColor)
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onClick() }
            .padding(vertical = 14.dp)
    ) {
        Text(
            text = text.uppercase(),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

