package net.melbourne.utils.miscellaneous;

public class Timer {
    private long startTime;

    public Timer() {
        startTime = System.nanoTime();
    }

    public boolean hasTimeElapsed(Number time) {
        return (System.nanoTime() - startTime) >= (time.longValue() * 1_000_000L);
    }

    public boolean passed(Number time) {
        return (System.nanoTime() - startTime) >= (time.longValue() * 1_000_000L);
    }

    public void reset() {
        startTime = System.nanoTime();
    }
}