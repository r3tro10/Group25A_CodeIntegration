package com.cs1704;
public class Triangle {

    private final int a;
    private final int b;
    private final int c;

    private final double angleA;
    private final double angleB;
    private final double angleC;

    public Triangle(int a, int b, int c) {
        this.a = a;
        this.b = b;
        this.c = c;

        this.angleA = calculateAngle(a, b, c);
        this.angleB = calculateAngle(b, a, c);
        this.angleC = calculateAngle(c, a, b);
    }

    private double calculateAngle(int opposite, int side1, int side2) {
        double numerator = (side1 * side1) + (side2 * side2) - (opposite * opposite);
        double denominator = 2.0 * side1 * side2;
        double value = numerator / denominator;

        value = Math.max(-1.0, Math.min(1.0, value));

        return Math.toDegrees(Math.acos(value));
    }

    public void draw(MovementController controller) {
        controller.moveForwardCm(a);
        controller.turnAngle(180 - angleB);
        controller.pauseMs(150);

        controller.moveForwardCm(b);
        controller.turnAngle(180 - angleC);
        controller.pauseMs(150);

        controller.moveForwardCm(c);

        controller.blinkGreenLEDs();
    }

    public int getA() {
        return a;
    }

    public int getB() {
        return b;
    }

    public int getC() {
        return c;
    }

    public double getAngleA() {
        return angleA;
    }

    public double getAngleB() {
        return angleB;
    }

    public double getAngleC() {
        return angleC;
    }

    public double getArea() {
        double s = (a + b + c) / 2.0;
        return Math.sqrt(s * (s - a) * (s - b) * (s - c));
    }
}
