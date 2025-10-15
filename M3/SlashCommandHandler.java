package M3;

import java.util.Random;

/*
Challenge 2: Simple Slash Command Handler
-----------------------------------------
- Accept user input as slash commands
  - "/greet <name>" → Prints "Hello, <name>!"
  - "/roll <num>d<sides>" → Roll <num> dice with <sides> and returns a single outcome as "Rolled <num>d<sides> and got <result>!"
  - "/echo <message>" → Prints the message back
  - "/quit" → Exits the program
- Commands are case-insensitive
- Print an error for unrecognized commands
- Print errors for invalid command formats (when applicable)
- Capture 3 variations of each command except "/quit"
*/

import java.util.Scanner;

public class SlashCommandHandler extends BaseClass {
    private static String ucid = "mt85"; // <-- change to your UCID

    public static void main(String[] args) {
        printHeader(ucid, 2, "Objective: Implement a simple slash command parser.");

        Scanner scanner = new Scanner(System.in);
        Random random = new Random();
        // Can define any variables needed here

        while (true) {
            System.out.print("Enter command: ");
            // get entered text
            String input = scanner.nextLine().trim();
            String lowerInput = input.toLowerCase();
            // check if greet
            //// process greet
            if(lowerInput.startsWith("/greet")){
                String name = input.substring(7).trim();
                if(!name.isEmpty()){
                    System.out.println("Hello, " + name + "!");
                }else{
                    System.out.println("Error: Missing name for /greet");
                }
            }
            // check if roll
            else if(lowerInput.startsWith("/roll")){
                String dice = input.substring(6).trim();
                if (dice.matches("\\d+d\\d+")) {
                    String[] parts = dice.split("d");
                    int num = Integer.parseInt(parts[0]);
                    int sides = Integer.parseInt(parts[1]);

                    int result = 0;
                    for (int i = 0; i < num; i++) {
                        result += random.nextInt(sides) + 1;
                    }
                    System.out.println("Roll " + num + "d" + sides + " and got " + result);
                }else{
                    System.out.println("Error: must have valid number and sides after /roll");
                }
            }else if(lowerInput.startsWith("/echo")){
                String message = input.substring(6).trim(); 
                if(!message.isEmpty()){
                    System.out.println(message);
                }else{
                    System.out.println("Error: message missing after /echo");
                }
            }else if (lowerInput.equals("/quit")) {
                System.out.println("Program ending");
                break;
            }else {
                System.out.println("Error: command unknown");
            }

            //// process roll
            //// handle invalid formats

            // check if echo
            //// process echo

            // check if quit
            //// process quit

            // handle invalid commnads

            // delete this condition/block, it's just here so the sample runs without edits
            if (1 == 1) {
                System.out.println("Breaking loop");
                break;
            }
        }

        printFooter(ucid, 2);
        scanner.close();
    }
}
