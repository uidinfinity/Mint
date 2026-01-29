package net.melbourne.script.impl;

import lombok.Getter;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.script.ScriptEnvironment;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.File;
import java.io.FileReader;

@Getter
public class ScriptModule extends Feature {

    private final File scriptFile;
    private Globals luaGlobals;

    public ScriptModule(File file) {
        super();
        this.setName(file.getName().replace(".lua", ""));
        this.setCategory(Category.Misc);
        this.scriptFile = file;
    }

    @Override
    public void onEnable() {
        try {
            luaGlobals = JsePlatform.standardGlobals();
            ScriptEnvironment.inject(luaGlobals);

            try (FileReader reader = new FileReader(scriptFile)) {
                LuaValue chunk = luaGlobals.load(reader, getName());
                chunk.call();
            }

            LuaValue onEnableFunc = luaGlobals.get("on_enable");
            if (onEnableFunc.isfunction()) {
                onEnableFunc.call();
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.setEnabled(false);
        }
    }

    @Override
    public void onDisable() {
        if (luaGlobals != null) {
            LuaValue onDisableFunc = luaGlobals.get("on_disable");
            if (onDisableFunc.isfunction()) {
                onDisableFunc.call();
            }
        }
        luaGlobals = null;
    }

    public void onTick() {
        if (luaGlobals == null || !isEnabled()) return;

        LuaValue tickFunc = luaGlobals.get("on_tick");
        if (tickFunc.isfunction()) {
            tickFunc.call();
        }
    }
}