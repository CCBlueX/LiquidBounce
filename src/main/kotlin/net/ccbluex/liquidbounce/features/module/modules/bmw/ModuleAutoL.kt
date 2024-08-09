package net.ccbluex.liquidbounce.features.module.modules.bmw

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.events.AttackEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.entity.Entity

object ModuleAutoL : Module("AutoL", Category.BMW) {

    enum class WordPatternChoices(override val choiceName: String) : NamedChoice {
        POEM("Poem"),
        CUSTOM("Custom"),
    }
    private val wordPattern by enumChoice("WordPattern", WordPatternChoices.POEM)
    private val customMessages by textArray("CustomMessages", mutableListOf())
    private val nameInFront by boolean("NameInFront", true)
    private val advertisementInEnd by boolean("AdvertisementInEnd", true)

    private var enemies = mutableListOf<Entity>()

    val worldChangeEvent = handler<WorldChangeEvent> {
        enemies.clear()
    }

    val attackEventHandler = handler<AttackEvent> { event ->
        if (event.enemy.isPlayer && !enemies.contains(event.enemy)) {
            enemies.add(event.enemy)
        }
    }

    val repeatHandler = repeatable {
        enemies.filter { !it.isAlive }.forEach {
            sayL(it)
            enemies.remove(it)
        }
    }

    private val poems = listOf(
        "立志用功如种树然，方其根芽，犹未有干；及其有干，尚未有枝；枝而后叶，叶而后花。",
        "骐骥一跃，不能十步；驽马十驾，功在不舍；锲而舍之，朽木不折；锲而不舍，金石可镂。",
        "天见其明，地见其光，君子贵其全也。",
        "只有功夫深，铁杵磨成针。",
        "一言既出，驷马难追。",
        "为一身谋则愚，而为天下则智。",
        "处其厚，不居其薄，处其实，不居其华。",
        "白沙在涅，与之俱黑。",
        "如果永远是晴天，土地也会布满裂痕。",
        "只有知识之海，才能载起成才之舟。",
        "谬论从门缝钻进，真理立于门前。",
        "自其变者而观之，则天地曾不能以一瞬；自其不变者而观之，则物与我皆无尽也。"
    )

    private fun sayL(entity: Entity) {
        var message = when (wordPattern) {
            WordPatternChoices.POEM -> poems.random()
            WordPatternChoices.CUSTOM -> customMessages.random()
        }
        if (nameInFront) {
            message = entity.name.literalString!! + " " + message
        }
        if (advertisementInEnd) {
            message += " --BMWClient kook 92333691"
        }
        network.sendChatMessage(message)
    }
}
