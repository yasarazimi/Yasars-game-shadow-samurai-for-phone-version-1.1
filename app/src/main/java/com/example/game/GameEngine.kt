package com.example.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

enum class GameMode {
    PLAYER_VS_AI,
    LOCAL_VS,
    AI_VS_AI
}

enum class AIDifficulty {
    EASY, MEDIUM, HARD
}

class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var size: Float,
    val color: Color,
    var alpha: Float = 1.0f,
    var maxLife: Int = 20,
    var currentLife: Int = 0
)

class GameEngine {
    // Fighters
    val player1 = VectorFighter(isPlayer1 = true, x = 150f, y = 330f)
    val player2 = VectorFighter(isPlayer1 = false, x = 800f, y = 330f)

    // Game states
    var mode by mutableStateOf(GameMode.PLAYER_VS_AI)
    var difficulty by mutableStateOf(AIDifficulty.MEDIUM)
    var timer by mutableStateOf(60)
    var isGameOver by mutableStateOf(false)
    var winnerText by mutableStateOf("")
    var timerRunning by mutableStateOf(false)
    
    // Physics & ticks
    var tick by mutableStateOf(0L)
    var screenShakeIntensity by mutableStateOf(0f)
    
    // Virtual Keys State - Player 1
    var p1LeftPressed = false
    var p1RightPressed = false
    
    // Virtual Keys State - Player 2
    var p2LeftPressed = false
    var p2RightPressed = false

    // Particle pool
    val particles = mutableListOf<Particle>()

    // Shop animation frame
    var shopFrame = 0
    private var shopFrameElapsed = 0

    // Cooldown support for AI actions
    private var aiAttackCooldown = 0
    private var aiJumpCooldown = 0

    fun startGame(selectedMode: GameMode, selectedDifficulty: AIDifficulty) {
        mode = selectedMode
        difficulty = selectedDifficulty
        resetGame()
    }

    fun resetGame() {
        player1.reset(startX = 150f, startY = 330f)
        player2.reset(startX = 824f, startY = 330f)
        timer = 60
        isGameOver = false
        winnerText = ""
        p1LeftPressed = false
        p1RightPressed = false
        p2LeftPressed = false
        p2RightPressed = false
        particles.clear()
        shopFrame = 0
        shopFrameElapsed = 0
        aiAttackCooldown = 0
        aiJumpCooldown = 0
        screenShakeIntensity = 0f
    }

    fun triggerScreenShake(intensity: Float) {
        screenShakeIntensity = intensity
    }

    fun spawnSlashParticles(x: Float, y: Float, isLeft: Boolean) {
        val count = 25
        val color = if (isLeft) Color(0xFF818CF8) else Color(0xFFF87171)
        for (i in 0 until count) {
            val angle = (if (isLeft) 180f else 0f) + Random.nextFloat() * 120f - 60f
            val rad = Math.toRadians(angle.toDouble())
            val speed = 3f + Random.nextFloat() * 9f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = (Math.cos(rad) * speed).toFloat(),
                    vy = (Math.sin(rad) * speed - (1f + Random.nextFloat() * 3f)).toFloat(),
                    size = 4f + Random.nextFloat() * 7f,
                    color = color,
                    maxLife = 15 + Random.nextInt(20)
                )
            )
        }
    }

    fun spawnBloodSplatter(x: Float, y: Float, count: Int = 15) {
        for (i in 0 until count) {
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = (Random.nextFloat() * 12f - 6f),
                    vy = (Random.nextFloat() * -10f - 2f),
                    size = 5f + Random.nextFloat() * 8f,
                    color = Color(0xFFEF4444), // Vibrant Blood Red
                    maxLife = 20 + Random.nextInt(30)
                )
            )
        }
    }

    fun startTimer(scope: CoroutineScope) {
        if (timerRunning) return
        timerRunning = true
        scope.launch {
            while (true) {
                delay(1000)
                if (!isGameOver) {
                    if (timer > 0) {
                        timer--
                    }
                    if (timer == 0) {
                        determineWinner()
                    }
                }
            }
        }
    }

    private fun determineWinner() {
        isGameOver = true
        winnerText = when {
            player1.health == player2.health -> "Tie"
            player1.health > player2.health -> "Player 1 wins"
            else -> "Player 2 wins"
        }
    }

    fun updateFrame() {
        if (isGameOver) {
            // Let the dead animation finish playing
            player1.updatePhysics()
            player2.updatePhysics()
            updateParticles()
            tick++
            return
        }

        // Apply background shop sprite frames (6 frames scaled at 60fps)
        shopFrameElapsed++
        if (shopFrameElapsed % 8 == 0) {
            shopFrame = (shopFrame + 1) % 6
        }

        // Apply screen shake decay
        if (screenShakeIntensity > 0f) {
            screenShakeIntensity *= 0.9f
            if (screenShakeIntensity < 0.1f) screenShakeIntensity = 0f
        }

        // AI Logic runs if appropriate
        handleAIInputs()

        // Handle Player 1 movement commands
        if (!player1.dead && player1.state != FighterState.ATTACK && player1.state != FighterState.TAKE_HIT) {
            if (p1LeftPressed && player1.lastKey == "a") {
                player1.velocityX = -5f
                player1.facingDirection = -1f
                player1.switchState(FighterState.RUN)
            } else if (p1RightPressed && player1.lastKey == "d") {
                player1.velocityX = 5f
                player1.facingDirection = 1f
                player1.switchState(FighterState.RUN)
            } else {
                player1.velocityX = 0f
                player1.switchState(FighterState.IDLE)
            }
        } else if (player1.dead || player1.state == FighterState.ATTACK || player1.state == FighterState.TAKE_HIT) {
            player1.velocityX = 0f
        }

        // Handle Player 2 movement commands (only in LOCAL_VS)
        if (mode == GameMode.LOCAL_VS) {
            if (!player2.dead && player2.state != FighterState.ATTACK && player2.state != FighterState.TAKE_HIT) {
                if (p2LeftPressed && player2.lastKey == "ArrowLeft") {
                    player2.velocityX = -5f
                    player2.facingDirection = -1f
                    player2.switchState(FighterState.RUN)
                } else if (p2RightPressed && player2.lastKey == "ArrowRight") {
                    player2.velocityX = 5f
                    player2.facingDirection = 1f
                    player2.switchState(FighterState.RUN)
                } else {
                    player2.velocityX = 0f
                    player2.switchState(FighterState.IDLE)
                }
            } else if (player2.dead || player2.state == FighterState.ATTACK || player2.state == FighterState.TAKE_HIT) {
                player2.velocityX = 0f
            }
        }

        // Run general fighter physics and boundary checks
        player1.updatePhysics()
        player2.updatePhysics()

        // Check for Attack active damage registers
        // Player 1 triggers attack on frame 4
        if (player1.isAttacking && player1.framesCurrent == 4) {
            val attackBox = player1.getAttackBoxRect()
            val victimBox = player2.getBodyRect()
            if (rectangularCollision(attackBox, victimBox)) {
                // Register hit on Player 2!
                spawnBloodSplatter(player2.x + player2.width / 2f, player2.y + player2.height / 3f)
                spawnSlashParticles(player2.x + player2.width / 2f, player2.y + player2.height / 2f, isLeft = false)
                player2.takeHit()
                triggerScreenShake(12f)
            }
            player1.isAttacking = false // Avoid double registering
        }
        
        // Player 2 triggers attack on frame 2
        if (player2.isAttacking && player2.framesCurrent == 2) {
            val attackBox = player2.getAttackBoxRect()
            val victimBox = player1.getBodyRect()
            if (rectangularCollision(attackBox, victimBox)) {
                // Register hit on Player 1!
                spawnBloodSplatter(player1.x + player1.width / 2f, player1.y + player1.height / 3f)
                spawnSlashParticles(player1.x + player1.width / 2f, player1.y + player1.height / 2f, isLeft = true)
                player1.takeHit()
                triggerScreenShake(12f)
            }
            player2.isAttacking = false // Avoid double registering
        }

        // Cleanup stale hits/attacks
        if (player1.isAttacking && player1.framesCurrent == 4) {
            player1.isAttacking = false
        }
        if (player2.isAttacking && player2.framesCurrent == 2) {
            player2.isAttacking = false
        }

        // Game over checks
        if (player1.health <= 0 || player2.health <= 0) {
            determineWinner()
        }

        // Particle dynamics Update
        updateParticles()

        tick++
    }

    private fun handleAIInputs() {
        if (player1.dead || player2.dead) return

        // 1. Player 1 (Mack) AI in AI_VS_AI mode
        if (mode == GameMode.AI_VS_AI) {
            simulateFighterAI(player1, player2)
        }

        // 2. Player 2 (Kenji) AI in PLAYER_VS_AI and AI_VS_AI modes
        if (mode == GameMode.PLAYER_VS_AI || mode == GameMode.AI_VS_AI) {
            simulateFighterAI(player2, player1)
        }
    }

    private fun simulateFighterAI(actor: VectorFighter, opponent: VectorFighter) {
        if (actor.dead || actor.state == FighterState.ATTACK || actor.state == FighterState.TAKE_HIT) return

        // Fetch AI difficulty parameters
        val attackRatePercent = when (difficulty) {
            AIDifficulty.EASY -> 1.5
            AIDifficulty.MEDIUM -> 3.0
            AIDifficulty.HARD -> 5.5
        }
        val jumpRatePercent = when (difficulty) {
            AIDifficulty.EASY -> 0.3
            AIDifficulty.MEDIUM -> 0.8
            AIDifficulty.HARD -> 1.5
        }

        val dx = opponent.x - actor.x
        val dist = abs(dx)

        // Face the enemy
        actor.facingDirection = if (dx >= 0) 1f else -1f

        // Cooldowns
        if (aiAttackCooldown > 0) aiAttackCooldown--
        if (aiJumpCooldown > 0) aiJumpCooldown--

        // AI decision matrix based on physical distance
        if (dist > 180f) {
            // Far distance: move closer
            actor.velocityX = if (dx > 0) 4f else -4f
            actor.switchState(FighterState.RUN)
        } else if (dist < 110f) {
            // Too close: back up slightly, or attack!
            if (Random.nextDouble() * 100 < attackRatePercent && aiAttackCooldown == 0) {
                actor.attack()
                aiAttackCooldown = 15
            } else {
                actor.velocityX = if (dx > 0) -3f else 3f
                actor.switchState(FighterState.RUN)
            }
        } else {
            // Optimal intermediate attack zone
            actor.velocityX = 0f
            actor.switchState(FighterState.IDLE)

            if (Random.nextDouble() * 100 < attackRatePercent && aiAttackCooldown == 0) {
                actor.attack()
                aiAttackCooldown = 12
            }
        }

        // Proactive dodging: If opponent is actively attacking and within danger range, jump to avoid!
        if (opponent.state == FighterState.ATTACK && opponent.framesCurrent <= 2 && dist < 220f) {
            val dodgeChance = when (difficulty) {
                AIDifficulty.EASY -> 15
                AIDifficulty.MEDIUM -> 45
                AIDifficulty.HARD -> 75
            }
            if (Random.nextInt(100) < dodgeChance && actor.velocityY == 0f && aiJumpCooldown == 0) {
                actor.velocityY = -18f
                aiJumpCooldown = 40
            }
        }

        // Random jump flavor logic
        if (Random.nextDouble() * 100 < jumpRatePercent && actor.velocityY == 0f && aiJumpCooldown == 0) {
            actor.velocityY = -19f
            aiJumpCooldown = 50
        }
    }

    private fun updateParticles() {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.currentLife++
            if (p.currentLife >= p.maxLife) {
                iterator.remove()
            } else {
                p.x += p.vx
                p.y += p.vy
                p.vy += 0.25f // Gravity pulling down spark debris
                p.alpha = 1.0f - (p.currentLife.toFloat() / p.maxLife.toFloat())
            }
        }
    }

    private fun rectangularCollision(rect1: Rect, rect2: Rect): Boolean {
        return rect1.left <= rect2.right &&
               rect1.right >= rect2.left &&
               rect1.top <= rect2.bottom &&
               rect1.bottom >= rect2.top
    }
}
