//This class walks through and tokenizes
//all documents in the given directory and builds the
//inverted index HashMap as well as all other HashMaps needed for the
// BM25 ranking. It is also in charge of saving and loading the
//inverted index to and from a file. 
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;


public class DatabaseBuilder{
    final static int BUFFER_SIZE = 8192; // 8KB buffer

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "a", "about", "above", "after", "again", "against", "all", "am", "an", "and",
        "any", "are", "aren't", "as", "at", "be", "because", "been", "before", "being",
        "below", "between", "both", "but", "by", "can", "cannot", "could", "couldn't",
        "did", "didn", "do", "does", "doesn", "doing", "don", "down", "during",
        "each", "few", "for", "from", "further", "had", "has", "have", "having",
        "he", "her"
    ));
        
    //HashMap to store the frequency of a term in the entire corpus; String is term & Integer is how many times it shows up in the corpus of documents
    public static ConcurrentHashMap<String, Integer> GlobalTermCount = new ConcurrentHashMap<>(); 
    
    //HashMap that stores how many times a term shows up per document; File is the file it shows up in, String is term and Integer is times it shows up in that document
    public static ConcurrentHashMap<File, HashMap<String, Integer>> TermFreqPerDoc = new ConcurrentHashMap<>(); 
    
    //HashMap for the number of documents that have a term; String is term and Integer is the number of documents in the corpus that contain that word
    public static ConcurrentHashMap<String, Integer> docsContainingTerm = new ConcurrentHashMap<>(); 
    
    //The length of a given file, needed for BM25; File is the document and Integer is how many terms it contains
    public static ConcurrentHashMap<File, Integer> docLengths = new ConcurrentHashMap<>();  
    
    //The inverted index; String is a term and List contains the documents that contain that word
    public static ConcurrentHashMap<String, List<File>> InvertedIndex = new ConcurrentHashMap<>();


    public static void buildIndex(String directPath) throws IOException{
        //Clear these objects if they have data from a previous run
        GlobalTermCount.clear();
        TermFreqPerDoc.clear();
        docsContainingTerm.clear();
        docLengths.clear();
        InvertedIndex.clear();

        Path pathToWalk = Paths.get(directPath);
        if(!Files.exists(pathToWalk)){
            System.out.println("Directory does not exist: " + directPath);
            return;
        }

        int threadCount = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();    //List of tasks assigned to threads

        try (Stream<Path> stream = Files.walk(pathToWalk)) {
            stream.filter(Files::isRegularFile).forEach(filePath -> {
                futures.add(executor.submit(() -> {
                    try {
                        System.out.println("Processing: " + filePath.getFileName());
                        processFile(filePath);
                    } catch (IOException e) {
                        System.err.println("Error processing file: " + filePath + " - " + e.getMessage());
                    }
                }));
            });
        } catch (IOException e) {
            System.err.println("Error walking directory: " + directPath);
            executor.shutdownNow();
            throw e;
        } finally {
            executor.shutdown();
        }

        try {
            if (!executor.awaitTermination(1, TimeUnit.HOURS)) { //Wait up to an hour for tasks to finish
                System.out.println("1 hour timeout waiting for tasks to finish has ended. Now forcing shutdown. Some files may not have been processed.");
                executor.shutdownNow();
            }
            for (Future<?> future : futures) { //Loop through all tasks and checks whether it completed successfully or threw an excepiton
                future.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Index build interrupted", e);
        } catch (ExecutionException e) {
            throw new IOException("Error processing file", e.getCause());
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
                    if (!Character.isLetterOrDigit(c)) {//
                        if (str.length() > 1){ //Only process if token is longer than 1 character
                            String token = str.toString();
                            if (!isStopWord(token)){
                                Stemmer STEMMER = new Stemmer();
                                token = stem(token, STEMMER); //Stem the token before processing
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
                    Stemmer STEMMER = new Stemmer();
                    token = stem(token, STEMMER); //Stem the token before processing
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

    @SuppressWarnings("unchecked")
    public static void loadIndex (String savePath) throws IOException, ClassNotFoundException{
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(savePath))){
            GlobalTermCount = new ConcurrentHashMap<>((Map<String, Integer>) ois.readObject());
            TermFreqPerDoc = new ConcurrentHashMap<>((Map<File, HashMap<String, Integer>>) ois.readObject());
            docsContainingTerm = new ConcurrentHashMap<>((Map<String, Integer>) ois.readObject());
            docLengths = new ConcurrentHashMap<>((Map<File, Integer>) ois.readObject());
            InvertedIndex = new ConcurrentHashMap<>();
            Map<String, List<File>> loadedIndex = (Map<String, List<File>>) ois.readObject();
            loadedIndex.forEach((term, files) -> InvertedIndex.put(term, new CopyOnWriteArrayList<>(files)));
        }
    }

    public static boolean isStopWord(String token){
        return STOP_WORDS.contains(token);
    }

    private static String stem(String token, Stemmer STEMMER){
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
            InvertedIndex.computeIfAbsent(token, k -> new CopyOnWriteArrayList<>()).add(file);
        }

    }
}