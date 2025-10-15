package M3;

/*
Challenge 1: Command-Line Calculator
------------------------------------
- Accept two numbers and an operator as command-line arguments
- Supports addition (+) and subtraction (-)
- Allow integer and floating-point numbers
- Ensures correct decimal places in output based on input (e.g., 0.1 + 0.2 → 1 decimal place)
- Display an error for invalid inputs or unsupported operators
- Capture 5 variations of tests
*/

public class CommandLineCalculator extends BaseClass {
    private static String ucid = "Rc728"; // <-- change to your ucid

    public static void main(String[] args) {
        printHeader(ucid, 1, "Objective: Implement a calculator using command-line arguments.");

        if (args.length != 3) {
            System.out.println("Usage: java M3.CommandLineCalculator <num1> <operator> <num2>");
            printFooter(ucid, 1);
            return;
        }

        try {
            System.out.println("Calculating result...");
            // extract the equation (format is <num1> <operator> <num2>)
            //Rc728 10/14/2025
            String num1String = args[0];
            String operator = args[1];
            String num2String = args[2];

            double num1 = Double.parseDouble(num1String);
            double num2 = Double.parseDouble(num2String);
            double sum;
            // check if operator is addition or subtraction
            if(operator.equals("+")){
                sum = num1 + num2;
            }else if (operator.equals("-")){
                sum = num1 - num2;
            }else{
                System.out.print("unsupported operator: " + operator);
                printFooter(num2String, 1);
                return;
            }

            
            // check the type of each number and choose appropriate parsing
            
            // generate the equation result (Important: ensure decimals display as the
            // longest decimal passed)
            // i.e., 0.1 + 0.2 would show as one decimal place (0.3), 0.11 + 0.2 would shows
            // as two (0.31), etc

        } catch (Exception e) {
            System.out.println("Invalid input. Please ensure correct format and valid numbers.");
        }

        printFooter(ucid, 1);
    }
}
