package util.game;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import util.configuration.RemoteResourceLoader;
import util.gdl.grammar.Gdl;
import util.kif.KifReader;
import external.JSON.JSONArray;
import external.JSON.JSONException;
import external.JSON.JSONObject;

/**
 * Remote game repositories provide access to game resources stored on game
 * repository servers on the web. These require a network connection to work.
 * 
 * @author Sam
 */
public final class RemoteGameRepository extends GameRepository {
    private final String theRepoURL;
    
    
    
    public RemoteGameRepository(String theURL) {
        theRepoURL = properlyFormatURL(theURL);
    }
    
    protected Set<String> getUncachedGameKeys() {
        try {
            Set<String> theGameKeys = new HashSet<String>();
            JSONArray theArray = RemoteResourceLoader.loadJSONArray(theRepoURL + "/games/");
            
            for(int i = 0; i < theArray.length(); i++) {
                theGameKeys.add(theArray.getString(i));
            }
            
            return theGameKeys;
        } catch (Exception e) {
        	System.err.println("Getting uncached game keys from repository failed");
            return new HashSet<String>();
        }
    }
    
    protected Game getUncachedGame(String theKey) {
        return loadSingleGame(getGameURL(theKey));
    }
    
    public static Game loadSingleGame(String theGameURL) {
        String[] theSplitURL = theGameURL.split("/");
        String theKey = theSplitURL[theSplitURL.length-1];

        try {
            JSONObject theMetadata = getGameMetadataFromRepository(theGameURL);
            return loadSingleGameFromMetadata(theKey, theGameURL, theMetadata);            
        } catch(IOException e) {
        	System.err.println("Loading game failed: " + theGameURL);
            return null;
        }
    }
    
    protected static Game loadSingleGameFromMetadata(String theKey, String theGameURL, JSONObject theMetadata) {
        // Ensure that the game URL has a version.
        try {
            int theVersion = theMetadata.getInt("version");
            if (!isVersioned(theGameURL, theVersion)) {
              theGameURL = addVersionToGameURL(theGameURL, theVersion);
            }
        } catch(JSONException e) {
        	System.err.println("Loading game from metadata failed: " + theGameURL);
            return null;
        }
        
        String theName = null;
        try {
            theName = theMetadata.getString("gameName");
        } catch(JSONException e) {}
        
        String theDescription = getGameResourceFromMetadata(theGameURL, theMetadata, "description");                
        String theStylesheet = getGameResourceFromMetadata(theGameURL, theMetadata, "stylesheet");
        List<Gdl> theRules = getGameRulesheetFromMetadata(theGameURL, theMetadata);
        
        if (theRules.size() == 0) return null;
        return new Game(theKey, theName, theDescription, theGameURL, theStylesheet, theRules);        
    }
    
    public JSONObject getBundledMetadata() {
        try {
            return RemoteResourceLoader.loadJSON(theRepoURL + "/games/metadata");
        } catch (IOException e) {
            return null;
        }
    }
    
    // ============================================================================================
    protected String getGameURL(String theGameKey) {
        return theRepoURL + "/games/" + theGameKey + "/";
    }
    
    protected static String addVersionToGameURL(String theGameURL, int theVersion) {
        return theGameURL + "v" + theVersion + "/";
    }
    
    protected static boolean isVersioned(String theGameURL, int theVersion) {
        return theGameURL.endsWith("/v" + theVersion + "/");
    }

    protected static JSONObject getGameMetadataFromRepository(String theGameURL) throws IOException {
        return RemoteResourceLoader.loadJSON(theGameURL);
    }
    
    protected static String getGameResourceFromMetadata(String theGameURL, JSONObject theMetadata, String theResource) {
        try {
            String theResourceFile = theMetadata.getString(theResource);
            return RemoteResourceLoader.loadRaw(theGameURL + theResourceFile);
        } catch (Exception e) {
            return null;
        }
    } 
        
    protected static List<Gdl> getGameRulesheetFromMetadata(String theGameURL, JSONObject theMetadata) {
        try {
            String theRulesheetFile = theMetadata.getString("rulesheet");
            return KifReader.readURL(theGameURL + theRulesheetFile);
        } catch (Exception e) {
            return null;
        }
    }
    
    public static String properlyFormatURL(String theURL) {
        if (!theURL.startsWith("http://"))
            theURL = "http://" + theURL;
        if (theURL.endsWith("/"))
            theURL = theURL.substring(0, theURL.length()-1);
        return theURL;
    }
}