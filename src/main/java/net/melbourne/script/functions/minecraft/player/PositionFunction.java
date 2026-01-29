package net.melbourne.script.functions.minecraft.player;

import net.melbourne.script.annotations.RegisterLua;
import net.melbourne.script.functions.minecraft.MinecraftBase;
import net.minecraft.util.math.BlockPos;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;


@RegisterLua
public class PositionFunction extends MinecraftBase
{

	@Override
	protected String getCategoryName()
	{
		return "player";
	}

	@Override
	protected void add(LuaTable table)
	{
		table.set("getPosition", new ZeroArgFunction()
		{
			@Override
			public LuaValue call()
			{
				if (mc.player == null)
					return LuaValue.NIL;

				BlockPos pos = mc.player.getBlockPos();

				LuaTable position = new LuaTable();
				position.set(1, LuaValue.valueOf(pos.getX()));
				position.set(2, LuaValue.valueOf(pos.getY()));
				position.set(3, LuaValue.valueOf(pos.getZ()));

				return position;
			}
		});

		table.set("setPosition", new ThreeArgFunction()
		{
			@Override
			public LuaValue call(LuaValue x, LuaValue y, LuaValue z)
			{
				if (mc.player == null)
					return LuaValue.NIL;

				mc.player.updatePosition(x.checkdouble(), y.checkdouble(), z.checkdouble());

				return LuaValue.NIL;
			}
		});
	}

}