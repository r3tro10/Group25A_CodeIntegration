package com.cs1704;

import java.util.Random;
import java.util.Scanner;
import swiftbot.*;

public class NoughtsAndCrosses {
	static SwiftBotAPI swiftbot;

	public static void startTask(SwiftBotAPI sharedBot, Scanner sharedScanner) {
		
		// 3x3 board to store game state
	    char[][] board = new char[3][3];
	    // scanner used for all user input
	    Scanner input = sharedScanner;

	    System.out.print("Enter your name: ");
	    String userName = input.nextLine();

	    System.out.print("Enter a name for the SwiftBot: ");
	    String botName = input.nextLine();

	    if (botName.equals("")) {
	        botName = "SwiftBot"; // default if user presses enter
	    }
	    
	    // track rounds and scores across the whole session
	    int roundNumber = 1;
	    int userWins = 0;
	    int botWins = 0;
	    int draws = 0;
	    // controls whether the game keeps looping
	    boolean playing = true;
	    
	    swiftbot = sharedBot;

	    System.out.println("SwiftBot connected");
	    System.out.println("Welcome " + userName);
	    System.out.println("Your opponent is " + botName + ".");
 
	    while (playing) {

	        initialiseBoard(board);

	        System.out.println("\n--- Round " + roundNumber + " ---");

	        boolean userStarts = decideFirstTurn();

	        char[] pieces = assignPieces(userStarts);
	        char userPiece = pieces[0];
	        char botPiece = pieces[1];

	        char currentPlayer;
	        if (userStarts) {
	            currentPlayer = userPiece;
	        } else {
	            currentPlayer = botPiece;
	        }

	        System.out.println("Place the SwiftBot in the centre square [1,1].");
	        System.out.println("Press Enter on the keyboard to begin the round.");
	        input.nextLine();
	        
	        boolean gameRunning = true;

	        while (gameRunning) {

	        	printBoard(board);

	        	if (currentPlayer == userPiece) {
	        	    System.out.println(userName + " (" + userPiece + ") turn");
	        	} else {
	        	    System.out.println(botName + " (" + botPiece + ") turn");
	        	}

	            if (currentPlayer == userPiece) {

	                System.out.print("Enter move [row,column]: ");
	                String moveInput = input.nextLine();

	                if (!isInputFormatValid(moveInput)) {
	                    System.out.println("Invalid input format. Please use [row,column] or row,column");
	                }
	                else {
	                    int[] move = parseMove(moveInput);

	                    if (move == null) {
	                        System.out.println("Invalid numbers. Please enter digits like [1,2]");
	                    }
	                    else {
	                        int row = move[0];
	                        int col = move[1];
	                        
	                        if (makeMove(board, row, col, userPiece)) {

	                        	System.out.println("[" + userName + " - " + userPiece + "] moved to [" + row + "," + col + "]");

	                            if (checkWin(board, userPiece)) {
	                                printBoard(board);
	                                System.out.println(userName + " wins");
	                                userWins++;
	                                winCelebration(userPiece);
	                                gameRunning = false;
	                            }
	                            else if (checkDraw(board)) {
	                                printBoard(board);
	                                System.out.println("It's a draw");
	                                draws++;
	                                drawCelebration();
	                                gameRunning = false;
	                            }
	                            else {
	                                currentPlayer = botPiece;
	                            }
	                        }
	                        else {
	                            System.out.println("Invalid move, try again.");
	                        }
	                    }
	                }

	            }
	            else {
	            	int[] botMove = swiftBotTurn(board, botPiece, botName);
	            	moveToSquareAndBack(botMove[0], botMove[1]);

	                if (checkWin(board, botPiece)) {
	                    printBoard(board);
	                    System.out.println(botName + " wins!");
	                    botWins++;
	                    winCelebration(botPiece);
	                    gameRunning = false;
	                }
	                else if (checkDraw(board)) {
	                    printBoard(board);
	                    System.out.println("It's a draw!");
	                    draws++;
	                    drawCelebration();
	                    gameRunning = false;
	                }
	                else {
	                    currentPlayer = userPiece;
	                }
	            }
	        }

	        System.out.println("Round " + roundNumber + " complete.");
	        System.out.println(userName + " piece: " + userPiece);
	        System.out.println(botName + " piece: " + botPiece);

	        System.out.println("Score:");
	        System.out.println(userName + ": " + userWins);
	        System.out.println(botName + ": " + botWins);
	        System.out.println("Draws: " + draws);

	        System.out.print("Play again? (y/n): ");
	        String answer = input.nextLine();

	        if (!answer.equalsIgnoreCase("y")) {
	            playing = false;
	        }
	        roundNumber++;
	    }
	    
	    try {
	        swiftbot.disableUnderlights();
	    } catch (Exception e) {
	        System.out.println("Cleanup error");
	    }

	    return;

	}
	
	// fills the board with empty spaces at the start of each round
    public static void initialiseBoard(char[][] board) {
    	// loop through every row and column
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
            	// reset each square back to empty
                board[i][j] = ' ';
            }
        }
    }

    // prints the current board to the console
    // keeps it simple so the user can clearly see moves
    public static void printBoard(char[][] board) {
        for (int i = 0; i < 3; i++) {
            System.out.println(" " + board[i][0] + " | " + board[i][1] + " | " + board[i][2]);
            // separator lines between rows
            if (i < 2) {
                System.out.println("---+---+---");
            }
        }
    }
    // checks if a move is allowed before placing a piece
    public static boolean isValidMove(char[][] board, int row, int col) {
    	// must be within board range
    	if (row < 0 || row > 2) {
            return false;
        }
        if (col < 0 || col > 2) {
            return false;
        }
        // square must not already be taken
        if (board[row][col] != ' ') {
            return false;
        }
        return true;
    }

    // attempts to place a piece on the board
    // returns true if successful, false if invalid
    public static boolean makeMove(char[][] board, int row, int col, char symbol) {
        if (isValidMove(board, row, col)) {
        	// update board with player's symbol piece
            board[row][col] = symbol;
            return true;
        }
        return false;
    }
    
    public static boolean checkWin(char[][] board, char symbol) {

        // check rows
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == symbol && board[i][1] == symbol && board[i][2] == symbol) {
                return true;
            }
        }

        // check columns
        for (int j = 0; j < 3; j++) {
            if (board[0][j] == symbol && board[1][j] == symbol && board[2][j] == symbol) {
                return true;
            }
        }

        // check main diagonal
        if (board[0][0] == symbol && board[1][1] == symbol && board[2][2] == symbol) {
            return true;
        }

        // check other diagonal
        if (board[0][2] == symbol && board[1][1] == symbol && board[2][0] == symbol) {
            return true;
        }

        return false;
    }    
    
    public static boolean checkDraw(char[][] board) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == ' ') {
                    return false;
                }
            }
        }
        return true;
    }
    
    public static boolean isInputFormatValid(String input) {
        if (input == null) {
            return false;
        }

        input = input.trim();

        if (input.length() < 3) {
            return false;
        }

        if (input.contains(",")) {
            return true;
        }

        return false;
    }

    public static int[] parseMove(String input) {
        input = input.trim();

        input = input.replace("[", "");
        input = input.replace("]", "");

        String[] parts = input.split(",");

        if (parts.length != 2) {
            return null;
        }

        try {
            int row = Integer.parseInt(parts[0].trim());
            int col = Integer.parseInt(parts[1].trim());

            int[] move = new int[2];
            move[0] = row;
            move[1] = col;
            return move;
        }
        catch (Exception e) {
            return null;
        }
    }
    
    // simulates a dice roll until someone wins
    // continues looping until user and swiftbot no longer tie 
    public static boolean decideFirstTurn() {
        Random rand = new Random();

        while (true) {
            int userRoll = rand.nextInt(6) + 1;
            int botRoll = rand.nextInt(6) + 1;

            System.out.println("You rolled: " + userRoll);
            System.out.println("SwiftBot rolled: " + botRoll);

            if (userRoll > botRoll) {
                System.out.println("You go first");
                return true;
            } else if (botRoll > userRoll) {
                System.out.println("SwiftBot goes first");
                return false;
            } else {
            	// tie means we just roll again
                System.out.println("Tie, Rolling again...");
            }
        }
    }
    
    public static char[] assignPieces(boolean userStarts) {
        char[] pieces = new char[2];

        if (userStarts) {
            pieces[0] = 'X'; // user
            pieces[1] = 'O'; // bot
        }
        else {
            pieces[0] = 'O';
            pieces[1] = 'X';
        }

        return pieces;
    }
    
    // handles the bot's move selection
    // picks a random empty square
    public static int[] swiftBotTurn(char[][] board, char botPiece, String botName) {
        Random rand = new Random();

        int row;
        int col;
        // keep generating until it find a free square
        do {
            row = rand.nextInt(3);
            col = rand.nextInt(3);
        } while (!isValidMove(board, row, col));

        System.out.println("[" + botName + " - " + botPiece + "] moved to [" + row + "," + col + "]");
        // apply the move to the board
        makeMove(board, row, col, botPiece);

        // return the move so the robot can physically go there
        int[] move = new int[2];
        move[0] = row;
        move[1] = col;
        return move;
    }
    
    public static void moveToSquareAndBack(int row, int col) {

        // centre square set as starting position
        if (row == 1 && col == 1) {
            return;
        }

        // left middle
        if (row == 1 && col == 0) {
            swiftbot.move(-40, 40, 700);
            pauseBot(200);

            swiftbot.move(45, 45, 1200);
            pauseBot(200);

            blinkGreenThreeTimes();
            pauseBot(200);

            swiftbot.move(-45, -45, 1200);
            pauseBot(200);

            swiftbot.move(40, -40, 700);
            pauseBot(200);
        }

        // right middle
        else if (row == 1 && col == 2) {
            swiftbot.move(40, -40, 700);
            pauseBot(200);

            swiftbot.move(45, 45, 1200);
            pauseBot(200);

            blinkGreenThreeTimes();
            pauseBot(200);

            swiftbot.move(-45, -45, 1200);
            pauseBot(200);

            swiftbot.move(-40, 40, 700);
            pauseBot(200);
        }

        // top middle
        else if (row == 0 && col == 1) {
            swiftbot.move(45, 45, 1200);
            pauseBot(200);

            blinkGreenThreeTimes();
            pauseBot(200);

            swiftbot.move(-45, -45, 1200);
            pauseBot(200);
        }

        // bottom middle
        else if (row == 2 && col == 1) {
            swiftbot.move(-45, -45, 1200);
            pauseBot(200);

            blinkGreenThreeTimes();
            pauseBot(200);

            swiftbot.move(45, 45, 1200);
            pauseBot(200);
        }

     // top left
        else if (row == 0 && col == 0) {
            // centre -> top middle
            swiftbot.move(45, 45, 1200);
            pauseBot(200);

            // face left
            swiftbot.move(-40, 40, 700);
            pauseBot(200);

            // top middle -> top left
            swiftbot.move(45, 45, 1200);
            pauseBot(200);

            blinkGreenThreeTimes();
            pauseBot(200);

            // top left -> top middle
            swiftbot.move(-45, -45, 1200);
            pauseBot(200);

            // face forward again
            swiftbot.move(40, -40, 700);
            pauseBot(200);

            // top middle -> centre
            swiftbot.move(-45, -45, 1200);
            pauseBot(200);
        }

        // top right
        else if (row == 0 && col == 2) {
            // centre -> top middle
            swiftbot.move(45, 45, 1200);
            pauseBot(200);

            // face right
            swiftbot.move(40, -40, 700);
            pauseBot(200);

            // top middle -> top right
            swiftbot.move(45, 45, 1200);
            pauseBot(200);

            blinkGreenThreeTimes();
            pauseBot(200);

            // top right -> top middle
            swiftbot.move(-45, -45, 1200);
            pauseBot(200);

            // face forward again
            swiftbot.move(-40, 40, 700);
            pauseBot(200);

            // top middle -> centre
            swiftbot.move(-45, -45, 1200);
            pauseBot(200);
        }

     // bottom left
        else if (row == 2 && col == 0) {
            // centre -> bottom middle
            swiftbot.move(-45, -45, 1200);
            pauseBot(200);

            // face left
            swiftbot.move(-40, 40, 700);
            pauseBot(200);

            // bottom middle -> bottom left
            swiftbot.move(45, 45, 1200);
            pauseBot(200);

            blinkGreenThreeTimes();
            pauseBot(200);

            // bottom left -> bottom middle
            swiftbot.move(-45, -45, 1200);
            pauseBot(200);

            // face forward again
            swiftbot.move(40, -40, 700);
            pauseBot(200);

            // bottom middle -> centre
            swiftbot.move(45, 45, 1200);
            pauseBot(200);
        }

        // bottom right
        else if (row == 2 && col == 2) {
            // centre -> bottom middle
            swiftbot.move(-45, -45, 1200);
            pauseBot(200);

            // face right
            swiftbot.move(40, -40, 700);
            pauseBot(200);

            // bottom middle -> bottom right
            swiftbot.move(45, 45, 1200);
            pauseBot(200);

            blinkGreenThreeTimes();
            pauseBot(200);

            // bottom right -> bottom middle
            swiftbot.move(-45, -45, 1200);
            pauseBot(200);

            // face forward again
            swiftbot.move(-40, 40, 700);
            pauseBot(200);

            // bottom middle -> centre
            swiftbot.move(45, 45, 1200);
            pauseBot(200);
        }
    }
	
    // simple delay so movements and lights don’t overlap
    public static void pauseBot(int time) {
        try {
            Thread.sleep(time);
        } catch (Exception e) {
            System.out.println("Pause error");
        }
    }
    
    public static void blinkGreenThreeTimes() {
        int[] green = {0, 255, 0};

        try {
            for (int i = 0; i < 3; i++) {
                swiftbot.fillUnderlights(green);
                Thread.sleep(200);
                swiftbot.disableUnderlights();
                Thread.sleep(200);
            }
        } catch (Exception e) {
            System.out.println("Error blinking lights");
        }
    }
    
 // celebration colour depends on the winning piece
 // O win = green, X win = red
 public static void winCelebration(char winningPiece) {
     int[] colour;

     if (winningPiece == 'O') {
         colour = new int[] {0, 255, 0};
     } else {
         colour = new int[] {255, 0, 0};
     }

     try {
         for (int i = 0; i < 3; i++) {
             swiftbot.fillUnderlights(colour);
             Thread.sleep(300);
             swiftbot.disableUnderlights();
             Thread.sleep(300);
         }

         swiftbot.move(50, -50, 800);
         swiftbot.move(-50, 50, 800);
     } catch (Exception e) {
         System.out.println("Celebration error");
     }
 }

    public static void drawCelebration() {
        int[] blue = {0, 0, 255};
        try {
            for (int i = 0; i < 3; i++) {
                swiftbot.fillUnderlights(blue);
                Thread.sleep(300);
                swiftbot.disableUnderlights();
                Thread.sleep(300);
            }
        } catch (Exception e) {
            System.out.println("Celebration error");
        }
    }
}
