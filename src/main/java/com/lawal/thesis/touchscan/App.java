package com.lawal.thesis.touchscan;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Created by Lawal on 16/04/2015.
 */
public class App {

    private static final Logger LOG = Logger.getLogger(App.class);

    private static final String dropBoxPath = "C:\\Users\\Lawal\\Dropbox\\Apps\\TouchScan\\CsvLogs\\FP31881DF87D1";


    //run like so :  java -jar C:\Users\Lawal\Dropbox\Apps\TouchScan\CsvLogs\FP31881DF87D1 ToyotaCorolla myusername mypassword

    public static void main(String[] args) throws IOException {


        Properties prop = new Properties();
        String url = null;

        try (InputStream input = App.class.getClassLoader().getResourceAsStream("app.properties")) {
            prop.load(input);
            url = prop.getProperty("server_url", "https://my.iot-ticket.com/api/v1");

        } catch (Exception ignored) {

        }


        int i = 0;
        String folderPath = args[i++];
        String carName = args[i++];
        String userName = args[i++];
        String password = args[i++];


        LOG.info("Using url " + url);
        LOG.info("Root Folder " + folderPath);

        TouchScanImporter importer = new TouchScanImporter(url, userName, password);

        Path processedDir = Paths.get(folderPath, "processed");
        if( Files.notExists(processedDir)){
            processedDir= Files.createDirectory(processedDir);
        }

        final Path rootDir  =processedDir;

       Files.list(Paths.get(folderPath))
                                    .filter(p -> p.toAbsolutePath().toString().endsWith("csv"))
                                    .collect(Collectors.toMap(pt -> pt, pt -> importer.run(pt, carName)))
                                    .forEach((source,success)-> {
                                    if(success){
                                        try {
                                            Files.move(source, rootDir.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                                            LOG.info(source.toAbsolutePath().toString()+" has been processed");
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }

                                    }});

    }


}
