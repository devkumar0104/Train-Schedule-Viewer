package com.java.ooplproject;

//javac -cp lib/json-20250517.jar com/java/ooplproject/TrainScheduleViewer.java 
//java -cp lib/json-20250517.jar:. com.java.ooplproject.TrainScheduleViewer

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import org.json.*;

public class TrainScheduleViewer extends JFrame {
    private JTextField trainNumberField;
    private JTextField trainNameField;
    private JTextField stationField;
    private JComboBox<String> dayComboBox;
    private JComboBox<String> sortComboBox;
    private JTextArea resultArea;

    public TrainScheduleViewer() {
        setTitle("Train Schedule Viewer");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(3, 4, 10, 10));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Search & Sort Filters"));

        trainNumberField = new JTextField();
        trainNameField = new JTextField();
        stationField = new JTextField();
        dayComboBox = new JComboBox<>(new String[] { "Any", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" });
        sortComboBox = new JComboBox<>(new String[] { "None", "Arrival Time", "Departure Time" });

        inputPanel.add(new JLabel("Train Number:"));
        inputPanel.add(trainNumberField);
        inputPanel.add(new JLabel("Train Name:"));
        inputPanel.add(trainNameField);
        inputPanel.add(new JLabel("Station in Route:"));
        inputPanel.add(stationField);
        inputPanel.add(new JLabel("Operating Day:"));
        inputPanel.add(dayComboBox);
        inputPanel.add(new JLabel("Sort By:"));
        inputPanel.add(sortComboBox);

        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> searchTrains());

        add(inputPanel, BorderLayout.NORTH);
        add(searchButton, BorderLayout.SOUTH);

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        add(new JScrollPane(resultArea), BorderLayout.CENTER);

        setVisible(true);
    }

    private void searchTrains() {
        String trainNumber = trainNumberField.getText().trim().toLowerCase();
        String trainName = trainNameField.getText().trim().toLowerCase();
        String station = stationField.getText().trim().toLowerCase();
        String selectedDay = dayComboBox.getSelectedItem().toString();
        String sortBy = sortComboBox.getSelectedItem().toString();

        try {
            File file = new File("Schedule.json");
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            reader.close();

            JSONArray trains = new JSONArray(jsonBuilder.toString());
            java.util.List<JSONObject> matchedTrains = new ArrayList<>();

            for (int i = 0; i < trains.length(); i++) {
                JSONObject train = trains.getJSONObject(i);
                String number = train.getString("train_number").toLowerCase();
                String name = train.getString("train_name").toLowerCase();
                JSONArray route = train.getJSONArray("route");
                JSONArray days = train.getJSONArray("operating_days");

                boolean matchesNumber = trainNumber.isEmpty() || number.contains(trainNumber);
                boolean matchesName = trainName.isEmpty() || name.contains(trainName);
                boolean matchesStation = station.isEmpty()
                        || route.toList().stream().anyMatch(s -> s.toString().toLowerCase().contains(station));
                boolean matchesDay = selectedDay.equals("Any") || days.toList().contains(selectedDay);

                if (matchesNumber && matchesName && matchesStation && matchesDay) {
                    matchedTrains.add(train);
                }
            }

            if (sortBy.equals("Arrival Time")) {
                matchedTrains.sort(Comparator.comparing(t -> t.getJSONObject("schedule").getString("arrival_time")));
            } else if (sortBy.equals("Departure Time")) {
                matchedTrains.sort(Comparator.comparing(t -> t.getJSONObject("schedule").getString("departure_time")));
            }

            resultArea.setText("Matching Trains:\n\n");
            if (matchedTrains.isEmpty()) {
                resultArea.setText("No trains matched your criteria.");
                return;
            }

            for (JSONObject train : matchedTrains) {
                resultArea.append("Train Number: " + train.getString("train_number") + "\n");
                resultArea.append("Train Name: " + train.getString("train_name") + "\n");
                resultArea.append("Operating Days: " + train.getJSONArray("operating_days").toString() + "\n");
                resultArea.append("Route:\n");

                JSONArray route = train.getJSONArray("route");
                JSONObject schedule = train.getJSONObject("schedule");
                String departure = schedule.getString("departure_time");
                String arrival = schedule.getString("arrival_time");

                for (int j = 0; j < route.length(); j++) {
                    String stationName = route.getString(j);
                    resultArea.append(stationName);
                    if (j == 0) {
                        resultArea.append(" | Dep: " + departure + "\n");
                    } else if (j == route.length() - 1) {
                        resultArea.append(" | Arr: " + arrival + "\n");
                    } else {
                        resultArea.append("\n");
                    }
                }
                resultArea.append("\n--------------------------\n\n");
            }

        } catch (IOException | JSONException ex) {
            resultArea.setText("Error reading file: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TrainScheduleViewer::new);
    }
}
