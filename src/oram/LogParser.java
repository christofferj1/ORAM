package oram;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * <p> ORAM <br>
 * Created by Christoffer S. Jensen on 06-05-2019. <br>
 * Master Thesis 2019 </p>
 *
 * Used to get data from log files
 */

public class LogParser {
    public static void main(String[] args) {
        File file = new File("log.log");
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String string;
            boolean notDone = true;
            int lastTime = 0;
            while ((string = reader.readLine()) != null) {
                if (string.contains("Done")) {
                    if (notDone) {
                        String percent = string.substring(10, 13);
                        String time = string.substring(29, 40);
                        int timeSoFar = millisecondsFromString(time);
                        System.out.println(percent + "; " + time + "; " + (timeSoFar - lastTime));
                        lastTime = timeSoFar;
                    }
                    if (string.contains("100%")) {
                        notDone = false;
                        System.out.println(string);
                    }
                }

                if (string.contains("Initialized Path ORAM strategy")) {
                    notDone = true;
                    lastTime = 0;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int millisecondsFromString(String string) {
        String tmp = string;
        String hoursString = tmp.substring(0, tmp.indexOf(":"));
        tmp = tmp.substring(tmp.indexOf(":") + 1, tmp.length());
        String minutesString = tmp.substring(0, tmp.indexOf(":"));
        tmp = tmp.substring(tmp.indexOf(":") + 1, tmp.length());
        String secondsString = tmp;

        int hours = Integer.parseInt(hoursString);
        int minutes = Integer.parseInt(minutesString);
        double seconds = Double.parseDouble(secondsString);

        return (int) (hours * 3600000 + minutes * 60000 + seconds * 1000);
    }
}
