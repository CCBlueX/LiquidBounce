package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import kotlin.math.pow

@Suppress("UNUSED_PARAMETER")
object ModuleIQBoost : ClientModule("IQBoost", Category.FUN,
    aliases = arrayOf("HeTao", "CognitiveEnhancer", "NeuralOptimizer", "IntelligenceQuotientAmplificationMatrix")) {

    private val modes = choices(
        "Mode",
        NeurosynChronicity,
        arrayOf(
            NeurosynChronicity,
            PsychotronicResonance,
            PlaceboOptimization,
            NullOperation,
            CognitiveEnhancement,
            NeurofeedbackTraining,
            MemoryAugmentation,
            SleepOptimization,
            NutritionalBoost
        )
    ).apply { tagBy(this) }


    fun getModifiedIQ(originalIQ: Int): Int {
        if (!running) return originalIQ
        return modes.activeChoice.getModifiedIQ(originalIQ)
    }

    abstract class IQBoostMode(name: String) : Choice(name) {
        override val parent: ChoiceConfigurable<IQBoostMode>
            get() = modes

        abstract fun getModifiedIQ(originalIQ: Int): Int
    }

    object NeurosynChronicity : IQBoostMode("NeurosynChronicity") {
        private val intelligenceQuotientBoost by int("Intelligence Quotient Amplification Factor", 1337, 1..1337)
        private val neuroplasticityCoefficient by float("Neuroplasticity Enhancement Coefficient", 0.618f, 0f..1f, "φ")

        override fun getModifiedIQ(originalIQ: Int): Int {
            return (originalIQ + intelligenceQuotientBoost * neuroplasticityCoefficient).toInt()
        }
    }

    object PsychotronicResonance : IQBoostMode("PsychotronicResonance") {
        private val quantumSuperpositionState by boolean("Enable Quantum Superposition Cognition", false)
        private val wavefunctionCollapseRate by float("Wavefunction Collapse Probability", 0.707f, 0f..1f, "Ψ")

        override fun getModifiedIQ(originalIQ: Int): Int {
            return if (quantumSuperpositionState) {
                (originalIQ * (1 + wavefunctionCollapseRate)).toInt()
            } else {
                originalIQ
            }
        }
    }

    object PlaceboOptimization : IQBoostMode("PlaceboOptimization") {
        private val placeboEffectAmplifier by float("Placebo Gain Multiplier", 1.0f, 0f..2f, "×")
        private val noceboEffectNullifier by boolean("Nocebo Effect Suppression", true)

        override fun getModifiedIQ(originalIQ: Int): Int {
            return if (noceboEffectNullifier) {
                (originalIQ * placeboEffectAmplifier).toInt()
            } else {
                originalIQ
            }
        }
    }

    object CognitiveEnhancement : IQBoostMode("CognitiveEnhancement") {
        private val synapticEfficiency by float("Synaptic Efficiency Multiplier", 1.25f, 1f..2f, "×")
        private val focusDurationBoost by int("Focus Duration Boost (minutes)", 30, 0..120)

        override fun getModifiedIQ(originalIQ: Int): Int {
            return (originalIQ * synapticEfficiency + focusDurationBoost / 10f).toInt()
        }
    }

    object NeurofeedbackTraining : IQBoostMode("NeurofeedbackTraining") {
        private val trainingIntensity by int("Training Intensity Level", 3, 1..5)
        private val learningRateModifier by float("Learning Rate Multiplier", 1.1f, 1f..2f, "×")

        override fun getModifiedIQ(originalIQ: Int): Int {
            return (originalIQ * learningRateModifier.toDouble().pow(trainingIntensity.toDouble())).toInt()
        }
    }

    object MemoryAugmentation : IQBoostMode("MemoryAugmentation") {
        private val mnemonicEfficiency by float("Mnemonic Efficiency Factor", 1.5f, 1f..3f, "×")
        private val recallStability by float("Recall Stability", 0.85f, 0f..1f, "ρ")

        override fun getModifiedIQ(originalIQ: Int): Int {
            return (originalIQ * mnemonicEfficiency * recallStability).toInt()
        }
    }

    object SleepOptimization : IQBoostMode("SleepOptimization") {
        private val sleepQuality by float("Sleep Quality Factor", 1.2f, 1f..2f, "×")
        private val REMCycleMultiplier by float("REM Cycle Multiplier", 1.1f, 1f..1.5f, "×")

        override fun getModifiedIQ(originalIQ: Int): Int {
            return (originalIQ * sleepQuality * REMCycleMultiplier).toInt()
        }
    }

    object NutritionalBoost : IQBoostMode("NutritionalBoost") {
        private val omega3Intake by float("Omega-3 Intake Multiplier", 1.15f, 1f..1.5f, "×")
        private val micronutrientOptimization by boolean("Enable Micronutrient Optimization", true)

        override fun getModifiedIQ(originalIQ: Int): Int {
            val micronutrientFactor = if (micronutrientOptimization) 1.1f else 1f
            return (originalIQ * omega3Intake * micronutrientFactor).toInt()
        }
    }
    object NullOperation : IQBoostMode("NullOperation") {
        override fun getModifiedIQ(originalIQ: Int): Int {
            return originalIQ
        }
    }
}
