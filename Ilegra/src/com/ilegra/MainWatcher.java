/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ilegra;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;

/**
 *
 * @author cfsan
 */
public class MainWatcher 
{
    //Pathes used in this class...
    private static final String PATH_IN = "C:\\data\\in";
    private static final String PATH_OUT = "C:\\data\\out";
    
    /**
     * 
     * @param args 
     */
    public static void main( String[] args )
    {
        // Folder to watch
        File dir = new File( PATH_IN );
        
        watchDirectoryPath( dir.toPath() );
    }
           
    /**
     * 
     * @param path 
     */
    public static void watchDirectoryPath( Path path ) 
    {
        //Check if path is a folder
        try
        {
            Boolean isFolder = (Boolean) Files.getAttribute( path, 
                                                             "basic:isDirectory", 
                                                             LinkOption.NOFOLLOW_LINKS );
            
            if ( !isFolder ) 
            {
                //throw new IllegalArgumentException( "Path: " + path + " is not a folder" );
                System.err.println( "Path: " + path + " is not a folder" );
                
                return;
            }
        } 
        catch ( IOException ioe ) 
        {
            System.out.println( "Folder does not exists" );
            
            //ioe.printStackTrace();
            System.err.println( ioe );
            
            return;
        }

        System.out.println( "Watching path: " + path );

        // We obtain the file system of the Path
        FileSystem fs = path.getFileSystem();

        // We create the new WatchService using the new try() block
        try ( WatchService service = fs.newWatchService() ) 
        {
            // We register the path to the service
            // We watch for creation events
            path.register(service, 
                          StandardWatchEventKinds.ENTRY_CREATE, 
                          StandardWatchEventKinds.ENTRY_MODIFY, 
                          StandardWatchEventKinds.ENTRY_DELETE ); 

            // Start the infinite polling loop
            //WatchKey key = null;
            
            while ( true ) 
            {
                WatchKey key = service.take();

                // Dequeueing events
                //Kind<?> kind = null;
                for ( WatchEvent<?> watchEvent : key.pollEvents() ) 
                {
                    // Get the type of the event
                    WatchEvent.Kind<?> kind = watchEvent.kind();
                    
//                    if ( StandardWatchEventKinds.OVERFLOW == kind ) 
//                    {
//                        continue; // loop
//                        
//                    } 
                    if ( kind == StandardWatchEventKinds.ENTRY_CREATE ) 
                    {
                        // A new Path was created
                        Path newPath = ((WatchEvent<Path>) watchEvent).context();
                        // Output
                        System.out.println( "New path created: " + newPath );
                        
                        registerPath( newPath );
                    } 
                    else if ( kind == StandardWatchEventKinds.ENTRY_MODIFY ) 
                    {
                        // modified
                        Path newPath = ((WatchEvent<Path>) watchEvent).context();
                        // Output
                        System.out.println( "New path modified: " + newPath );
                        
                        Path child = path.resolve( newPath );
                        
                        registerPath( child );
                    }
                    else if ( kind == StandardWatchEventKinds.ENTRY_DELETE ) 
                    {
                        // Deleted
                        Path newPath = ((WatchEvent<Path>) watchEvent).context();
                        // Output
                        System.out.println( "New path deleted: " + newPath );
                    }
                }

                if ( !key.reset() )
                {
                    break; // loop
                }
            }

        } 
        catch ( IOException | InterruptedException ioe ) 
        {
            ioe.printStackTrace();
        }

    }

    /**
     * 
     * @param path
     * @throws IOException 
     */
    private static void registerPath( Path path ) throws IOException
    {
        //Stream<String> lines = Files.lines( path );  
        //String data = lines.collect( Collectors.joining("\n") );
        //String[] datas = data.split( "\n" );
        
        Path outp = Paths.get( PATH_OUT );
                
        //Verify output folder
        try
        {
            Boolean isFolder = (Boolean) Files.getAttribute( outp, 
                                                             "basic:isDirectory", 
                                                             LinkOption.NOFOLLOW_LINKS );
            
            if ( !isFolder ) 
            {
                System.err.println( "Path: " + path + " is not a folder" );
                
                return;
            }
        } 
        catch ( IOException ioe ) 
        {
            System.out.println( "Folder does not exists" );
            System.err.println( ioe );
            
            return;
        }
                
        BufferedReader buffRead = new BufferedReader( new FileReader( path.toString() ) );
        
        if ( buffRead.ready() )
        {
            String line = buffRead.readLine();
            
            //Accountant for Salesman and customer
            int countCustumer = 0;
            int countSalesman = 0;
            
            float maxSale = -1;
            String idSale = "";
            
            float minSale = -1;
            String name = "";
            
            //Hash for saving the total of sales, by Salesman
            HashMap<String,Float> totalSa = new HashMap<>();
            
            while( line != null )
            {
                String[] content = line.split( "ç" );
                
                if ( line.startsWith( "001" ) ) //Salesmen
                {
                    countSalesman++;
                }
                else if ( line.startsWith( "002" ) ) //Costumer
                {
                    countCustumer++;
                }
                else if ( line.startsWith( "003" ) ) //Sale
                {                    
                    //Get a list of sales, removing "[]" caracters and split by ","
                    String[] sales = content[2].replace( "[", "" ).replace( "]", "").split( "," ); 
                    
                    float sub = 0;
                    
                    for ( String sale : sales )
                    {
                        String[] item = sale.split( "-" );
                        
                        sub += Float.valueOf( item[2] );
                    }
                    
                    //Verify the most expensive sale
                    if ( maxSale < sub )
                    {
                        maxSale = sub;                        
                        idSale = content[1]; //1 - SALE ID
                    }
                    else                    
                    {
                        //Verify the worst salesman
                        if ( minSale == -1 || minSale > sub )
                        {
                            minSale = sub;
                            name = content[3]; //1 - Salasman name
                        }
                    }
                    
                }
                
                line = buffRead.readLine();
            }
            
            buffRead.close(); //Close file from in
            
            //Prepare content for report
            //String lenght is 4:
            //1: Amount of costumers; 
            //2: Amount of Salemen; 
            //3: ID most expensive sale
            //4: The worst saleman
            String[] report = new String[4]; 
            report[0] = "Amount of Costumers: " + countCustumer;
            report[1] = "Amount of Salesmen: " + countSalesman;
            report[2] = "ID the Most Expensive Sale: " + idSale + " Value: " + maxSale;
            report[3] = "Name of the worst salesman: " + name + ". Value of Sale: " + minSale;
            
            updateReport( report );
        }
       
    }

    public static boolean updateReport( String[] content )
    {
        boolean updated = false;
        
        try 
        {
            //Verify if Report file exists            
            File file = new File( PATH_OUT + "\\Report.txt" );
            
            if ( !file.exists() ) //If does not exists, than creat the file...
            {
                new File( PATH_OUT + "\\Report.txt" ).createNewFile();
            }
            else //TODO: Get the current data....
            {
                
            }
            
            BufferedWriter buffFile = new BufferedWriter( new FileWriter( PATH_OUT + "\\Report.txt" ) );
            
            for ( String string : content ) 
            {            
                buffFile.append( string + "\n" );           
            }
            
            buffFile.flush();
            buffFile.close();            
        } 
        catch ( FileNotFoundException ex ) 
        {
            ex.printStackTrace();
        }
        catch ( IOException ex ) 
        {
            ex.printStackTrace();
        }
        
        return updated;
    }
    
    
}
