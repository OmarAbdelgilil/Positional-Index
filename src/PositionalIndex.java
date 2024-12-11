import com.sun.source.tree.Tree;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class PositionalIndex {
    public static TreeMap<String, TreeMap<Integer, ArrayList<Integer>>> postingList = new TreeMap<>();
    public static TreeMap<String,TreeMap<Integer,Integer>> termFrequency = new TreeMap<>();

    public static TreeMap<String,TreeMap<Integer,Double>> tfWeight = new TreeMap<>();

    public static  TreeMap<String,HashMap<String,Double>> dfIdf = new TreeMap<>();

    public static TreeMap<String,TreeMap<Integer,Double>> tfIdf  = new TreeMap<>();

    public static  TreeMap<Integer,Double> docLength = new TreeMap<>();

    public static TreeMap<String,TreeMap<Integer,Double>> tfIdfNormalized  = new TreeMap<>();
    public static HashSet<String> distinctTerms = new HashSet<>();
    public static HashSet<Integer> distinctDocs = new HashSet<>();
     public  static  String[] keyWords = {"and","or"};
    public static void main(String[] args) throws IOException {
        String folderPath = (new File("").getAbsolutePath()) + "\\docs";
        readFileAndBuildPostingList(folderPath);
        calculateTermFrequency();
        calculateTfWeight();
        calculateDFandIDF();
        calculateTfIdf();
        calculateDocLength();
        calculateTfIdfNormalized();
        System.out.println("Posting List : ");
        System.out.println(postingList.toString());
        System.out.println("==========================================");
        System.out.println("Term frequency: ");
        printIntegerMatrix(termFrequency);
        System.out.println("==========================================");
        System.out.println("TF Weight: ");
        printDoubleMatrix(tfWeight);
        System.out.println("==========================================");
        System.out.println("DF IDF: ");
        printDfIDF();
        System.out.println("==========================================");
        System.out.println("TF.IDF: ");
        printDoubleMatrix(tfIdf);
        System.out.println("==========================================");
        System.out.println("Document length (|DOC|): ");
        printDocLength();
        System.out.println("==========================================");
        System.out.println("TF IDF Normalized: ");
        printDoubleMatrix(tfIdfNormalized);
        System.out.println("==========================================");
        Scanner in = new Scanner(System.in);
        String queryString;
        do {
            System.out.println("Enter Query:(home AND/OR increase)");
            queryString = in.nextLine();
            query(queryString);
            System.out.println("to end search: stop");
            System.out.println();
            System.out.println();
        } while (!queryString.equals("stop"));
        in.close();
        ;

    }
    public static void readFileAndBuildPostingList(String folderPath) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(folderPath + "/part-r-00000"));
        try {
            String line = br.readLine();
            while (line != null) {
                String[] parts = line.split("\t");
                //['term', '1:0;2:1']
                String term = parts[0];
                TreeMap<Integer, ArrayList<Integer>> docIds = new TreeMap<>();

                if (parts.length > 1) {
                    String[] docIds_and_positions = parts[1].split(";");
                    //['1:2','2:1']

                    for (String doc : docIds_and_positions) {
                        String[] docsId_and_positions = doc.split(":");
                        //['1','1,2,3'];
                        Integer docId;

                        docId = Integer.parseInt(docsId_and_positions[0]);
                        distinctDocs.add(docId);
                        String[] positions_strings = docsId_and_positions[1].split(",");
                        ArrayList<Integer> positions = new ArrayList<>();
                        for (String num : positions_strings) {
                            if (!num.isEmpty()) {
                                positions.add(Integer.parseInt(num));
                            }
                        }
                        docIds.put(docId, positions);

                    }


                }
                postingList.put(term,docIds);
                line = br.readLine();
            }
        } finally {
            br.close();
            distinctTerms.addAll(postingList.keySet());
        }


    }

    public static void calculateTermFrequency()
    {

        for (Map.Entry<String, TreeMap<Integer, ArrayList<Integer>>> entry : postingList.entrySet()) {
            String term = entry.getKey();
            TreeMap<Integer,Integer> countTerm = new TreeMap<>();
            for(Integer docId : distinctDocs) {
                countTerm.put(docId, 0);
            }
            for (Map.Entry<Integer, ArrayList<Integer>> postions : entry.getValue().entrySet())
            {
                countTerm.put(postions.getKey(),postions.getValue().size());
            }
            termFrequency.put(term,countTerm);
        }
    }

    public static void calculateTfWeight()
    {
        for (Map.Entry<String, TreeMap<Integer,Integer>> entry : termFrequency.entrySet())
        {
            String term = entry.getKey();
            TreeMap<Integer,Double> countTermWeight = new TreeMap<>();
            for (Map.Entry<Integer, Integer> frequency : entry.getValue().entrySet())
            {
                Integer docId = frequency.getKey();
                if(frequency.getValue() == 0)
                {
                    countTermWeight.put(docId,0.0);
                    continue;
                }
                countTermWeight.put(docId, 1+Math.log10(frequency.getValue()));
             }
            tfWeight.put(term,countTermWeight);
        }
    }

    public static void calculateDFandIDF()
    {

        for (Map.Entry<String, TreeMap<Integer, ArrayList<Integer>>> entry : postingList.entrySet())
        {
            String term = "";
            Double docFrequency = 0.0;
            Integer totalDocs = distinctDocs.size();
            HashMap<String,Double> df_idf = new HashMap<>();
            term = entry.getKey();
            docFrequency = (double)entry.getValue().size();
            df_idf.put("DF",docFrequency);
            df_idf.put("IDF",Math.log10((totalDocs/docFrequency)));
            dfIdf.put(term,df_idf);
        }

    }

    public static  void calculateTfIdf()
    {
        for (Map.Entry<String, TreeMap<Integer,Double>> entry : tfWeight.entrySet())
        {
            String term = entry.getKey();
            TreeMap<Integer,Double> docs = new TreeMap<>();
            for (Map.Entry<Integer,Double> frequency : entry.getValue().entrySet())
            {
                Integer docId = frequency.getKey();
                Double tf_weight = frequency.getValue();
                Double idf = dfIdf.get(term).get("IDF");
                docs.put(docId,tf_weight*idf);
            }
            tfIdf.put(term,docs);
        }
    }

    public static void calculateDocLength()
    {
        TreeMap<Integer,Double> sum_of_squares = new TreeMap<>();
        for (Map.Entry<String, TreeMap<Integer,Double>> entry : tfIdf.entrySet())
        {

            String term = entry.getKey();
            for (Map.Entry<Integer,Double> tfIdfs : entry.getValue().entrySet())
            {
                Integer docId = tfIdfs.getKey();
                if(sum_of_squares.containsKey(docId))
                {
                    sum_of_squares.put(docId,sum_of_squares.get(docId)+(tfIdfs.getValue()*tfIdfs.getValue()));
                }
                else {
                    sum_of_squares.put(docId,(tfIdfs.getValue()*tfIdfs.getValue()));
                }


            }


        }
        for (Map.Entry<Integer,Double> sum : sum_of_squares.entrySet())
        {
            docLength.put(sum.getKey(),Math.sqrt(sum.getValue()));
        }

    }

    public  static void calculateTfIdfNormalized()
    {

        for (Map.Entry<String, TreeMap<Integer,Double>> entry : tfIdf.entrySet())
        {

            String term = entry.getKey();
            TreeMap<Integer,Double> normalizedDoc = new TreeMap<>();
            for (Map.Entry<Integer,Double> tfIdfs : entry.getValue().entrySet())
            {
                Integer docId = tfIdfs.getKey();
                normalizedDoc.put(docId,(tfIdfs.getValue()/docLength.get(docId)));

            }
            tfIdfNormalized.put(term,normalizedDoc);


        }
    }

    public  static  void printIntegerMatrix(TreeMap<String,TreeMap<Integer,Integer>> matrix)
    {
        System.out.print("\t\t\t");
        for(Integer docId : distinctDocs)
        {
            System.out.print("d"+docId+"\t");
        }
        System.out.print("\n");
        for (Map.Entry<String, TreeMap<Integer,Integer>> entry : matrix.entrySet())
        {
            String term = entry.getKey();
            for(int i = term.length() ; i < 9 ; i++ )
            {
                term+=' ';
            }
            System.out.print(term+"\t");
            for (Map.Entry<Integer, Integer> frequency : entry.getValue().entrySet())
            {
                System.out.print(frequency.getValue()+"\t");
            }
            System.out.print("\n");

        }

    }
    public  static  void printDoubleMatrix(TreeMap<String,TreeMap<Integer,Double>> matrix)
    {
        System.out.print("\t\t\t\t");
        for(Integer docId : distinctDocs)
        {
            System.out.print("d"+docId+"\t\t\t");
        }
        System.out.print("\n");
        for (Map.Entry<String, TreeMap<Integer,Double>> entry : matrix.entrySet())
        {
            String term = entry.getKey();
            for(int i = term.length() ; i < 9 ; i++ )
            {
                term+=' ';
            }
            System.out.print(term+"\t");
            for (Map.Entry<Integer, Double> frequency : entry.getValue().entrySet())
            {
                String formatted = String.format("%.6f", frequency.getValue());
                System.out.print(formatted+"\t");
            }
            System.out.print("\n");

        }

    }

    public  static void  printDfIDF()
    {
        System.out.print("\t\t\t");
        System.out.print("DF"+"\t\t\t");
        System.out.print("IDF\n");
        for (Map.Entry<String, HashMap<String,Double>> entry : dfIdf.entrySet())
        {
            String term = entry.getKey();
            for(int i = term.length() ; i < 9 ; i++ )
            {
                term+=' ';
            }
            System.out.print(term+"\t");
            String formattedDF = String.format("%.1f", entry.getValue().get("DF"));
            System.out.print(formattedDF+"\t\t");
            String formattedIDF = String.format("%.6f", entry.getValue().get("IDF"));
            System.out.print(formattedIDF+'\n');
        }
    }

    public static void printDocLength()
    {
        for (Map.Entry<Integer,Double> entry : docLength.entrySet())
        {
            System.out.print("d"+entry.getKey()+" length\t");
            String formatted = String.format("%.6f", entry.getValue());
            System.out.print(formatted+'\n');
        }
    }
    private static void query(String QueryString) {
        // Split the query into terms
        Set<Integer> docs = new HashSet<>();
        String[] queryTerms = {};
        HashMap<String,Integer> query_tf = new HashMap<>();
        HashMap<String,Double> query_tf_weight = new HashMap<>();
        HashMap<String,Double> query_idf = new HashMap<>();
        HashMap<String,Double> query_tfidf = new HashMap<>();
        Double queryLength = 0.0;
        HashMap<String,Double> query_norm_tfidf = new HashMap<>();
        HashMap<String,HashMap<Integer,Double>> docProduct= new HashMap<>();
        HashMap<Integer,Double> docSum = new HashMap<>();
        if(QueryString.contains("and not")){
            String[] querySplitted = QueryString.split("and not");
            docs = docsForAndNot(querySplitted);
            queryTerms = querySplitted[0].split(" ");
        }
        else if(QueryString.contains("and"))
        {
            String[] querySplitted = QueryString.split("and");
             docs = docsForAnd(querySplitted);
             String andRemoved = QueryString.replaceFirst("and","");
             queryTerms = andRemoved.split(" ");
        }
        else if(QueryString.contains("or"))
        {
            String[] querySplitted = QueryString.split("or");

             HashMap<Integer,Set<Integer>>  m = docsForOr(querySplitted);
             if(m.containsKey(1))
             {
                 docs = m.get(1);
                 String[]e = {};
                 queryTerms = e;
             }
             else if(m.containsKey(2))
             {
                 docs = m.get(2);
                 queryTerms = querySplitted[1].split(" ");
             }
             else if(m.containsKey(3))
             {
                 docs = m.get(3);
                 queryTerms = querySplitted[0].split(" ");
             } else if (m.containsKey(4)) {
                 docs = m.get(4);
                 queryTerms = QueryString.replaceFirst("or","").split(" ");
             }

        }
        else {
             docs = getDocsForPhrase(QueryString.split(" "));
            queryTerms =QueryString.split(" ");
        }

       if(queryTerms.length > 0 && !docs.isEmpty())
       {
          queryTerms = Arrays.stream(queryTerms)
               .filter(term -> term != null && !term.trim().isEmpty())
               .toArray(String[]::new);
           query_tf = calculateQueryTf(queryTerms);
           query_tf_weight = calculateQueryTfWeight(query_tf);
           query_idf = calculateQueryIdf(queryTerms);
           query_tfidf = calculateQueryTfIdf(query_tf_weight,query_idf);
           queryLength = calculateQueryLength(query_tfidf);
           query_norm_tfidf = calculateQueryNormTfIdf(queryLength,query_tfidf);
           docProduct = calculateDocProduct(query_norm_tfidf,docs);
           docSum = sumDocs(docProduct);
           System.out.println("Query: ");
           printQueryTable(queryTerms,query_tf,query_tf_weight,query_idf,query_tfidf,query_norm_tfidf);
           System.out.println("===========================================");
           System.out.println("Product: ");
           printQueryDocTable(docProduct,docs,docSum);
           System.out.println("===========================================");
           printSimilarity(docSum,docs);
           printReturnedDocs(docSum);



       }
       else {
           System.out.println("Nothing found");
       }

    }

    private static Set<Integer> docsForAndNot(String[] queryList)
    {
        String[] leftPhraseTerms = queryList[0].split(" ");
        String[] rightPhraseTerms = queryList[1].split(" ");
        Set<Integer> docsForLeftPhrase = getDocsForPhrase(leftPhraseTerms);
        Set<Integer> docsForRightPhrase = getDocsForPhrase(rightPhraseTerms);
        docsForLeftPhrase.removeAll(docsForRightPhrase);
        return docsForLeftPhrase;

    }
    private static Set<Integer> docsForAnd(String[] queryList)
    {
        String[] leftPhraseTerms = queryList[0].split(" ");
        String[] rightPhraseTerms = queryList[1].split(" ");
        Set<Integer> docsForLeftPhrase = getDocsForPhrase(leftPhraseTerms);
        Set<Integer> docsForRightPhrase = getDocsForPhrase(rightPhraseTerms);
        docsForLeftPhrase.retainAll(docsForRightPhrase);
        return docsForLeftPhrase;
    }

    private static HashMap<Integer, Set<Integer>> docsForOr(String[] queryList) {

        String[] leftPhraseTerms = queryList[0].split(" ");
        String[] rightPhraseTerms = queryList[1].split(" ");


        Set<Integer> docsForLeftPhrase = getDocsForPhrase(leftPhraseTerms);
        Set<Integer> docsForRightPhrase = getDocsForPhrase(rightPhraseTerms);


        HashMap<Integer, Set<Integer>> map = new HashMap<>();

        Set<Integer> resultDocs = new HashSet<>(docsForLeftPhrase);

        if (docsForLeftPhrase.isEmpty() && docsForRightPhrase.isEmpty()) {
            resultDocs.addAll(docsForRightPhrase);
            map.put(1, resultDocs);
            return map;
        }

        if (docsForLeftPhrase.isEmpty()) {
            resultDocs.addAll(docsForRightPhrase);
            map.put(2, resultDocs);
            return map;
        }


        if (docsForRightPhrase.isEmpty()) {
            resultDocs.addAll(docsForRightPhrase);
            map.put(3, resultDocs);
            return map;
        }


        resultDocs.addAll(docsForRightPhrase);
        map.put(4, resultDocs);
        return map;
    }


    private static Set<Integer> getDocsForPhrase(String[] termList)
    {
        TreeMap<Integer,ArrayList<Integer>>positions = new TreeMap<>();

        try{

            int i = 0;
            for(String s : termList)
            {
                if(s.equals(" ") || s.equals(""))continue;
                TreeMap<Integer,ArrayList<Integer>> temp =  postingList.get(s);
                if(i ==0 )
                {
                    positions = temp;
                }
                else {
                    TreeMap<Integer,ArrayList<Integer>> to_keep = new TreeMap<>();
                    for(Map.Entry<Integer, ArrayList<Integer>> entry : temp.entrySet())
                    {
                        if(positions.containsKey(entry.getKey()))
                        {
                            for(Integer p :  positions.get(entry.getKey()))
                            {
                                for(Integer pt : temp.get(entry.getKey()))
                                {
                                    if(p+1 == pt)
                                    {
                                        to_keep.putIfAbsent(entry.getKey(),new ArrayList<>());
                                        to_keep.get(entry.getKey()).add(pt);

                                    }
                                }
                            }
                        }
                    }
                    positions = to_keep;
                }
                i++;
            }
            return positions.keySet();
        }
        catch (Exception e)
        {
            return new HashSet<Integer>();
        }

    }

    private static HashMap<String ,Integer> calculateQueryTf(String[] queryTerms)
    {
        HashMap<String ,Integer>tf = new HashMap<>();
        for(String s : queryTerms)
        {
            tf.putIfAbsent(s,0);
            Integer n = tf.get(s)+1;
            tf.put(s,n);
        }
        return tf;
    }

    private static HashMap<String ,Double> calculateQueryTfWeight( HashMap<String ,Integer>tf)
    {
        HashMap<String ,Double>tf_weight = new HashMap<>();
        for (Map.Entry<String,Integer> entry : tf.entrySet())
        {
            tf_weight.put(entry.getKey(),1+Math.log10(entry.getValue()));
        }
        return tf_weight;
    }

    private static HashMap<String ,Double> calculateQueryIdf(String[] queryTerms)
    {

        HashMap<String ,Double>idf = new HashMap<>();
        for(String s : queryTerms)
        {
            idf.put(s,dfIdf.get(s).get("IDF"));
        }
        return idf;
    }

    private  static HashMap<String,Double> calculateQueryTfIdf(HashMap<String ,Double> query_tf_weight,HashMap<String ,Double>query_idf)
    {
        HashMap<String,Double> tf_idf = new HashMap<>();
        for (Map.Entry<String,Double> entry : query_tf_weight.entrySet())
        {
            tf_idf.put(entry.getKey(), entry.getValue()*query_idf.get(entry.getKey()));
        }
        return tf_idf;
    }

    private static Double calculateQueryLength(HashMap<String ,Double> tf_idf)
    {
        Double sum = 0.0;
        for (Map.Entry<String,Double> entry : tf_idf.entrySet())
        {
            sum+=(entry.getValue()*entry.getValue());
        }
        return Math.sqrt(sum);
    }

    private static HashMap<String,Double> calculateQueryNormTfIdf(Double queryLength, HashMap<String ,Double> tf_idf)
    {
        HashMap<String ,Double> norm_tf_idf = new HashMap<>();
        for (Map.Entry<String,Double> entry : tf_idf.entrySet())
        {
            norm_tf_idf.put(entry.getKey(), entry.getValue()/queryLength);
        }
        return  norm_tf_idf;
    }

    public static HashMap<String,HashMap<Integer,Double>> calculateDocProduct(HashMap<String,Double> query_norm_tfidf,Set<Integer> docs)
    {
        HashMap<String,HashMap<Integer,Double>> prod = new HashMap<>();
        for (Map.Entry<String,Double> entry : query_norm_tfidf.entrySet())
        {
            String term = entry.getKey();
            HashMap<Integer,Double> norms = new HashMap<>();
            for(Integer doc : docs)
            {
                norms.put(doc,entry.getValue()*tfIdfNormalized.get(entry.getKey()).get(doc));
            }
            prod.put(term,norms);
        }

       return prod;
    }
    private static void printQueryTable(String[]queryTerms,HashMap<String,Integer>query_tf,HashMap<String,Double>query_tf_weight,HashMap<String,Double>query_idf,HashMap<String,Double>query_tfidf,HashMap<String,Double>query_norm_tfidf)
    {
        System.out.print("\t\t\t");
        System.out.print("tf\t\t\t");
        System.out.print("tf_weight\t\t\t");
        System.out.print("idf\t\t\t\t\t");
        System.out.print("tf*idf\t\t\t\t");
        System.out.print("normalized\t\t\t");
        System.out.print("\n");
        for (Map.Entry<String,Integer> entry : query_tf.entrySet())
        {
            String term = entry.getKey();
            for(int i = term.length() ; i < 9 ; i++ )
            {
                term+=' ';
            }
            System.out.print(term+"\t");
            System.out.print(query_tf.get(entry.getKey())+"\t\t\t");

            String formatted_w = String.format("%.6f",query_tf_weight.get(entry.getKey()));
            System.out.print(formatted_w+"\t\t\t");
            String formatted_i = String.format("%.6f",query_idf.get(entry.getKey()));
            System.out.print(formatted_i+"\t\t\t");
            String formatted_ti = String.format("%.6f",query_tfidf.get(entry.getKey()));
            System.out.print(formatted_ti+"\t\t\t");
            String formatted_n = String.format("%.6f",query_norm_tfidf.get(entry.getKey()));
            System.out.print(formatted_n+"\t\t\t");

            System.out.print("\n");

        }
    }

    private static void printQueryDocTable(HashMap<String,HashMap<Integer,Double>> prod,Set<Integer>docs,HashMap<Integer,Double>docSum)
    {
        System.out.print("\t\t\t\t");
        for (Integer docId: docs)
        {

            System.out.print("d"+docId+"\t\t\t\t\t");

        }
        System.out.print("\n");
        for (Map.Entry<String,HashMap<Integer,Double>> entry : prod.entrySet())
        {
            String term = entry.getKey();
            for(int i = term.length() ; i < 9 ; i++ )
            {
                term+=' ';
            }
            System.out.print(term+"\t");
            for (Integer docId: docs)
            {

                String formatted_w = String.format("%.6f",entry.getValue().get(docId));
                System.out.print(formatted_w+"\t\t\t");

            }
            System.out.println();

        }
        System.out.println();
        System.out.print("Sum :       ");
        for (Integer docId: docs)
        {

            String formatted_w = String.format("%.6f",docSum.get(docId));
            System.out.print(formatted_w+"\t\t\t");
        }
        System.out.println();


    }

    public static HashMap<Integer,Double> sumDocs (HashMap<String,HashMap<Integer,Double>> prod)
    {
        HashMap<Integer,Double> sum  = new HashMap<>();
        for (Map.Entry<String,HashMap<Integer,Double>> entry : prod.entrySet())
        {
            for (Map.Entry<Integer,Double> entry1 : entry.getValue().entrySet())
            {
                sum.putIfAbsent(entry1.getKey(),0.0);
                sum.put(entry1.getKey(),sum.get(entry1.getKey())+entry1.getValue());
            }
        }
        return sum;
    }

    public static void printSimilarity(HashMap<Integer,Double>docSum,Set<Integer>docs)
    {
        for (Integer docId: docs)
        {

            String formatted_w = String.format("%.6f",docSum.get(docId));
            System.out.println("similarity(q, doc"+docId+")   "+formatted_w);
        }
        System.out.println();
    }

    public static void printReturnedDocs(HashMap<Integer,Double>docSum)
    {
        List<Entry<Integer, Double>> list = new ArrayList<>(docSum.entrySet());
        list.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));
        LinkedHashMap<Integer, Double> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<Integer, Double> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        System.out.print("Returned docs :  ");
        for(Map.Entry<Integer, Double> entry : sortedMap.entrySet())
        {
            System.out.print("d"+entry.getKey()+",");
        }
        System.out.println();
    }




}