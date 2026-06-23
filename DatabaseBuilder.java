//This class should parse through and tokenize
//all documents in the directory the user gave and build a
//an inverted index HashMap. Will also save index to a txt/csv
//file to rebuild later.
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;


public class DatabaseBuilder{
    final static int BUFFER_SIZE = 8192; // 8KB buffer
    
    private static final Stemmer STEMMER = new Stemmer();

    private static int totalTokenCount = 0; //Count of tokens in the document

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "a", "about", "above", "after", "again", "against", "all", "am", "an", "and",
        "any", "are", "aren't", "as", "at", "be", "because", "been", "before", "being",
        "below", "between", "both", "but", "by", "can't", "cannot", "could", "couldn't",
        "did", "didn't", "do", "does", "doesn't", "doing", "don't", "down", "during",
        "each", "few", "for", "from", "further", "had", "hadn't", "has", "hasn't",
        "have", "haven't", "having", "he", "he'd", "he'll", "he's", "her"));
        
    //HashMap to store the frequency of a term in the entire corpus; String is term & Integer is how many times it shows up in the corpus of documents
    public static HashMap<String, Integer> GlobalTermCount = new HashMap<>(); 
    
    //HashMap that stores how many times a term shows up per document; File is the file it shows up in, String is term and Integer is times it shows up in that document
    public static HashMap<File, HashMap<String, Integer>> TermFreqPerDoc = new HashMap<>(); 
    
    //HashMap for the number of documents that have a term; String is term and Integer is the number of documents in the corpus that contain that word
    public static HashMap<String, Integer> docsContainingTerm = new HashMap<>(); 
    
    //The length of a given file, needed for BM25; File is the document and Integer is how many terms it contains
    public static HashMap<File, Integer> docLengths = new HashMap<>();  
    
    //The inverted index; String is a term and List contains the documents that contain that word
    public static HashMap<String, List<File>> InvertedIndex = new HashMap<>(); 


    public static void buildIndex(String directPath) throws IOException{
        //Clear these objects if they have data from a previous run
        GlobalTermCount.clear();
        TermFreqPerDoc.clear();
        docsContainingTerm.clear();
        docLengths.clear();
        InvertedIndex.clear();

        //Walk through files to grab everything at once, loading it all into memory
        Path startPath = Paths.get(directPath);
        if(!Files.exists(Paths.get(directPath))){
            System.out.println("Directory does not exist: " + directPath);
            return;
            
        }
        try (Stream<Path> stream = Files.walk(startPath)) {
        stream.filter(Files::isRegularFile).forEach(filePath -> {
                try {
                    System.out.println("Processing: " + filePath.getFileName());
                    processFile(filePath);
                } catch (IOException e) {
                    System.err.println("Error processing file: " + filePath + " - " + e.getMessage());
                }
            });
        } catch (IOException e) {
            System.err.println("Error walking directory: " + directPath);
            throw e;
        }
    }
    private static void processFile(Path filePath) throws IOException {
        File file = filePath.toFile();
        HashMap<String, Integer> termFreq = new HashMap<>();
        HashSet<String> seenTermsInDoc = new HashSet<>();
        int tokenCountInDoc = 0;
        try(BufferedReader br = Files.newBufferedReader(filePath)){
            char[] buf = new char[BUFFER_SIZE]; //Our own buffer to read in chunks
            StringBuilder str = new StringBuilder(); //StringBuilder to read tokens in memory
            int read;
            while((read = br.read(buf)) != -1){
                for(int i = 0; i < read; i++){
                    char c = buf[i];
                    if (Character.isWhitespace(c)){
                        if (str.length() > 1){ //Only process if token is longer than 1 character
                            String token = str.toString();
                            if (!isStopWord(token)){
                                token = stem(token); //Stem the token before processing
                                processToken(token, file, termFreq, seenTermsInDoc);
                                tokenCountInDoc++;
                            }
                            str.setLength(0); //Reset StringBuilder for next token
                        } else {
                            str.setLength(0); //Reset StringBuilder for next token
                        }
                    } else { //If not whitespace, we add to StringBuilder
                        if(c <= 127){ //This quickly lowercases ASCII characters as most documents will be English text
                            if (c >= 'A' && c <= 'Z') c = (char)(c + 32); // Convert uppercase to lowercase
                            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) str.append(c); // Only append lowercase alphanumeric characters
                        } else { // For non-ASCII characters, we can use Character.toLowerCase as a fallback
                            char lowerCase = Character.toLowerCase(c);
                            if (Character.isLetterOrDigit(lowerCase)) str.append(lowerCase);
                        }
                    }
                }
            }
            // Process any remaining token after the loop
            if (str.length() > 1){
                String token = str.toString();
                if (!isStopWord(token)){
                    token = stem(token); //Stem the token before processing
                    if(token.length() > 1){
                        processToken(token, file, termFreq, seenTermsInDoc);
                        tokenCountInDoc++;
                    }
                }
            }
            // After processing all tokens, update the term frequency for this document
            TermFreqPerDoc.put(file, termFreq);
            // Update the document length after processing all tokens
            docLengths.put(filePath.toFile(), tokenCountInDoc);



        } catch (IOException e) {
            System.err.println("Error reading file: " + filePath + " - " + e.getMessage());
            throw e;
        }
    }

    public static void saveIndex(String savePath) throws IOException{
        try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(savePath))){
            oos.writeObject(GlobalTermCount);
            oos.writeObject(TermFreqPerDoc);
            oos.writeObject(docsContainingTerm);
            oos.writeObject(docLengths);
            oos.writeObject(InvertedIndex);
        }
    }

    public static void loadIndex (String savePath) throws IOException, ClassNotFoundException{
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(savePath))){
            GlobalTermCount = (HashMap<String,Integer>)ois.readObject();
            TermFreqPerDoc = (HashMap<File, HashMap<String, Integer>>)ois.readObject();
            docsContainingTerm = (HashMap<String, Integer>)ois.readObject();
            docLengths = (HashMap<File, Integer>)ois.readObject();
            InvertedIndex = (HashMap<String, List<File>>)ois.readObject();
        }
    }

    public static boolean isStopWord(String token){
        return STOP_WORDS.contains(token);
    }

    private static String stem(String token){
        for(int i = 0; i < token.length(); i++){
            STEMMER.add(token.charAt(i));
        }
        STEMMER.stem();
        return STEMMER.toString();
    }

    public static void processToken(String token, File file, HashMap<String, Integer> termFreq, HashSet<String> seenTermsInDoc) {
        //Update the frequency of this token across the entire corpus
        GlobalTermCount.merge(token, 1, Integer::sum);

        //Update the frequency of this token in the specific document
        termFreq.merge(token, 1, Integer::sum);

        //Update the number of documents that contain this token
        if(seenTermsInDoc.add(token)) { // Only increment if this is the first time we've seen this token in this document
            docsContainingTerm.merge(token, 1, Integer::sum);
            InvertedIndex.computeIfAbsent(token, k -> new ArrayList<>()).add(file);
        }

    }
}