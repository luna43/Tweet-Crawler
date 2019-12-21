import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class linkcrawler {

    public static HashMap<String,Double> inLinkMap = new HashMap<String,Double>();
    public static HashMap<String,Double> outLinkMap = new HashMap<String,Double>();
    public static HashMap<String,Double> depthLinkMap = new HashMap<String,Double>();
    public static HashMap<String,Double> linkTypeMap = new HashMap<String,Double>();

    /**
     * this manually extracts link type from the end of the input string and puts it into my map
     *
     * NOTE I DID NOT USE THIS FUNCTION BECAUSE THE END OF THE URL IS JUST A GUESS, need to
     * connect to get actual content type
     * @param url
     */
    public static void getLinkType(String url){
        int len = url.length() - 1;
        int begin = len-5;
        String type = "";
        boolean found = false;
        for (int i = len; i>=begin; i--){
            char c = url.charAt(i);
            if(c=='.'){
                found = true;
                break;

            }
            //add to front of string
            type = c + type;
        }
        if (found){
            if(!linkTypeMap.containsKey(type)){
                linkTypeMap.put(type,1.0);
            } else {
                double freq = linkTypeMap.get(type);
                freq++;
                linkTypeMap.put(type,freq);
            }
        }
    }

    /**
     * this function extracts domains from a hashset of urls
     * @param urls
     * @return
     */
    public static HashMap<String, Double> getDomains(HashSet<String> urls){
        HashMap<String,Double> domainMap = new HashMap<String,Double>();
        for (String url : urls){
            if(url.contains("bit.ly")){
                continue;
            }
            if(url.contains("ow.ly")){
                continue;
            }
            String domain = "";
            int index = url.indexOf("//");
            if (index+2 < url.length()-1){
                index +=2;
            }

            while (url.charAt(index)!='/'){
                domain += url.charAt(index);
                index++;
                if (index==url.length()-1){
                    break;
                }
            }
            //now domain is set and needs to be trimmed
            if (domain.startsWith("www.")){
                domain=domain.substring(4);
            }

            //now add domain to map
            if (!domainMap.containsKey(domain)){
                domainMap.put(domain,1.0);
            } else {
                double count = domainMap.get(domain);
                count++;
                domainMap.put(domain,count);
            }
        }

        return domainMap;
    }

    /**
     * this function outputs a file with the top 50 most frequent domains, and their frequency
     * @param domains
     * @throws IOException
     */
    public static void outputDomainInfo(HashMap<String,Double> domains) throws IOException {
        String pathname = "domaininfo.xls";
        File file = new File(pathname);
        FileWriter writer = new FileWriter(file);
        LinkedHashMap<String, Double> sortedMap = new LinkedHashMap<>();
        //now i sort my map
        domains.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));
        int i = 0;
        for (String domain : sortedMap.keySet()){
            i++;
            if (i>50){
                break;
            }
            writer.write(domain + "\t");
            writer.write(sortedMap.get(domain) + "\n");
        }
        writer.close();
    }

    /**
     * this function outputs a file with the top 50 most frequent link types, and their frequency
     * @param suffixes
     * @throws IOException
     */
    public static void outputLinkTypeMap(HashMap<String,Double> suffixes) throws IOException {
        String pathname = "linktypes.txt";
        File file = new File(pathname);
        FileWriter writer = new FileWriter(file);
        LinkedHashMap<String, Double> sortedMap = new LinkedHashMap<>();
        //now i sort my map
        suffixes.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));
        int i = 0;
        for (String domain : sortedMap.keySet()){
            i++;
            if (i>50){
                break;
            }
            writer.write(domain + "\t");
            writer.write(sortedMap.get(domain) + "\n");
        }
        writer.close();
    }

    /**
     * This function recursively crawls links
     * @param url
     * @param depth
     * @return
     */
    public static double crawl(String url,double depth){
        if(inLinkMap.size() > 20000){
            return depth;
        }
        if(!depthLinkMap.containsKey(url)){
            depthLinkMap.put(url,depth);
        }
        if (outLinkMap.containsKey(url)){
            return depth;
        }
        if (depth>=5){
            return depth;
        }
        //i dont want to collect more twitter links because they dominate my results
        if (url.contains("twitter.com")){
            return depth;
        }
        Double outLinkCount = 0.0;

        //connect to HTTP and get file type
        Connection.Response response = null;
        Document doc = null;
        try {
            response = Jsoup.connect(url).timeout(10000).execute();
            doc = response.parse();
        } catch (IOException e) {
            return depth;
        }
        String type = response.contentType();
        if (type!=null){
            if(!linkTypeMap.containsKey(type)){
                linkTypeMap.put(type,1.0);
            } else {
                double freq = linkTypeMap.get(type);
                freq++;
                linkTypeMap.put(type,freq);
            }
        }
        //get contents of webpage
        if (doc==null){
            return depth;
        }

        Elements links = doc.select("a");
        if (links==null){
            return depth;
        }

        //add links to my two maps
        for (Element link : links) {
            outLinkCount++;
            String newlink = link.attr("href");
            if (!newlink.startsWith("https:")){
                newlink = url + newlink;
            }
            //update inlinkmap
            if(!inLinkMap.containsKey(newlink)){
                inLinkMap.put(newlink,1.0);
            } else {
                double freq = inLinkMap.get(newlink);
                freq++;
                inLinkMap.put(newlink, freq);
            }
        }
        outLinkMap.put(url,outLinkCount);
        //recursive call on links
        for (Element link:links) {
            String newlink = link.attr("href");
            if (!newlink.startsWith("https:")){
                newlink = url + newlink;
            }
            if (newlink.substring(0, 9).equals(url.substring(0, 9))){
                continue;
            }
            depth++;
            if (depth>=5){
                return depth;
            }

            depth = crawl(newlink,depth);

        }
        return depth;
    }

    /**
     * this function dispatches my recursive crawl function
     * @param urls
     */
    public static void getLinks(HashSet<String> urls) {
        for (String url: urls){
            crawl(url,0);
            if(inLinkMap.size() > 20000){
                return;
            }
        }
        System.out.println("Finished Seed URLS");
    }

    /**
     * this function generates a text file with some stats about my results
     */
    public static void makeSTAT() throws IOException {
        HashSet<String> domainurls = new HashSet<String>();
        String pathname = "results.txt";
        File file = new File(pathname);
        FileWriter writer = new FileWriter(file);


        System.out.println("size of inLinkMap: " + inLinkMap.size());
        writer.write("Unique Links Encountered: " + inLinkMap.size() + "\n");
        writer.write("Unique Links Traversed: " + depthLinkMap.size() + "\n");
        System.out.println("size of outLinkMap: " + outLinkMap.size());
        System.out.println("size of depthLinkMap: " + depthLinkMap.size());

        //calc average depth
        double sumdepth = 0.0;
        for (String url: depthLinkMap.keySet()){
            sumdepth = sumdepth + depthLinkMap.get(url);
        }
        double averagedepth = sumdepth / depthLinkMap.size();
        System.out.println("Average Depth: " + averagedepth);
        writer.write("Average Depth: " + averagedepth + "\n");

        //output InLink top25
        LinkedHashMap<String, Double> sortedInMap = new LinkedHashMap<>();
        inLinkMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> sortedInMap.put(x.getKey(), x.getValue()));
        int x =0;
        for (String url : sortedInMap.keySet()){
            x++;
            double linkcount = sortedInMap.get(url);
            writer.write("InLink Count: " + linkcount + "\n");
            writer.write(url + "\n");
            if (x==25){
                break;
            }

        }
        writer.write("\n \n \n");

        //get domains
        domainurls.addAll(sortedInMap.keySet());


        //output OutLink top25
        LinkedHashMap<String, Double> sortedOutMap = new LinkedHashMap<>();
        outLinkMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(y -> sortedOutMap.put(y.getKey(), y.getValue()));
        int y =0;
        for (String url : sortedOutMap.keySet()){
            y++;
            if(y==25){
                break;
            }
            double linkcount = sortedOutMap.get(url);
            writer.write("OutLink Count: " + linkcount + "\n");
            writer.write(url + "\n");
        }
        HashMap<String, Double> domains = getDomains(domainurls);
        //make a file with info about the domains
        outputDomainInfo(domains);

        //make file with link type
        outputLinkTypeMap(linkTypeMap);

        writer.close();

    }


    public static void main(String[] args) throws IOException {
        String pathname = "URLS001.txt";
        //get URLS into a HashSet
        HashSet<String> urls = twitter.checkTXT(pathname);
        System.out.println("Number of seed URLS: " + urls.size());

        //crawl my links for more links
        getLinks(urls);

        //otuput results
        makeSTAT();








    }

}
