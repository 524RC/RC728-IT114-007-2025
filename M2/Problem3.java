package M2;

public class Problem3 extends BaseClass {
    private static Integer[] array1 = {42, -17, 89, -256, 1024, -4096, 50000, -123456};
    private static Double[] array2 = {3.14159265358979, -2.718281828459, 1.61803398875, -0.5772156649, 0.0000001, -1000000.0};
    private static Float[] array3 = {1.1f, -2.2f, 3.3f, -4.4f, 5.5f, -6.6f, 7.7f, -8.8f};
    private static String[] array4 = {"123", "-456", "789.01", "-234.56", "0.00001", "-99999999"};
    private static Object[] array5 = {-1, 1, 2.0f, -2.0d, "3", "-3.0"};
    private static void bePositive(Object[] arr, int arrayNumber) {
        // Only make edits between the designated "Start" and "End" comments
        printArrayInfo(arr, arrayNumber);

        // Challenge 1: Make each value positive
        // Challenge 2: Convert the values back to their original data type and assign it to the proper slot of the `output` array
        // Step 1: sketch out plan using comments (include ucid and date)
        // Step 2: Add/commit your outline of comments (required for full credit)
        // Step 3: Add code to solve the problem (add/commit as needed)
        Object[] output = new Object[arr.length];
        // Start Solution Edits
        //Rc728 9/29/2025
        //first the for loop, loops over the arrays given and makes a new variable of Object called num to be able to use the instanceof
        //then num uses instanceof to check if its a type integer, float, double, string, or object.
        //once it is passed through the checking system, it would change the variable num to a positive using Math.
        //if it's a string, then it will check the first character in the string and if it equals to "-", then it removes it making the
        //string positive
        //then if it is and instance of the object class, then it will run through the entire system thing for each variable in the array
        for(int x = 0; x < arr.length; x++){
            Object num = arr[x];
            if(num instanceof Integer){
                output[x] = Math.abs((int) num);
            }else if(num instanceof Double){
                output[x] = Math.abs((double) num);
            }else if(num instanceof Float){
                output[x] = Math.abs((Float) num);
            }else if(num instanceof String){
                String str = (String) num;
                if(str.substring(0, 1).equals("-")) {
                    output[x] = str.substring(1);
                } else {
                    output[x] = str;
                }
            }else if( num instanceof Object){
                    if( num instanceof Integer){
                        output[x] = Math.abs((int) num);
                    }else if(num instanceof Double){
                        output[x] = Math.abs((double) num);
                    }else if(num instanceof Float){
                        output[x] = Math.abs((Float) num);
                    }else if(num instanceof String){
                        String str = (String) num;
                        if(str.substring(0, 1).equals("-")) {
                            output[x] = str.substring(1);
                        } else {
                            output[x] = str;
                        }    
                    }
            }
        }        
        // End Solution Edits
        System.out.println("Output: ");
        printOutputWithType(output);
        System.out.println("");
        System.out.println("______________________________________");
    }

    public static void main(String[] args) {
        final String ucid = "Rc728"; // <-- change to your UCID
        // no edits below this line
        printHeader(ucid, 3);
        bePositive(array1, 1);
        bePositive(array2, 2);
        bePositive(array3, 3);
        bePositive(array4, 4);
        bePositive(array5, 5);
        printFooter(ucid, 3);

    }
}