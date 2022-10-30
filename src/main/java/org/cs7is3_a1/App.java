package org.cs7is3_a1;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.search.similarities.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;


public class App
{
    private static final String INDEX_DIRECTORY = "../index";
    public static void processDataset(File file, IndexWriter iwriter) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String st = "";
        for (int i = 0; i < 3; i++) {
            st = br.readLine();
        }
        int id = 1;
        while (st != null){
            StringBuilder T = new StringBuilder();
            StringBuilder A = new StringBuilder();
            StringBuilder B = new StringBuilder();
            StringBuilder W = new StringBuilder();
            Document doc = new Document();
            if(st.startsWith(".T")){
                st = br.readLine();
            }
            while (!st.startsWith(".A")){
                T.append(st).append(' ');
                st = br.readLine();
            }
            st = br.readLine();
            while (!st.startsWith(".B")){
                A.append(st).append(' ');
                st = br.readLine();
            }
            st = br.readLine();
            while (!st.startsWith(".W")){
                B.append(st).append(' ');
                st = br.readLine();
            }
            st = br.readLine();
            while (st != null && !st.startsWith(".I")){
                W.append(st).append(' ');
                st = br.readLine();
            }
            doc.add(new TextField("id", Integer.toString(id), Field.Store.YES));
            doc.add(new TextField("title", T.toString(), Field.Store.YES));
            doc.add(new TextField("author", A.toString(), Field.Store.YES));
            doc.add(new TextField("dept", B.toString(), Field.Store.YES));
            doc.add(new TextField("text", W.toString(), Field.Store.YES));
            iwriter.addDocument(doc);
            st = br.readLine();
            id++;
        }
    }

    static ArrayList<Query> getParsedQueries() throws IOException, ParseException {
        WhitespaceAnalyzer whitespaceAnalyzer = new WhitespaceAnalyzer();
        SimpleAnalyzer simpleAnalyzer = new SimpleAnalyzer();
        KeywordAnalyzer keywordAnalyzer = new KeywordAnalyzer();
        Analyzer analyzer = new StandardAnalyzer();
        ArrayList<String> queries = new ArrayList<>();
        String text = String.join(" ", Files.readAllLines(Path.of("cran/cran.qry")));
        text = text.replace("?", "");
        String[] lines = text.split("\\.I.*?.W");
        Collections.addAll(queries, lines);
        queries.remove(0);

        ArrayList <Query> parsedQueries = new ArrayList<>();
        QueryParser qp = new QueryParser("text", simpleAnalyzer);
        Query query;
        for(int i = 0; i < queries.size(); i++){
            query = qp.parse(queries.get(i));
            parsedQueries.add(i, query);
        }

        return parsedQueries;

    }

    static ScoreDoc[] queryIndex(int idx, ArrayList<Query>parsedQueries, int max_hits, IndexWriter iwriter, IndexSearcher isearcher) throws IOException, ParseException {
        Directory idxDirectory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
        DirectoryReader ireader = DirectoryReader.open(idxDirectory);

//        ArrayList <String> queries = getQueries();
//        int idx = 0;
        TopDocs docs = isearcher.search(parsedQueries.get(idx), max_hits);
        ScoreDoc[] hits = docs.scoreDocs;

//        System.out.println("Found " + hits.length + " hits.");
//        System.out.println("Query: " + queries.get(idx));
//        for(int i=0; i < hits.length; i++) {
//            int docId = hits[i].doc;
//            Document d = isearcher.doc(docId);
//            System.out.println((i + 1) + ". " + d.get("id") + "\t" + hits[i].score + " \t" + d.get("title"));
//        }

        ireader.close();
        idxDirectory.close();
        return hits;
    }

    static ArrayList<ScoreDoc[]> evaluate(IndexWriter iwriter, IndexSearcher isearcher) throws IOException, ParseException {
        ArrayList<ScoreDoc[]> hits = new ArrayList<ScoreDoc[]>();
        ArrayList <Query> parsedQueries = getParsedQueries();
        for(int i = 0; i < 225; i++){
            hits.add(queryIndex(i, parsedQueries, 50, iwriter, isearcher));
        }
        return hits;
    }

    static void run(int sim_choice) throws IOException, ParseException {
        Analyzer analyzer = new StandardAnalyzer();
        WhitespaceAnalyzer whitespaceAnalyzer = new WhitespaceAnalyzer();
//        StopAnalyzer stopAnalyzer = new StopAnalyzer();
        SimpleAnalyzer simpleAnalyzer = new SimpleAnalyzer();
        Directory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
        
        IndexWriterConfig config = new IndexWriterConfig(simpleAnalyzer);
        DirectoryReader ireader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(ireader);
        BM25Similarity bm25Similarity = new BM25Similarity();
        ClassicSimilarity classicSimilarity = new ClassicSimilarity();

        String simName;
        if(sim_choice == 1){
            config.setSimilarity(bm25Similarity);
            indexSearcher.setSimilarity(bm25Similarity);
            simName = "BM25";
        }
        else if(sim_choice == 0){
            config.setSimilarity(classicSimilarity);
            indexSearcher.setSimilarity(classicSimilarity);
            simName = "VM";
        }
        else{
            System.out.println("Invalid similarity choice");
            return;
        }

        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter iwriter = new IndexWriter(directory, config);

        File file = new File("cran/cran.all.1400");
        processDataset(file, iwriter);

        System.out.println("Creating Index");
        System.out.println("Evaluating");


        ArrayList<ScoreDoc[]> hits = evaluate(iwriter, indexSearcher);
//        System.out.println(Arrays.toString(hits.get(0)));

        FileWriter fileWriter = new FileWriter("results_" + simName +".txt");
        PrintWriter printWriter = new PrintWriter(fileWriter);
        System.out.println("Writing Results");
        int k = 1;
        for(int i = 0; i < 225; i++) {
            for (int j = 0; j < hits.get(i).length; j++) {
                int docId = hits.get(i)[j].doc;
                Document d = indexSearcher.doc(docId);
                printWriter.print((i+1)  + " 0 " + d.get("id")+ " " + k + " " + hits.get(i)[j].score + " STANDARD " + '\n');
                k++;
            }
        }

        iwriter.close();
        directory.close();
        printWriter.close();
        System.out.println("Done");
    }

    public static void main( String[] args ) throws IOException, ParseException {
        run(0);
        run(1);
    }
}
