package com.cs1704;
public class Square {

    private final int sideLength;

    public Square(int sideLength) {
        this.sideLength = sideLength;
    }

    public void draw(MovementController controller) {
        for (int i = 0; i < 4; i++) {
            controller.moveForwardCm(sideLength);

            if (i < 3) {
                controller.turnAngle(90);
                controller.pauseMs(150);
            }
        }

        controller.blinkGreenLEDs();
    }

    public int getSideLength() {
        return sideLength;
    }

    public double getArea() {
        return sideLength * sideLength;
    }
}
