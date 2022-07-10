/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import net.ccbluex.liquidbounce.injection.implementations.IMixinGuiSlot
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.extensions.sin
import net.ccbluex.liquidbounce.utils.misc.HttpUtils
import net.ccbluex.liquidbounce.utils.render.CustomTexture
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.runAsync
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiSlot
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.*
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import javax.imageio.ImageIO

class GuiContributors(private val prevGui: GuiScreen) : GuiScreen()
{
    @Suppress("PrivatePropertyName")
    private val DECIMAL_FORMAT = NumberFormat.getInstance(Locale.US) as DecimalFormat
    private lateinit var list: GuiList

    private var credits: List<Credit> = Collections.emptyList()
    private var exception: Throwable? = null

    override fun initGui()
    {
        list = GuiList(this)
        list.registerScrollButtons(7, 8)
        list.elementClicked(-1, false, 0, 0)

        buttonList.add(GuiButton(1, (width shr 1) - 100, height - 30, "Back"))

        exception = null

        runAsync(::loadCredits)
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
    {
        drawBackground(0)

        list.drawScreen(mouseX, mouseY, partialTicks)

        RenderUtils.drawRect((width shr 2).toFloat(), 40.0f, width.toFloat(), height - 40.0f, Integer.MIN_VALUE)

        if (list.getSelectedSlot() != -1)
        {
            val credit = credits[list.getSelectedSlot()]

            var y = 45
            val x = (width shr 2) + 5
            var infoOffset = 0

            val avatar = credit.avatar

            val imageSize = fontRendererObj.FONT_HEIGHT shl 2

            if (avatar != null)
            {
                GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)

                GlStateManager.enableAlpha()
                GlStateManager.enableBlend()
                GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO)
                GlStateManager.enableTexture2D()

                RenderUtils.resetColor()

                GlStateManager.bindTexture(avatar.textureId)


                GL11.glBegin(GL11.GL_QUADS)

                GL11.glTexCoord2f(0f, 0f)
                GL11.glVertex2i(x, y)
                GL11.glTexCoord2f(0f, 1f)
                GL11.glVertex2i(x, y + imageSize)
                GL11.glTexCoord2f(1f, 1f)
                GL11.glVertex2i(x + imageSize, y + imageSize)
                GL11.glTexCoord2f(1f, 0f)
                GL11.glVertex2i(x + imageSize, y)

                GL11.glEnd()

                GlStateManager.bindTexture(0)

                GlStateManager.disableBlend()

                infoOffset = imageSize

                GL11.glPopAttrib()
            }

            y += imageSize

            Fonts.font40.drawString("@" + credit.name, x + infoOffset + 5.0f, 48f, -1, true)
            Fonts.font40.drawString("${credit.commits} commits \u00A7a${DECIMAL_FORMAT.format(credit.additions)}++ \u00A74${DECIMAL_FORMAT.format(credit.deletions)}--", x + infoOffset + 5.0f, (y - Fonts.font40.fontHeight).toFloat(), -1, true)

            for (s in credit.contributions)
            {
                y += Fonts.font40.fontHeight + 2

                GlStateManager.disableTexture2D()
                RenderUtils.resetColor()
                GL11.glBegin(GL11.GL_LINES)

                val middleFont = Fonts.font40.fontHeight shr 1
                GL11.glVertex2f(x.toFloat(), y + middleFont - 1f)
                GL11.glVertex2f(x + 3.0f, y + middleFont - 1f)

                GL11.glEnd()

                Fonts.font40.drawString(s, x + 5f, y.toFloat(), -1, true)
            }
        }

        Fonts.font40.drawCenteredString("Contributors", (width shr 1).toFloat(), 6F, 0xffffff)

        if (credits.isEmpty())
        {
            val posX = (width shr 3).toFloat()
            val posY = (height shr 1).toFloat()

            if (exception != null)
            {
                val gb = (((System.currentTimeMillis() * (1 / 333.0F)).sin + 1) * (0.5 * 255)).toInt()
                Fonts.font40.drawCenteredString("Failed to load ($exception)", posX, posY, Color(255, gb, gb).rgb)
            }
            else
            {
                Fonts.font40.drawCenteredString("Loading...", posX, posY, -1)
                RenderUtils.drawLoadingCircle(posX, (height shr 1) - 40.0f)
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun actionPerformed(button: GuiButton)
    {
        if (button.id == 1) mc.displayGuiScreen(prevGui)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int)
    {
        when (keyCode)
        {
            Keyboard.KEY_ESCAPE ->
            {
                mc.displayGuiScreen(prevGui)
                return
            }

            Keyboard.KEY_UP -> list.elementClicked((list.getSelectedSlot() - 1).coerceAtLeast(0), false, 0, 0)
            Keyboard.KEY_DOWN -> list.elementClicked((list.getSelectedSlot() + 1).coerceAtMost(list.size - 1), false, 0, 0)
            Keyboard.KEY_NEXT -> list.scrollBy(height - 100)

            Keyboard.KEY_PRIOR ->
            {
                list.scrollBy(-height + 100)
                return
            }

            else -> super.keyTyped(typedChar, keyCode)
        }
    }

    override fun handleMouseInput()
    {
        super.handleMouseInput()
        list.handleMouseInput()
    }

    private fun loadCredits() = try
    {
        val gson = Gson()
        val jsonParser = JsonParser()

        val gitHubContributors = gson.fromJson(HttpUtils["https://api.github.com/repos/hsheric0210/LiquidBounce/stats/contributors"], Array<GitHubContributor>::class.java)
        val additionalInformation = jsonParser.parse(HttpUtils["https://raw.githubusercontent.com/CCBlueX/LiquidCloud/master/LiquidBounce/contributors.json"]).asJsonObject

        val credits = ArrayList<Credit>(gitHubContributors.size)

        for ((_, weeks, author) in gitHubContributors)
        {
            var contributorInformation: ContributorInformation? = null
            val jsonElement = additionalInformation[author.id.toString()]

            if (jsonElement != null) contributorInformation = gson.fromJson(jsonElement, ContributorInformation::class.java)

            var additions = 0
            var deletions = 0
            var commits = 0

            for ((_, weekAdditions, weekDeletions, weekCommits) in weeks)
            {
                additions += weekAdditions
                deletions += weekDeletions
                commits += weekCommits
            }

            credits.add(Credit(author.name, author.avatarUrl, null, additions, deletions, commits, contributorInformation?.teamMember ?: false, contributorInformation?.contributions ?: Collections.emptyList()))
        }

        credits.sortWith(object : Comparator<Credit>
        {
            override fun compare(o1: Credit, o2: Credit): Int
            {
                if (o1.isTeamMember && o2.isTeamMember) return -o1.commits.compareTo(o2.commits)

                if (o1.isTeamMember) return -1
                if (o2.isTeamMember) return 1

                return -o1.additions.compareTo(o2.additions)
            }
        })

        this.credits = credits

        for (credit in credits)
        {
            try
            {
                HttpUtils.requestStream("${credit.avatarUrl}?s=${fontRendererObj.FONT_HEIGHT shl 2}", "GET").use { credit.avatar = run { CustomTexture(ImageIO.read(it) ?: return@run null) } }
            }
            catch (e: Exception)
            {
                ClientUtils.logger.error("Failed to load ${credit.name}'s avatar.", e)
            }
        }
    }
    catch (e: Exception)
    {
        ClientUtils.logger.error("Failed to load credits.", e)
        exception = e
    }

    internal class ContributorInformation(val name: String, val teamMember: Boolean, val contributions: List<String>)

    internal data class GitHubContributor(@SerializedName("total") val totalContributions: Int, val weeks: List<GitHubWeek>, val author: GitHubAuthor)
    internal data class GitHubWeek(@SerializedName("w") val timestamp: Long, @SerializedName("a") val additions: Int, @SerializedName("d") val deletions: Int, @SerializedName("c") val commits: Int)
    internal data class GitHubAuthor(@SerializedName("login") val name: String, val id: Int, @SerializedName("avatar_url") val avatarUrl: String)

    internal class Credit(val name: String, val avatarUrl: String, var avatar: CustomTexture?, val additions: Int, val deletions: Int, val commits: Int, val isTeamMember: Boolean, val contributions: List<String>)

    private inner class GuiList(gui: GuiScreen) : GuiSlot(mc, gui.width shr 2, gui.height, 40, gui.height - 40, 15)
    {
        init
        {
            @Suppress("CAST_NEVER_SUCCEEDS")
            val mixin = this as IMixinGuiSlot
            mixin.setListWidth(gui.width * 3 / 13)
            mixin.setEnableScissor(true)
        }

        private var selectedSlot = 0

        override fun isSelected(id: Int) = selectedSlot == id

        public override fun getSize() = credits.size

        fun getSelectedSlot() = if (selectedSlot > credits.size) -1 else selectedSlot

        public override fun elementClicked(id: Int, doubleClick: Boolean, var3: Int, var4: Int)
        {
            selectedSlot = id
        }

        override fun drawSlot(id: Int, x: Int, y: Int, var4: Int, mouseXIn: Int, mouseYIn: Int)
        {
            val credit = credits[id]

            Fonts.font40.drawCenteredString(credit.name, (width shr 1).toFloat(), y + 2F, -1, true)
        }

        override fun drawBackground()
        {
        }
    }
}
