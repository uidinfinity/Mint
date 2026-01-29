package net.melbourne.script.functions.minecraft;

import net.melbourne.script.LuaFunctionProvider;
import net.melbourne.utils.Globals;
import org.luaj.vm2.LuaTable;


public abstract class MinecraftBase implements LuaFunctionProvider, Globals
{

	@Override
	public String getName()
	{
		return "mc";
	}

	@Override
	public void export(LuaTable root)
	{
		LuaTable category = new LuaTable();
		add(category);
		root.set(getCategoryName(), category);
	}

	protected abstract String getCategoryName();

	protected abstract void add(LuaTable table);

}