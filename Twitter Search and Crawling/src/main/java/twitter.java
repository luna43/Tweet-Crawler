import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class twitter {
    /*
    I need this tweets array to store my tweets
     */
    public static List<Status> tweets = new ArrayList<Status>();

    /**
     * does initiation stuff
     * @return twitter instance that is authenticated
     */
    public static Twitter startup(){
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey("j5adCXSO6PzKZUgbxLiARd6ps")
                .setOAuthConsumerSecret("fvehnR6BvG56htyhvKiq1KRcWb2bJSJj5zBqaezbzgz4bsn6ml")
                .setOAuthAccessToken("1196974064390172673-hJ074sK50dVts64VxMva4oMKD8DBOY")
                .setOAuthAccessTokenSecret("sXSScdqQkjjDFlsFZCvYe70ObCS2Wdiob0XRAlzJdgbLT");
        TwitterFactory tf = new TwitterFactory(cb.build());
        return tf.getInstance();
    }

    /**
     * this function gets tweets from twitter, and returns them in an array
     * @param t is an instance of twitter that im doing shit on
     * @param term is the term your looking for
     * @return
     */
    public static HashSet<String> getTweets(HashSet<String> urlMap, Twitter t, String term)
    {
        //toggle this to change how many tweets to search at once, it will collect links even if it hits a rate limit
        int wantedTweets = 1000;

        long lastSearchID = Long.MAX_VALUE;
        int remainingTweets = wantedTweets;
        Query query = new Query(term);

        try {
            while(remainingTweets > 0) {
                remainingTweets = wantedTweets - tweets.size();
                if(remainingTweets > 100) {
                    query.count(100);
                }
                else {
                    query.count(remainingTweets);
                }
                QueryResult result = t.search(query);
                tweets.addAll(result.getTweets());
                Status s = tweets.get(tweets.size()-1);
                long firstQueryID = s.getId();
                query.setMaxId(firstQueryID);
                remainingTweets = wantedTweets - tweets.size();
            }
            System.out.println("Number of tweets requested: " + wantedTweets);
            System.out.println("Number of tweets found: "+tweets.size() );
        }
        catch(TwitterException te)
        {
            System.out.println("Failed to search tweets: " + te.getMessage());
            System.exit(-1);
        }
        //add to urlMap
        for (Status s : tweets){
            String text = s.getText();
            System.out.println(text);
            URLEntity[] test = s.getURLEntities();
            for (URLEntity entity: test){
                String thisURL = entity.getExpandedURL();
                if (!urlMap.contains(thisURL)){
                    urlMap.add(thisURL);
                }

            }
        }
        return urlMap;
    }

    /**
     * This function checks my text file for URLS and loads them into a Hashset
     * @param pathname path of the rtext file
     * @return a hashset of strings of all my urls
     * @throws IOException
     */
    public static HashSet<String> checkTXT(String pathname) throws IOException {
        List<String> urls = Files.readAllLines(Paths.get(pathname), StandardCharsets.UTF_8);
        HashSet<String> urlMap = new HashSet<String>();
        for (String url : urls){
            if (!urlMap.contains(url)) {
                urlMap.add(url);
            }
            File file = new File(pathname);
            FileWriter writer = new FileWriter(file);
            for (String anURL : urlMap) {
                writer.write(anURL + "\n");
            }
        }
        int removed = urls.size() - urlMap.size();
        System.out.println("Duplicate URLs Removed " + removed);
        return urlMap ;
    }

    /**
     * main function, searches the query and then adds to a text file unique URLS.
     * note that I do this in increments because theres a paywall on the limit of queries I can do per 15 min
     * @param args
     * @throws TwitterException
     * @throws IOException
     */
    public static void main(String[] args) throws TwitterException, IOException {
        //setup
        Twitter twitter = startup();
        String pathname = "URLS001.txt";
        File file = new File(pathname);
        FileWriter writer = new FileWriter(file, true);

        //load into memory what i have so far
        HashSet<String> urls = checkTXT(pathname);
        int  size = urls.size();

        //now harvest more
        getTweets(urls, twitter, "toffee");

        //print stats
        System.out.println("Number of URLS total: " + urls.size());
        for (String url : urls) {
            writer.write(url + "\n");
        }

        //cleanup
        writer.close();
    }

}
