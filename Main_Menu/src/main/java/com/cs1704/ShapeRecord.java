package com.cs1704;
public class ShapeRecord {

    private final String type;
    private final String sizeDescription;
    private final String angleDescription;
    private final long timeMs;
    private final double area;

    public ShapeRecord(String type, String sizeDescription, String angleDescription, long timeMs, double area) {
        this.type = type;
        this.sizeDescription = sizeDescription;
        this.angleDescription = angleDescription;
        this.timeMs = timeMs;
        this.area = area;
    }

    public String getType() {
        return type;
    }

    public String getSizeDescription() {
        return sizeDescription;
    }

    public String getAngleDescription() {
        return angleDescription;
    }

    public long getTimeMs() {
        return timeMs;
    }

    public double getArea() {
        return area;
    }

    public String toLogLine() {
        if (angleDescription == null || angleDescription.isEmpty()) {
            return type + ": " + sizeDescription + " (time: " + timeMs + " ms)";
        }
        return type + ": " + sizeDescription + " (angles: " + angleDescription + "; time: " + timeMs + " ms)";
    }
}
