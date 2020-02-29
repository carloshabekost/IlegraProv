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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
                        
                        registerPath( path );
                    } 
                    else if ( kind == StandardWatchEventKinds.ENTRY_MODIFY ) 
                    {
                        // modified
                        Path newPath = ((WatchEvent<Path>) watchEvent).context();
                        // Output
                        System.out.println( "New path modified: " + newPath );
                        
                        Path child = path.resolve( newPath );
                        
                        registerPath( path );
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
            
        //Hash for saving the total of sales, by Salesman
        HashMap<String,Float> totalSa = new HashMap<>();
        
        //Save all cpfs and cnpj for avoid duplicates.
        ArrayList<String> cpfs = new ArrayList<>();
        ArrayList<String> cnpj = new ArrayList<>();
        
        //Save ID of the most expensive sale
        float maxSale = -1;
        String idSale = "";
        
        File dir = new File( path.toUri() );        
        File[] files = dir.listFiles();
        
        //Get all files from diretory "in"
        for ( File file : files )
        {
            BufferedReader buffRead = new BufferedReader( new FileReader( file ) );
            
            if ( buffRead.ready() )
            {
                String line = buffRead.readLine();

                while( line != null )
                {
                    String[] content = line.split( "ç" );

                    if ( line.startsWith( "001" ) ) //Salesmen
                    {
                        if ( !cpfs.contains( content[1] ) )  //Position 1 has the CPF
                        {
                            cpfs.add( content[1] );
                        }
                    }
                    else if ( line.startsWith( "002" ) ) //Costumer
                    {
                        if ( !cnpj.contains( content[1] ) )  //Position 1 has the CNPJ
                        {
                            cnpj.add( content[1] );
                        }
                    }
                    else if ( line.startsWith( "003" ) ) //Sale
                    {        
                        String name = content[3];
                        float current = 0;                        

                        //Verify if there is another sale for the salesman....
                        //If exists, then upadate de total. 
                        //
                        float sub = ( totalSa.get( name ) == null ) ? 0 : totalSa.get( name );                    

                        //Get a list of sales, removing "[]" caracters and split by ","
                        String[] sales = content[2].replace( "[", "" ).replace( "]", "").split( "," ); 

                        //Sum of total sales
                        //Also sum the total of current sale;
                        for ( String sale : sales )
                        {
                            String[] item = sale.split( "-" );

                            sub += Float.valueOf( item[2] );
                            current += Float.valueOf( item[2] );
                        }
                        
                        //Update or put the name of salesman with the total of sales
                        if ( totalSa.containsKey( name ) )
                            totalSa.replace( name, sub );
                        else
                            totalSa.put( name, sub );
                        
                        //Verify the most expensive sale
                        if ( maxSale < current )
                        {
                            maxSale = current;                        
                            idSale = content[1]; //1 = SALE ID
                        }
                    }
                
                    line = buffRead.readLine(); //Read next line
                }            

                buffRead.close(); //Close the file            
            }                   
                     
        }
        
        //After list all files, we build the report content

        float minSale = -1;
        String name = "";
        
        //Get the worst salesman
        for ( String key : totalSa.keySet() )
        {
            //Gets the total of sales of a salesman
            float current = totalSa.get( key );
            
            //Verifies if the current sale is less than min registred
            if ( minSale == -1 || minSale > current )
            {
                minSale = current;
                name = key;
            }             
        }
        
        DateFormat dateFormat = new SimpleDateFormat( "dd/MM/yyyy HH:mm:ss" );
        
        //First, we prepare content for report
        //String lenght is 4:
        //1: Amount of costumers; 
        //2: Amount of Salemen; 
        //3: ID most expensive sale
        //4: The worst saleman   
        String[] report = new String[7];
        report[0] = "Relátorio de Vendas Loja XXXXX";
        report[1] = "Data/Hora da Verificação: " + dateFormat.format( new Date() );
        report[2] = "Total de Arquivos Verificados: " + dir.listFiles().length;
        report[3] = "Quantidade de clientes: " + cnpj.size();
        report[4] = "Quantidade de vendedores: " + cpfs.size();
        report[5] = "ID da Venda Mais cara: " + idSale + " Valor: " + maxSale;
        report[6] = "Pior vendedor: " + name + ". Valor Total de Vendas: " + minSale;
            
        updateReport( report );
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
