package net.melbourne.script.impl;

import lombok.Getter;
import net.melbourne.script.LuaApiRegistry;
import net.minecraft.client.gui.DrawContext;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;


@Getter
public class ScriptInstance
{

	private final File file;
	private final String name;
	private final Globals globals;

	public ScriptInstance(File file)
	{
		this.file = file;
		this.name = file.getName();
		this.globals = JsePlatform.standardGlobals();
		LuaApiRegistry.injectAll(globals);
	}

	public void load() throws IOException
	{
		LuaValue chunk = globals.load(Files.readString(file.toPath()), name);
		chunk.call();
	}

	public void onTick()
	{
		LuaValue func = globals.get("on_tick");
		if (func.isfunction())
		{
			func.call();
		}
	}

	public void onRender(DrawContext context)
	{
		LuaValue func = globals.get("on_render");
		if (func.isfunction())
		{
			func.call();
		}
	}

}