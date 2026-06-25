//This class recieves the query from ConsoleInterface and uses the HashMap
//created by DatabaseBuilder and BM25 to rank the documents and return a list
//that ConsoleInterface will use for outputting to the user
//BM25 algorithms is as follows:
//S = Sum (IDF(term q_i))
//N = TermFrequency(term q_i, Document D) * (k_1 + 1)
//D = TermFrequency(term q_i, Document D) * (1 - b + b * (|Document D| / avg. doc length))
//BM25 = S * (N/D)
import java.io.File;
import java.util.AbstractMap.SimpleEntry;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
public class QueryProcessor {
    //public static ConcurrentHashMap<String, Integer> GlobalTermCount = DatabaseBuilder.GlobalTermCount;
    public static ConcurrentHashMap<File, HashMap<String, Integer>> TermFreqPerDoc = DatabaseBuilder.TermFreqPerDoc;
    //public static ConcurrentHashMap<String, Integer> docsContainingTerm = DatabaseBuilder.docsContainingTerm;
    public static ConcurrentHashMap<File, Integer> docLengths = DatabaseBuilder.docLengths;
    public static ConcurrentHashMap<String, List<File>> InvertedIndex = DatabaseBuilder.InvertedIndex;
    private static final double K1 = 1.2;
    private static final double B = 0.75;
    public static int getFilesToList(){
        return ConsoleInterface.filesToList;
    }
    public static void main(String[] args) {
        
    }
    
    public static PriorityQueue<Map.Entry<File, Double>> searchDatabase(String query){
        if (query == null || query.trim().isEmpty()){
            System.out.println("Query empty. Please enter a valid search query");
            return null;
        }
        String[] queryTerms = query.toLowerCase().trim().split("\\s+");
        PriorityQueue<Map.Entry<File, Double>> topResults = computeTopKBM25Scores(queryTerms);
        return topResults;
    }
    
    public static double BM25ForTerm(File document, String term, double idf, double avgDocLength){
        //First get term frequencies for this document
        HashMap<String, Integer> termFreqs = TermFreqPerDoc.get(document);
        if(termFreqs == null || !termFreqs.containsKey(term)){
            return 0.0;
        }
        int tf = termFreqs.get(term);
        int docLength = docLengths.get(document);
        double numerator = tf * (K1 + 1); //The numerator of the BM25 formaula
        double denom = tf + K1 * (1 - B + B * ((double) docLength / avgDocLength)); //The denominator of the BM25 formula
        double score = idf * (numerator / denom);
        return score;
    }
    public static PriorityQueue<Map.Entry<File, Double>> computeTopKBM25Scores(String[] queryTerms){

        PriorityQueue<Map.Entry<File, Double>> minHeap = new PriorityQueue<>(Comparator.comparingDouble(Map.Entry::getValue));
        Map<File, Double> runningScores = new HashMap<>();
        double avgDocLength = getAverageDocLength();
        int totalDocs = docLengths.size();
    
        for(String term : queryTerms){
            List<File> candidateDocs = InvertedIndex.get(term);
            if(candidateDocs == null){
                continue; //If this term appears in no documents, skip it
            }
            int docFreq = candidateDocs.size();
            double idf = calculateIDF(totalDocs, docFreq);

            for(File doc : candidateDocs){
                double termScore = BM25ForTerm(doc, term, idf, avgDocLength);
                double newTotal = runningScores.getOrDefault(doc, 0.0) + termScore;
                runningScores.put(doc, newTotal);
            }
        }
        //Build min heap to only have the top k scores
        for(Map.Entry<File, Double> entry : runningScores.entrySet()){
            if(entry.getValue() == 0.0){
                continue; //Skip documents without a score
            }

            if(minHeap.size() < getFilesToList()) {
            minHeap.offer(new SimpleEntry<>(entry.getKey(), entry.getValue()));

            } else if (entry.getValue() > minHeap.peek().getValue()){
            minHeap.poll();
            minHeap.offer(new SimpleEntry<>(entry.getKey(), entry.getValue()));
            }
        }
        return minHeap;
    }

    public static double calculateIDF(int totalDocs, int docFreq){
        return Math.log((totalDocs - docFreq + 0.5) / (docFreq + 0.5) + 1.0);

    }


    public static double getAverageDocLength(){
        if(docLengths.isEmpty()){
            return 0.0;
        }

        int totalLength = 0;
        for(int length : docLengths.values()){
            totalLength += length;
        }
        return (double) totalLength / docLengths.size();
    }
}