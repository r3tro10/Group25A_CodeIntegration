import swiftbot.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;

// CS1814 Task 5 - SpyBot
// Student ID: 2527645

public class Spybot {

    static int MOVE_SPEED  = 50;
    static int SIDE_MS     = 2350;
    static int TURN_SPEED  = 40;
    static int TURN_60_MS  = 410;
    static int TURN_90_MS  = 728;
    static int TURN_120_MS = 996;
    static int TURN_180_MS = 1200;

    static int DOT_MS   = 300;
    static int DASH_MS  = 600;
    static int GAP_MS   = 200;
    static int CHAR_MS  = 300;
    static int WORD_MS  = 300;
    static int END_MS   = 500;
    static int PAUSE_MS = 400;

    static int[] WHITE = {255, 255, 255};
    static int[] BLUE  = {0,   0,   255};
    static int[] AMBER = {255, 165, 0  };
    static int[] RED   = {255, 0,   0  };
    static int[] GREEN = {0,   255, 0  };
    static int[] OFF   = {0,   0,   0  };

    static SwiftBotAPI bot;
    static Scanner scanner;

    // button flags - set to true when pressed
    static boolean pressedA = false;
    static boolean pressedB = false;
    static boolean pressedX = false;
    static boolean pressedY = false;

    static String currentLocation;

    // stores details of the last scanned agent
    static String agentCallSign;
    static String agentLocation;

    static PrintWriter logFile;
    static String logPath;

    static HashMap<String, String> morseToLetter = new HashMap<String, String>();
    static HashMap<String, String> letterToMorse = new HashMap<String, String>();


    public void startTask(SwiftBot sharedBot, Scanner sharedScanner) {

        bot     = (SwiftBotAPI) sharedBot;
        scanner = sharedScanner;

        startLog();
        loadDictionary();
        setupButtons();

        System.out.println("================================");
        System.out.println("     ##         #        #    ");
        System.out.println("    #   ### # # ### ### ###   ");
        System.out.println("     #  # # ### # # # #  #    ");
        System.out.println("      # ###   # ### ###  ##   ");
        System.out.println("    ##  #   ###             \n"+ "  ");
        System.out.println("--------------------------------");
        System.out.println("    Morse Code Communicator");
        System.out.println("================================");
        System.out.println("Log file: " + logPath);
        System.out.println();
        writeLog("System started");

        boolean running = true;
        while (running) {

            System.out.println("================================");
            System.out.println("Press A to send a message");
            System.out.println("");
            System.out.println("Press X to exit");
            System.out.println("================================");

            boolean start = waitAorX();
            if (start == false) {
                System.out.println("Exiting Spybot...");
                writeLog("System exit");
                running = false;
                break;
            }

            // step 1 - sender scans their qr code
            System.out.println("\n-- Step 1: Scan your QR code --");
            boolean senderOk = scanQR();
            if (senderOk == false) {
                System.out.println("Authentication cancelled. Back to menu.");
                writeLog("Auth cancelled");
                continue;
            }
            String senderCallSign = agentCallSign;
            String senderLocation = agentLocation;
            writeLog("Sender authenticated: " + senderCallSign + " at " + senderLocation);

            System.out.println("Press A when ready to type your message...");
            waitA();

            // step 2 - sender enters message in morse code
            System.out.println("\n-- Step 2: Enter message in Morse --");
            String[] result = enterMessage();
            if (result == null) {
                System.out.println("Message failed. Back to menu.");
                writeLog("Message input failed");
                continue;
            }

            String destination = result[0];
            String message     = result[1];

            if (destination.equals(senderLocation)) {
                System.out.println("ERROR: You cannot send to your own location.");
                writeLog("Error: sender tried to message own location");
                continue;
            }

            System.out.println("\nMessage ready:");
            System.out.println("  From    : " + senderCallSign + " at " + senderLocation);
            System.out.println("  To      : " + destination);
            System.out.println("  Message : " + message);
            writeLog("Message recorded: " + senderCallSign + " to " + destination + " - " + message);

            // step 3 - robot drives to the destination
            System.out.println("\n-- Step 3: Navigating --");
            if (currentLocation == null) {
                currentLocation = senderLocation;
            }
            writeLog("Navigating from " + senderLocation + " to " + destination);

            boolean moved = goTo(destination);
            if (moved == false) {
                System.out.println("Navigation failed. Press A to retry or X to skip.");
                if (waitAorX() == true) {
                    moved = goTo(destination);
                }
                if (moved == false) {
                    System.out.println("Giving up. Back to menu.");
                    writeLog("Navigation failed");
                    continue;
                }
            }

            // step 4 - receiver scans their qr code
            System.out.println("\n-- Step 4: Receiver scans QR code --");
            blinkArrival();

            boolean receiverOk = scanReceiver(destination);
            if (receiverOk == false) {
                System.out.println("Receiver failed. Returning to sender.");
                writeLog("Receiver auth failed - returning");
                returnTo(senderLocation);
                continue;
            }
            String receiverCallSign = agentCallSign;
            writeLog("Receiver authenticated: " + receiverCallSign + " at " + destination);

            // step 5 - deliver the message using led flashes
            System.out.println("\n-- Step 5: Delivering message --");
            deliverMessage(senderLocation, message);
            writeLog("Message delivered to " + receiverCallSign + " - " + message);

            // wait 10 seconds then return to sender
            System.out.println("\nWaiting 10 seconds before returning...");
            pause(10000);

            System.out.println("Returning to " + senderLocation);
            writeLog("Returning to " + senderLocation);
            returnTo(senderLocation);

            System.out.println("\nDone. Log saved to: " + logPath);
        }

        System.out.println("\nShutting down...");
        writeLog("Shutdown");
        closeLog();
        try {
            bot.setUnderlights(0, 0, 0);
            bot.stopMove();
        } catch (Exception e) {}
        System.out.println(" ###     ##      ##     ###      ###     #   #    ####  ");
        System.out.println("#       #  #    #  #    #  #     #  #     # #     #    ");
        System.out.println("#  #    #  #    #  #    #  #     ###       #      ###   ");
        System.out.println("#  #    #  #    #  #    #  #     #  #      #      #  ");
        System.out.println(" ###     ##      ##     ###      ###       #      ####  ");
        return;
    }


    // scans a qr code and checks if the agent is valid
    static boolean scanQR() {
        System.out.println("Hold your QR code up to the camera.");
        System.out.println("Press X to cancel.");
        flash(BLUE, 500);

        while (true) {
            try {
                BufferedImage photo = bot.getQRImage();
                String qrText = bot.decodeQRImage(photo);

                if (qrText == null || qrText.trim().equals("")) {
                    System.out.println("No QR code found. Try again or press X to cancel.");
                    flash(RED, 300);
                } else {
                    String[] parts = qrText.trim().split(":");

                    if (parts.length != 2) {
                        System.out.println("Wrong QR format. QR contents should be CALLSIGN:LOCATION");
                        flash(RED, 400);
                    } else {
                        String cs  = parts[0].trim().toUpperCase();
                        String loc = parts[1].trim().toUpperCase();

                        if (isValidAgent(cs, loc)) {
                            agentCallSign = cs;
                            agentLocation = loc;
                            System.out.println("Welcome Agent " + cs + "!");
                            flash(GREEN, 1500);
                            bot.disableUnderlights();
                            return true;
                        } else {
                            System.out.println("Unknown agent or wrong location. Try again.");
                            flash(RED, 400);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Camera error: " + e.getMessage());
            }

            if (pressedX == true) {
                pressedX = false;
                return false;
            }
            pause(500);
        }
    }

    // checks the receiver at the destination, gives 5 attempts
    static boolean scanReceiver(String expectedLocation) {
        System.out.println("Agent at " + expectedLocation + " - scan your QR code.");
        System.out.println("You have 5 attempts to verify your identity.");
        flash(RED, 400);

        for (int i = 1; i <= 5; i++) {
            System.out.println("Attempt " + i + " of 5...");
            try {
                BufferedImage photo = bot.getQRImage();
                String qrText = bot.decodeQRImage(photo);

                if (qrText == null || qrText.trim().equals("")) {
                    System.out.println("No QR code found.");
                    flash(RED, 300);
                } else {
                    String[] parts = qrText.trim().split(":");
                    if (parts.length == 2) {
                        String cs  = parts[0].trim().toUpperCase();
                        String loc = parts[1].trim().toUpperCase();

                        if (isValidAgent(cs, loc) && loc.equals(expectedLocation)) {
                            agentCallSign = cs;
                            agentLocation = loc;
                            System.out.println("Receiver confirmed: Agent " + cs);
                            flash(GREEN, 1000);
                            bot.disableUnderlights();
                            return true;
                        } else {
                            System.out.println("Wrong agent for this location.");
                            flash(RED, 300);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Camera error: " + e.getMessage());
            }
            pause(2000);
        }

        System.out.println("Receiver authentication failed.");
        return false;
    }

    static boolean isValidAgent(String callSign, String location) {
        if (callSign.equals("ALPHA1")   && location.equals("A")) return true;
        if (callSign.equals("BRAVO2")   && location.equals("B")) return true;
        if (callSign.equals("CHARLIE3") && location.equals("C")) return true;
        return false;
    }


    // records morse input from buttons and returns [destination, message]
    static String[] enterMessage() {
        System.out.println("================================");
        System.out.println("Morse code input:");
        System.out.println();
        System.out.println("  X = dot     Y = dash");
        System.out.println("  A = end character");
        System.out.println("  B = end word");
        System.out.println("  End message: enter 0 (-----)  then press A");
        System.out.println();
        System.out.println("First word = destination (A, B or C)");
        System.out.println("================================");

        ArrayList<String> words   = new ArrayList<String>();
        ArrayList<String> letters = new ArrayList<String>();
        String currentMorse       = "";
        boolean finished          = false;

        while (finished == false) {
            char btn = waitMorseButton();

            if (btn == 'X') {
                currentMorse = currentMorse + ".";
                System.out.print(".");
                flash(WHITE, 100);

            } else if (btn == 'Y') {
                currentMorse = currentMorse + "-";
                System.out.print("-");
                flash(BLUE, 100);

            } else if (btn == 'A') {
                if (currentMorse.equals("")) {
                    System.out.println("\n(Nothing entered, ignored)");
                } else if (currentMorse.equals("-----")) {
                    // 0 in morse means end of message
                    if (letters.size() > 0) {
                        words.add(buildWord(letters));
                        letters.clear();
                    }
                    finished = true;
                    System.out.println("\n[End of message]");
                    flash(GREEN, 300);
                } else {
                    String letter = morseToLetter.get(currentMorse);
                    if (letter != null) {
                        letters.add(letter);
                        System.out.println(" -> " + letter);
                        flash(AMBER, 200);
                    } else {
                        System.out.println("\nInvalid morse: " + currentMorse + " - try again.");
                        flash(RED, 300);
                    }
                }
                currentMorse = "";

            } else if (btn == 'B') {
                if (currentMorse.equals("") == false) {
                    String letter = morseToLetter.get(currentMorse);
                    if (letter != null) letters.add(letter);
                    currentMorse = "";
                }
                if (letters.size() > 0) {
                    String word = buildWord(letters);
                    words.add(word);
                    System.out.println("\n[Word: " + word + "]");
                    letters.clear();
                    flash(RED, 200);
                }
            }
        }

        if (words.size() == 0) {
            System.out.println("No message recorded.");
            return null;
        }

        String dest = words.get(0).toUpperCase();
        if (!dest.equals("A") && !dest.equals("B") && !dest.equals("C")) {
            System.out.println("Invalid destination: " + dest + ". Must be A, B or C.");
            return null;
        }

        String msg = "";
        for (int i = 1; i < words.size(); i++) {
            if (i > 1) msg = msg + " ";
            msg = msg + words.get(i);
        }

        System.out.println("\nDestination: " + dest);
        System.out.println("Message: " + msg);
        return new String[]{dest, msg};
    }

    static char waitMorseButton() {
        while (true) {
            if (pressedX == true) { pressedX = false; return 'X'; }
            if (pressedY == true) { pressedY = false; return 'Y'; }
            if (pressedA == true) { pressedA = false; return 'A'; }
            if (pressedB == true) { pressedB = false; return 'B'; }
            pause(30);
        }
    }

    static String buildWord(ArrayList<String> letters) {
        String word = "";
        for (int i = 0; i < letters.size(); i++) {
            word = word + letters.get(i);
        }
        return word;
    }


    static boolean goTo(String destination) {
        if (destination.equals(currentLocation)) {
            System.out.println("Already at " + destination);
            return true;
        }

        System.out.println("Going from " + currentLocation + " to " + destination);
        flash(AMBER, 200);

        try {
            String journey = currentLocation + destination;

            if      (journey.equals("AB")) { turn(false, TURN_120_MS); drive(); }
            else if (journey.equals("AC")) { turn(true,  TURN_120_MS); drive(); }
            else if (journey.equals("BA")) { turn(true,  TURN_60_MS);  drive(); }
            else if (journey.equals("BC")) { turn(true,  TURN_90_MS);  drive(); }
            else if (journey.equals("CA")) { turn(false, TURN_60_MS);  drive(); }
            else if (journey.equals("CB")) { turn(false, TURN_90_MS);  drive(); }

            currentLocation = destination;
            bot.disableUnderlights();
            System.out.println("Arrived at " + currentLocation);
            return true;
        } catch (Exception e) {
            System.out.println("Navigation error: " + e.getMessage());
            flash(RED, 300);
            try { bot.stopMove(); } catch (Exception ex) {}
            return false;
        }
    }

    static void returnTo(String destination) {
        System.out.println("Returning to " + destination);
        flash(AMBER, 200);
        try {
            turn(true, TURN_180_MS);
            drive();
            currentLocation = destination;
            bot.disableUnderlights();
            System.out.println("Arrived at " + currentLocation);
        } catch (Exception e) {
            System.out.println("Navigation error: " + e.getMessage());
            flash(RED, 300);
            try { bot.stopMove(); } catch (Exception ex) {}
        }
    }

    static void turn(boolean right, int ms) throws InterruptedException {
        if (right) {
            System.out.println("Turning right");
            bot.startMove(TURN_SPEED, -TURN_SPEED);
        } else {
            System.out.println("Turning left");
            bot.startMove(-TURN_SPEED, TURN_SPEED);
        }
        Thread.sleep(ms);
        bot.stopMove();
        Thread.sleep(300);
    }

    static void drive() throws InterruptedException {
        System.out.println("Driving forward...");
        bot.startMove(MOVE_SPEED - 3, MOVE_SPEED);
        Thread.sleep(SIDE_MS);
        bot.stopMove();
        Thread.sleep(300);
    }


    static void blinkArrival() {
        System.out.println("Message arrived - waiting for receiver...");
        try {
            for (int i = 0; i < 5; i++) {
                setLED(RED);  Thread.sleep(400);
                setLED(OFF);  Thread.sleep(300);
            }
        } catch (InterruptedException e) {}
    }

    // sends the message as morse code using the LEDs
    static void deliverMessage(String senderLocation, String message) {
        System.out.println("================================");
        System.out.println("Delivering: [" + senderLocation + "] " + message);
        System.out.println("================================");

        try {
            pause(1000);

            System.out.print("Sender [" + senderLocation + "]: ");
            sendWord(senderLocation);
            System.out.println();
            blinkLED(RED, WORD_MS);
            pause(PAUSE_MS);

            String[] words = message.trim().toUpperCase().split("\\s+");
            for (int i = 0; i < words.length; i++) {
                System.out.print("Word [" + words[i] + "]: ");
                sendWord(words[i]);
                System.out.println();
                blinkLED(RED, WORD_MS);
                pause(PAUSE_MS);
            }

            System.out.println("End of message.");
            blinkLED(GREEN, END_MS);

            System.out.println("Delivery complete.");
            setLED(GREEN);
            pause(1500);
            setLED(OFF);

        } catch (InterruptedException e) {
            System.out.println("Delivery interrupted.");
        }
    }

    static void sendWord(String word) throws InterruptedException {
        for (int i = 0; i < word.length(); i++) {
            String letter = String.valueOf(word.charAt(i)).toUpperCase();
            String morse  = letterToMorse.get(letter);

            if (morse == null) {
                System.out.print("[?" + letter + "?]");
                continue;
            }

            System.out.print(letter + "(" + morse + ") ");

            for (int j = 0; j < morse.length(); j++) {
                char sym = morse.charAt(j);
                if (sym == '.') {
                    blinkLED(WHITE, DOT_MS);
                } else {
                    blinkLED(BLUE, DASH_MS);
                }
                pause(GAP_MS);
            }

            blinkLED(AMBER, CHAR_MS);
            pause(PAUSE_MS);
        }
    }


    // loads morse_dictionary.txt, each line is LETTER MORSE e.g. A .-
    static void loadDictionary() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("morse_dictionary.txt"));
            String line = reader.readLine();
            while (line != null) {
                line = line.trim();
                if (line.equals("") == false) {
                    int space = line.indexOf(' ');
                    if (space != -1) {
                        String letter = line.substring(0, space).trim().toUpperCase();
                        String morse  = line.substring(space + 1).trim();
                        morseToLetter.put(morse, letter);
                        letterToMorse.put(letter, morse);
                    }
                }
                line = reader.readLine();
            }
            reader.close();
            System.out.println("Dictionary loaded: " + morseToLetter.size() + " entries.");
        } catch (IOException e) {
            System.out.println("ERROR: Cannot find morse_dictionary.txt");
        }
    }


    static void setupButtons() {
    	bot.enableButton(Button.A, () -> { pressedA = true; });
    	bot.enableButton(Button.B, () -> { pressedB = true; });
    	bot.enableButton(Button.X, () -> { pressedX = true; });
    	bot.enableButton(Button.Y, () -> { pressedY = true; });
        System.out.println("Buttons set up.");
    }

    static boolean waitAorX() {
        pressedA = false;
        pressedX = false;
        while (true) {
            if (pressedA == true) { pressedA = false; return true;  }
            if (pressedX == true) { pressedX = false; return false; }
            pause(30);
        }
    }

    static void waitA() {
        pressedA = false;
        while (pressedA == false) { pause(30); }
        pressedA = false;
    }


    static void setLED(int[] rgb) {
        try {
            bot.setUnderlight(Underlight.FRONT_LEFT,   rgb);
            bot.setUnderlight(Underlight.FRONT_RIGHT,  rgb);
            bot.setUnderlight(Underlight.MIDDLE_LEFT,  rgb);
            bot.setUnderlight(Underlight.MIDDLE_RIGHT, rgb);
            bot.setUnderlight(Underlight.BACK_LEFT,    rgb);
            bot.setUnderlight(Underlight.BACK_RIGHT,   rgb);
        } catch (Exception e) {}
    }

    static void flash(int[] rgb, int ms) {
        setLED(rgb);
        pause(ms);
        setLED(OFF);
        pause(150);
    }

    static void blinkLED(int[] rgb, int ms) throws InterruptedException {
        setLED(rgb);
        Thread.sleep(ms);
        setLED(OFF);
        Thread.sleep(80);
    }


    static void startLog() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        logPath = "Spybot_Log_" + timestamp + ".txt";
        try {
            logFile = new PrintWriter(new FileWriter(logPath, true));
            writeLog("=== Session started ===");
        } catch (IOException e) {
            System.out.println("Warning: could not create log file.");
        }
    }

    static void writeLog(String message) {
        if (logFile != null) {
            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
            logFile.println("[" + time + "] " + message);
            logFile.flush();
        }
    }

    static void closeLog() {
        writeLog("=== Session ended ===");
        if (logFile != null) {
            logFile.close();
        }
        System.out.println("Log saved: " + logPath);
    }


    static void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {}
    }
}
