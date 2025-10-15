package M3;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/*
Challenge 3: Mad Libs Generator (Randomized Stories)
-----------------------------------------------------
- Load a **random** story from the "stories" folder
- Extract **each line** into a collection (i.e., ArrayList)
- Prompts user for each placeholder (i.e., <adjective>) 
    - Any word the user types is acceptable, no need to verify if it matches the placeholder type
    - Any placeholder with underscores should display with spaces instead
- Replace placeholders with user input (assign back to original slot in collection)
*/

public class MadLibsGenerator extends BaseClass {
    private static final String STORIES_FOLDER = "M3/stories";
    private static String ucid = "Rc728"; // <-- change to your ucid

    public static void main(String[] args) {
        printHeader(ucid, 3,
                "Objective: Implement a Mad Libs generator that replaces placeholders dynamically.");

        Scanner scanner = new Scanner(System.in);
        File folder = new File(STORIES_FOLDER);

        if (!folder.exists() || !folder.isDirectory() || folder.listFiles().length == 0) {
            System.out.println("Error: No stories found in the 'stories' folder.");
            printFooter(ucid, 3);
            scanner.close();
            return;
        }
        List<String> lines = new ArrayList<>();
        // Start edits
        //RC728 10/14/25
        File file;

        // load a random story file
        int Random = (int)(Math.random()*5+1);
        if(Random == 1){
             file = new File("M3/stories/story1.txt");
        }else if(Random == 2){
            file = new File("M3/stories/story2.txt");
        }else if(Random == 3){
            file = new File("M3/stories/story3.txt");
        }else if(Random == 4){
            file = new File("M3/stories/story4.txt");
        }else if(Random == 5){
            file = new File("M3/stories/story5.txt");
        }

        try (Scanner fileScanner = new Scanner(File)) {
            while (fileScanner.hasNextLine()) {
                lines.add(fileScanner.nextLine());
            }
        }catch(Exception e){
            System.out.print("Error ");
        }

        // parse the story lines
        for(int x = 0; x < lines.size(); x ++){
            String line = lines.get(x);
            while(line.contains("<") && line.contains(">")){
                int start = line.indexOf("<");
                int end = line.indexOf(">", start);
                if(end == -1) break;

                String placeholder = line.substring(start + 1, end);
                String prompt = placeholder.replace("_", " ");
                System.out.print("Enter a(n) " + prompt + ": ");
                String userInput = scanner.nextLine();

                line = line.substring(0, start) + userInput + line.substring(end + 1);
            }
        }
        // iterate through the lines

        // prompt the user for each placeholder (note: there may be more than one
        // placeholder in a line)

        // apply the update to the same collection slot

        // End edits
        System.out.println("\nYour Completed Mad Libs Story:\n");
        StringBuilder finalStory = new StringBuilder();
        for (String line : lines) {
            finalStory.append(line).append("\n");
        }
        System.out.println(finalStory.toString());

        printFooter(ucid, 3);
        scanner.close();
    }
}
