package com.assiance.scabbard.ui.effect

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import kotlin.math.cos
import kotlin.math.sin

/**
 * 炫彩流光背景的 shader 封装。
 *
 * 负责把 [BgEffectConfig] 的静态点/色 + 动画时间转成 shader uniform，并输出一个
 * Compose [ShaderBrush] 供绘制。参数更新都做缓存比较，避免无谓的 uniform 写入。
 *
 * **与移植来源 LoveScore 版的两点差异**，都是本项目环境使然：
 * 1. 那边用 miuix 的 `top.yukonga.miuix.kmp.shader.RuntimeShader` + `asBrush()`。那个库的
 *    AAR 元数据要求 `minCompileSdk = 37`，本项目是 36 —— 为它抬一条编译轴不值得。
 *    本项目是纯 Android，直接用 framework 的 [android.graphics.RuntimeShader]（API 33+）
 *    加一个十几行的 [ShaderBrush] 子类就顶上了，**零新依赖、不动 compileSdk**。
 * 2. 那边 [brush] 必须是 getter（Skia 后端每次要拿新的不可变快照，缓存会让画面静止）。
 *    本项目只有 Android 一种后端，RuntimeShader 本身可变、uniform 改完立即对已发出的
 *    brush 生效，所以这里缓存一个实例即可。
 */
internal class BgEffectPainter {

    /**
     * 编译好的着色器；**当前设备撑不起来时为 null**（API < 33，或 AGSL 编译失败）。
     * 调用点据此静默降级，背景永远不该是空白或崩溃。
     */
    private val shader: RuntimeShader? = compile()

    /** 当前设备能否画炫彩。 */
    val supported: Boolean get() = shader != null

    /** 绘制用画笔。shader 不可用时为 null。 */
    val brush: ShaderBrush? = shader?.let { compiled ->
        object : ShaderBrush() {
            override fun createShader(size: Size): Shader = compiled
        }
    }

    // --- uniform 写入的缓存，避免同一帧内重复写 ---
    private val resolution = FloatArray(2)
    private val colorsBuffer = FloatArray(16)
    private val pointsAnimBuffer = FloatArray(8)
    private var animTime = Float.NaN
    private var cachedPreset: BgEffectConfig? = null
    private var cachedColorStage = Float.NaN
    private var cachedPointsAnimTime = Float.NaN

    private fun compile(): RuntimeShader? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        // 构造 RuntimeShader 就会编译 AGSL。源码语法错 / 驱动不支持都在这里抛，
        // 吞掉并返回 null —— 让降级发生在进入绘制之前，而不是首帧画到一半炸掉。
        return runCatching {
            RuntimeShader(BG_EFFECT_FRAG).also { shader ->
                shader.setFloatUniform("uTranslateY", U_TRANSLATE_Y)
                shader.setFloatUniform("uNoiseScale", U_NOISE_SCALE)
                shader.setFloatUniform("uPointRadiusMulti", U_POINT_RADIUS_MULTI)
                shader.setFloatUniform("uAlphaMulti", U_ALPHA_MULTI)
                shader.setFloatUniform("uBound", BOUND_FULL)
            }
        }.getOrNull()
    }

    /**
     * 把这一帧要用的 uniform 全部写进 shader。
     *
     * @param time 动画时间，单位**秒**。
     */
    fun prepare(width: Float, height: Float, time: Float, preset: BgEffectConfig) {
        val target = shader ?: return
        applyPreset(target, preset)
        updateResolution(target, width, height)
        updateAnimTime(target, time)
        updatePointsAnim(target, time, preset)
        // 色彩循环的「阶段」= 时间除以该预设的插值周期，见 BgEffectConfig.colorInterpPeriod。
        updateColors(target, preset, time / preset.colorInterpPeriod)
    }

    private fun applyPreset(shader: RuntimeShader, preset: BgEffectConfig) {
        if (cachedPreset === preset) return
        shader.setFloatUniform("uPoints", preset.points)
        shader.setFloatUniform("uLightOffset", preset.lightOffset)
        shader.setFloatUniform("uSaturateOffset", preset.saturateOffset)
        cachedPreset = preset
    }

    private fun updateResolution(shader: RuntimeShader, width: Float, height: Float) {
        if (resolution[0] == width && resolution[1] == height) return
        resolution[0] = width
        resolution[1] = height
        shader.setFloatUniform("uResolution", resolution)
    }

    private fun updateAnimTime(shader: RuntimeShader, time: Float) {
        if (animTime == time) return
        animTime = time
        shader.setFloatUniform("uAnimTime", time)
    }

    /**
     * 让 4 个控制点各自绕着原位画小圈。
     *
     * 横纵两轴用的是不同的三角函数（x 用 sin、y 用 cos，且 cos 的相位又叠上了算好的 x），
     * 所以四个点不会同步漂移成整齐的方阵 —— 这条式子是移植来源里就这么写的，没改。
     */
    private fun updatePointsAnim(shader: RuntimeShader, time: Float, preset: BgEffectConfig) {
        if (cachedPointsAnimTime == time && cachedPreset === preset) return
        val offset = preset.pointOffset
        for (i in 0 until POINT_COUNT) {
            val srcX = preset.points[i * 3]
            val srcY = preset.points[i * 3 + 1]
            val animatedX = srcX + sin(time + srcY) * offset
            val animatedY = srcY + cos(time + animatedX) * offset
            pointsAnimBuffer[i * 2] = animatedX
            pointsAnimBuffer[i * 2 + 1] = animatedY
        }
        shader.setFloatUniform("uPointsAnim", pointsAnimBuffer)
        cachedPointsAnimTime = time
    }

    /**
     * 在 [BgEffectConfig] 的三组循环色之间插值。
     *
     * [stage] 的小数部分做插值、整数部分按 `mod 4` 轮转，所以四步一循环
     * （0→colors2、1→colors1、2→colors2、3→colors3）。
     */
    private fun updateColors(shader: RuntimeShader, preset: BgEffectConfig, stage: Float) {
        if (cachedColorStage == stage && cachedPreset === preset) return
        val base = stage.toInt()
        val fraction = stage - base
        val start = colorsForCycleIndex(preset, base)
        val end = colorsForCycleIndex(preset, base + 1)
        for (i in 0 until COLOR_COMPONENT_COUNT) {
            colorsBuffer[i] = start[i] + (end[i] - start[i]) * fraction
        }
        shader.setFloatUniform("uColors", colorsBuffer)
        cachedColorStage = stage
    }

    private fun colorsForCycleIndex(preset: BgEffectConfig, index: Int): FloatArray =
        when (index.mod(COLOR_STAGE_COUNT)) {
            1 -> preset.colors1
            3 -> preset.colors3
            else -> preset.colors2
        }

    private companion object {
        const val U_TRANSLATE_Y = 0f
        const val U_ALPHA_MULTI = 1f
        const val U_NOISE_SCALE = 1.5f
        const val U_POINT_RADIUS_MULTI = 1f

        /** 铺满整个绘制区域的 bound 常量。 */
        val BOUND_FULL = floatArrayOf(0f, 0f, 1f, 1f)

        const val POINT_COUNT = 4

        /** 4 个颜色 × 4 个 RGBA 分量。 */
        const val COLOR_COMPONENT_COUNT = 16

        const val COLOR_STAGE_COUNT = 4
    }
}

/**
 * 炫彩背景的运行时状态：一份 [BgEffectPainter] + 一个由帧回调推进的时间。
 *
 * [preset] 是普通字段而不是 State —— 它由 [rememberBgEffectState] 在组合期赋值，
 * 跟着主题深浅走；写成 State 反而会在组合期产生写入、招来无谓的重组。
 */
internal class BgEffectState {
    internal val painter = BgEffectPainter()
    internal var preset: BgEffectConfig = BgEffectConfig.of(isDark = false)
    internal val animTime = mutableFloatStateOf(0f)

    internal val supported: Boolean get() = painter.supported

    /** 把当前帧的 uniform 写好并把整屏铺满。 */
    internal fun draw(scope: DrawScope) {
        val pen = painter.brush ?: return
        painter.prepare(
            width = scope.size.width,
            height = scope.size.height,
            time = animTime.value,
            preset = preset,
        )
        scope.drawRect(brush = pen)
    }
}

/** 炫彩背景的绘制帧率。背景是缓慢流动的，30fps 足够；跑满 60 只是白烧电。 */
private const val BG_EFFECT_FRAME_RATE = 30

/** 每帧之间的最小间隔（毫秒），用来把 60Hz 的帧回调压到 [BG_EFFECT_FRAME_RATE]。 */
private const val BG_EFFECT_FRAME_INTERVAL_MS = 1000f / BG_EFFECT_FRAME_RATE

/**
 * 建立并记住一份 [BgEffectState]，同时起一个帧回调推进动画时间。
 *
 * 时间由 [withFrameMillis] 推，而不是 `rememberInfiniteTransition` —— 后者只能给出一个
 * 规整的 0→1 插值量，而 shader 要的是**单调递增的秒数**（[BgEffectPainter] 拿它既算
 * 噪声相位、又算点位漂移、还算色彩循环的阶段）。帧回调里按 [BG_EFFECT_FRAME_RATE] 节流。
 *
 * 设备不支持 shader 时**不起帧回调**，state 的 [BgEffectState.supported] 为 false，
 * 调用点自己决定降级画什么。
 */
@Composable
internal fun rememberBgEffectState(): BgEffectState {
    // 深浅模式从当前 scheme 的 surface 亮度判断，而不是 isSystemInDarkTheme()：
    // 用固定色板时系统深浅和 App 的配色可能不是一回事。
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val state = remember { BgEffectState() }
    state.preset = remember(isDark) { BgEffectConfig.of(isDark) }

    if (state.supported) {
        LaunchedEffect(state) {
            var startMillis = 0L
            var lastEmitMillis = 0L
            while (true) {
                withFrameMillis { nowMillis ->
                    if (startMillis == 0L) {
                        startMillis = nowMillis
                        lastEmitMillis = nowMillis
                    }
                    if (nowMillis - lastEmitMillis >= BG_EFFECT_FRAME_INTERVAL_MS) {
                        lastEmitMillis = nowMillis
                        state.animTime.value = (nowMillis - startMillis) / 1000f
                    }
                }
            }
        }
    }
    return state
}

/**
 * 把炫彩 shader 铺满本 Modifier 的区域。
 *
 * 设备不支持时**原样返回、什么都不画** —— 降级由调用点负责铺一层别的背景，
 * 而不是在这里偷偷画个近似的东西。
 */
internal fun Modifier.bgEffectShader(state: BgEffectState): Modifier =
    if (state.supported) drawBehind { state.draw(this) } else this
