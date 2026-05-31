package com.example.game

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.cos
import kotlin.math.sin

class GameRenderer {

    fun drawGame(
        scope: DrawScope,
        engine: GameEngine,
        width: Float,
        height: Float
    ) {
        if (width <= 0f || height <= 0f || width.isNaN() || height.isNaN()) return
        
        // Calculate uniform scale to fit 1024x576 aspect ratio
        val gameWidth = 1024f
        val gameHeight = 576f
        val scale = minOf(width / gameWidth, height / gameHeight)
        val dx = (width - gameWidth * scale) / 2f
        val dy = (height - gameHeight * scale) / 2f

        // Apply global transform (centering + scaling + screen shake)
        val shakeX = if (engine.screenShakeIntensity > 0) {
            (Math.random().toFloat() * 2f - 1f) * engine.screenShakeIntensity
        } else 0f
        val shakeY = if (engine.screenShakeIntensity > 0) {
            (Math.random().toFloat() * 2f - 1f) * engine.screenShakeIntensity
        } else 0f

        scope.withTransform({
            translate(left = dx + shakeX, top = dy + shakeY)
            scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
            // Clip to virtual canvas bounds
            clipRect(left = 0f, top = 0f, right = gameWidth, bottom = gameHeight)
        }) {
            // 1. Sky Gradient
            drawSky(this, engine.tick)

            // 2. Mountains Silhouette (Parallax)
            drawMountains(this)

            // 3. Shop (Interactive glowing asset at 600, 128)
            drawShop(this, engine.shopFrame, engine.tick)

            // 4. Ground details
            drawGround(this)

            // 5. Particles Update & Drawing
            drawParticles(this, engine.particles)

            // 6. Draw Fighters (Samurai Mack & Kenji)
            drawFighter(this, engine.player1, engine.tick)
            drawFighter(this, engine.player2, engine.tick)

            // 7. Hitboxes (For debugging - un-comment to see active bounding bounds)
            // drawHitboxes(this, engine.player1, engine.player2)
        }
    }

    private fun drawSky(scope: DrawScope, tick: Long) {
        // Multi-stop twilight gradient from deep cyber slate to neon violet
        val brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F172A), // Slate 900
                Color(0xFF1E1B4B), // Indigo 950
                Color(0xFF311042), // Dark Crimson-Violet
                Color(0xFF881337)  // Rose 950
            ),
            startY = 0f,
            endY = 576f
        )
        scope.drawRect(
            brush = brush,
            topLeft = Offset.Zero,
            size = Size(1024f, 576f)
        )

        // Draw a giant, gorgeous crimson sun/moon
        val glowRadius = 140f + sin(tick * 0.04f) * 6f
        scope.drawCircle(
            color = Color(0x33F43F5E), // Rose 500 glow tint
            radius = glowRadius,
            center = Offset(250f, 180f)
        )
        scope.drawCircle(
            color = Color(0xFFF43F5E), // Solid Rose
            radius = 110f,
            center = Offset(250f, 180f)
        )
    }

    private fun drawMountains(scope: DrawScope) {
        val path1 = Path().apply {
            moveTo(0f, 576f)
            lineTo(0f, 380f)
            cubicTo(150f, 330f, 250f, 430f, 450f, 360f)
            cubicTo(600f, 310f, 750f, 410f, 1024f, 320f)
            lineTo(1024f, 576f)
            close()
        }
        scope.drawPath(
            path = path1,
            color = Color(0xFF1E1E38) // Deep dark indigo mountain far layer
        )

        val path2 = Path().apply {
            moveTo(0f, 576f)
            lineTo(0f, 420f)
            cubicTo(200f, 390f, 350f, 450f, 550f, 410f)
            cubicTo(700f, 380f, 850f, 440f, 1024f, 390f)
            lineTo(1024f, 576f)
            close()
        }
        scope.drawPath(
            path = path2,
            color = Color(0xFF121226) // Darker near mountain layer
        )
    }

    private fun drawShop(scope: DrawScope, frame: Int, tick: Long) {
        val startX = 640f
        val startY = 180f

        // Draw Shop Building Pillars and Pagoda Roof
        // Roof
        val roofPath = Path().apply {
            moveTo(startX - 10f, startY)
            lineTo(startX + 180f, startY)
            lineTo(startX + 160f, startY - 40f)
            lineTo(startX + 10f, startY - 40f)
            close()
        }
        scope.drawPath(roofPath, Color(0xFF111111))
        
        // Pagoda trim (Crimson outline)
        val trimPath = Path().apply {
            moveTo(startX - 23f, startY + 5f)
            quadraticTo(startX + 10f, startY - 10f, startX + 10f, startY - 45f)
            lineTo(startX + 160f, startY - 45f)
            quadraticTo(startX + 160f, startY - 10f, startX + 193f, startY + 5f)
            lineTo(startX + 175f, startY + 12f)
            lineTo(startX - 5f, startY + 12f)
            close()
        }
        scope.drawPath(trimPath, Color(0xFFB91C1C)) // Dark red lacquer

        // Main canopy
        scope.drawRect(
            color = Color(0xFF262626),
            topLeft = Offset(startX + 15f, startY + 12f),
            size = Size(140f, 110f)
        )

        // Wooden pillars
        scope.drawRect(Color(0xFF451A03), Offset(startX + 15f, startY + 12f), Size(12f, 110f))
        scope.drawRect(Color(0xFF451A03), Offset(startX + 143f, startY + 12f), Size(12f, 110f))

        // Counter screen
        scope.drawRect(Color(0xFF171717), Offset(startX + 27f, startY + 50f), Size(116f, 60f))

        // Dynamic Merchant/Shopkeeper pixel silhouette inside
        val merchantBobble = sin(tick * 0.07f) * 2f
        scope.drawCircle(
            color = Color(0xFF4B5563),
            radius = 12f,
            center = Offset(startX + 85f, startY + 74f + merchantBobble)
        )
        scope.drawRect(
            color = Color(0xFF374151),
            topLeft = Offset(startX + 75f, startY + 86f + merchantBobble),
            size = Size(20f, 24f)
        )

        // Draw swaying shop lanterns that glow with flicker (6 animation frames simulation)
        for (i in 0..1) {
            val lanternX = startX + 35f + i * 100f
            val lanternY = startY + 28f
            val lanternSlide = sin(tick * 0.05f + i * 2f) * 2f
            
            // Wire
            scope.drawLine(
                color = Color.Black,
                start = Offset(lanternX, lanternY),
                end = Offset(lanternX + lanternSlide, lanternY + 18f),
                strokeWidth = 2f
            )

            // Lantern glow bulb
            val glowColor = when (frame) {
                0, 3 -> Color(0xFFFF9800)
                1, 4 -> Color(0xFFFFC107)
                2, 5 -> Color(0xFFFF5722)
                else -> Color(0xFFFF9800)
            }
            
            // Radiant glow aura
            scope.drawCircle(
                color = glowColor.copy(alpha = 0.35f + sin(tick * 0.1f) * 0.08f),
                radius = 16f,
                center = Offset(lanternX + lanternSlide, lanternY + 28f)
            )

            // Lantern capsule body
            scope.drawRoundRect(
                color = glowColor,
                topLeft = Offset(lanternX - 8f + lanternSlide, lanternY + 18f),
                size = Size(16f, 22f),
                cornerRadius = CornerRadius(5f, 5f)
            )
            
            // Black cage grill bands
            scope.drawRect(
                color = Color.Black,
                topLeft = Offset(lanternX - 9f + lanternSlide, lanternY + 26f),
                size = Size(18f, 3f)
            )
        }
    }

    private fun drawGround(scope: DrawScope) {
        val groundY = 480f
        
        // Deep rock base floor
        scope.drawRect(
            color = Color(0xFF0F0F1A),
            topLeft = Offset(0f, groundY),
            size = Size(1024f, 96f)
        )

        // Brick platform top edge
        scope.drawRect(
            color = Color(0xFF4A4A6A),
            topLeft = Offset(0f, groundY),
            size = Size(1024f, 6f)
        )

        // Draw elegant wooden bridge/arcade grid texture lines
        for (i in 0..20) {
            val startX = i * 54f
            scope.drawLine(
                color = Color(0xFF1E1E2E),
                start = Offset(startX, groundY + 6f),
                end = Offset(startX - 24f, 576f),
                strokeWidth = 3f
            )
        }

        // Horizontal brick lines
        scope.drawLine(
            color = Color(0xFF2E2E4A),
            start = Offset(0f, groundY + 36f),
            end = Offset(1024f, groundY + 36f),
            strokeWidth = 2f
        )
        scope.drawLine(
            color = Color(0xFF1A1A2E),
            start = Offset(0f, groundY + 70f),
            end = Offset(1024f, groundY + 70f),
            strokeWidth = 2f
        )
    }

    private fun drawParticles(scope: DrawScope, particles: List<Particle>) {
        particles.forEach { p ->
            scope.drawRect(
                color = p.color.copy(alpha = p.alpha),
                topLeft = Offset(p.x, p.y),
                size = Size(p.size, p.size)
            )
        }
    }

    private fun drawFighter(scope: DrawScope, f: VectorFighter, tick: Long) {
        val x = f.x
        val y = f.y

        // Determine main rendering assets and colors depending on identity
        val colorPrimary = if (f.isPlayer1) Color(0xFFEF4444) else Color(0xFF818CF8) // Samurai Mack: Red, Kenji: Blue
        val colorAccent = if (f.isPlayer1) Color(0xFFFEE2E2) else Color(0xFFE0E7FF)

        // Create character bobble cycle
        val bobble = if (f.state == FighterState.IDLE) {
            sin(tick * 0.18f) * 3f
        } else 0f

        // Draw shadow on ground (scaling with jumping height)
        val groundY = 480f
        val distanceToGround = groundY - (y + f.height)
        val shadowScale = maxOf(0.2f, 1f - (distanceToGround / 250f))
        
        scope.drawOval(
            color = Color(0x73000000),
            topLeft = Offset(x + f.width / 2f - 30f * shadowScale, groundY - 5f),
            size = Size(60f * shadowScale, 10f)
        )

        // Draw Phantom Trail Shadows for Kenji when running
        if (!f.isPlayer1 && f.state == FighterState.RUN) {
            scope.drawRect(
                color = Color(0x334F46E5),
                topLeft = Offset(x - f.velocityX * 1.5f, y + bobble),
                size = Size(f.width, f.height)
            )
            scope.drawRect(
                color = Color(0x1A4F46E5),
                topLeft = Offset(x - f.velocityX * 3f, y + bobble),
                size = Size(f.width, f.height)
            )
        }

        // Apply visual tilt/shear based on state
        val tiltAngle = when (f.state) {
            FighterState.RUN -> f.facingDirection * 10f
            FighterState.ATTACK -> f.facingDirection * 15f
            FighterState.TAKE_HIT -> -f.facingDirection * 20f
            FighterState.DEATH -> -f.facingDirection * 45f
            else -> 0f
        }

        scope.withTransform({
            // Rotate/Tilt the warrior during states
            rotate(degrees = tiltAngle, pivot = Offset(x + f.width / 2f, y + f.height))
        }) {
            if (f.state == FighterState.DEATH && f.dead) {
                // Fallen state - draw simple collapsed geometry on floor
                drawCollapsedFighter(this, f, colorPrimary)
                return@withTransform
            }

            // Flash effect for takeHit
            val isFlashing = f.state == FighterState.TAKE_HIT && (f.framesCurrent % 2 == 0)
            val fighterColor = if (isFlashing) Color.White else colorPrimary

            // A. Draw flapping dynamic cloak or sash
            drawCloak(this, f, tick, fighterColor)

            // B. Draw Main Robes/Body (Gutter details for traditional kimono look)
            scope.drawRoundRect(
                color = fighterColor,
                topLeft = Offset(x, y + 25f + bobble),
                size = Size(f.width, f.height - 25f),
                cornerRadius = CornerRadius(10f, 10f)
            )
            // Vest/Kimono collar lines (V neck accent)
            val vNeck = Path().apply {
                moveTo(x + 10f, y + 25f + bobble)
                lineTo(x + f.width / 2f, y + 55f + bobble)
                lineTo(x + f.width - 10f, y + 25f + bobble)
            }
            scope.drawPath(vNeck, colorAccent, style = Stroke(width = 4f))

            // White belt (Obi sash)
            scope.drawRect(
                color = colorAccent,
                topLeft = Offset(x - 2f, y + 75f + bobble),
                size = Size(f.width + 4f, 8f)
            )

            // C. Legs / stance
            drawStanceFeet(this, f, tick, bobble)

            // D. Head, Hair, Mask, and Headband
            scope.drawCircle(
                color = Color(0xFFFDBA74), // Warm flesh tone peach skin
                radius = 16f,
                center = Offset(x + f.width / 2f, y + 15f + bobble)
            )
            
            // Samurai Helmet / Hood / Ninja Mask
            if (f.isPlayer1) {
                // Crimson hair/helmet top
                scope.drawArc(
                    color = fighterColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(x + f.width / 2f - 18f, y - 5f + bobble),
                    size = Size(36f, 32f)
                )
                // Flapping Headband tassel lines
                val headbandSlide = sin(tick * 0.12f) * 6f
                scope.drawLine(
                    color = fighterColor,
                    start = Offset(x + f.width / 2f - f.facingDirection * 15f, y + 10f + bobble),
                    end = Offset(x + f.width / 2f - f.facingDirection * 35f, y + 18f + headbandSlide + bobble),
                    strokeWidth = 4f
                )
            } else {
                // Ninja Shadow Mask covering lower face completely
                scope.drawRect(
                    color = Color(0xFF111111),
                    topLeft = Offset(x + f.width / 2f - 15f, y + 10f + bobble),
                    size = Size(30f, 16f)
                )
                // Indigo hood cowl
                scope.drawArc(
                    color = fighterColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(x + f.width / 2f - 18f, y - 4f + bobble),
                    size = Size(36f, 32f)
                )
            }

            // Gaining focus visual helper - Gladiator glowing eyes!
            val eyeX1 = x + f.width / 2f + f.facingDirection * 2f
            val eyeX2 = x + f.width / 2f + f.facingDirection * 10f
            val eyeColor = if (f.isPlayer1) Color(0xFFFF5252) else Color(0xFF536DFE)
            scope.drawCircle(eyeColor, radius = 2.5f, center = Offset(eyeX1, y + 12f + bobble))
            scope.drawCircle(eyeColor, radius = 2.5f, center = Offset(eyeX2, y + 12f + bobble))

            // E. Arms and Weapons (Katanas / Slashes)
            drawWeaponFighter(this, f, tick, bobble)
        }

        // Draw Slash visual arc trail on active swing moments (not rotated with torso, scales independently)
        drawSlashVFX(scope, f)
    }

    private fun drawCloak(scope: DrawScope, f: VectorFighter, tick: Long, col: Color) {
        val x = f.x
        val y = f.y

        // Flowing cloak geometry billowing behind movement direction
        val flowSlide = when (f.state) {
            FighterState.RUN -> -f.facingDirection * 30f
            FighterState.JUMP -> -f.facingDirection * 15f
            FighterState.FALL -> -f.facingDirection * 10f
            else -> sin(tick * 0.1f) * 8f - f.facingDirection * 10f
        }

        val cloakPath = Path().apply {
            moveTo(x + f.width / 2f, y + 26f)
            quadraticTo(
                x + f.width / 2f + flowSlide, y + f.height / 2f,
                x + f.width / 2f + flowSlide * 1.5f, y + f.height - 10f
            )
            lineTo(x + f.width / 2f, y + f.height - 18f)
            close()
        }
        scope.drawPath(cloakPath, col)
    }

    private fun drawStanceFeet(scope: DrawScope, f: VectorFighter, tick: Long, bobble: Float) {
        val x = f.x
        val y = f.y

        if (f.state == FighterState.RUN) {
            // Cyclical scissor leg animation runs
            val runCycle = tick * 0.3f
            val foot1Y = y + f.height - 8f + sin(runCycle) * 8f
            val foot2Y = y + f.height - 8f + cos(runCycle) * 8f

            // Foot Left
            scope.drawRect(
                Color.Black,
                topLeft = Offset(x + 5f, foot1Y),
                size = Size(14f, 8f)
            )
            // Foot Right
            scope.drawRect(
                Color.Black,
                topLeft = Offset(x + f.width - 19f, foot2Y),
                size = Size(14f, 8f)
            )
        } else {
            // Draw static crouched stance
            scope.drawRect(
                Color.Black,
                topLeft = Offset(x + 2f, y + f.height - 10f + bobble),
                size = Size(15f, 10f)
            )
            scope.drawRect(
                Color.Black,
                topLeft = Offset(x + f.width - 17f, y + f.height - 10f + bobble),
                size = Size(15f, 10f)
            )
        }
    }

    private fun drawWeaponFighter(scope: DrawScope, f: VectorFighter, tick: Long, bobble: Float) {
        val x = f.x
        val y = f.y

        // Position katana depending on action states
        if (f.state == FighterState.ATTACK) {
            // Lunge swing forward motion
            val armLength = 40f
            val handOffset = Offset(x + f.width / 2f + f.facingDirection * 30f, y + 65f + bobble)
            
            // Draw sleeve
            scope.drawLine(
                color = if (f.isPlayer1) Color(0xFFEF4444) else Color(0xFF818CF8),
                start = Offset(x + f.width / 2f, y + 55f + bobble),
                end = handOffset,
                strokeWidth = 10f,
                cap = StrokeCap.Round
            )

            // Draw katana extending out
            val bladeEnd = Offset(
                handOffset.x + f.facingDirection * 65f,
                handOffset.y - 15f
            )
            // Katana guard
            scope.drawCircle(Color(0xFFF59E0B), radius = 6f, center = handOffset)
            // Steel Katana Blade
            scope.drawLine(
                color = Color(0xFFE2E8F0),
                start = handOffset,
                end = bladeEnd,
                strokeWidth = 4f
            )
        } else {
            // Idle sword in sheathe/hilt style
            val hiltStart = Offset(x + f.width / 2f - f.facingDirection * 15f, y + 70f + bobble)
            val sheatheEnd = Offset(x + f.width / 2f - f.facingDirection * 44f, y + 115f + bobble)

            // Draw Katana sheathed across back/obique
            scope.drawLine(
                color = Color(0xFF1A1A1A),
                start = hiltStart,
                end = sheatheEnd,
                strokeWidth = 5f
            )
            scope.drawLine(
                color = Color(0xFFF59E0B), // Golden Hilt wrap
                start = sheatheEnd,
                end = Offset(sheatheEnd.x - f.facingDirection * 12f, sheatheEnd.y + 12f),
                strokeWidth = 4f
            )
        }
    }

    private fun drawSlashVFX(scope: DrawScope, f: VectorFighter) {
        if (f.state != FighterState.ATTACK) return

        // Verify active slash visual indicators match damage frames precisely
        // Samurai Mack displays slash during frame 3 & 4
        // Kenji displays slash during frame 1 & 2
        val shouldDrawSlash = if (f.isPlayer1) {
            f.framesCurrent in 3..4
        } else {
            f.framesCurrent in 1..2
        }

        if (!shouldDrawSlash) return

        val centerSlashX = f.x + f.width / 2f + f.facingDirection * 85f
        val centerSlashY = f.y + f.height / 2f
        val radiusX = f.attackBoxWidth * 0.7f

        val path = Path().apply {
            // Draw sweeping glowing blade arc trail shapes
            moveTo(f.x + f.width / 2f + f.facingDirection * 25f, centerSlashY - 45f)
            quadraticTo(
                centerSlashX, centerSlashY - 70f,
                f.x + f.width / 2f + f.facingDirection * (25f + radiusX), centerSlashY
            )
            quadraticTo(
                centerSlashX, centerSlashY + 70f,
                f.x + f.width / 2f + f.facingDirection * 25f, centerSlashY + 45f
            )
        }

        val slashColor = if (f.isPlayer1) {
            Color(0x99EF4444) // Bright translucent Red for Mack
        } else {
            Color(0x99818CF8) // Translucent Neon Blue for Kenji
        }

        val glowColor = if (f.isPlayer1) Color(0xFFFF8A8A) else Color(0xFFC7D2FE)

        scope.drawPath(
            path = path,
            color = slashColor,
            style = Stroke(width = 16f, cap = StrokeCap.Round)
        )
        scope.drawPath(
            path = path,
            color = glowColor,
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )
    }

    private fun drawCollapsedFighter(scope: DrawScope, f: VectorFighter, color: Color) {
        // Draw the character defeated, lying flat on the platform floor
        val x = f.x
        val y = 480f - 24f // flat on ground

        scope.drawRoundRect(
            color = color.copy(alpha = 0.6f),
            topLeft = Offset(x - 20f, y + 10f),
            size = Size(f.height - 40f, 14f),
            cornerRadius = CornerRadius(5f, 5f)
        )
        // fallen head
        scope.drawCircle(
            color = Color(0xFFFDBA74).copy(alpha = 0.6f),
            radius = 11f,
            center = Offset(x + f.height - 50f, y + 10f)
        )
        // dropped steel blade lying separately
        scope.drawLine(
            color = Color(0xFFCBD5E1),
            start = Offset(x - 35f, y + 16f),
            end = Offset(x - 65f, y + 21f),
            strokeWidth = 3f
        )
        scope.drawCircle(Color(0xFFF59E0B), radius = 4f, center = Offset(x - 35f, y + 16f))
    }

    // Un-comment and append to drawGame call during local device validation if needed
    @Suppress("unused")
    private fun drawHitboxes(scope: DrawScope, f1: VectorFighter, f2: VectorFighter) {
        // Debug box indicators
        // Fighter 1 Body
        scope.drawRect(
            color = Color.Green.copy(alpha = 0.35f),
            topLeft = Offset(f1.x, f1.y),
            size = Size(f1.width, f1.height)
        )
        // Fighter 1 Active Attack Box
        if (f1.state == FighterState.ATTACK) {
            val ab = f1.getAttackBoxRect()
            scope.drawRect(
                color = Color.Red.copy(alpha = 0.45f),
                topLeft = Offset(ab.left, ab.top),
                size = Size(ab.width, ab.height)
            )
        }

        // Fighter 2 Body
        scope.drawRect(
            color = Color.Green.copy(alpha = 0.35f),
            topLeft = Offset(f2.x, f2.y),
            size = Size(f2.width, f2.height)
        )
        // Fighter 2 Active Attack Box
        if (f2.state == FighterState.ATTACK) {
            val ab = f2.getAttackBoxRect()
            scope.drawRect(
                color = Color.Red.copy(alpha = 0.45f),
                topLeft = Offset(ab.left, ab.top),
                size = Size(ab.width, ab.height)
            )
        }
    }
}
