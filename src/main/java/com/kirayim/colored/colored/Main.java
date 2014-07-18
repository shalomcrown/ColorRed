package com.kirayim.colored.colored;

import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.codehaus.jackson.map.ObjectMapper;

import com.ibm.icu.text.Bidi;

/**
 * Hello world!
 *
 */
public class Main {
    Thread updateThread = null;
    public static final String orefUrl = "http://www.oref.org.il/WarningMessages/alerts.json";
    String lastId = null;
    volatile String requiredArea = "160";
    URL sirenUrl = null;
    long lastModified = 0L;
    private JFrame frmColorRed;
    private JTextField textField;
    private JList<String> alertList;
    ArrayList<String> listData = new ArrayList<>();


    // ===================================================================

    public Main(Integer areaCode) {
        sirenUrl = getClass().getResource("/91244-SIREN2.mp3");
        if (sirenUrl != null) {
            //TODO: Use javafx maybe never
        }

        if (areaCode != null) {
            this.requiredArea = Integer.toString(areaCode);
        }

        System.out.println("Working on area: " + requiredArea);

        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    initialize();
                    frmColorRed.setVisible(true);

                    updateThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            updateThread();
                        }
                    }, "Update thread");

                    updateThread.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // ===================================================================


    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        frmColorRed = new JFrame();
        frmColorRed.setTitle("Color Red - area " + requiredArea);
        frmColorRed.setBounds(100, 100, 450, 300);
        frmColorRed.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[]{0, 0, 0};
        gridBagLayout.rowHeights = new int[]{0, 0, 0};
        gridBagLayout.columnWeights = new double[]{1.0, 0.0, Double.MIN_VALUE};
        gridBagLayout.rowWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
        frmColorRed.getContentPane().setLayout(gridBagLayout);

        textField = new JTextField(requiredArea);
        GridBagConstraints gbc_textField = new GridBagConstraints();
        gbc_textField.insets = new Insets(0, 0, 5, 5);
        gbc_textField.fill = GridBagConstraints.HORIZONTAL;
        gbc_textField.gridx = 0;
        gbc_textField.gridy = 0;
        frmColorRed.getContentPane().add(textField, gbc_textField);
        textField.setColumns(10);

        JButton btnNewButton = new JButton("Change Area");
        btnNewButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                requiredArea = textField.getText();
                frmColorRed.setTitle("Color Red - area " + requiredArea);
            }
        });
        GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
        gbc_btnNewButton.insets = new Insets(0, 0, 5, 0);
        gbc_btnNewButton.gridx = 1;
        gbc_btnNewButton.gridy = 0;
        frmColorRed.getContentPane().add(btnNewButton, gbc_btnNewButton);

        JScrollPane scrollPane = new JScrollPane();
        GridBagConstraints gbc_scrollPane = new GridBagConstraints();
        gbc_scrollPane.gridwidth = 2;
        gbc_scrollPane.insets = new Insets(0, 0, 0, 5);
        gbc_scrollPane.fill = GridBagConstraints.BOTH;
        gbc_scrollPane.gridx = 0;
        gbc_scrollPane.gridy = 1;
        frmColorRed.getContentPane().add(scrollPane, gbc_scrollPane);

        alertList = new JList<>();
        scrollPane.setViewportView(alertList);
    }

    private void addListElement(final String data) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                listData.add(0, data);
                alertList.setListData(listData.toArray(new String[0]));
            }
        });
    }

    // ===================================================================

    public void updateThread() {
        ObjectMapper mapper = new ObjectMapper();

        try {
            while (true) {
                boolean alert = false;
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

                        if (lastId == null || ! newId.equals(lastId)) {
                            long idTime = Long.parseLong(newId);
                            int offset = TimeZone.getDefault().getOffset(new Date().getTime());
                            idTime -= offset;
                            Date lastTime = new Date(idTime);

                            System.out.println("Timestamp: " + lastTime);

                            String reconstructedJson = map.toString();
                            Bidi bidi = new Bidi();
                            bidi.setReorderingMode(Bidi.REORDER_DEFAULT);
                            bidi.setPara(reconstructedJson, Bidi.LEVEL_DEFAULT_RTL, new byte[reconstructedJson.length() * 2]);
                            System.out.println(bidi.writeReordered(Bidi.DO_MIRRORING));

                            @SuppressWarnings("unchecked")
                            ArrayList<String> data = (ArrayList<String>)map.get("data");

                            if (data != null && data.size() > 0) {
                                StringBuffer buffer = new StringBuffer();
                                boolean firstname = true;

                                for (String line:data) {
                                    for (String innerSplit : line.split(",")) {

                                        innerSplit = innerSplit.trim();

                                        int lastSpace = innerSplit.lastIndexOf(' ');
                                        String lastField = innerSplit.substring(lastSpace + 1);
                                        String placename = innerSplit.substring(0, lastSpace - 1);

                                        if (firstname) {
                                            firstname = false;
                                        } else {
                                            buffer.append(", ");
                                        }
                                        buffer.append(placename);

                                        if (lastField.equals(requiredArea)) {
                                            alert = true;
                                            System.out.println("**** ALERT ALERT ALERT ****");
                                            InputStream sitenIn = sirenUrl.openStream();
                                            Player player = new Player(sitenIn);
                                            player.play();
                                        }
                                    }
                                }
                                bidi = new Bidi();
                                bidi.setReorderingMode(Bidi.REORDER_DEFAULT);
                                bidi.setPara(buffer.toString(), Bidi.LEVEL_DEFAULT_RTL, new byte[buffer.length() * 2]);
                                String output = lastTime.toString() + "\n" + bidi.writeReordered(Bidi.DO_MIRRORING);

                                if (alert) {
                                    output += " ****** ";
                                }

                                alertList.add(new JTextArea(output), 0);
                            } else if (lastId == null) {
                                addListElement(lastTime.toString() + "\n No alerts");
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
                "java  -jar colored.jar  \n" +
                "  [options] \n\n",
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

        Main window = new Main(areaCode);

    }
}
