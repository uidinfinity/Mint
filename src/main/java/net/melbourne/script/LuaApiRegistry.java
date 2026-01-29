package net.melbourne.script;

import lombok.Getter;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;

import java.util.ArrayList;
import java.util.List;


@Getter
public class LuaApiRegistry
{

	public static final List<LuaFunctionProvider> providers = new ArrayList<>();

	public static void register(LuaFunctionProvider provider)
	{
		providers.add(provider);
	}

	public static void injectAll(Globals globals)
	{
		for (LuaFunctionProvider provider : providers)
		{
			LuaTable table = new LuaTable();
			provider.export(table);
			globals.set(provider.getName(), table);
		}
	}

}