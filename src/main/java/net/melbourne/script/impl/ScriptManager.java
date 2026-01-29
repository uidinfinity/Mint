package net.melbourne.script.impl;

import lombok.Getter;
import net.melbourne.Manager;
import net.melbourne.Managers;
import net.melbourne.Melbourne;
import net.melbourne.script.LuaApiRegistry;
import net.melbourne.script.LuaFunctionProvider;
import net.melbourne.script.annotations.RegisterLua;
import net.melbourne.utils.miscellaneous.FileUtils;
import org.reflections.Reflections;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Getter
public class ScriptManager extends Manager {

    private final Map<String, ScriptModule> loadedScripts = new HashMap<>();
    private final List<ScriptModule> scripts = new ArrayList<>();
    private final File scriptFolder = new File(Melbourne.NAME + "/scripts");

    public ScriptManager() {
        super("Scripts", "Manages Lua scripting");
    }

    @Override
    public void onInit() {
        try {
            FileUtils.createDirectory(Melbourne.NAME);
            FileUtils.createDirectory(scriptFolder.getAbsolutePath());
        } catch (IOException e) {
            Melbourne.getLogger().error("Failed to create script directories", e);
        }

        registerApis();
        refreshScripts();
    }

    public void refreshScripts() {
        for (ScriptModule script : scripts) {
            Managers.FEATURE.getFeatures().remove(script);
        }

        scripts.clear();

        File[] files = scriptFolder.listFiles((dir, name) -> name.endsWith(".lua"));
        if (files == null) return;

        for (File file : files) {
            ScriptModule module = new ScriptModule(file);
            scripts.add(module);
            Managers.FEATURE.getFeatures().add(module);
        }
    }

    private void registerApis() {
        try {
            Reflections reflections = new Reflections("net.melbourne.script.functions");
            Set<Class<? extends LuaFunctionProvider>> classes = reflections.getSubTypesOf(LuaFunctionProvider.class);

            for (Class<? extends LuaFunctionProvider> clazz : classes) {
                if (clazz.getAnnotation(RegisterLua.class) == null) continue;
                LuaApiRegistry.register(clazz.getDeclaredConstructor().newInstance());
            }
        } catch (Exception e) {
            Melbourne.getLogger().error("Failed to register Lua APIs", e);
        }
    }
}