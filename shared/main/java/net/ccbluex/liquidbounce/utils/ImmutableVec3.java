package net.ccbluex.liquidbounce.utils;

import net.ccbluex.liquidbounce.api.minecraft.util.WVec3;
import net.minecraft.util.Vec3;

public class ImmutableVec3
{
	private final double x;
	private final double y;
	private final double z;

	public ImmutableVec3(final double x, final double y, final double z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public ImmutableVec3(final Vec3 mcVec3)
	{
		x = mcVec3.xCoord;
		y = mcVec3.yCoord;
		z = mcVec3.zCoord;
	}

	public final double getX()
	{
		return x;
	}

	public final double getY()
	{
		return y;
	}

	public final double getZ()
	{
		return z;
	}

	public final ImmutableVec3 addVector(final double x, final double y, final double z)
	{
		return new ImmutableVec3(this.x + x, this.y + y, this.z + z);
	}

	public final ImmutableVec3 floor()
	{
		return new ImmutableVec3(Math.floor(x), Math.floor(y), Math.floor(z));
	}

	public final double squareDistanceTo(final ImmutableVec3 v)
	{
		return Math.pow(v.x - x, 2) + Math.pow(v.y - y, 2) + Math.pow(v.z - z, 2);
	}

	public final ImmutableVec3 add(final ImmutableVec3 v)
	{
		return this.addVector(v.getX(), v.getY(), v.getZ());
	}

	public final WVec3 wrap()
	{
		return new WVec3(x, y, z);
	}

	@Override
	public final String toString()
	{
		return "ImmutableVec3(" + x + ", " + y + ", " + z + ")";
	}
}
