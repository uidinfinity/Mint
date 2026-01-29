package net.melbourne.modules.impl.misc.announcer;

import java.util.HashMap;
import java.util.Random;

public class MessagePrefixes {
    private static final HashMap<TaskType, MessagePrefixes.MessageMaker> messageMakers = new HashMap<>();

    public static String getMessage(TaskType type, String... args) {
        return messageMakers.get(type).getMessage(args);
    }

    static {
        messageMakers.put(
                TaskType.JOIN,
                new MessagePrefixes.MessageMaker(
                        new String[]{
                                "I feel like %s0 has made a huge mistake",
                                "why? %s0",
                                "you have made a grave mistake %s0",
                                "welcome to this shithole %s0",
                                "welcome to my house %s0",
                                "assalamu alaikum %s0",
                                "Welcome to my island, %s0",
                                "Epstein wants you in his private room, %s0.",
                                "Welcome to hell, %s0.",
                                "Beware of the dangers that lurk within this server, %s0.",
                                "Time to show us what your made out of, %s0."
                        }
                )
        );
        messageMakers.put(
                TaskType.LEAVE,
                new MessagePrefixes.MessageMaker(
                        new String[]{
                                "EZ LOG %s0!",
                                "keep logging pussy, %s0",
                                "Uh, BYE! %s0",
                                "yeah fuck off %s0 you fucking pussy",
                                "and dont you fucking come back %s0",
                                "LOL %s0 stupid dog logging out!",
                                "See you in hell, %s0",
                                "See you soon, %s0",
                                "Goodbye %s0!",
                                "Farewell, %name%",
                                "ragequit %s0",
                                "rq %s0",
                                "and don't come back, %s0."
                        }
                )
        );
        messageMakers.put(
                TaskType.BREAK,
                new MessagePrefixes.MessageMaker(new String[]{"I just broke %s1 %s0 thanks to Mint!", "I just zoinked %s1 %s0 thanks to Mint!"})
        );
        messageMakers.put(
                TaskType.PLACE,
                new MessagePrefixes.MessageMaker(
                        new String[]{"I just created an entire kingdom with %s1 %s0 thanks to Mint!", "I just made a really really small base with %s1 %s0 thanks to Mint!"}
                )
        );
        messageMakers.put(TaskType.EAT, new MessagePrefixes.MessageMaker(new String[]{"I just munched on a %s0 thanks to Mint!"}));
        messageMakers.put(
                TaskType.WALK,
                new MessagePrefixes.MessageMaker(
                        new String[]{
                                "I just ran %s0 meters like a track star thanks to Mint!",
                                "Я только что пробежал %s0 метров как звезда марафона благодаря Mint!"
                        }
                )
        );
    }

    private static class MessageMaker {
        public String[] messages;

        public MessageMaker(String[] messages) {
            this.messages = messages;
        }

        public String getMessage(String... values) {
            String toReturn = messages[(new Random()).nextInt(messages.length)];

            for (int i = 0; i < values.length; ++i) {
                toReturn = toReturn.replace("%s" + i, values[i]);
            }

            return toReturn;
        }
    }
}
