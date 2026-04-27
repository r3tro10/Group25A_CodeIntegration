public class QRProcessor {

    public static String[] parseShapes(String qrData) {
        return qrData.split("&");
    }

    public static boolean isSquare(String command) {
        return command.startsWith("S");
    }

    public static boolean isTriangle(String command) {
        return command.startsWith("T");
    }

    public static boolean validateCommands(String[] commands) {

        if (commands.length > 5) {
            System.out.println("There are more than 5 shapes");
            return false;
        }

        for (String command : commands) {
            command = command.trim().toUpperCase();

            if (isSquare(command)) {
                if (!isValidSquare(command)) {
                    System.out.println("Invalid square: " + command);
                    return false;
                }
            } else if (isTriangle(command)) {
                if (!isValidTriangle(command)) {
                    System.out.println("Invalid triangle: " + command);
                    return false;
                }
            } else {
                System.out.println("Invalid shape detected: " + command);
                return false;
            }
        }

        return true;
    }

    public static boolean isValidSquare(String command) {
        String[] parts = command.split(":");

        if (parts.length != 2) {
            return false;
        }

        try {
            int length = Integer.parseInt(parts[1]);
            return length >= 15 && length <= 85;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidTriangle(String command) {
        String[] parts = command.split(":");

        if (parts.length != 4) {
            return false;
        }

        try {
            int a = Integer.parseInt(parts[1]);
            int b = Integer.parseInt(parts[2]);
            int c = Integer.parseInt(parts[3]);

            if (a < 15 || a > 85 || b < 15 || b > 85 || c < 15 || c > 85) {
                return false;
            }

            return a + b > c && a + c > b && b + c > a;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static int getSquareSide(String command) {
        String[] parts = command.trim().toUpperCase().split(":");
        return Integer.parseInt(parts[1]);
    }

    public static int[] getTriangleSides(String command) {
        String[] parts = command.trim().toUpperCase().split(":");
        return new int[] {
            Integer.parseInt(parts[1]),
            Integer.parseInt(parts[2]),
            Integer.parseInt(parts[3])
        };
    }
}