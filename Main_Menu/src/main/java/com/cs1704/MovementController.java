package com.cs1704;
import swiftbot.SwiftBotAPI;

public class MovementController {

    private static final double MS_PER_DEGREE = 5;
    private static final int VELOCITY = 70;
    private static final double SPEED_CM_PER_SEC = 24.0;

    private final SwiftBotAPI bot;

    public MovementController(SwiftBotAPI bot) {
        this.bot = bot;
    }

    public void moveForwardCm(int distanceCm) {
        int timeMs = (int) ((distanceCm / SPEED_CM_PER_SEC) * 1000);
        bot.move(VELOCITY, VELOCITY, timeMs);
    }

    public void moveBackwardCm(int distanceCm) {
        int timeMs = (int) ((distanceCm / SPEED_CM_PER_SEC) * 1000);
        bot.move(-VELOCITY, -VELOCITY, timeMs);
    }

    public void turnAngle(double angleDegrees) {
        int timeMs = (int) (Math.abs(angleDegrees) * MS_PER_DEGREE);

        if (angleDegrees >= 0) {
            bot.move(VELOCITY, -VELOCITY, timeMs);
        } else {
            bot.move(-VELOCITY, VELOCITY, timeMs);
        }
    }

    public void blinkGreenLEDs() {
        int[] green = {0, 255, 0};
        int[] off = {0, 0, 0};

        try {
            for (int i = 0; i < 3; i++) {
                bot.fillUnderlights(green);
                Thread.sleep(250);
                bot.fillUnderlights(off);
                Thread.sleep(250);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void pauseMs(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
