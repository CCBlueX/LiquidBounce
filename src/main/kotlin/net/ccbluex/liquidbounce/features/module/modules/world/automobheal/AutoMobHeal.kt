/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.features.module.modules.world.automobheal

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.world.automobheal.MobFoodOption.Companion.foodNutritionHeal
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.entity.interactEntity
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.minecraft.tags.ItemTags
import net.minecraft.util.ToFloatFunction
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.camel.Camel
import net.minecraft.world.entity.animal.camel.CamelHusk
import net.minecraft.world.entity.animal.equine.AbstractHorse
import net.minecraft.world.entity.animal.equine.Llama
import net.minecraft.world.entity.animal.equine.SkeletonHorse
import net.minecraft.world.entity.animal.equine.ZombieHorse
import net.minecraft.world.entity.animal.feline.Cat
import net.minecraft.world.entity.animal.golem.IronGolem
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.minecraft.world.entity.animal.wolf.Wolf
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3
import java.util.function.ToIntFunction
import kotlin.math.abs

/**
 * Automatically heals nearby vanilla-healable mobs using their vanilla repair or feeding items.
 */
object AutoMobHeal : ClientModule(
    "AutoMobHeal",
    ModuleCategories.WORLD,
    aliases = listOf("AutoGolemRepair"),
) {

    private val range by float("Range", 4.5f, 1f..6f)
    private val delay by intRange("Delay", 4..4, 0..40, "ticks")
    private val slotResetDelay by int("SlotResetDelay", 2, 0..20, "ticks")
    private val swingMode by enumChoice("SwingMode", SwingMode.DO_NOT_HIDE)

    private data class HealPlan(
        val target: LivingEntity,
        val slot: HotbarItemSlot,
        val distanceSq: Double,
    )

    private data class FoodCandidate(
        val slot: HotbarItemSlot,
        val option: MobFoodOption,
        val healAmount: Float,
    )

    private sealed class HealTarget<T : LivingEntity>(
        name: String,
        private val entityClass: Class<T>,
    ) : ToggleableValueGroup(this@AutoMobHeal, name, enabled = true) {

        private val healthThreshold by float("HealthThreshold", 75f, 1f..100f, "%")

        @Suppress("LoopWithTooManyJumpStatements")
        fun findPlan(maxRangeSq: Double, eyePosition: Vec3): HealPlan? {
            if (!running) {
                return null
            }

            val minHealthRatio = (healthThreshold / 100f).coerceIn(0f, 1f)
            var bestPlan: HealPlan? = null

            for (entity in world.entitiesForRendering()) {
                val typedEntity = cast(entity) ?: continue
                if (!player.hasLineOfSight(typedEntity) ||
                    !canInteract(typedEntity) ||
                    !shouldHeal(typedEntity, minHealthRatio)
                ) {
                    continue
                }

                val distanceSq = typedEntity.squaredBoxedDistanceTo(eyePosition)
                if (distanceSq > maxRangeSq || distanceSq >= (bestPlan?.distanceSq ?: Double.POSITIVE_INFINITY)) {
                    continue
                }

                val slot = findSlot(typedEntity) ?: continue
                bestPlan = HealPlan(typedEntity, slot, distanceSq)
            }

            return bestPlan
        }

        protected abstract fun findSlot(entity: T): HotbarItemSlot?

        protected open fun canInteract(entity: T): Boolean {
            return !isBlockedBySecondaryUse(entity)
        }

        protected open fun shouldHeal(entity: T, minHealthRatio: Float): Boolean {
            return entity.isAlive && entity.health <= entity.maxHealth * minHealthRatio
        }

        @Suppress("UNCHECKED_CAST")
        private fun cast(entity: Entity): T? {
            return if (entityClass.isInstance(entity)) {
                entity as T
            } else {
                null
            }
        }

        protected open class FoodHealTarget<T : LivingEntity>(
            name: String,
            entityClass: Class<T>,
        ) : HealTarget<T>(name, entityClass) {
            private val noWaste by boolean("NoWaste", true)

            protected open val preferNonBucketFoodAlways: Boolean = false
            protected open val allowBucketFood: Boolean
                get() = true

            protected open fun foodOptions(entity: T): List<MobFoodOption> = emptyList()

            final override fun findSlot(entity: T): HotbarItemSlot? {
                val missingHealth = (entity.maxHealth - entity.health).coerceAtLeast(0f)
                val candidates = Slots.OffhandWithHotbar.mapNotNull { slot ->
                    val option = foodOptions(entity).firstOrNull {
                        it.test.test(slot.itemStack)
                    } ?: return@mapNotNull null
                    if (option.isBucket && !allowBucketFood) {
                        return@mapNotNull null
                    }

                    FoodCandidate(
                        slot = slot,
                        option = option,
                        healAmount = option.healAmount.applyAsFloat(slot.itemStack),
                    )
                }

                if (candidates.isEmpty()) {
                    return null
                }

                val allowedCandidates = if (noWaste) {
                    candidates.filterNot { wouldWasteHealing(it, missingHealth) }
                } else {
                    candidates
                }

                return allowedCandidates.minWithOrNull(foodCandidateComparator(missingHealth))?.slot
            }

            private fun foodCandidateComparator(missingHealth: Float): Comparator<FoodCandidate> {
                return Comparator.comparingInt(ToIntFunction(::bucketPenalty))
                    .thenComparing { wouldWasteHealing(it, missingHealth) }
                    .thenComparingDouble { healingDelta(it, missingHealth).toDouble() }
                    .thenComparing({ it.slot }, HotbarItemSlot.PREFER_NEARBY)
            }

            private fun wouldWasteHealing(candidate: FoodCandidate, missingHealth: Float): Boolean {
                return candidate.healAmount > missingHealth
            }

            private fun healingDelta(candidate: FoodCandidate, missingHealth: Float): Float {
                return abs(candidate.healAmount - missingHealth)
            }

            private fun bucketPenalty(candidate: FoodCandidate): Int {
                return if (preferNonBucketFoodAlways && candidate.option.isBucket) 1 else 0
            }
        }

        protected sealed class BucketFoodHealTarget<T : LivingEntity>(
            name: String,
            entityClass: Class<T>,
        ) : FoodHealTarget<T>(name, entityClass) {
            override val allowBucketFood by boolean("AllowBucketFood", false)
        }

        protected class SimpleHealFoodTarget<T : LivingEntity>(
            name: String,
            entityClass: Class<T>,
            private val options: List<MobFoodOption>,
        ) : FoodHealTarget<T>(name, entityClass) {
            override fun foodOptions(entity: T): List<MobFoodOption> = options
        }

        /**
         * @see net.minecraft.world.entity.animal.golem.IronGolem.mobInteract
         */
        object IronGolemTarget : HealTarget<IronGolem>(
            "IronGolem",
            IronGolem::class.java,
        ) {
            override fun findSlot(entity: IronGolem): HotbarItemSlot? =
                Slots.OffhandWithHotbar.findClosestSlot(Items.IRON_INGOT)
        }

        /**
         * @see net.minecraft.world.entity.animal.wolf.Wolf.mobInteract
         */
        object WolfTarget : FoodHealTarget<Wolf>(
            "Wolf",
            Wolf::class.java,
        ) {
            private val options = listOf(
                MobFoodOption(ItemTags.WOLF_FOOD, healAmount2xNutrition),
            )

            override fun foodOptions(entity: Wolf): List<MobFoodOption> = options

            override fun shouldHeal(entity: Wolf, minHealthRatio: Float): Boolean {
                return entity.isTame && super.shouldHeal(entity, minHealthRatio)
            }
        }

        /**
         * @see net.minecraft.world.entity.animal.feline.Cat.mobInteract
         */
        object CatTarget : FoodHealTarget<Cat>(
            "Cat",
            Cat::class.java,
        ) {
            private val options = listOf(
                MobFoodOption(ItemTags.CAT_FOOD, healAmount1xNutrition),
            )

            override fun foodOptions(entity: Cat): List<MobFoodOption> = options

            override fun shouldHeal(entity: Cat, minHealthRatio: Float): Boolean {
                return entity.isOwnedBy(player) && super.shouldHeal(entity, minHealthRatio)
            }
        }

        /**
         * @see net.minecraft.world.entity.animal.equine.Horse.mobInteract
         * @see net.minecraft.world.entity.animal.equine.AbstractHorse.handleEating
         */
        object HorseFamilyTarget : FoodHealTarget<AbstractHorse>(
            "HorseFamily",
            AbstractHorse::class.java,
        ) {
            private val allowGoldenCarrot by boolean("AllowGoldenCarrot", false)
            private val allowGoldenApple by boolean("AllowGoldenApple", false)
            private val allowEnchantedGoldenApple by boolean("AllowEnchantedGoldenApple", false)

            override fun foodOptions(entity: AbstractHorse): List<MobFoodOption> = buildList {
                add(MobFoodOption(Items.SUGAR, 1f))
                add(MobFoodOption(Items.WHEAT, 2f))
                add(MobFoodOption(Items.APPLE, 3f))
                add(MobFoodOption(Items.CARROT, 3f))
                add(MobFoodOption(Items.HAY_BLOCK, 20f))

                if (allowGoldenCarrot) {
                    add(MobFoodOption(Items.GOLDEN_CARROT, 4f))
                }
                if (allowGoldenApple) {
                    add(MobFoodOption(Items.GOLDEN_APPLE, 10f))
                }
                if (allowEnchantedGoldenApple) {
                    add(MobFoodOption(Items.ENCHANTED_GOLDEN_APPLE, 10f))
                }
            }

            override fun shouldHeal(
                entity: AbstractHorse,
                minHealthRatio: Float,
            ): Boolean {
                return entity !is Llama &&
                    entity !is Camel &&
                    entity !is ZombieHorse &&
                    entity !is SkeletonHorse &&
                    super.shouldHeal(entity, minHealthRatio)
            }
        }

        /**
         * @see Camel.handleEating
         */
        object CamelTarget : FoodHealTarget<Camel>(
            "Camel",
            Camel::class.java,
        ) {
            private val options = listOf(
                MobFoodOption(Items.CACTUS, 2f),
            )

            override fun foodOptions(entity: Camel): List<MobFoodOption> = options

            override fun shouldHeal(entity: Camel, minHealthRatio: Float): Boolean {
                return entity !is CamelHusk && super.shouldHeal(entity, minHealthRatio)
            }
        }

        /**
         * @see net.minecraft.world.entity.animal.nautilus.AbstractNautilus.mobInteract
         */
        object NautilusTarget : BucketFoodHealTarget<AbstractNautilus>(
            "Nautilus",
            AbstractNautilus::class.java,
        ) {
            override val preferNonBucketFoodAlways: Boolean
                get() = true

            private val options = listOf(
                MobFoodOption(Items.COD, healAmount2xNutrition),
                MobFoodOption(Items.SALMON, healAmount2xNutrition),
                MobFoodOption(Items.TROPICAL_FISH, healAmount2xNutrition),
                MobFoodOption(Items.PUFFERFISH, healAmount2xNutrition),
                MobFoodOption(Items.COOKED_COD, healAmount2xNutrition),
                MobFoodOption(Items.COOKED_SALMON, healAmount2xNutrition),
                MobFoodOption.ofBucket(Items.COD_BUCKET),
                MobFoodOption.ofBucket(Items.SALMON_BUCKET),
                MobFoodOption.ofBucket(Items.TROPICAL_FISH_BUCKET),
                MobFoodOption.ofBucket(Items.PUFFERFISH_BUCKET),
            )

            override fun foodOptions(entity: AbstractNautilus): List<MobFoodOption> = options

            override fun shouldHeal(
                entity: AbstractNautilus,
                minHealthRatio: Float,
            ): Boolean {
                return !entity.isBaby && entity.isTame && super.shouldHeal(entity, minHealthRatio)
            }
        }

        companion object {
            private val healAmount1xNutrition = ToFloatFunction<ItemStack> { stack -> foodNutritionHeal(stack, 1f) }
            private val healAmount2xNutrition = ToFloatFunction<ItemStack> { stack -> foodNutritionHeal(stack, 2f) }

            /**
             * @see ZombieHorse.mobInteract
             * @see AbstractHorse.handleEating
             */
            @JvmField
            val ZombieHorseTarget = SimpleHealFoodTarget(
                "ZombieHorse",
                ZombieHorse::class.java,
                listOf(
                    MobFoodOption(Items.RED_MUSHROOM, 3f),
                ),
            )

            /**
             * @see Llama.handleEating
             */
            @JvmField
            val LlamaTarget = SimpleHealFoodTarget(
                "Llama",
                Llama::class.java,
                listOf(
                    MobFoodOption(Items.WHEAT, 2f),
                    MobFoodOption(Items.HAY_BLOCK, 10f),
                ),
            )

            /**
             * @see CamelHusk.isFood
             * @see Camel.handleEating
             */
            @JvmField
            val CamelHuskTarget = SimpleHealFoodTarget(
                "CamelHusk",
                CamelHusk::class.java,
                listOf(
                    MobFoodOption(Items.RABBIT_FOOT, 2f),
                ),
            )
        }
    }

    private val healTargets = arrayOf(
        HealTarget.IronGolemTarget,
        HealTarget.WolfTarget,
        HealTarget.CatTarget,
        HealTarget.HorseFamilyTarget,
        HealTarget.ZombieHorseTarget,
        HealTarget.LlamaTarget,
        HealTarget.CamelTarget,
        HealTarget.CamelHuskTarget,
        HealTarget.NautilusTarget,
    )

    init {
        healTargets.forEach { tree(it) }
    }

    @Suppress("unused")
    private val repeatable = tickHandler {
        if (player.isUsingItem || mc.screen != null) {
            return@tickHandler
        }

        val maxRange = minOf(range.toDouble(), player.entityInteractionRange())
        val maxRangeSq = maxRange * maxRange
        val eyePosition = player.eyePosition

        val plan = healTargets
            .mapNotNull { it.findPlan(maxRangeSq, eyePosition) }
            .minByOrNull { it.distanceSq }
            ?: return@tickHandler

        SilentHotbar.selectSlotSilently(this, plan.slot, slotResetDelay)

        val result = interactEntity(
            plan.target,
            hand = plan.slot.useHand,
            swingMode = swingMode,
        )

        if (result?.consumesAction() == true) {
            waitTicks(delay.random())
        }
    }

    override fun onDisabled() {
        SilentHotbar.resetSlot(this)
    }

    private fun isBlockedBySecondaryUse(entity: LivingEntity): Boolean {
        if (!player.isSecondaryUseActive) {
            return false
        }

        return when (entity) {
            is Camel -> !entity.isBaby
            is AbstractHorse -> entity.isTamed && !entity.isBaby
            is AbstractNautilus -> entity.isTame && !entity.isBaby
            else -> false
        }
    }

}
