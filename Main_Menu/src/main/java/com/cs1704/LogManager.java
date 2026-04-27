package com.cs1704;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class LogManager {

    private final ArrayList<ShapeRecord> records = new ArrayList<>();

    public void addRecord(ShapeRecord record) {
        records.add(record);
    }

    public String writeSummaryToFile(String filePath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {

            writer.println("Shapes drawn in order:");
            for (ShapeRecord record : records) {
                writer.println(record.toLogLine());
            }
            writer.println();

            ShapeRecord largest = getLargestShape();
            if (largest != null) {
                writer.println("Largest shape by area: " + largest.getType() + ": " + largest.getSizeDescription());
            }

            writer.println("Most frequent shape: " + getMostFrequentShape());
            writer.println("Average draw time: " + getAverageTimeMs() + " ms");

            return filePath;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private ShapeRecord getLargestShape() {
        if (records.isEmpty()) {
            return null;
        }

        ShapeRecord largest = records.get(0);
        for (ShapeRecord record : records) {
            if (record.getArea() > largest.getArea()) {
                largest = record;
            }
        }
        return largest;
    }

    private String getMostFrequentShape() {
        int squareCount = 0;
        int triangleCount = 0;

        for (ShapeRecord record : records) {
            if ("Square".equals(record.getType())) {
                squareCount++;
            } else if ("Triangle".equals(record.getType())) {
                triangleCount++;
            }
        }

        if (squareCount == 0 && triangleCount == 0) {
            return "None";
        }

        if (squareCount >= triangleCount) {
            return "Square: " + squareCount + " times";
        }

        return "Triangle: " + triangleCount + " times";
    }

    private long getAverageTimeMs() {
        if (records.isEmpty()) {
            return 0;
        }

        long total = 0;
        for (ShapeRecord record : records) {
            total += record.getTimeMs();
        }

        return total / records.size();
    }
}
