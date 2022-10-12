package org.cs7is3_a1;
import org.apache.lucene.analysis.Analyzer;
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

    static ArrayList<String> getQueries() throws IOException {
        ArrayList<String> queries = new ArrayList<>();
        String text = String.join(" ", Files.readAllLines(Path.of("cran/cran.qry")));
        text = text.replace("?", "");
        String[] lines = text.split("\\.I.*?.W");
        Collections.addAll(queries, lines);
        queries.remove(0);

        return queries;

    }

    static void queryIndex() throws IOException, ParseException {
        Directory idxDirectory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
        DirectoryReader ireader = DirectoryReader.open(idxDirectory);
        IndexSearcher isearcher = new IndexSearcher(ireader);
        StandardAnalyzer analyzer = new StandardAnalyzer();

        ArrayList <String> queries = getQueries();
        ArrayList <Query> parsedQueries = new ArrayList<>();

        QueryParser qp = new QueryParser("text", analyzer);
        Query query;
        for(int i = 0; i < queries.size(); i++){
            query = qp.parse(queries.get(i));
            parsedQueries.add(i, query);
        }


        int hitsPerPage = 10;
        int idx = 0;
        TopDocs docs = isearcher.search(parsedQueries.get(idx), hitsPerPage);
        ScoreDoc[] hits = docs.scoreDocs;

        System.out.println("Found " + hits.length + " hits.");
        System.out.println("Query: " + queries.get(idx));
        for(int i=0; i < hits.length; i++) {
            int docId = hits[i].doc;
            Document d = isearcher.doc(docId);
            System.out.println((i + 1) + ". " + d.get("id") + "\t" + hits[i].score + " \t" + d.get("title"));
        }

        ireader.close();
        idxDirectory.close();
    }

    public static void main( String[] args ) throws IOException, ParseException {
        Analyzer analyzer = new StandardAnalyzer();
        Directory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter iwriter = new IndexWriter(directory, config);

        File file = new File("cran/cran.all.1400");
        processDataset(file, iwriter);

        System.out.println("Creating Index");
        System.out.println("Done");
        iwriter.close();
        directory.close();

        queryIndex();


    }
}
