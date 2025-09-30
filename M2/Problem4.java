package M2;

public class Problem4 extends BaseClass {
    private static String[] array1 = { "hello world!", "java programming", "special@#$%^&characters", "numbers 123 456",
            "mIxEd CaSe InPut!" };
    private static String[] array2 = { "hello world", "java programming", "this is a title case test",
            "capitalize every word", "mixEd CASE input" };
    private static String[] array3 = { "  hello   world  ", "java    programming  ",
            "  extra    spaces  between   words   ",
            "      leading and trailing spaces      ", "multiple      spaces" };
    private static String[] array4 = { "hello world", "java programming", "short", "a", "even" };

    private static void transformText(String[] arr, int arrayNumber) {
        // Only make edits between the designated "Start" and "End" comments
        printArrayInfoBasic(arr, arrayNumber);

        // Challenge 1: Remove non-alphanumeric characters except spaces
        // Challenge 2: Convert text to Title Case
        // Challenge 3: Trim leading/trailing spaces and remove duplicate spaces
        // Result 1-3: Assign final phrase to `placeholderForModifiedPhrase`
        // Challenge 4 (extra credit): Extract up to middle 3 characters when possible (beginning starts at middle of phrase excluding the first and last characters),
        // assign to 'placeholderForMiddleCharacters'
        
        // if not enough characters assign "Not enough characters"
 
        // Step 1: sketch out plan using comments (include ucid and date)
        // Step 2: Add/commit your outline of comments (required for full credit)
        // Step 3: Add code to solve the problem (add/commit as needed)
        String placeholderForModifiedPhrase = "";
        String placeholderForMiddleCharacters = "";
        
        for(int i = 0; i <arr.length; i++){
            // Start Solution Edits
            //Rc728 9/29/2025
            //first noSpecialChar repalces all the special characters that aren't a-z, A-Z, or 0-9 with "" then noSpiecialsTrimmed
            //will trim the edges of the string so " hello world " would become "hello world"
            //noSpecialTrimmednoWS removes all the whitespaces with " " and chars splits and creates an array for each character 
            //first it sets capitalize to true so the first character would be capital and when it sees a " " it will make the character
            //after that capitalized so all the words start with capital letters
            //after everything has been tweaked, it all gets put into sentenceProcessed to join all the words together
            //then everything gets assigned to placeholderForModifiedPhrase
            String[] chars = {};
            for(int x = 0; x < arr.length; x++){
               String noSpecialChar = arr[i].replaceAll("[^a-zA-Z0-9 ]", "");
               String noSpecialsTrimmed = noSpecialChar.trim();
               String noSpecialsTrimmednoWS = noSpecialsTrimmed.replaceAll("\\s+", " ");
               chars = noSpecialsTrimmednoWS.split("");

               boolean capitalize = true;
               for(int y = 0; y < noSpecialsTrimmednoWS.length(); y++){
                    if(chars[y].equals(" ")){
                        capitalize = true;
                        if(y + 1 < chars.length){
                            chars[y+1] = chars[y+1].toUpperCase();
                        }
                    }else{
                        if (capitalize){
                            chars[y] = chars[y].toUpperCase();

                        }else{
                            chars[y] = chars[y].toLowerCase();
                        }
                    } 
                    capitalize = false;
               }
            }
            String sentenceProcessed = String.join("", chars);
            placeholderForModifiedPhrase = sentenceProcessed;
             // End Solution Edits
            System.out.println(String.format("Index[%d] \"%s\" | Middle: \"%s\"",i, placeholderForModifiedPhrase, placeholderForMiddleCharacters));
        }

       

        
        System.out.println("\n______________________________________");
    }

    public static void main(String[] args) {
        final String ucid = "Rc728"; // <-- change to your UCID
        // No edits below this line
        printHeader(ucid, 4);

        transformText(array1, 1);
        transformText(array2, 2);
        transformText(array3, 3);
        transformText(array4, 4);
        printFooter(ucid, 4);
    }

}