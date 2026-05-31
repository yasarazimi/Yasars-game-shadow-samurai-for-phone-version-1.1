package com.example.game

import androidx.compose.ui.geometry.Rect
import kotlin.math.abs

enum class FighterState {
    IDLE, RUN, JUMP, FALL, ATTACK, TAKE_HIT, DEATH
}

class VectorFighter(
    val isPlayer1: Boolean, // Player 1 is Samurai Mack, Player 2 is Kenji
    var x: Float,
    var y: Float
) {
    var velocityX: Float = 0f
    var velocityY: Float = 0f
    
    val width: Float = 50f
    val height: Float = 150f
    
    var health: Int = 100
    var isAttacking: Boolean = false
    var dead: Boolean = false
    
    var state: FighterState = FighterState.IDLE
    var framesCurrent: Int = 0
    var framesElapsed: Int = 0
    var framesHold: Int = 6 // controls animation speed
    
    // Facing direction: 1 = right, -1 = left
    var facingDirection: Float = if (isPlayer1) 1f else -1f
    var lastKey: String = ""

    // Attack box relative dimensions
    val attackBoxWidth: Float = if (isPlayer1) 160f else 170f
    val attackBoxHeight: Float = 50f
    
    // Gets the dynamic attack box coordinates
    fun getAttackBoxRect(): Rect {
        val attackBoxOffsetX = if (isPlayer1) {
            if (facingDirection >= 0) 60f else -attackBoxWidth - 10f
        } else {
            if (facingDirection >= 0) 10f else -attackBoxWidth - 10f
        }
        val attackBoxOffsetY = 30f // positioned mid-height
        return Rect(
            left = x + attackBoxOffsetX,
            top = y + attackBoxOffsetY,
            right = x + attackBoxOffsetX + attackBoxWidth,
            bottom = y + attackBoxOffsetY + attackBoxHeight
        )
    }

    // Gets the bounding box coordinates
    fun getBodyRect(): Rect {
        return Rect(
            left = x,
            top = y,
            right = x + width,
            bottom = y + height
        )
    }

    // Animation frames layout matched directly to HTML/JS
    fun getFramesMax(): Int {
        return when (state) {
            FighterState.IDLE -> if (isPlayer1) 8 else 4
            FighterState.RUN -> if (isPlayer1) 8 else 8
            FighterState.JUMP -> if (isPlayer1) 2 else 2
            FighterState.FALL -> if (isPlayer1) 2 else 2
            FighterState.ATTACK -> if (isPlayer1) 6 else 4
            FighterState.TAKE_HIT -> if (isPlayer1) 4 else 3
            FighterState.DEATH -> if (isPlayer1) 6 else 7
        }
    }

    // Physics constants
    companion object {
        const val GRAVITY = 0.7f
        const val VERTICAL_BOUND_LIMIT = 330f // matching: y = 330, height = 150 -> bottom = 480
        const val CANVAS_HEIGHT = 576f
        const val CANVAS_WIDTH = 1024f
    }

    fun updatePhysics() {
        // Friction decay for taking hits/knockback sliding to feel extra polished!
        if (state == FighterState.TAKE_HIT || state == FighterState.DEATH) {
            velocityX *= 0.85f
            if (abs(velocityX) < 0.2f) velocityX = 0f
        }

        // Apply velocity
        x += velocityX
        y += velocityY

        // Constrain horizontal boundaries to stay inside canvas
        if (x < 0) x = 0f
        if (x + width > CANVAS_WIDTH) x = CANVAS_WIDTH - width

        // Gravity function
        val groundY = CANVAS_HEIGHT - 96f // 480f
        if (y + height + velocityY >= groundY) {
            // Check if we just landed to play a thump sound
            if (velocityY > 1f) {
                SoundManager.playMove()
            }
            velocityY = 0f
            y = VERTICAL_BOUND_LIMIT // 330f
        } else {
            velocityY += GRAVITY
        }

        // Handle simple states depending on physics
        if (!dead && state != FighterState.ATTACK && state != FighterState.TAKE_HIT) {
            if (velocityY < 0) {
                switchState(FighterState.JUMP)
            } else if (velocityY > 0) {
                switchState(FighterState.FALL)
            } else if (velocityX != 0f) {
                switchState(FighterState.RUN)
                // Periodically play running footstep sounds
                if (framesElapsed % 32 == 0) {
                    SoundManager.playMove()
                }
            } else {
                switchState(FighterState.IDLE)
            }
        }

        // Handle animation frame advancement
        if (!dead || (state == FighterState.DEATH && framesCurrent < getFramesMax() - 1)) {
            framesElapsed++
            if (framesElapsed % framesHold == 0) {
                val framesMax = getFramesMax()
                if (framesCurrent < framesMax - 1) {
                    framesCurrent++
                } else {
                    if (state == FighterState.ATTACK) {
                        isAttacking = false
                        switchState(FighterState.IDLE)
                    } else if (state == FighterState.TAKE_HIT) {
                        switchState(FighterState.IDLE)
                    } else if (state == FighterState.DEATH) {
                        // Keep sitting dead at final frame
                    } else {
                        framesCurrent = 0
                    }
                }
            }
        }
    }

    fun attack() {
        if (dead) return
        switchState(FighterState.ATTACK)
        isAttacking = true
        SoundManager.playSlash()
    }

    fun takeHit(attackerFacingDirection: Float) {
        if (dead) return
        health -= 20
        
        // Superior Knockback: apply heavy horizontal impulse away from attacker + vertical pop-up!
        val knockbackForceX = 15f
        velocityX = attackerFacingDirection * knockbackForceX
        velocityY = -4.5f // Slight pop upwards to create awesome fighting game launch effect
        
        if (health <= 0) {
            health = 0
            switchState(FighterState.DEATH)
        } else {
            switchState(FighterState.TAKE_HIT)
        }
        SoundManager.playHit()
    }

    fun reset(startX: Float, startY: Float) {
        x = startX
        y = startY
        velocityX = 0f
        velocityY = 0f
        health = 100
        isAttacking = false
        dead = false
        state = FighterState.IDLE
        framesCurrent = 0
        framesElapsed = 0
        facingDirection = if (isPlayer1) 1f else -1f
    }

    fun switchState(newState: FighterState) {
        // Rule: if dead, stay dead!
        if (state == FighterState.DEATH && framesCurrent == getFramesMax() - 1) {
            dead = true
            return
        }

        // Rule: cannot interrupt death initialization
        if (state == FighterState.DEATH) return

        // Override run/idle/jump with active attack animations until complete
        if (state == FighterState.ATTACK && framesCurrent < getFramesMax() - 1) {
            if (newState != FighterState.DEATH) return
        }

        // Override with takeHit animation until complete
        if (state == FighterState.TAKE_HIT && framesCurrent < getFramesMax() - 1) {
            if (newState != FighterState.DEATH) return
        }

        if (state != newState) {
            state = newState
            framesCurrent = 0
            framesElapsed = 0
        }
    }
}
