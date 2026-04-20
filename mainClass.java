import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import swiftbot.SwiftBotAPI;
import swiftbot.Button;
import swiftbot.ImageSize;
import java.awt.image.BufferedImage;
import java.util.Random;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.PrintWriter;
import java.io.FileWriter;

public class mainClass {
    private static SwiftBotAPI robot;
    private static int thresholdValue;
    private static int motorSpeed;
    private static int turnDuration;
    private static long startTime;

    private static ArrayList<String> movementLog = new ArrayList<>();
    private static ArrayList<Integer> lightIntensitiesLog = new ArrayList<>();
    private static ArrayList<Long> obstacleTimestamps = new ArrayList<>();
    private static ArrayList<String> imagePaths = new ArrayList<>();
    private static int maxIntensityObserved = 0;
    private static int lightDetectionCount = 0;
    private static double totalDistanceCm = 0.0;

    private static int objectCount = 0;
    private static int totalObjectsEncountered = 0;

    public static void main(String[] args) {
        try {
            robot = SwiftBotAPI.INSTANCE;
            
            System.out.println("========================================");
            System.out.println("  S W I F T B O T   L I G H T   H U N T E R  ");
            System.out.println("========================================");
            System.out.println("[SYSTEM] Robot is online!");

            Scanner userInput = new Scanner(System.in);
            int sensitivity = 0;

            do {
                System.out.println("[INPUT] Enter Sensitivity Level (1-3):");
                if (userInput.hasNextInt()) {
                    sensitivity = userInput.nextInt();

                    if (sensitivity < 1 || sensitivity > 3) {
                        System.out.println("[ERROR] Invalid input. Please enter a number between 1 and 3.");
                    }
                } else {
                    System.out.println("[ERROR] Invalid input. Please enter a whole number.");
                    userInput.next();
                }
            } while (sensitivity < 1 || sensitivity > 3);

            switch (sensitivity) {
                case 1:
                    motorSpeed = 30;
                    turnDuration = 200;
                    break;
                case 2:
                    motorSpeed = 40;
                    turnDuration = 174; 
                    break;
                case 3:
                    motorSpeed = 80;
                    turnDuration = 117; 
                    break;
            }

            System.out.println("[SYSTEM] Sensitivity set to: " + sensitivity);
            System.out.println("[SYSTEM] Motor Speed configured to: " + motorSpeed + "%");
            System.out.println("[ACTION REQUIRED] Press 'A' button to start the robot.");

            robot.enableButton(Button.A, () -> {
                System.out.println("----------------------------------------");
                System.out.println("[EVENT] Button A pressed!");
                robot.disableButton(Button.A);

                startTime = System.currentTimeMillis();

                calibrateThreshold();
                searchLoop();
            });

        } catch (Exception e) {
            System.out.println("[ERROR] The robot failed to start: " + e.getMessage());
        }
    }

    public static void calibrateThreshold() {
        System.out.println("[SYSTEM] Starting Calibration... ");
        int[] initialLight = processImage();
        thresholdValue = (initialLight[0] + initialLight[1] + initialLight[2]) / 3;
        System.out.println("[SYSTEM] Ambient threshold set at: " + thresholdValue);
    }

    public static boolean avoidObstacle() {
        try {
            double distance = robot.useUltrasound();

            String formattedDistance = String.format("%.2f", distance);

            if (distance < 50.0) {
                System.out.println("[WARNING] Object detected at " + formattedDistance + "cm");

                objectCount++;
                totalObjectsEncountered++;
                obstacleTimestamps.add(System.currentTimeMillis());

                robot.fillUnderlights(new int[] { 255, 0, 0 });

                System.out.println("[SYSTEM] Capturing obstacle image...");
                BufferedImage obstacleImg = robot.takeStill(ImageSize.SQUARE_480x480);
                if (obstacleImg != null) {
                    File outputFile = new File("obstacle_" + totalObjectsEncountered + ".jpg");
                    ImageIO.write(obstacleImg, "jpg", outputFile);
                    imagePaths.add(outputFile.getAbsolutePath());
                    System.out.println("[SYSTEM] Image saved to: obstacle_" + totalObjectsEncountered + ".jpg");
                }

                robot.disableUnderlights();
                return true;
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Ultrasonic sensor failed: " + e.getMessage());
        }
        return false;
    }

    public static void searchLoop() {
        Random rand = new Random();
        Scanner loopScanner = new Scanner(System.in);

        while (true) {
            
            System.out.println("----------------------------------------");

            if (obstacleTimestamps.size() >= 5) {
                long oldestTime = obstacleTimestamps.get(0);
                long newestTime = obstacleTimestamps.get(obstacleTimestamps.size() - 1);
                long timeDifference = newestTime - oldestTime;

                if (timeDifference < 300000) {
                    System.out.println("========================================");
                    System.out.println("[ALERT] 5 objects detected in under 5 minutes.");
                    System.out.println(">> SYSTEM SECURITY PROTOCOL: Enter 'TERMINATE' to stop.");
                    System.out.println("========================================");
                    System.out.print("[INPUT] ");

                    String input = loopScanner.nextLine();
                    if (input.equals("TERMINATE")) {
                        System.out.println("[SYSTEM] Terminating program...");
                        saveLog();
                        System.exit(0);
                    } else {
                        System.out.println("[SYSTEM] Invalid command. Resuming search...");
                        obstacleTimestamps.clear();
                    }
                } else {
                    obstacleTimestamps.remove(0);
                }
            }

            int[] lightLevels = processImage();
            int left = lightLevels[0];
            int centre = lightLevels[1];
            int right = lightLevels[2];

            int brightestSection = Math.max(left, Math.max(centre, right));

            if (brightestSection > maxIntensityObserved) {
                maxIntensityObserved = brightestSection;
            }

            System.out.println("[DATA] Light: L:" + left + " | C:" + centre + " | R:" + right);
            System.out.println("[SYSTEM] Target Speed: " + motorSpeed + "%");

            if (brightestSection > thresholdValue) {
                lightDetectionCount++;
                lightIntensitiesLog.add(brightestSection);

                boolean isBlocked = avoidObstacle();
                int targetIntensity = brightestSection;
                
                ArrayList<String> possibleMoves = new ArrayList<>();

                if (isBlocked) {
                    System.out.println("[ACTION] Path blocked! Taking ALTERNATIVE PATH.");
                    int[] sortedLevels = lightLevels.clone();
                    Arrays.sort(sortedLevels);
                    targetIntensity = sortedLevels[1];

                    robot.fillUnderlights(new int[] { 255, 0, 0 });
                    try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    robot.disableUnderlights();
                    
                    if (left >= right) {
                        possibleMoves.add("Left");
                    } else {
                        possibleMoves.add("Right");
                    }
                } else {
                    robot.fillUnderlights(new int[] { 0, 255, 0 });
                    
                    if (left == targetIntensity) possibleMoves.add("Left");
                    if (centre == targetIntensity) possibleMoves.add("Centre");
                    if (right == targetIntensity) possibleMoves.add("Right");
                }

                String chosenMove = possibleMoves.get(rand.nextInt(possibleMoves.size()));

                if (chosenMove.equals("Left")) {
                    System.out.println("[MOVE] Turning Left 30 degrees");
                    robot.move(-motorSpeed, motorSpeed, turnDuration);
                    movementLog.add("Turned Left 30 degrees");
                } else if (chosenMove.equals("Centre")) {
                    System.out.println("[MOVE] Moving Straight for 1 second");
                    robot.move(motorSpeed, motorSpeed, 1000);
                    movementLog.add("Moved Straight for 1 second");
                    totalDistanceCm += (motorSpeed * 0.5);
                } else {
                    System.out.println("[MOVE] Turning Right 30 degrees");
                    robot.move(motorSpeed, -motorSpeed, turnDuration);
                    movementLog.add("Turned Right 30 degrees");
                }
                robot.disableUnderlights();

            } else {
                int direction = rand.nextInt(3);
                switch (direction) {
                    case 0:
                        System.out.println("[MOVE] Wandering: turning left randomly");
                        robot.move(-motorSpeed, motorSpeed, turnDuration);
                        movementLog.add("Wander: Turned Left");
                        break;
                    case 1:
                        System.out.println("[MOVE] Wandering: going straight randomly");
                        robot.move(motorSpeed, motorSpeed, 1000);
                        movementLog.add("Wander: Moved Straight");
                        totalDistanceCm += (motorSpeed * 0.5);
                        break;
                    case 2:
                        System.out.println("[MOVE] Wandering: turning right randomly");
                        robot.move(motorSpeed, -motorSpeed, turnDuration);
                        movementLog.add("Wander: Turned Right");
                        break;
                }
            }
        }
    }

    public static int[] processImage() {
        BufferedImage image = robot.takeStill(ImageSize.SQUARE_480x480);
        if (image == null) {
            System.out.println("[ERROR] Camera failed to take a picture!");
            return new int[] { 0, 0, 0 };
        }
        int height = image.getHeight();
        int width = image.getWidth();
        int sectionWidth = width / 3;
        long leftSum = 0, centreSum = 0, rightSum = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);

                int r = (pixel >> 16) & 0xff;
                int g = (pixel >> 8) & 0xff;
                int b = (pixel & 0xff);
                int brightness = (r + g + b) / 3;

                if (x < sectionWidth) {
                    leftSum += brightness;
                } else if (x < sectionWidth * 2) {
                    centreSum += brightness;
                } else {
                    rightSum += brightness;
                }
            }
        }
        int numPixels = height * sectionWidth;

        int leftAvg = (int) (leftSum / numPixels);
        int centreAvg = (int) (centreSum / numPixels);
        int rightAvg = (int) (rightSum / numPixels);

        return new int[] { leftAvg, centreAvg, rightAvg };
    }

    public static void saveLog() {
        String logPath = "task8_log.txt";
        try (PrintWriter writer = new PrintWriter(new FileWriter(logPath))) {
            writer.println("=== SEARCH FOR LIGHT: EXECUTION LOG ===");
            writer.println("A. Threshold Light Intensity: " + thresholdValue);
            writer.println("B. Brightest Light Source Detected: " + maxIntensityObserved);
            writer.println("C. Number of times light detected: " + lightDetectionCount);
            writer.println("D. Intensity of light at each instance: " + lightIntensitiesLog.toString());

            long durationSec = (System.currentTimeMillis() - startTime) / 1000;
            writer.println("E. Duration of Execution: " + durationSec + " seconds");
            writer.println("F. Total Distance Travelled (Estimate): " + String.format("%.2f", totalDistanceCm) + " cm");
            writer.println("H. Number of Objects Detected: " + totalObjectsEncountered);
            writer.println("I. Image File Locations: " + imagePaths.toString());
            writer.println("I. Log File Location: " + logPath);
            writer.println("\nG. Movement Journey:");
            for (int i = 0; i < movementLog.size(); i++) {
                writer.println((i + 1) + ". " + movementLog.get(i));
            }

            System.out.println("[SYSTEM] Final log successfully saved to: " + logPath);
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to save log file: " + e.getMessage());
        }
    }
}