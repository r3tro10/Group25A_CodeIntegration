import swiftbot.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileWriter;
import java.util.Random;
import java.util.Scanner;

/*
 * SwiftBot Task Program
 * 
 * This program makes the SwiftBot wander around and react to objects
 * depending on the selected mode scanned from a QR code.
 * 
 * Modes:
 * 1. Curious SwiftBot
 * 2. Scaredy SwiftBot
 * 3. Dubious SwiftBot
 * 
 * The program uses:
 * - QR scanning
 * - Ultrasound sensor
 * - Underlights
 * - Camera
 * - Buttons
 */

public class SwiftBotTask {

    // Access the SwiftBot API
    static SwiftBotAPI api = SwiftBotAPI.INSTANCE;

    // program variables
    static String mode = "";
    static int objectCount = 0;
    static long startTime;
    static long lastObjectTime;
    static int wanderSpeed = 30;
    static boolean limitTriggered = false;
    static Random r = new Random();
    static long lastCaptureTime = 0;

    // flag for stopping program
    static boolean running = true;

    public static void main(String[] args) throws Exception {

        System.out.println("=================================");
        System.out.println(" SwiftBot Survey Program ");
        System.out.println(" Scan a QR Code to select mode");
        System.out.println("=================================");

        startTime = System.currentTimeMillis();
        lastObjectTime = System.currentTimeMillis();

        // enable X button to terminate program
        api.enableButton(Button.X, () -> {
            running = false;
        });

        Scanner scanner = new Scanner(System.in);

        while(true){
            System.out.println("Enter wandering speed (10 - 50): ");

            if(scanner.hasNextInt()){
                int input = scanner.nextInt();

                if(input >= 10 && input <= 50){
                    wanderSpeed = input;
                    break;
                }
                else{
                    System.out.println("Invalid range. Please enter a number between 10 and 50.");
                }
            }
            else{
                System.out.println("Invalid input. Please enter a number.");
                scanner.next(); // clears invalid input
            }
        }

        System.out.println("Wandering speed set to: " + wanderSpeed);

        // scan QR code
        mode = scanQRMode();

        System.out.println("Mode selected: " + mode);

        // main loop
        while(running){

        double distance = api.useUltrasound();

            if(distance < 50 && System.currentTimeMillis() - lastCaptureTime > 5000){

            objectCount++;
            lastObjectTime = System.currentTimeMillis();
            lastCaptureTime = System.currentTimeMillis();

            if(mode.equals("Curious SwiftBot")){
                curiousMode(distance);
            }
            else if(mode.equals("Scaredy SwiftBot")){
                scaredyMode(distance);
            }
            else{
                dubiousMode(distance);
            }

            checkObjectLimit();
            }

            else{
                wander();
            }

            // no object for 5 seconds
            if(System.currentTimeMillis() - lastObjectTime > 5000){

                Thread.sleep(1000);
            }
        }

        terminateProgram();
    }

    /*
     * QR Code scanning method
     * keeps checking camera until valid QR is found
     */
    public static String scanQRMode() throws Exception{

        while(true){

            BufferedImage img = api.getQRImage();
            String text = "";

            try {
                text = api.decodeQRImage(img);
            } catch(Exception e) {
                System.out.println("QR scan interrupted.");
            }

            if(!text.isEmpty()){

                if(text.equals("Curious SwiftBot") ||
                   text.equals("Scaredy SwiftBot") ||
                   text.equals("Dubious SwiftBot")){

                    return text;
                }

                else{
                    System.out.println("Invalid QR code. Try again.");
                }
            }

            Thread.sleep(500);
        }
    }

    /*
     * Wandering behaviour
     * robot moves slowly in random directions
     */
    public static void wander() throws Exception{

        api.fillUnderlights(new int[]{0,0,255}); // blue

        int left = wanderSpeed + r.nextInt(10);
        int right = wanderSpeed + r.nextInt(10);

        api.move(left,right,1000);
    }

    /*
     * Checks if too many objects detected in short time
     * if more than 3 objects in 5 minutes, program terminates
     */
    public static void checkObjectLimit() throws Exception {

    long runtime = (System.currentTimeMillis() - startTime)/1000;

        if(objectCount > 3 && runtime < 300 && !limitTriggered){

            limitTriggered = true;

            System.out.println("More than 3 objects detected within 5 minutes.");
            System.out.println("Press A to change mode or X to terminate.");

            api.disableAllButtons(); // clear old ones first
            api.enableButton(Button.A, () -> {
                try{
                    mode = scanQRMode();
                    objectCount = 0;
                    startTime = System.currentTimeMillis();
                    limitTriggered = false;
                    System.out.println("Mode changed to: " + mode);
                }
                catch(Exception e){}
            });

            api.enableButton(Button.X, () -> {
                running = false;
            });
        }
    }

    /*
     * Curious Mode Behaviour
     * maintains 30cm gap from object
     */
    public static void curiousMode(double distance) throws Exception {

    api.fillUnderlights(new int[]{0,255,0}); // green

        if(distance > 30){
            System.out.println("Moving forward");
            api.move(30,30,800);
        }
        else if(distance < 30){
            System.out.println("Moving backward");
            api.move(-30,-30,800);
        }
        else{
            System.out.println("Correct distance");
            api.stopMove();

            for(int i=0;i<3;i++){
                api.fillUnderlights(new int[]{0,255,0});
                Thread.sleep(300);
                api.disableUnderlights();
                Thread.sleep(300);
            }
        }

        // ALWAYS stop after movement
        api.stopMove();

        takeImage();
        api.disableUnderlights();

        // wait 5 seconds
        Thread.sleep(5000);

        double newDistance = api.useUltrasound();

        // adjust if object moved
        if(Math.abs(newDistance - distance) > 2){
            if(newDistance < 50){
                curiousMode(newDistance);
            }
        }
    }

    /*
     * Scaredy Mode Behaviour
     */
        public static void scaredyMode(double distance) throws Exception{

        api.fillUnderlights(new int[]{255,0,0}); // red

        takeImage();

        // blink lights first
        for(int i=0;i<3;i++){
            api.fillUnderlights(new int[]{255,0,0});
            Thread.sleep(300);
            api.disableUnderlights();
            Thread.sleep(300);
        }

        // move away
        api.move(-40,-40,1000);

        // turn opposite direction
        api.move(40,-40,800);

        // run away for 3 seconds
        api.move(40,40,3000);

        api.disableUnderlights();
    }

    /*
     * Dubious Mode Behaviour
     * randomly chooses curious or scaredy
     */
    public static void dubiousMode(double distance) throws Exception{

        Random r = new Random();

        if(r.nextBoolean()){
            curiousMode(distance);
        }
        else{
            scaredyMode(distance);
        }
    }

    /*
     * Capture image using camera
     */
    public static void takeImage() throws Exception{

        BufferedImage img = api.takeStill(ImageSize.SQUARE_720x720);

        String filename = "object_" + System.currentTimeMillis() + ".jpg";

        File file = new File("images/" + filename); // saves in current directory

        try{
            ImageIO.write(img,"jpg", file);
            System.out.println("Image saved: " + file.getAbsolutePath());
        }
        catch(Exception e){
            System.out.println("Error saving image: " + e.getMessage());
        }
    }

    /*
     * Program termination procedure
     */
    public static void terminateProgram() throws Exception{

        api.stopMove();
        api.disableUnderlights();

        long duration = (System.currentTimeMillis() - startTime)/1000;

        System.out.println("Program terminating...");

        writeLog(duration);

        api.disableAllButtons();

        System.exit(0);
    }

    /*
     * Writes log file
     */
    public static void writeLog(long duration) throws Exception {

    File file = new File("swiftbot_log.txt");

    FileWriter writer = new FileWriter(file);

    writer.write("=== SwiftBot Log ===\n");
    writer.write("Mode: " + mode + "\n");
    writer.write("Duration: " + duration + " seconds\n");
    writer.write("Objects detected: " + objectCount + "\n");
    writer.write("Images saved in: /data/home/pi/images/\n");
    writer.write("Wander speed: " + wanderSpeed + "\n");

    writer.close();

    System.out.println("Log saved to: " + file.getAbsolutePath());
    }

}