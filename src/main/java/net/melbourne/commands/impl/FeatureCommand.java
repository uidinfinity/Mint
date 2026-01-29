package net.melbourne.commands.impl;

import net.melbourne.Managers;
import net.melbourne.services.Services;
import net.melbourne.commands.Command;
import net.melbourne.commands.CommandInfo;
import net.melbourne.modules.Feature;
import net.melbourne.settings.Setting;
import net.melbourne.settings.types.*;
import net.melbourne.utils.miscellaneous.ColorUtils;
import net.minecraft.text.Text;

import java.awt.*;
import java.util.Comparator;
import java.util.stream.Stream;

@CommandInfo(name = "Feature", desc = "Modify feature settings.")
public class FeatureCommand extends Command {

    @Override
    public void onCommand(String[] args) {
        if (args.length == 0) {
            sendUsage();
            return;
        }

        String featureNameInput = args[0];
        Feature feature = Managers.FEATURE.getFeatureByName(featureNameInput);

        if (feature == null) {
            String closest = getClosestFeature(featureNameInput);
            Services.CHAT.sendRaw("§cUnknown feature: §s" + featureNameInput + "§7.");
            if (closest != null)
                Services.CHAT.sendRaw("§7Did you mean: §s" + closest + "§7?");
            return;
        }

        if (args.length == 1) {
            Services.CHAT.sendRaw("§sSettings for §7" + feature.getName() + ":");
            for (Setting s : feature.getSettings())
                Services.CHAT.sendRaw(" §7- §s" + s.getName());
            return;
        }

        String settingName = args[1];
        Setting target = feature.getSettings().stream()
                .filter(s -> s.getName().equalsIgnoreCase(settingName))
                .findFirst()
                .orElse(null);

        if (target == null) {
            String closest = getClosestSetting(feature, settingName);
            Services.CHAT.sendRaw("§cUnknown setting: §s" + settingName + "§7.");
            if (closest != null)
                Services.CHAT.sendRaw("§7Did you mean: §s" + closest + "§7?");
            return;
        }

        String value = args.length > 2 ? args[2] : "";
        String[] colorArgs = args.length > 2 ? Stream.of(args).skip(2).toArray(String[]::new) : new String[]{};
        String featureName = feature.getName();
        String settingNameFinal = target.getName();
        String notificationKey = featureName + ":" + settingNameFinal;

        if (target instanceof BooleanSetting b) {
            if (value.isEmpty()) {
                sendUsage();
                return;
            }
            boolean val = !value.equals("0") && !value.equalsIgnoreCase("false");
            b.setValue(val);
            Services.CHAT.sendMacroToggle(notificationKey,
                    Text.literal("§s" + settingNameFinal + " §7has been set to §s" + val + " §7for §s" + featureName + "§7."), true);
            return;
        }

        if (target instanceof NumberSetting n) {
            try {
                double num = Double.parseDouble(value);
                n.setValue(num);
                Services.CHAT.sendMacroToggle(notificationKey,
                        Text.literal("§s" + settingNameFinal + " §7has been set to §s" + num + " §7for §s" + featureName + "§7."), true);
            } catch (NumberFormatException ignored) {
                Services.CHAT.sendRaw("§cInvalid number: §s" + value);
            }
            return;
        }

        if (target instanceof ModeSetting m) {
            if (!value.isEmpty()) {
                String matched = m.getModes().stream()
                        .filter(v -> v.equalsIgnoreCase(value))
                        .findFirst().orElse(null);

                if (matched != null) {
                    m.setValue(matched);
                    Services.CHAT.sendMacroToggle(notificationKey,
                            Text.literal("§s" + settingNameFinal + " §7has been set to §s" + matched + " §7for §s" + featureName + "§7."), true);
                } else {
                    Services.CHAT.sendRaw("§cInvalid mode: §s" + value + "§7.");
                }
            } else {
                Services.CHAT.sendRaw("§sModes: §7" + String.join(", ", m.getModes()));
                sendUsage();
            }
            return;
        }

        if (target instanceof TextSetting t) {
            t.setValue(value);
            Services.CHAT.sendMacroToggle(notificationKey,
                    Text.literal("§s" + settingNameFinal + " §7has been set to §s" + value + " §7for §s" + featureName + "§7."), true);
            return;
        }

        if (target instanceof ColorSetting c) {
            if (colorArgs.length == 0) {
                sendUsage();
                return;
            }

            if (colorArgs.length == 2 && colorArgs[0].equalsIgnoreCase("sync")) {
                boolean v = colorArgs[1].equalsIgnoreCase("true");
                c.setSync(v);
                Services.CHAT.sendMacroToggle(notificationKey,
                        Text.literal("§s" + settingNameFinal + " §7sync set to §s" + v + " §7for §s" + featureName + "§7."), true);
                return;
            }

            String comp = colorArgs[0].toLowerCase();

            if (colorArgs.length == 2 && (
                    comp.equals("red") || comp.equals("green") || comp.equals("blue") || comp.equals("alpha")
            )) {
                try {
                    int val = Integer.parseInt(colorArgs[1]);
                    val = Math.max(0, Math.min(255, val));
                    Color old = c.getColor();
                    Color out;

                    switch (comp) {
                        case "red":
                            out = new Color(val, old.getGreen(), old.getBlue(), old.getAlpha());
                            break;
                        case "green":
                            out = new Color(old.getRed(), val, old.getBlue(), old.getAlpha());
                            break;
                        case "blue":
                            out = new Color(old.getRed(), old.getGreen(), val, old.getAlpha());
                            break;
                        default:
                            out = new Color(old.getRed(), old.getGreen(), old.getBlue(), val);
                            break;
                    }

                    c.setValue(out);
                    Services.CHAT.sendMacroToggle(notificationKey,
                            Text.literal("§s" + settingNameFinal + " §7" + comp + " set to §s" + val + " §7for §s" + featureName + "§7."), true);
                } catch (Exception ignored) {
                    Services.CHAT.sendRaw("§cInvalid number: §s" + colorArgs[1]);
                }
                return;
            }

            boolean valid = false;
            int r = -1, g = -1, b = -1, a = -1;
            String cname = null;

            if (colorArgs.length == 1) {
                try {
                    int hex = (int) Long.parseLong(colorArgs[0].replace("#", ""), 16);
                    Color col = new Color(hex, true);
                    r = col.getRed();
                    g = col.getGreen();
                    b = col.getBlue();
                    a = col.getAlpha();
                    valid = true;
                } catch (Exception ignored) {
                    Color col = ColorUtils.getColorByName(colorArgs[0]);
                    if (col != null) {
                        r = col.getRed();
                        g = col.getGreen();
                        b = col.getBlue();
                        a = c.getColor().getAlpha();
                        cname = colorArgs[0];
                        valid = true;
                    }
                }
            }

            if (colorArgs.length >= 3) {
                try {
                    r = Integer.parseInt(colorArgs[0]);
                    g = Integer.parseInt(colorArgs[1]);
                    b = Integer.parseInt(colorArgs[2]);
                    a = colorArgs.length > 3 ? Integer.parseInt(colorArgs[3]) : c.getColor().getAlpha();
                    valid = true;
                } catch (Exception ignored) {
                }
            }

            if (!valid) {
                Services.CHAT.sendRaw("§cInvalid color format");
                return;
            }

            r = Math.max(0, Math.min(255, r));
            g = Math.max(0, Math.min(255, g));
            b = Math.max(0, Math.min(255, b));
            a = Math.max(0, Math.min(255, a));

            Color out = new Color(r, g, b, a);
            c.setValue(out);

            String str = cname != null ? cname : "R:" + r + " G:" + g + " B:" + b + " A:" + a;
            Services.CHAT.sendMacroToggle(notificationKey,
                    Text.literal("§s" + settingNameFinal + " §7set to §s" + str + " §7for §s" + featureName + "§7."), true);
        }
    }

    private void sendUsage() {
        Services.CHAT.sendRaw("§sUsage: §7.feature <feature> <setting> <value>");
        Services.CHAT.sendRaw("§sExample: §7.feature AutoCrystal BreakDelay 15");
    }

    private String getClosestFeature(String input) {
        String lower = input.toLowerCase();
        var names = Managers.FEATURE.getFeatures().stream()
                .map(Feature::getName)
                .toList();

        var prefixMatches = names.stream()
                .filter(n -> n.toLowerCase().startsWith(lower))
                .toList();
        if (!prefixMatches.isEmpty())
            return prefixMatches.get(0);

        var substringMatches = names.stream()
                .filter(n -> n.toLowerCase().contains(lower))
                .toList();
        if (!substringMatches.isEmpty())
            return substringMatches.get(0);

        return names.stream()
                .min(Comparator.comparingInt(n -> distance(n.toLowerCase(), lower)))
                .orElse(null);
    }

    private String getClosestSetting(Feature feature, String input) {
        String lower = input.toLowerCase();
        var names = feature.getSettings().stream()
                .map(Setting::getName)
                .toList();

        var prefixMatches = names.stream()
                .filter(n -> n.toLowerCase().startsWith(lower))
                .toList();
        if (!prefixMatches.isEmpty())
            return prefixMatches.get(0);

        var substringMatches = names.stream()
                .filter(n -> n.toLowerCase().contains(lower))
                .toList();
        if (!substringMatches.isEmpty())
            return substringMatches.get(0);

        return names.stream()
                .min(Comparator.comparingInt(n -> distance(n.toLowerCase(), lower)))
                .orElse(null);
    }

    private int distance(String a, String b) {
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }
}
