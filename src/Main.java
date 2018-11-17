import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Header;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

public class Main {

    public final String customMusicPath = "D:\\SteamLibrary\\steamapps\\common\\Crypt of the NecroDancer\\data\\custom_music\\";
    public final String OSUSongsPath = "C:\\Users\\welsa\\AppData\\Local\\osu!\\Songs\\";

    public static void main(String[] args) throws IOException{
        Scanner scanner = new Scanner(System.in);
        System.out.println("Drag and drop .osu file here");
        String inputString = scanner.nextLine();
        Main main = new Main();
        File dotOSU = new File(inputString);
        main.convertOSU(dotOSU, main.getMapSongTitle(dotOSU) + ".mp3");
    }

    public void convertOSU(File dotOSU, String outputName) throws IOException {
        Map<Double, Double> timingPoints = getTimingPoints(dotOSU);
        int iterationCount = 1;
        ArrayList<Double> beats = new ArrayList<>();
        for (Double offset : timingPoints.keySet()) {
            Double MBB = timingPoints.get(offset);
            if (timingPoints.size() == iterationCount) {
                for (double beat : getBeatsBetween(offset, getDurationOfMP3(getMapMP3(dotOSU)), MBB)) {
                    beats.add(beat);
                }
            } else {
                for (double beat : getBeatsBetween(offset, (double) timingPoints.keySet().toArray()[iterationCount], MBB)) {
                    beats.add(beat);
                }
            }
            iterationCount++;
        }
        FileUtils.copyFile(getMapMP3(dotOSU), new File(customMusicPath + outputName));
        writeOneBeatPerLineInSeconds(customMusicPath + outputName + ".txt", beats);
    }

    public ArrayList<Double> getBeatsBetween(double start, double end, double MBB) {
        ArrayList<Double> beats = new ArrayList<>();
        int totalBeatsBetween = (int) ((end - start) / MBB);
        for (int i = 0; i <= totalBeatsBetween; i++) {
            beats.add(start + MBB * i);
        }
        return beats;
    }

    public String getMapSongTitle(File dotOSU) throws IOException {
        //Assumes v14 .osu atm
        BufferedReader OSUReader = new BufferedReader(new FileReader(dotOSU));
        String line = null;
        while ((line = OSUReader.readLine()) != null) {
            if (line.startsWith("Title:")) {
                return line.substring(6).trim();
            }
        }
        return null;
    }

    public File getMapMP3(File dotOSU) throws IOException {
        //Assumes v14 .osu atm
        BufferedReader OSUReader = new BufferedReader(new FileReader(dotOSU));
        String line = null;
        while ((line = OSUReader.readLine()) != null) {
            if (line.startsWith("AudioFilename: ")) {
                return new File(dotOSU.getParent() + "\\" + line.substring(14).trim());
            }
        }
        return null;
    }

    public TreeMap<Double, Double> getTimingPoints(File dotOSU) throws IOException {
        //Assumes V14 .osu file atm
        //Offset, Milliseconds between beat
        TreeMap<Double, Double> timingPoints = new TreeMap<>();
        BufferedReader OSUReader = new BufferedReader(new FileReader(dotOSU));
        String line = null;
        boolean atTiming = false;
        while ((line = OSUReader.readLine()) != null) {
            if (line.equals("[TimingPoints]")) {
                atTiming = true;
            } else if (atTiming && line.equals("")) {
                atTiming = false;
            } else if (atTiming) {
                //Offset, Milliseconds per Beat, Meter, Sample Set, Sample Index, Volume, Inherited, Kiai Mode
                String timingData[] = line.split(",");
                if (timingData[6].equals("1")) {
                    timingPoints.put(Double.parseDouble(timingData[0]), Double.parseDouble(timingData[1]));
                }
            }
        }
        OSUReader.close();
        return timingPoints;
    }

    public void writeOneBeatPerLineInSeconds(String name, ArrayList<Double> beats) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(name)));
        for (double beat : beats) {
            bw.write((beat / 1000) + "\n");
        }
        bw.close();
    }

    //Copied most of this from Lăng Minh on overflow
    public double getDurationOfMP3(File mp3) throws IOException{

        try {
            Header h = null;
            FileInputStream file = null;
            file = new FileInputStream(mp3);
            Bitstream bitstream = new Bitstream(file);
            h = bitstream.readFrame();
            long tn = 0;
            tn = file.getChannel().size();
            return h.total_ms((int) tn);
        }catch (BitstreamException e){
            e.printStackTrace();
        }
        return 3600000;
    }
}
