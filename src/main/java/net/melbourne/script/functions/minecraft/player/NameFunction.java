package net.melbourne.script.functions.minecraft.player;

import net.melbourne.script.annotations.RegisterLua;
import net.melbourne.script.functions.minecraft.MinecraftBase;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;


@RegisterLua
public class NameFunction extends MinecraftBase
{

	@Override
	protected String getCategoryName()
	{
		return "player";
	}

	@Override
	protected void add(LuaTable table)
	{
		table.set("getName", new ZeroArgFunction()
		{
			@Override
			public LuaValue call()
			{
				if (mc.player == null)
					return LuaValue.NIL;

				return LuaValue.valueOf(mc.player.getGameProfile().getName());
			}
		});
	}

}