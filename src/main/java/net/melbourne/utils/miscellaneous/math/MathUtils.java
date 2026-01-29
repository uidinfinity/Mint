package net.melbourne.utils.miscellaneous.math;

public class MathUtils {

    public static float clamp(float num, float min, float max) {
        return num < min ? min : Math.min(num, max);
    }

    public static double round(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }

    // author: random chinese guy
    public static String convertNumberToRoman(int inputNumber) {
        isNumberWithinNormalLimits(inputNumber);

        String numberString = String.valueOf(inputNumber);
        byte[] stringBytes = numberString.getBytes();
        StringBuilder resultRoman = new StringBuilder();

        String[][] romanTable = {
                { "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", },
                { "", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC", },
                { "", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM", },
                { "", "M", "MM", "MMM", "", "", "", "", "", "", } };

        addRoman(stringBytes, resultRoman, romanTable);
        return resultRoman.toString().toUpperCase();
    }

    public static double random(double max, double min) {
        return Math.random() * (max - min) + min;
    }

    private static void isNumberWithinNormalLimits(int inputNumber) {
        if (inputNumber >= 4000 || 0 >= inputNumber) {
            throw new RuntimeException("Out of conversion range," + "Please enter a number between 1 and 3999");
        }
    }

    private static void addRoman(byte[] stringBytes, StringBuilder resultRoman,
                          String[][] romanTable) {
        int numberDigit = stringBytes.length;
        int byteDigit = 0;

        switch (numberDigit) {
            default:
                throw new RuntimeException("Invalid number");
            case 4:
                resultRoman.append(romanTable[3][(stringBytes[byteDigit] - '0')]);
                byteDigit++;
            case 3:
                resultRoman.append(romanTable[2][(stringBytes[byteDigit] - '0')]);
                byteDigit++;
            case 2:
                resultRoman.append(romanTable[1][(stringBytes[byteDigit] - '0')]);
                byteDigit++;
            case 1:
                resultRoman.append(romanTable[0][(stringBytes[byteDigit] - '0')]);
        }
    }

    public static int angleDirection(float yaw, int disfunctional) {
        int angle = (int) (yaw + (double) 360 / (2 * disfunctional) + 0.5) % 360;

        if (angle < 0) {
            angle += 360;
        }

        return angle / (360 / disfunctional);
    }
}
