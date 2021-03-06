/**
 * $Id$ 
 *
 * $LastChangedDate$ 
 * 
 * $LastChangedBy$
 */


import java.util.logging.Logger;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import StateMachine.Parser1;
import StateMachine.StateMachineBuilder;
import snaq.db.ConnectionPoolManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.text.html.HTMLEditorKit.Parser;
import data.*;


public class Indexer extends Thread
{
    private static Logger log = Logger.getLogger (Indexer.class.getName());
    private Options __opt;
    private CommandLine __cl;
    private int __docBatchSize = 100;
    private ConnectionPoolManager __cpm = null;
    private boolean __shutdown = false;
    private Thread __main;


    private Indexer ()
    {
        __opt = new Options(); 
        __opt.addOption("h", false, "Print help");
        __opt.addOption("c", true, "document batch size (default " + __docBatchSize + ")");
    }

     
    public static void main (String[] args)
    {
        new Indexer().execute (args);
    }

    
    private void printUsage (String message, int rc)
    {
        HelpFormatter hf = new HelpFormatter();
        hf.printHelp (message, __opt);
        System.exit (rc);
    }

    
    private void execute (String[] args)
    {
        //__main = Thread.currentThread();
        //Runtime.getRuntime().addShutdownHook (this);
        try
        {        
            __cl = (new BasicParser()).parse (__opt, args); 
            if ( __cl.hasOption ('h') ) printUsage ("help", 0);
            if ( __cl.hasOption ('c') ) __docBatchSize = Integer.parseInt (__cl.getOptionValue ('c'));
        }
        catch (ParseException ex)
        {
            printUsage (ex.getMessage(), 1);
            System.exit (1);
        }
        
        // Load keywords
        log.info ("Loading keywords");
        List<Keyword> keywords = KeywordDAO.getInstance().getKeywords();

        // Build state machine
         StateMachineBuilder sm=new StateMachineBuilder();
         sm.createStateMachine(keywords);
        
                
        // Index documents
        log.info ("Indexer starting. Document batch size set to " + __docBatchSize);
        __main = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook (this);
        while ( ! shutdown() )
        //while(true)
        {
            List<Document> documents = DocumentDAO.getInstance().getDocuments (__docBatchSize);
            log.info ("Retrieved " + documents.size() + " unprocessed documents.");

           
            for (Document document : documents)
            {
            	log.info (document.toString());
            	// update indexx for a given document
                IndexDAO.getInstance().UpdateIndex(Parser1.parse(document));
            }

            // Create index - just sleep for now
            //log.info ("Indexing documents");
            try
            {
            	
                Thread.currentThread().sleep (5000);
            }
            catch (InterruptedException ex)
            {
                log.severe ("Interrupted sleep!");
            }
        }

        log.info ("Indexer shutting down");
        try
        {
            ConnectionPoolManager.getInstance().release ();
        }
        catch (IOException ex)
        {
            log.severe ("Exception releasing connection pool manager: " + ex.getMessage());
        }
    }


    /**
     * Shutdown hook
     */
    public void run ()
    {
        log.info ("Running shutdown hook");
        shutdownNotify ();
        try 
        {
            __main.join ();
        }
        catch (InterruptedException ex)
        {
            log.info ("Interrupted running shutdown hook.");
        }
    }

    
    private synchronized boolean shutdown ()
    {
        return __shutdown;
    }

    
    private synchronized void shutdownNotify ()
    {
        __shutdown = true;
    }
}




