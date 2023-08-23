import net.minecraft.block.BlockAir
import net.minecraft.block.BlockSlime
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.settings.GameSettings
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemEnderPearl
import net.minecraft.network.Packet
import net.minecraft.network.play.INetHandlerPlayServer
import net.minecraft.network.play.client.*
import net.minecraft.network.play.client.C03PacketPlayer.*
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.potion.Potion
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import javax.vecmath.Vector2f
import kotlin.math.*
//Vulcan
 private val timerValue =
        FloatValue("VulcanFast-Timer", 3f, 1f, 3f) { modeValue.get().equals("vulcanfast", ignoreCase = true) }
    
