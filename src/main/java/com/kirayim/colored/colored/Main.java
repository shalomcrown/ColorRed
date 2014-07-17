package com.kirayim.colored.colored;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Hello world!
 *
 */
public class Main implements Runnable {
    Thread updateThread = null;
    public static final String orefUrl = "http://www.oref.org.il/WarningMessages/alerts.json";
    String lastId = "--";
    String requiredArea = "160";
    URL sirenUrl = null;
    long lastModified = 0L;

    public Main(Integer areaCode) {
        sirenUrl = getClass().getResource("/91244-SIREN2.mp3");
        if (sirenUrl != null) {
            //TODO: Use javafx maybe never
        }

        if (areaCode != null) {
            this.requiredArea = Integer.toString(areaCode);
        }

        System.out.println("Working on area: " + areaCode);

        updateThread = new Thread(this, "Update thread");
        updateThread.start();
    }

    // ===================================================================

    public void run() {
        ObjectMapper mapper = new ObjectMapper();

        try {
            while (true) {

                URL url = new URL(orefUrl);

                try {
                    HttpURLConnection conn = (HttpURLConnection)url.openConnection();

                    if (lastModified != 0L) {
                        conn.setIfModifiedSince(lastModified);
                    }

                    conn.connect();
                    int responseCode = conn.getResponseCode();

                    if (responseCode >= 400) {
                        System.out.println("Error contacting site:" + responseCode);

                    } else if (responseCode == 304) {
                        // Ignore

                    } else  if (responseCode == 200) {
                        InputStream in = conn.getInputStream();

                        lastModified = conn.getLastModified();

                        @SuppressWarnings("unchecked")
                        HashMap<String, ?> map = mapper.readValue(in, HashMap.class);

                        String newId =(String) map.get("id");

                        if (! newId.equals(lastId)) {
                            long idTime = Long.parseLong(newId);
                            int offset = TimeZone.getDefault().getOffset(new Date().getTime());
                            idTime -= offset;
                            Date lastTime = new Date(idTime);

                            System.out.println("Timestamp: " + lastTime);
                            System.out.println(map);

                            @SuppressWarnings("unchecked")
                            ArrayList<String> data = (ArrayList<String>)map.get("data");

                            if (data != null && data.size() > 0) {
                                for (String line:data) {
                                    for (String innerSplit : line.split(",")) {

                                        innerSplit = innerSplit.trim();

                                        int lastSpace = innerSplit.lastIndexOf(' ');
                                        String lastField = innerSplit.substring(lastSpace + 1);

                                        if (lastField.equals(requiredArea)) {
                                            System.out.println("**** ALERT ALERT ALERT ****");
                                            InputStream sitenIn = sirenUrl.openStream();
                                            Player player = new Player(sitenIn);
                                            player.play();
                                        }
                                    }
                                }
                            }

                            lastId = newId;
                        }
                    }

                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (JavaLayerException ex) {
                    ex.printStackTrace();
                }


                Thread.sleep(5000);
            }
        } catch (InterruptedException ex) {
            // Ignore
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        }
    }

    // ===================================================================

    static void printHelp(Options options) {
        new HelpFormatter().printHelp(
                "java  -jar ${jar_name}  \\\n" +
                "  ${extra_args}\n\n",
                "Code Red monitor",
                options,
                "");
    }

    // ===================================================================

    public static void main( String[] args )  {
        Integer areaCode = null;
        Options options = new Options();

        options.addOption("a", "area-code", true, "Area code number");
        options.addOption("h", "help", false, "This Help message");

        CommandLineParser parser = new GnuParser();
        CommandLine cmdLine = null;

        try  {
           cmdLine = parser.parse(options, args);
        } catch (ParseException exp)  {
            printHelp(options);

           System.err.println("\n\nCommand line Parsing failed.  Reason: " + exp.getMessage());
           System.exit(-1);
        }

        if (cmdLine.hasOption('h')) {
            printHelp(options);
            System.exit(0);
        }

        if (cmdLine.hasOption('a')) {
            areaCode = Integer.parseInt(cmdLine.getOptionValue('a'));
        }

        new Main(areaCode);
    }
}
