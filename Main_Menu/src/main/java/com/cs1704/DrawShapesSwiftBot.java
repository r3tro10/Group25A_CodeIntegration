package com.cs1704;
import java.awt.image.BufferedImage;
import swiftbot.*;
import java.util.Scanner;

public class DrawShapesSwiftBot {

    static SwiftBotAPI swiftBot;
    static LogManager logManager;
    static Scanner scanner;
    static volatile boolean shouldQuit = false;

    public static void startTask(SwiftBotAPI sharedBot, Scanner sharedScanner) {
        swiftBot = sharedBot;
        scanner = sharedScanner;
        logManager = new LogManager();

        System.out.println("Draw Shape program started.");
        System.out.println("Scan a QR code or enter QR data manually.");
        System.out.println("Press X on the SwiftBot at any time to terminate.");

        enableQuitButton();

        while (!shouldQuit) {
            if (processShapes()) {
                System.out.println("QR processed successfully.");
            } else {
                System.out.println("QR processing failed.");
            }
        }

        terminateProgram();
        swiftBot.disableButton(Button.X);
    }
    
    public static boolean checkForQuit() {
        if (shouldQuit) {
            terminateProgram();
            swiftBot.disableButton(Button.X);
            return true;
        }
        return false;
    }
    
    public static void enableQuitButton() {
        swiftBot.enableButton(Button.X, () -> {
            shouldQuit = true;
            System.out.println("\nX button pressed. Terminating program...");
        });
    }
    
    public static String getQRData() {

        BufferedImage img = swiftBot.getQRImage();

        if (img != null) {
            String decodedMessage = swiftBot.decodeQRImage(img);

            if (decodedMessage != null) {
                System.out.println("QR scanned: " + decodedMessage);
                return decodedMessage.trim();
            }
        }

        // fallback
        System.out.println("QR scan failed.");
        System.out.println("Enter QR data manually (or press Enter to cancel):");

        String manualInput = scanner.nextLine().trim();

        if (manualInput.isEmpty()) {
            return null;
        }

        return manualInput;
    }

    public static boolean processShapes() {
    	
    	 if (shouldQuit) {
    	        return false;
    	    }
    	 
    	String decodedMessage = getQRData();

    	if (decodedMessage == null) {
    	    System.out.println("No QR data provided");
    	    return false;
    	}

        String[] commands = QRProcessor.parseShapes(decodedMessage);

        if (!QRProcessor.validateCommands(commands)) {
            return false;
        }

        MovementController controller = new MovementController(swiftBot);

        for (int i = 0; i < commands.length; i++) {
            String command = commands[i].trim().toUpperCase();

            if (QRProcessor.isSquare(command)) {
                int side = QRProcessor.getSquareSide(command);

                System.out.println("Drawing Square with side length: " + side + " cm");

                Square square = new Square(side);

                long start = System.currentTimeMillis();
                square.draw(controller);
                long end = System.currentTimeMillis();

                logManager.addRecord(new ShapeRecord(
                    "Square",
                    String.valueOf(side),
                    "",
                    end - start,
                    square.getArea()
                ));

            } else if (QRProcessor.isTriangle(command)) {
                int[] sides = QRProcessor.getTriangleSides(command);

                System.out.println("Drawing Triangle with sides: "
                        + sides[0] + ", " + sides[1] + ", " + sides[2] + " cm");

                Triangle triangle = new Triangle(sides[0], sides[1], sides[2]);

                long start = System.currentTimeMillis();
                triangle.draw(controller);
                long end = System.currentTimeMillis();

                String angleDescription = String.format("%.2f, %.2f, %.2f",
                        triangle.getAngleA(), triangle.getAngleB(), triangle.getAngleC());

                logManager.addRecord(new ShapeRecord(
                    "Triangle",
                    sides[0] + ", " + sides[1] + ", " + sides[2],
                    angleDescription,
                    end - start,
                    triangle.getArea()
                ));
            }

            if (i < commands.length - 1) {
                System.out.println("Moving backwards 15 cm before next shape");
                controller.moveBackwardCm(15);
                controller.pauseMs(200);
            }
        }

        return true;
    }

    public static void terminateProgram() {
        String filePath = "draw_shapes_log.txt";
        String writtenPath = logManager.writeSummaryToFile(filePath);

        if (writtenPath != null) {
            System.out.println("Log file saved at: " + writtenPath);
        } else {
            System.out.println("Failed to write log file.");
        }
    }
}
