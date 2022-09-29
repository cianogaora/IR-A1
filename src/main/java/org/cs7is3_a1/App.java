package org.cs7is3_a1;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class App 
{
    private static final String INDEX_DIRECTORY = "../index";

    public static void process(File file, IndexWriter iwriter) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String st = "";
        for (int i = 0; i < 3; i++) {
            st = br.readLine();
        }
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
            doc.add(new TextField("title", T.toString(), Field.Store.YES));
            doc.add(new TextField("author", A.toString(), Field.Store.YES));
            doc.add(new TextField("dept", B.toString(), Field.Store.YES));
            doc.add(new TextField("text", W.toString(), Field.Store.YES));
            iwriter.addDocument(doc);
            st = br.readLine();
        }
    }

    public static void main( String[] args ) throws IOException {
        Analyzer analyzer = new StandardAnalyzer();
        Directory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter iwriter = new IndexWriter(directory, config);

        File file = new File("cran/cran.all.1400");
        process(file, iwriter);

        System.out.println("done");
        iwriter.close();
        directory.close();
    }
}
