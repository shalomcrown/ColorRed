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

    public Main() {
        sirenUrl = getClass().getResource("/91244-SIREN2.mp3");
        if (sirenUrl != null) {
            //TODO: Use javafx
        }

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

                    conn.connect();
                    InputStream in = conn.getInputStream();

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
                                int lastSpace = line.lastIndexOf(' ');
                                String lastField = line.substring(lastSpace + 1);

                                if (lastField.equals(requiredArea)) {
                                    InputStream sitenIn = sirenUrl.openStream();
                                    Player player = new Player(sitenIn);
                                    player.play();
                                }
                            }
                        }

                        lastId = newId;
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

    public static void main( String[] args )  {

        new Main();
    }
}
