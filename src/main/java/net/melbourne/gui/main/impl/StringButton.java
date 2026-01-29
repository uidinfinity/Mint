package net.melbourne.gui.main.impl;

import net.melbourne.Melbourne;
import net.melbourne.settings.types.TextSetting;
import net.melbourne.utils.Globals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Formatting;
import net.melbourne.gui.main.api.Button;
import net.melbourne.gui.main.api.Window;
import net.melbourne.utils.miscellaneous.ColorUtils;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class StringButton extends Button implements Globals {
    private final TextSetting setting;

    private boolean listening = false;
    boolean selecting = false;

    private String currentString = "";
    private int cursorIndex = 0;

    public StringButton(TextSetting setting, Window window) {
        super(setting, window);
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        String text;

        if (listening) {
            if (selecting) {
                text = currentString;
            } else {
                int clampedIndex = Math.max(0, Math.min(cursorIndex, currentString.length()));
                String before = currentString.substring(0, clampedIndex);
                String after = currentString.substring(clampedIndex);
                String cursorChar = Melbourne.CLICK_GUI.isLine() ? "|" : " ";
                text = before + cursorChar + after;
            }
        } else {
            text = setting.getName() + " " + Formatting.GRAY + setting.getValue();
        }

        drawTextWithShadow(context, text, getX() + 1, getY() + getVerticalPadding(), selecting ? ColorUtils.getGlobalColor() : Color.WHITE);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if(isHovering(mouseX, mouseY) && !listening && button == 0) {
            listening = true;
            currentString = setting.getValue();
            cursorIndex = currentString.length();
        } else {
            listening = false;
        }
        selecting = false;
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!listening) return;

        long handle = mc.getWindow().getHandle();
        boolean ctrl = InputUtil.isKeyPressed(handle, MinecraftClient.IS_SYSTEM_MAC ? GLFW.GLFW_KEY_LEFT_SUPER : GLFW.GLFW_KEY_LEFT_CONTROL);

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            listening = false;
            selecting = false;
            return;
        }

        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            setting.setValue(currentString);
            selecting = false;
            listening = false;
            return;
        }

        if (keyCode == GLFW.GLFW_KEY_LEFT && !currentString.isEmpty()) {
            if (selecting) {
                selecting = false;
                cursorIndex = 0;
            } else {
                if (cursorIndex > 0) cursorIndex--;
            }
            return;
        }

        if (keyCode == GLFW.GLFW_KEY_RIGHT && !currentString.isEmpty()) {
            if (selecting) {
                selecting = false;
                cursorIndex = currentString.length();
            } else {
                if (cursorIndex < currentString.length()) cursorIndex++;
            }
            return;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (selecting) {
                currentString = "";
                cursorIndex = 0;
                selecting = false;
            } else {
                if (cursorIndex > 0) {
                    currentString = currentString.substring(0, cursorIndex - 1) + currentString.substring(cursorIndex);
                    cursorIndex--;
                }
            }
            return;
        }

        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (selecting) {
                currentString = "";
                cursorIndex = 0;
                selecting = false;
            } else {
                if (cursorIndex < currentString.length()) {
                    currentString = currentString.substring(0, cursorIndex) + currentString.substring(cursorIndex + 1);
                }
            }
            return;
        }

        if (ctrl) {
            if (keyCode == GLFW.GLFW_KEY_V) {
                try {
                    String clipboard = mc.keyboard.getClipboard();
                    if (selecting) {
                        currentString = clipboard;
                        cursorIndex = currentString.length();
                        selecting = false;
                    } else {
                        currentString = currentString.substring(0, cursorIndex) + clipboard + currentString.substring(cursorIndex);
                        cursorIndex += clipboard.length();
                    }
                } catch (Exception exception) {
                    Melbourne.getLogger().error("{}: Failed to process clipboard paste", exception.getClass().getName(), exception);
                }
                return;
            }

            if (keyCode == GLFW.GLFW_KEY_C && selecting) {
                try {
                    mc.keyboard.setClipboard(currentString);
                } catch (Exception exception) {
                    Melbourne.getLogger().error("{}: Failed to process clipboard change", exception.getClass().getName(), exception);
                }
                return;
            }

            if (keyCode == GLFW.GLFW_KEY_A) {
                if (!currentString.isEmpty()) {
                    selecting = true;
                    cursorIndex = currentString.length();
                }
            }
        }
    }

    @Override
    public void charTyped(char chr, int modifiers) {
        if (!listening || Character.isISOControl(chr)) return;

        if (selecting) {
            currentString = String.valueOf(chr);
            cursorIndex = 1;
            selecting = false;
        } else {
            currentString = currentString.substring(0, cursorIndex) + chr + currentString.substring(cursorIndex);
            cursorIndex++;
        }
    }
}
