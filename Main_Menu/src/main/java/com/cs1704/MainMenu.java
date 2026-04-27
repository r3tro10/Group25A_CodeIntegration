package com.cs1704;

import java.util.Scanner;
import swiftbot.SwiftBotAPI;

public class MainMenu {

    private static final int MIN_TASK = 1;
    private static final int MAX_TASK = 9;

    public static void main(String[] args) {
        Scanner sharedScanner = new Scanner(System.in);

        SwiftBotAPI sharedBot = initialiseSwiftBotInstance();

        boolean running = true;
        while (running) {
            displayMainMenu();

            String input = sharedScanner.nextLine().trim();

            if (isValidTaskSelection(input)) {
                int selection = Integer.parseInt(input);
                executeTask(selection, sharedBot, sharedScanner);
            } else if (input.equalsIgnoreCase("X")) {
                saveLogsAndTerminate(sharedBot);
                running = false;
            } else {
                displayInformativeErrorMessage();
            }
        }

        // Intentionally not closing sharedScanner here to preserve stdin ownership rules
        // for integrated task execution under a single shared console lifecycle.
    }

    private static SwiftBotAPI initialiseSwiftBotInstance() {
        return SwiftBotAPI.INSTANCE;
    }

    private static void displayMainMenu() {
        System.out.println();
        System.out.println("============================================");
        System.out.println("        SWIFTBOT MAIN MENU                 ");
        System.out.println("============================================");
        System.out.println("Select 1-9 to launch a task, or X to quit");
        System.out.println("  1. Snakes and Ladders");
        System.out.println("  2. Detect Object");
        System.out.println("  3. Search For Light");
        System.out.println("  4. Dance");
        System.out.println("  5. Noughts and Crosses");
        System.out.println("  6. Draw Shape");
        System.out.println("  7. SpyBot");
        System.out.println("  8. ZigZag");
        System.out.println("  9. Master Mind");
        System.out.print("Selection: ");
    }

    private static boolean isValidTaskSelection(String input) {
        if (input.length() != 1 || !Character.isDigit(input.charAt(0))) {
            return false;
        }

        int selection = Character.getNumericValue(input.charAt(0));
        return selection >= MIN_TASK && selection <= MAX_TASK;
    }

    private static void executeTask(int taskNumber, SwiftBotAPI swiftBotInstance, Scanner scanner) {
        switch (taskNumber) {
            case 1:
                runSnakesAndLadders(swiftBotInstance, scanner);
                break;
            case 2:
                runDetectObject(swiftBotInstance, scanner);
                break;
            case 3:
                runSearchForLight(swiftBotInstance, scanner);
                break;
            case 4:
                runDance(swiftBotInstance, scanner);
                break;
            case 5:
                runNoughtsAndCrosses(swiftBotInstance, scanner);
                break;
            case 6:
                runDrawShape(swiftBotInstance, scanner);
                break;
            case 7:
                runSpyBot(swiftBotInstance, scanner);
                break;
            case 8:
                runZigZag(swiftBotInstance, scanner);
                break;
            case 9:
                runMasterMind(swiftBotInstance, scanner);
                break;
            default:
                displayInformativeErrorMessage();
        }
    }

    private static void runSnakesAndLadders(SwiftBotAPI swiftBotInstance, Scanner scanner) {
        runTaskPlaceholder("Snakes and Ladders", swiftBotInstance, scanner);
    }

    private static void runDetectObject(SwiftBotAPI swiftBotInstance, Scanner scanner) {
        runTaskPlaceholder("Detect Object", swiftBotInstance, scanner);
    }

    private static void runSearchForLight(SwiftBotAPI swiftBotInstance, Scanner scanner) {
        SearchForLight task = new SearchForLight();
        task.startTask(swiftBotInstance, scanner);
    }

    private static void runDance(SwiftBotAPI swiftBotInstance, Scanner scanner) {
        runTaskPlaceholder("Dance", swiftBotInstance, scanner);
    }

    private static void runNoughtsAndCrosses(SwiftBotAPI swiftBotInstance, Scanner scanner) {
        runTaskPlaceholder("Noughts and Crosses", swiftBotInstance, scanner);
    }

    private static void runDrawShape(SwiftBotAPI swiftBotInstance, Scanner scanner) {
        runTaskPlaceholder("Draw Shape", swiftBotInstance, scanner);
    }

    private static void runSpyBot(SwiftBotAPI swiftBotInstance, Scanner scanner) {
        Spybot.startTask(swiftBotInstance, scanner);
    }

    private static void runZigZag(SwiftBotAPI swiftBotInstance, Scanner scanner) {
        runTaskPlaceholder("ZigZag", swiftBotInstance, scanner);
    }

    private static void runMasterMind(SwiftBotAPI swiftBotInstance, Scanner scanner) {
        runTaskPlaceholder("Master Mind", swiftBotInstance, scanner);
    }

    private static void runTaskPlaceholder(String taskName, SwiftBotAPI swiftBotInstance, Scanner scanner) {
        System.out.println();
        System.out.println("--------------------------------------------");
        System.out.println(taskName + " is selected.");
        System.out.println("TODO: Integrate by calling task.startTask(sharedBot, sharedScanner)");
        System.out.println("Do not create a new bot instance, do not close sharedScanner,");
        System.out.println("and do not use System.exit(0) inside task code.");
        System.out.println("Returning to the main menu when the task completes.");
        System.out.println("--------------------------------------------");

        // Keep the parameters in place so future task integrations can use the same signature.
        if (swiftBotInstance == null || scanner == null) {
            throw new IllegalStateException("Shared menu context was not initialised correctly.");
        }
    }

    private static void displayInformativeErrorMessage() {
        System.out.println();
        System.out.println("Invalid input. Please enter a number between 1 and 9, or X to quit.");
    }

    private static void saveLogsAndTerminate(SwiftBotAPI sharedBot) {
        System.out.println();
        turnOffUnderlightsIfSupported(sharedBot);
        System.out.println("Saving logs and terminating the program...");
    }

    private static void turnOffUnderlightsIfSupported(SwiftBotAPI sharedBot) {
        if (sharedBot == null) {
            return;
        }
        try {
            sharedBot.disableUnderlights();
        } catch (Exception ignored) {
            // Light cleanup is best-effort at shutdown.
        }
    }
}