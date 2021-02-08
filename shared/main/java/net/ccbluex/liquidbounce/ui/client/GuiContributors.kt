/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiScreen
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen
import net.ccbluex.liquidbounce.api.util.WrappedGuiSlot
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.WorkerUtils
import net.ccbluex.liquidbounce.utils.misc.HttpUtils
import net.ccbluex.liquidbounce.utils.render.CustomTexture
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import javax.imageio.ImageIO

class GuiContributors(private val prevGui: IGuiScreen) : WrappedGuiScreen()
{
	@Suppress("PrivatePropertyName")
	private val DECIMAL_FORMAT = NumberFormat.getInstance(Locale.US) as DecimalFormat
	private lateinit var list: GuiList

	private var credits: List<Credit> = Collections.emptyList()
	private var failed = false

	override fun initGui()
	{
		list = GuiList(representedScreen)
		list.represented.registerScrollButtons(7, 8)
		list.represented.elementClicked(-1, false, 0, 0)

		representedScreen.buttonList.add(classProvider.createGuiButton(1, representedScreen.width / 2 - 100, representedScreen.height - 30, "Back"))

		failed = false

		WorkerUtils.workers.submit(::loadCredits)
	}

	override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
	{
		representedScreen.drawBackground(0)

		list.represented.drawScreen(mouseX, mouseY, partialTicks)

		RenderUtils.drawRect(representedScreen.width / 4.0f, 40.0f, representedScreen.width.toFloat(), representedScreen.height - 40.0f, Integer.MIN_VALUE)

		if (list.getSelectedSlot() != -1)
		{
			val credit = credits[list.getSelectedSlot()]

			var y = 45
			val x = representedScreen.width / 4 + 5
			var infoOffset = 0

			val avatar = credit.avatar

			val imageSize = representedScreen.fontRendererObj.fontHeight * 4

			if (avatar != null)
			{
				GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)

				classProvider.getGlStateManager().enableAlpha()
				classProvider.getGlStateManager().enableBlend()
				classProvider.getGlStateManager().tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO)
				classProvider.getGlStateManager().enableTexture2D()

				GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)

				classProvider.getGlStateManager().bindTexture(avatar.textureId)


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

				classProvider.getGlStateManager().bindTexture(0)

				classProvider.getGlStateManager().disableBlend()

				infoOffset = imageSize

				GL11.glPopAttrib()
			}

			y += imageSize

			Fonts.font40.drawString("@" + credit.name, x + infoOffset + 5.0f, 48f, Color.WHITE.rgb, true)
			Fonts.font40.drawString(
				"${credit.commits} commits \u00A7a${DECIMAL_FORMAT.format(credit.additions)}++ \u00A74${DECIMAL_FORMAT.format(credit.deletions)}--", x + infoOffset + 5.0f, (y - Fonts.font40.fontHeight).toFloat(), Color.WHITE.rgb, true
			)

			for (s in credit.contributions)
			{
				y += Fonts.font40.fontHeight + 2

				classProvider.getGlStateManager().disableTexture2D()
				GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
				GL11.glBegin(GL11.GL_LINES)

				GL11.glVertex2f(x.toFloat(), y + Fonts.font40.fontHeight / 2.0f - 1)
				GL11.glVertex2f(x + 3.0f, y + Fonts.font40.fontHeight / 2.0f - 1)

				GL11.glEnd()

				Fonts.font40.drawString(s, (x + 5f), y.toFloat(), Color.WHITE.rgb, true)
			}
		}

		Fonts.font40.drawCenteredString("Contributors", representedScreen.width / 2F, 6F, 0xffffff)

		if (credits.isEmpty())
		{
			if (failed)
			{
				val gb = ((functions.sin(System.currentTimeMillis() * (1 / 333.0F)) + 1) * (0.5 * 255)).toInt()
				Fonts.font40.drawCenteredString("Failed to load", representedScreen.width / 8.0f, representedScreen.height / 2.0f, Color(255, gb, gb).rgb)
			}
			else
			{
				Fonts.font40.drawCenteredString("Loading...", representedScreen.width / 8.0f, representedScreen.height / 2.0f, Color.WHITE.rgb)
				RenderUtils.drawLoadingCircle((representedScreen.width / 8).toFloat(), representedScreen.height / 2 - 40.0f)
			}
		}

		super.drawScreen(mouseX, mouseY, partialTicks)
	}

	override fun actionPerformed(button: IGuiButton)
	{
		if (button.id == 1)
		{
			mc.displayGuiScreen(prevGui)
		}
	}

	override fun keyTyped(typedChar: Char, keyCode: Int)
	{
		if (Keyboard.KEY_ESCAPE == keyCode)
		{
			mc.displayGuiScreen(prevGui)
			return
		}

		super.keyTyped(typedChar, keyCode)
	}

	override fun handleMouseInput()
	{
		super.handleMouseInput()
		list.represented.handleMouseInput()
	}

	private fun loadCredits() = try
	{
		val gson = Gson()
		val jsonParser = JsonParser()

		val gitHubContributors = gson.fromJson(HttpUtils["https://api.github.com/repos/hsheric0210/LiquidBounce/stats/contributors"], Array<GitHubContributor>::class.java)
		val additionalInformation = jsonParser.parse(HttpUtils["https://raw.githubusercontent.com/CCBlueX/LiquidCloud/master/LiquidBounce/contributors.json"]).asJsonObject

		val credits = ArrayList<Credit>(gitHubContributors.size)

		for (gitHubContributor in gitHubContributors)
		{
			var contributorInformation: ContributorInformation? = null
			val jsonElement = additionalInformation[gitHubContributor.author.id.toString()]

			if (jsonElement != null) contributorInformation = gson.fromJson(jsonElement, ContributorInformation::class.java)

			var additions = 0
			var deletions = 0
			var commits = 0

			for (week in gitHubContributor.weeks)
			{
				additions += week.additions
				deletions += week.deletions
				commits += week.commits
			}

			credits.add(Credit(gitHubContributor.author.name, gitHubContributor.author.avatarUrl, null, additions, deletions, commits, contributorInformation?.teamMember ?: false, contributorInformation?.contributions ?: Collections.emptyList()))
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
				HttpUtils.requestStream("${credit.avatarUrl}?s=${representedScreen.fontRendererObj.fontHeight * 4}", "GET")?.use { credit.avatar = CustomTexture(ImageIO.read(it)!!) }
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
		failed = true
	}

	internal class ContributorInformation(val name: String, val teamMember: Boolean, val contributions: List<String>)

	internal class GitHubContributor(@SerializedName("total") val totalContributions: Int, val weeks: List<GitHubWeek>, val author: GitHubAuthor)
	internal class GitHubWeek(@SerializedName("w") val timestamp: Long, @SerializedName("a") val additions: Int, @SerializedName("d") val deletions: Int, @SerializedName("c") val commits: Int)
	internal class GitHubAuthor(@SerializedName("login") val name: String, val id: Int, @SerializedName("avatar_url") val avatarUrl: String)

	internal class Credit(val name: String, val avatarUrl: String, var avatar: CustomTexture?, val additions: Int, val deletions: Int, val commits: Int, val isTeamMember: Boolean, val contributions: List<String>)

	private inner class GuiList(gui: IGuiScreen) : WrappedGuiSlot(mc, gui.width / 4, gui.height, 40, gui.height - 40, 15)
	{

		init
		{
			represented.setListWidth(gui.width * 3 / 13)
			represented.setEnableScissor(true)
		}

		private var selectedSlot = 0

		override fun isSelected(id: Int) = selectedSlot == id

		override fun getSize() = credits.size

		fun getSelectedSlot() = if (selectedSlot > credits.size) -1 else selectedSlot

		override fun elementClicked(id: Int, doubleClick: Boolean, var3: Int, var4: Int)
		{
			selectedSlot = id
		}

		override fun drawSlot(id: Int, x: Int, y: Int, var4: Int, mouseXIn: Int, mouseYIn: Int)
		{
			val credit = credits[id]

			Fonts.font40.drawCenteredString(credit.name, represented.width / 2F, y + 2F, Color.WHITE.rgb, true)
		}

		override fun drawBackground()
		{
		}
	}
}
