package net.melbourne.modules.impl.misc;

import net.melbourne.services.Services;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Formatting;

import java.util.*;

@FeatureInfo(name = "StaffDetector", category = Category.Misc)
public class StaffDetectorFeature extends Feature {

    private final ModeSetting server = new ModeSetting(
            "Server",
            "Select server staff list",
            "Hypixel",
            new String[]{"Hypixel", "BlocksMC", "GommeHD", "Pika", "QPlay", "Syuu", "Stardix", "MinemenClub", "MushMC", "Twerion", "BedwarsPractice", "QuickMacro", "Heypixel", "HylexMC", "Jartex", "Mineland", "Spectator"}
    );

    private final BooleanSetting autoLobby = new BooleanSetting("Auto Lobby", "Automatically return to lobby on staff detect", false);
    private final Set<String> detected = new HashSet<>();

    private static final Map<String, Set<String>> STAFF_MAP = new HashMap<>();

    static {
        STAFF_MAP.put("Hypixel", Set.of("Minikloon", "Rhune", "Greeenn", "Fr0z3n", "SnowyPai", "Rozsa", "Quack", "ZeaBot", "Cheesey", "MCVisuals", "carstairs95", "inventivetalent", "JacobRuby", "Jayavarmen", "Judg3", "LandonHP", "Liffeh", "xHascox", "DEADORKAI", "Brandonjja", "skyerzz", "Dctr", "AdamWho", "aPunch", "jamzs", "Phaige", "Likaos", "Plummel", "Bloozing", "BlocksKey", "MistressEldrid", "Nausicaah", "ChiLynn", "TheMGRF", "Revengeee", "_PolynaLove_", "Sylent_", "Teddy", "Relenter", "_fudgiethewhale", "sfarnham", "NoxyD", "WilliamTiger", "vinny8ball666", "Nitroholic_", "Donpireso", "Plancke", "ConnorLinfoot", "RapidTheNerd", "Rezzus", "eeyitscoco", "Cecer", "Externalizable", "Bembo", "Taytale", "JamieTheGeek", "williamgburns", "BOGA32", "boomerzap", "MFN", "Gainful", "Octaverei", "TacNayn", "TimeDeo"));
        STAFF_MAP.put("BlocksMC", Set.of("CowNecromancer", "Dmoha", "YungLOL", "TC6", "SeaLegend", "imconnorngl", "ambmt", "Engineous", "Gawkan", "_lightninq", "ObamaFootFungus", "RYgamer1", "Xitharis", "KZfr", "iiKoala", "luukeyy", "LeoIsGod_", "rozzerbtw", "Queezz_", "Tado", "Chazm", "iDhoom", "Jinaaan", "Eissaa", "Ev2n", "1Mhmmd", "mohmadq8", "1Daykel", "Aliiyah", "1Brhom", "xImTaiG", "comsterr", "8layh", "M7mmd", "1LaB", "xIBerryPlayz", "iiRaivy", "Refolt", "1Sweet", "Aba5z3l", "EyesODiamond", "bestleso", "Firas", "reallyisntfair", "e9", "MK_F16", "unrelievable", "Ixfaris_0", "LuvDark", "420kinaka", "NonameIsHere", "iS3od", "3Mmr", "Wesccar", "1MeKo", "losingtears", "KaaReeeM", "loovq", "rarticalss", "1RealFadi", "JustDrink", "AFG_progamer92", "Jxicide", "D7oMz", "1AhMqD", "Omaaaaaaaaaar", "Classic190", "Only7oDa", "sylx69", "13bdalH", "frank124", "dfdox", "1Mohq", "1Sweleh", "Om2r", "epicmines33", "1Devesty", "BagmaTwT", "Azyyq", "A2boD", "Ba1z", "100k", "Watchdog", "nv0ola", "KinderBueno", "Invxe", "GreatMjd", "zixgamer", "Salvctore", "420Lalilala", "vIon3", "wstre", "AstroSaif", "plaintiveness", "ImS3G", "1Flick", "EstieMeow", "ItsNqf", "MVP11", "Daddy_Naif", "shichirouu", "Lordui", "1Reyleigh", "BIocksMc", "1Retired", "Olp", "L6mh", "63myh", "1Mawja", "Tqfi", "3iDO", "1M7mmd__", "M4rwaan72074", "ThisWeek", "deficency", "Morninng", "KinderBueno__", "Reflxctively", "ImMEHDI_", "Aymann_", "xfahadq", "BoMshary", "1Adam__", "E3Y", "ALMARDAWI", "xL2d", "Postme", "DrugsOverdose", "0ayt", "5ald_KSA", "GsOMAR", "GsMrxDJ", "TryLat3rjAs", "Ruwq", "teddynicelol", "MightyM7MD", "tsittie", "Jrx7", "Mwl4", "itsjust24", "m7mdxjw", "kieax", "Vengeant", "JustRois_", "Mr_1990", "CASHFL0WW", "A3loosh", "Neeres", "luqqr", "_1Dark", "Werthly", "1Khalid", "t0tallynotMaram", "dxs7", "0Strong", "1Az_", "Morgan_So", "Just_Kanki", "Z_1HypersX_Z", "1SuchRod_", "MILG511", "tHeViZi", "_Lsantoss", "MightyFiras", "xVerif", "whahahahahahaha", "1Sharlock", "xorbera", "BPEJ", "GymIsTherapy", "wallacce", "0Leqf", "Ditraghxel", "catbisou", "M5CS_", "Attraactive", "Xx_ZeroMc_xX", "_IxM", "Claegness", "yyodey", "K2reem", "Dfivz", "KingH0", "glowingsunsets", "Dyala10", "OnlyK1nq", "YOUVA", "rosulate", "I_Shling", "Yarin", "0PvP_", "FANOR_SYR", "EveryHitIsCrit", "Bradenton", "m7xar", "certfiedgirl", "1LoST_", "m3qv", "Rikoshy", "2lbk", "iidouble", "GsYousef", "Just_XYZ", "TruthZ_", "5bzzz", "Ovqrn", "_iMuslim_", "damjqn", "5loo", "9lrv", "D7iiem", "PavleDjajic", "SweetyAlice", "A5q_", "D_1V", "Abo_3losh", "1Pre", "Peree", "1lhana", "Gaboo6", "7amze__", "Erosiion", "FastRank", "Everlqst", "EgaSS", "Vanitas_0", "1Prometheus_", "PoisonL", "ixstorm_", "1MSA", "AnotherHero", "Pogor", "D3vi1Joex", "38l_ba6n", "izLORDeX", "RAGHAVV", "Impassivelly", "Reixo", "DasPukas_", "NotriousAsser", "AspectApolo", "swhq", "1Abd2llah", "1ith", "ArabPixel", "Majd83", "4T0_", "LIONMohamed", "Hlazny", "Mc_Suchter", "Mxhesh", "SuperRa2ft", "TheCre4tor", "Mo3Az1", "ln5b", "B7rl", "BasilFoto", "Teqches", "einmeterhecht", "BeFriends", "7sO", "BasselFTW"));
        STAFF_MAP.put("GommeHD", Set.of("IchMagKeksexD", "Klaus", "lukas81298", "Marius051099", "ZettelRaus", "Zwunja"));
        STAFF_MAP.put("Heypixel", Set.of("绿豆乃SAMA", "nightbary", "体贴的炼金术雀", "StarNO1", "妖猫", "小妖猫", "神伦子"));
        STAFF_MAP.put("HylexMC", Set.of("Gerbor12", "JordWG", "LeBrillant", "Pensul", "LadyBleu", "Citria", "DeluxeRose", "TheBirmanator", "TorWolf"));
        STAFF_MAP.put("Jartex", Set.of("voodootje0", "Max", "Rodagave115", "JustThiemo", "Andeh", "Axteroid", "stupxd", "JstMental", "QuFox"));
        STAFF_MAP.put("Mineland", Set.of("TehNeon", "LeftBooob", "dwefs", "XenonServices", "Rowteh", "Goawaynotgg", "HiitSayZ", "Nyolu"));
        STAFF_MAP.put("MinemenClub", Set.of("Anjeel", "Feelipee", "FEIJAOO", "ISlowneR", "mariaum", "Nicoolass", "Start_", "daarkl"));
        STAFF_MAP.put("MushMC", Set.of("Max", "voodootjeo", "MrFrenco", "TryHardMarktin", "Axteroid", "JustThiemo", "MrEpiko"));
        STAFF_MAP.put("Pika", Set.of("BACs", "YT_BACs", "无年a", "BoogerTheCat", "花雨庭审判骑士", "血樱丶星梦", "Toxic_AslGy"));
        STAFF_MAP.put("QuickMacro", Set.of("IaG0D", "yJaoo_", "mnszz", "laraaz"));
        STAFF_MAP.put("QPlay", Set.of("Profikk", "_Spetty_", "Ayessha", "ItzTadeas", "_razorRalfcz", "Dastrokk", "Linuuus", "KwenT_", "M1che_", "GodLikeKubiss", "SeptunLover", "Veverka14", "DeKoN_CZ", "H0t_Mamka", "Hrib", "Majulka", "DarkBanan", "iArthurr_", "matyraptycz", "stepanpanik", "Sunnyyyyyyy", "RuzovaTabletkaa", "dulisek231"));
        STAFF_MAP.put("Stardix", Set.of("libhalt", "Jin_xD", "Siph_", "Depla", "miruzero", "Otsukisama"));
        STAFF_MAP.put("Syuu", Set.of("Hyronymos", "totKing", "TenoxYT", "Gamingcode", "xXBSEXx", "cyrixx18"));
        STAFF_MAP.put("Twerion", Set.of("Hyronymos", "totKing", "TenoxYT", "Gamingcode", "xXBSEXx", "FreakyDj", "LeDxniel", "CraftlesssTV", "Adexeron", "Julian09x", "Achtstellig"));
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (MinecraftClient.getInstance().world == null || MinecraftClient.getInstance().player == null) return;

        String currentServer = server.getValue();

        for (PlayerEntity player : MinecraftClient.getInstance().world.getPlayers()) {
            String name = player.getName().getString();

            if (detected.contains(name)) continue;

            if ("Spectator".equals(currentServer)) {
                if (player.isSpectator()) {
                    detected.add(name);
                    Services.CHAT.sendRaw("[StaffDetector] Staff detected: " + Formatting.BOLD + name + Formatting.RESET + " is in spectator.");
                    if (autoLobby.getValue()) {
                        MinecraftClient.getInstance().player.networkHandler.sendChatCommand("lobby");
                        Services.CHAT.sendRaw("Returned to lobby due to spectator presence.");
                    }
                }
            } else {
                Set<String> staffList = STAFF_MAP.getOrDefault(currentServer, Collections.emptySet());
                if (staffList.contains(name)) {
                    detected.add(name);
                    Services.CHAT.sendRaw("[StaffDetector] Staff detected: " + Formatting.BOLD + name + Formatting.RESET + ".");
                    if (autoLobby.getValue()) {
                        MinecraftClient.getInstance().player.networkHandler.sendChatCommand("lobby");
                        Services.CHAT.sendRaw("Returned to lobby due to staff presence.");
                    }
                }
            }
        }
    }

    @Override
    public void onEnable() {
        detected.clear();
        super.onEnable();
    }

    @Override
    public String getInfo() {
        return server.getValue();
    }
}
