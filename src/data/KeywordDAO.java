/**
 * $Id$ 
 *
 * $LastChangedDate$ 
 * 
 * $LastChangedBy$
 */
package data;

import java.io.IOException;
import java.util.logging.Logger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import snaq.db.ConnectionPoolManager;


public class KeywordDAO
{
    private static Logger log = Logger.getLogger (KeywordDAO.class.getName());
    private static KeywordDAO __instance = new KeywordDAO (); //singleton instance
    private ConnectionPoolManager __cpm = null;
    private String __connectionPoolName = "mysql";

    // Cache: map keywordID=>keyword
    private HashMap<Integer, Keyword> __keywords = new HashMap<Integer, Keyword>();
    // Cache: map synonym=>keyword
    private HashMap<String, String> __synonyms = new HashMap<String, String>();


    private KeywordDAO ()
    {
        try
        {
            __cpm = ConnectionPoolManager.getInstance ();
            loadCache ();
        }
        catch (IOException ex)
        {
            log.severe ("Exception creating connection pool: " + ex.toString());
        }
    }

    
    private void loadCache ()
    {
        log.info ("Loading keyword cache");
        try 
        {
            Connection con = __cpm.getConnection (__connectionPoolName);
            ResultSet rs = con.createStatement().executeQuery
                ("SELECT canon, synonym FROM Keyword");
            
            while (rs.next()) 
            {
                String canon = rs.getString("canon");
                String synonym = rs.getString("synonym");
                Keyword keyword = __keywords.get (canon.hashCode());

                if (keyword == null)
                {
                    keyword = new Keyword (canon);
                    keyword.addSynonym (canon);  
                    __synonyms.put (canon, canon);
                    __keywords.put (keyword.getId(), keyword);
                }

                keyword.addSynonym (synonym);
                __synonyms.put (synonym, canon);
            }
            
            con.close();
        } 
        catch(SQLException ex) 
        {
            log.severe ("Exception retrieving keywords: " + ex.toString());
        } 
    }
    
    
    public static KeywordDAO getInstance ()
    {
        return __instance;
    }

    
    /**
     * Return the canonical form of a keyword
     */
    public String canonize (String synonym)
    {
        return __synonyms.get (synonym);
    } 

    
    /**
     * Return a set of all keywords.
     */
    public List<Keyword> getKeywords ()
    {
        return new ArrayList (__keywords.values());
    }


    public Keyword getKeywordById (int id)
    {
        return __keywords.get (id);
    }
}
