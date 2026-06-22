//This class should parse through and tokenize
//all documents in the directory the user gave and build a
//an inverted index HashMap. Will also save index to a txt/csv
//file to rebuild later.
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class DatabaseBuilder{
    
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
                    processFile(filePath.toFile());
                } catch (IOException e) {
                    System.err.println("Error processing file: " + filePath + " - " + e.getMessage());
                }
            });
        } catch (IOException e) {
            System.err.println("Error walking directory: " + directPath);
            throw e;
        }
    }
    private static void processFile(File file) throws IOException {
    String content = new String(Files.readAllBytes(file.toPath()))
                         .toLowerCase()
                         .replaceAll("[^a-z0-9\\s]", "");
    String[] tokens = content.split("\\s+");
    int actualTokenCount = 0;
    for(String token : tokens){
        if (token.isEmpty()) {
            continue;
        }
        actualTokenCount++;
    }

    HashMap<String, Integer> termFreq = new HashMap<>();
    for (String token : tokens) {
        if (token.isEmpty()) continue;
        termFreq.merge(token, 1, Integer::sum);
        GlobalTermCount.merge(token, 1, Integer::sum);

        InvertedIndex.computeIfAbsent(token, k -> new ArrayList<>());
        if (!InvertedIndex.get(token).contains(file)) {
            InvertedIndex.get(token).add(file);
        }
    }

    TermFreqPerDoc.put(file, termFreq);
    docLengths.put(file, actualTokenCount);

    for (String term : termFreq.keySet()) {
        docsContainingTerm.merge(term, 1, Integer::sum);
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
}