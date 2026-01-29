package net.melbourne.script;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;


public class ScriptEnvironment
{

	public static void inject(Globals globals)
	{
		for (LuaFunctionProvider provider : LuaApiRegistry.providers)
		{
			LuaTable table = new LuaTable();
			provider.export(table);
			globals.set(provider.getName(), table);
		}
	}

}