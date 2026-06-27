import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Scanner;

//This class handles all user input and output
public class ConsoleInterface {
    public static int filesToList = 5;
    public static String searchPath = "";
    public static String pathToSaveDatabase = "index.idx";
    static boolean runProgram = true;

    public static void main(String[] args) throws InterruptedException {
        Scanner scan = new Scanner(System.in);
        System.out.println("Welcome to Awesome Retrieval System!");
        while (runProgram) {
            if (searchPath.equals("")) {
                System.out.println(
                        "The directroy to search through is not provided. Please provide the path to a folder to use when building inverted index table:");
                searchPath = scan.nextLine();

            } else {
                mainMenu(scan);

            }
        }
    }

    public static void mainMenu(Scanner scan) throws InterruptedException {
        System.out.println("Please select an option");
        printOptions();
        int option = scan.nextInt();
        scan.nextLine();
        switch (option) {
            case 1 -> searchDatabase(scan);
            case 2 -> buildDatabase(searchPath);
            case 3 -> settings(scan);
            case 4 -> saveDatabase(pathToSaveDatabase, scan);
            case 5 -> loadDatabase(pathToSaveDatabase);
            case 0 -> {
                System.out.println("Goodbye");
                scan.close();
                runProgram = false;
            }
            default -> System.out.println("Not an option\n");
        }
        Thread.sleep(1000);

    }

    public static void printOptions() {
        System.out.println("1. Perform search - query files");
        System.out.println("2. Build database - create inverted index table using provided search directory");
        System.out.println("3. Settings - change settings");
        System.out.println("4. Save database - save inverted index to .idx file");
        System.out.println("5. Load database - create inverted index from .idx file");
        System.out.println("0. Quit\n");
    }

    public static void settings(Scanner scan) {
        boolean runSettings = true;
        while (runSettings) {
            System.out.println("1. Set directory to traverse");
            System.out.println("2. Change number of documents to list");
            System.out.println("3. Set path to save/load database");
            System.out.println("0. Exit settings");
            int option = scan.nextInt();
            scan.nextLine();
            switch (option) {
                case 1 -> {
                    System.out.printf("Enter path to directory (current set path is: %s):\n", searchPath);
                    searchPath = scan.nextLine();
                }
                case 2 -> {
                    System.out.printf(
                            "Enter the number of documents to use (Default is 5, current set number is: %s):\n",
                            filesToList);
                    filesToList = scan.nextInt();
                    scan.nextLine();
                }
                case 3 -> {
                    System.out.printf(
                            "Enter path to file to save database to and load database from. Must be a .idx file. (Current file is: %s)",
                            pathToSaveDatabase);
                    pathToSaveDatabase = scan.nextLine();
                }
                case 0 -> {
                    System.out.println("");
                    runSettings = false;
                }
            }

        }
    }

    public static void searchDatabase(Scanner scan) {
        System.out.println("Enter your search query:");
        String query = scan.nextLine();
        System.out.println("Searching...");
        PriorityQueue<Map.Entry<File, Double>> minHeap = QueryProcessor.searchDatabase(query);
        if (minHeap == null || minHeap.isEmpty()) {
            System.out.println("Query returned no results");
            return;
        }
        List<Map.Entry<File, Double>> results = new ArrayList<>();
        while (!minHeap.isEmpty()) {
            results.add(minHeap.poll());
        }
        Collections.reverse(results);

        boolean runProgram = true;
        while (runProgram) {
            System.out.printf("======= Top %d Results =======%n", results.size());
            for (int i = 0; i < results.size(); i++) {
                Map.Entry<File, Double> result = results.get(i);
                System.out.printf("%d. %s (Score: %.4f)%n", i + 1, result.getKey().getName(), result.getValue());

            }
            System.out.println("Which file would you like to open? 0 to exit to main menu");
            int option = scan.nextInt();
            scan.nextLine();
            if (option == 0) {
                runProgram = false;
            } else if (option >= 1 && option <= results.size()) {
                File selectedFile = results.get(option - 1).getKey();
                openFile(selectedFile);
            } else {
                System.out.println("Not an option, please try again");
            }

        }
    }

    public static void openFile(File file) {
        if (!Desktop.isDesktopSupported()) {
            System.out.println("Desktop operations are not supported");
            return;
        }
        Desktop desktop = Desktop.getDesktop();
        if (!file.exists()) {
            System.out.println("File not found " + file.getAbsolutePath());
            return;
        }

        try {
            desktop.open(file);
            System.out.println("Opening: " + file.getName());

        } catch (IOException e) {
            System.out.println("Couldn't open file: " + file.getName());
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void buildDatabase(String pathToDirectory) {
        try {
            DatabaseBuilder.buildIndex(pathToDirectory);
        } catch (IOException e) {
            System.out.println("Error, could not open directory: " + pathToDirectory);
            e.printStackTrace();
        }
    }

    public static void saveDatabase(String pathToSaveDatabase, Scanner scan) {
        if (pathToSaveDatabase.isEmpty()) {
            System.out.println("Please set path in settings first");
            return;
        }
        File dbFile = new File(pathToSaveDatabase);
        if (dbFile.exists()) {
            System.out.println("File already exists: " + pathToSaveDatabase);
            System.out.print("Overwrite? (Y/n): ");
            String response = scan.nextLine().trim().toLowerCase();
            if (!response.equals("y") && !response.equals("yes")) {
                System.out.println("Did not save");
                return;
            }
        }
        try {
            DatabaseBuilder.saveIndex(pathToSaveDatabase);
            System.out.println("Database saved to: " + pathToSaveDatabase);
        } catch (IOException e) {
            System.out.println("Error, could not save database");
            e.printStackTrace();
        }
    }

    public static void loadDatabase(String pathToSaveDatabase) {
        if (pathToSaveDatabase.isEmpty()) {
            System.out.println("Please set path in settings first");
            return;
        }
        try {
            DatabaseBuilder.loadIndex(pathToSaveDatabase);
            System.out.println("Loaded database from: " + pathToSaveDatabase);
        } catch (Exception e) {
            System.out.println("Error, could not load database: " + pathToSaveDatabase);
            e.printStackTrace();
        }
    }

    public static void processQuery(String query) {// This will process our query that same way that we process our corpus
        StringBuilder str = new StringBuilder();
        StringBuilder res = new StringBuilder();
        Stemmer STEMMER = new Stemmer();
        for (int i = 0; i < query.length(); i++) {
            char c = query.charAt(i);
            if (!Character.isLetterOrDigit(c)) {
                if (str.length() > 1) { // Only process if token is longer than 1 character
                    String token = str.toString();
                    if (!DatabaseBuilder.isStopWord(token)) {
                        token = DatabaseBuilder.stem(token, STEMMER);
                        res.append(token);
                    }
                    str.setLength(0); // Reset StringBuilder for next token
                } else {
                    str.setLength(0);// Reset StringBuilder for next token
                }
            } else {// If not whitespace, we add to StringBuilder
                if (c <= 127) { // This quickly lowercases ASCII characters as most documents will be English
                                // text
                    if (c >= 'A' && c <= 'Z')
                        c = (char) (c + 32); // Convert uppercase to lowercase
                    if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9'))
                        str.append(c); // Only append lowercase alphanumeric characters
                } else { // For non-ASCII characters, we can use Character.toLowerCase as a fallback
                    char lowerCase = Character.toLowerCase(c);
                    if (Character.isLetterOrDigit(lowerCase))
                        str.append(lowerCase);
                }
            }
        }
    }
}