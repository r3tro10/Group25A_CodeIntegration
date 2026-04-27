package com.cs1704;
import java.io.*;
import java.util.*;
import swiftbot.*;
import java.awt.image.BufferedImage; 
import java.io.File;

public class MasterMind {
	
	// create an instance of the  API to control the hardware
	static SwiftBotAPI swiftBot;
	
    // these will help to track if a button has been pressed
    public static boolean ButtonY_Pressed = false;
    public static boolean ButtonX_Pressed = false;
    public static boolean ButtonA_Pressed = false;
    public static boolean ButtonB_Pressed = false;
	
    // available colours
    private static final char[] COLOURS = {'R', 'G', 'B', 'Y', 'O', 'P'};
    // scanner for detecting keyboard inputs for setting up a custom mode game
    private static Scanner scanner;

    // score tracking
    private int playerScore = 0;
    private int botScore = 0;
    private int roundNumber = 1;

    // used for logging game date
    private File logFile;

    // program entry point
    public static void startTask(SwiftBotAPI sharedBot, Scanner sharedScanner) {
    	swiftBot = sharedBot;
    	scanner = sharedScanner;
        MasterMind game = new MasterMind();
        game.start();
    }

    // main game loop
    public void start() {
        setupLogFile();

        System.out.println("========================================");
        System.out.println("       SWIFTBOT MASTERMIND GAME         ");
        System.out.println("========================================");
      

        boolean running = true;
        while (running) {
        	// Ask player to choose Game mode
            char mode = selectMode();

            // Default Mode
            if (mode == 'A') {
                System.out.println("----------------------------------------");
                System.out.println("DEFAULT MODE SELECTED                   ");
                System.out.println("----------------------------------------"); 
                playGame(4, 6);
            
            // Custom Mode
            } else if (mode == 'B') {
                System.out.println("----------------------------------------");
                System.out.println("CUSTOM MODE SELECTED                   ");
                System.out.println("----------------------------------------"); 
                int codeLength = getValidatedInt("Enter number of colours (3-6): ", 3, 6);
                int maxGuesses = getValidatedInt("Enter maximum number of guesses: ", 1, 20);
                playGame(codeLength, maxGuesses);
                

                
            }
            // Ask if player wants to continue or quit
            running = continueOrQuit();
            roundNumber++;
        }

        // shows where the log file was printed
        System.out.println("Log file saved at: " + logFile.getAbsolutePath());
    }

    // handles the selection of the game modes using the swiftbot buttons
    private char selectMode() {

    	System.out.println("Press:");
    	System.out.println("");
        System.out.println(" [A] Default Mode");
        System.out.println(" [B] Custom Mode");

        // resets buttons before listening
        ButtonA_Pressed = false;
        ButtonB_Pressed = false;

        // enables the button listeners
        swiftBot.enableButton(Button.A, () -> {
            ButtonA_Pressed = true;
        });

        swiftBot.enableButton(Button.B, () -> {
            ButtonB_Pressed = true;
        });

        // waits for and detects the button press
        while (true) {

            if (ButtonA_Pressed) {
                swiftBot.disableButton(Button.A);
                swiftBot.disableButton(Button.B);
                return 'A';
            }

            if (ButtonB_Pressed) {
                swiftBot.disableButton(Button.A);
                swiftBot.disableButton(Button.B);
                return 'B';
            }

            System.out.println("Waiting for button input...");
            
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // main gameplay logic
    private void playGame(int codeLength, int maxGuesses) {
    	
    	// for generating the random code
        String code = generateCode(codeLength);
        
        int attempts = 0;
        boolean won = false;

        // loop through guesses
        while (attempts < maxGuesses) {
            System.out.println("Attempt " + (attempts + 1) + " of " + maxGuesses);

            // gets the players guess from the camera
            String guess = getGuessFromCamera(codeLength);

            // evaluates the guess from the camera
            String feedback = evaluateGuess(code, guess);
            System.out.println("Feedback: " + feedback);

            // logs the round details
            logRound(code, guess, feedback, maxGuesses - attempts - 1);

            // check win condition
            if (guess.equals(code)) {
                won = true;
                break;
            }

            attempts++;
        }

        // handles win/loss sequence
        if (won) {
            playerScore++;
            System.out.println("========================================");
            System.out.println("Congratulations                         ");
            System.out.println("========================================");
            System.out.println("You cracked the code!");
            System.out.println("Secret Code: " + code);
        } else {
            botScore++;
            System.out.println("========================================");
            System.out.println("GAME OVER                               ");
            System.out.println("========================================");
            System.out.println("You have run out of guesses");
            System.out.println("Secret Code: " + code);
        }

        // displays the score
        System.out.println("Score: Player " + playerScore + " - " + botScore + " Bot");
    }

    // generates the random code without repeating a colour
    private String generateCode(int length) {
        List<Character> list = new ArrayList<>();
        for (char c : COLOURS) list.add(c);
        Collections.shuffle(list);

        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(list.get(i));
        }
        return code.toString();
    }

    // uses the camera to scan the sequence of cards
    private String getGuessFromCamera(int length) {
        StringBuilder guess = new StringBuilder();

        System.out.println("Prepare to scan " + length + " colour cards...");

        for (int i = 0; i < length; i++) {
            System.out.println("Hold card " + (i + 1) + " in front of the camera and press Button A...");

            waitForCaptureButton(); // waits for the user to capture

            char detected = captureAndDetectColour();
            System.out.println("Detected: " + detected);

            guess.append(detected);
        }

        return guess.toString();
    }

    // Wait for Button A to trigger capture
    private void waitForCaptureButton() {
        final Object lock = new Object();

        swiftBot.enableButton(Button.A, () -> {
            synchronized (lock) {
                lock.notify();
            }
        });

        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        swiftBot.disableButton(Button.A);
    }

// Capture image and determines the average colour colour
private char captureAndDetectColour() {
try {
    SwiftBotAPI sb = SwiftBotAPI.INSTANCE;
    BufferedImage img = sb.takeStill(ImageSize.SQUARE_1080x1080);

    int width = img.getWidth();
    int height = img.getHeight();

    long totalR = 0, totalG = 0, totalB = 0;
    int sampleCount = 0;

    // Sample pixels to calculate average RGB
    for (int x = 0; x < width; x += 10) {
        for (int y = 0; y < height; y += 10) {
            int rgb = img.getRGB(x, y);

            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;

            totalR += r;
            totalG += g;
            totalB += b;
            sampleCount++;
        }
    }

    int avgR = (int) (totalR / sampleCount);
    int avgG = (int) (totalG / sampleCount);
    int avgB = (int) (totalB / sampleCount);

    return mapRGBToColour(avgR, avgG, avgB);

} catch (Exception e) {
    System.out.println("Error capturing image. Try again.");
    return captureAndDetectColour();
}
}

// Converts RGB values to the closest game colour
private char mapRGBToColour(int r, int g, int b) {

    System.out.println("RGB Detected: R=" + r + " G=" + g + " B=" + b);

    // Find dominant channel
    if (r > g && r > b) {
        if (g > 100) return 'O'; // Red + some green = Orange
        if (b > 100) return 'P'; // Red + blue = Pink
        return 'R';
    }

    if (g > r && g > b) {
        if (r > 150) return 'Y'; // Red + green = Yellow
        return 'G';
    }

    if (b > r && b > g) {
        return 'B';
    }

    // Fallback
    System.out.println("Colour unclear, defaulting to Red.");
    return 'R';
}

 	// compares guess with code and generates the feedback
    private String evaluateGuess(String code, String guess) {
        int plus = 0;
        int minus = 0;

        boolean[] codeUsed = new boolean[code.length()];
        boolean[] guessUsed = new boolean[guess.length()];

        // Check for correct colour and position
        for (int i = 0; i < code.length(); i++) {
            if (code.charAt(i) == guess.charAt(i)) {
                plus++;
                codeUsed[i] = true;
                guessUsed[i] = true;
            }
        }

        // Check for correct colour but wrong position
        for (int i = 0; i < guess.length(); i++) {
            if (guessUsed[i]) continue;

            for (int j = 0; j < code.length(); j++) {
                if (!codeUsed[j] && guess.charAt(i) == code.charAt(j)) {
                    minus++;
                    codeUsed[j] = true;
                    break;
                }
            }
        }

        return "+".repeat(plus) + "-".repeat(minus);
    }

    // creates a log file if it doesn't exist
    private void setupLogFile() {
        try {
            logFile = new File("Mastermind_log.txt");
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
        } catch (IOException e) {
            System.out.println("Error creating log file.");
        }
    }

    // writes data to log file
    private void logRound(String code, String guess, String feedback, int guessesLeft) {
        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.write("Round: " + roundNumber + "\n");
            writer.write("Code: " + code + "\n");
            writer.write("Guess: " + guess + "\n");
            writer.write("Feedback: " + feedback + "\n");
            writer.write("Guesses left: " + guessesLeft + "\n");
            writer.write("Score: " + playerScore + "-" + botScore + "\n");
            writer.write("--------------------------\n");
        } catch (IOException e) {
            System.out.println("Error writing to log file.");
        }
    }

    // handles the continue or quit input using the swiftbot buttons
    private boolean continueOrQuit() {

    	
        System.out.println("----------------------------------------");
        System.out.println("Would you like to play again?           ");
        System.out.println("----------------------------------------"); 
    	System.out.println("Press:");
    	System.out.println("");
        System.out.println(" [Y] Continue Playing");
        System.out.println(" [X] Quit Game");
        
        
        
        // RESET FLAGS
        ButtonY_Pressed = false;
        ButtonX_Pressed = false;

        swiftBot.enableButton(Button.Y, () -> {
            ButtonY_Pressed = true;
        });

        swiftBot.enableButton(Button.X, () -> {
            ButtonX_Pressed = true;
        });

        while (true) {

            if (ButtonY_Pressed) {
                swiftBot.disableButton(Button.X);
                swiftBot.disableButton(Button.Y);
                return true;
            }

            if (ButtonX_Pressed) {
                swiftBot.disableButton(Button.X);
                swiftBot.disableButton(Button.Y);
                return false;
            }

            System.out.println("Waiting for button input...");

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    //validates the numeric input for custom mode
    private int getValidatedInt(String prompt, int min, int max) {
        while (true) {
            try {
                System.out.print(prompt);
                int value = Integer.parseInt(scanner.nextLine());
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("Value must be between " + min + " and " + max);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Try again.");
            }
        }
    }
}
