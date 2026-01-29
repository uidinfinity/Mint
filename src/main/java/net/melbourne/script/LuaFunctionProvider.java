package net.melbourne.script;

import org.luaj.vm2.LuaTable;


public interface LuaFunctionProvider
{

	String getName();

	void export(LuaTable table);

}