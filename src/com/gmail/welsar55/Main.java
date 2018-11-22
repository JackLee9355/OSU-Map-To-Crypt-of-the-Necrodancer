package com.gmail.welsar55;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Header;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.*;

public class Main {

    public String customMusicPath = null;
    public String OSUSongsPath = null;
    public String saveFilePath = null;

    public int maxBPM = 360;
    public int minBPM = 80;

    public int minTimingPoints = 1;
    public int maxTimingPoints = 50;

    public static void main(String[] args) throws IOException {
        while(true) {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Commands: \nrandomize\nconvertall (Might run out of memory atm.)\nconvert [path to .osu file]\nexit");
            Main main = new Main();
            main.loadConfig();
            String command = scanner.nextLine();
            if (command.trim().equalsIgnoreCase("randomize")) {
                main.randomizeMusic();
            } else if (command.trim().equalsIgnoreCase("convertall")) {
                main.convertEntireOSULibrary();
            } else if (command.startsWith("convert ")) {
                String filePath = command.substring("convert ".length());
                File osu = new File(filePath);
                main.convertOSU(osu, main.getMapSongTitle(osu));
            }else if(command.startsWith("exit")){
                System.exit(1);
            }
        }
    }

    public void randomizeMusic() {
        for(int i=0;i<=20;i++){
            boolean exception = true;
            while(exception) {
                try {
                    File dotOSU = null;
                    while (dotOSU == null) {
                        dotOSU = randomDotOSU();
                    }
                    convertOSU(dotOSU, getMapSongTitle(dotOSU));
                    setCustomSongAs(i,getMapSongTitle(dotOSU) + ".mp3");
                    exception = false;
                }
                catch (Exception e){
                    System.out.println("Error occured while trying to randomize music. Trying another song. Error: " + e.getMessage());
                }
            }
        }
    }

    public void loadConfig() throws IOException{
        BufferedReader configReader = new BufferedReader(new FileReader(new File("config.txt")));
        String line = null;
        while ((line = configReader.readLine()) != null) {
            if (line.startsWith("OSUPath:")){
                OSUSongsPath = line.substring("OSUPath:".length());
            }else if(line.startsWith("MusicPath:")){
                customMusicPath = line.substring("MusicPath:".length());
            }else if(line.startsWith("SavePath:")){
                saveFilePath = line.substring("SavePath:".length());
            }else if(line.startsWith("MaximumAmountOfTimingPoints:")){
                maxTimingPoints = Integer.parseInt(line.substring("MaximumAmountOfTimingPoints:".length()));
            }else if(line.startsWith("MinimumAmountOfTimingPoints:")){
                minTimingPoints = Integer.parseInt(line.substring("MinimumAmountOfTimingPoints:".length()));
            }else if(line.startsWith("MaximumBPM:")) {
                maxBPM = Integer.parseInt(line.substring("MaximumBPM:".length()));
            }else if(line.startsWith("MinimumBPM:")){
                minBPM = Integer.parseInt(line.substring("MinimumBPM:".length()));
            }
        }
        if(OSUSongsPath == null || customMusicPath == null || saveFilePath == null){
            System.out.println("Error reading config");
        }
        configReader.close();
    }

    public void setCustomSongAs(int customSongCount, String MP3Name) throws IOException{
        BufferedReader XMLReader = new BufferedReader(new FileReader(new File(saveFilePath)));
        String line = null;
        String XML = null;
        while ((line = XMLReader.readLine()) != null) {
            if(XML == null){
                XML = line;
            }
            else{
                XML = XML + "\n " + line;
            }
        }
        XMLReader.close();
        StringBuilder toWrite;
        if(XML.indexOf("customSong" + customSongCount + "=\"") != -1) {
            toWrite = new StringBuilder(XML.replaceAll("(?<=customSong" + customSongCount + "=\")(.*?)(?=\")", (customMusicPath + MP3Name).replaceAll("\\\\", "\\\\\\\\")));
        }else{
            toWrite = new StringBuilder(XML);
            toWrite.insert(toWrite.indexOf("<game ") + "<game ".length(), "customSong"+customSongCount+"=\""+customMusicPath+MP3Name+"\" ");
        }
        BufferedWriter XMLWriter = new BufferedWriter(new FileWriter(new File(saveFilePath)));
        XMLWriter.write(toWrite.toString());
        XMLWriter.close();
    }

    public File randomDotOSU() throws IOException {
        for (int i=0;i<50;i++) {
            File songsDirectory = new File(OSUSongsPath);
            ArrayList<File> potentialFolders = new ArrayList<>();
            for (File songFolders : songsDirectory.listFiles()) {
                if (songFolders.isDirectory()) {
                    potentialFolders.add(songFolders);
                }
            }
            File songFolder = potentialFolders.get(new Random().nextInt(potentialFolders.size()));
            for (File songFiles : songFolder.listFiles()) {
                if (songFiles.getName().endsWith(".osu")) {
                    double highestBPM = getMaxBPM(songFiles);
                    Map<Double, Double> timingPoints = getTimingPoints(songFiles);
                    if(highestBPM >= minBPM && highestBPM <= maxBPM && timingPoints.size() >= minTimingPoints && timingPoints.size() <= maxTimingPoints) {
                        return songFiles;
                    }
                    break;
                }
            }
        }
        return null;
    }

    //Currently results in a memory leak or something. OutOfMemoryError
    public void convertEntireOSULibrary() throws IOException {
        File songsDirectory = new File(OSUSongsPath);
        for(File songFolders : songsDirectory.listFiles()){
            if (songFolders.isDirectory()){
                for(File songFiles : songFolders.listFiles()){
                    if(songFiles.getName().endsWith(".osu")){
                        try {
                            String mapTitle = getMapSongTitle(songFiles);
                            convertOSU(songFiles, mapTitle);
                        }catch (Exception e){
                            e.printStackTrace();
                            //System.out.println("Failure to convert a song: " + getMapSongTitle(songFiles) + " Reason: " + e.getMessage());
                        }
                        break;
                    }
                }
            }
        }
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
        String fileName = sanitizeFileName(outputName);
        FileUtils.copyFile(getMapMP3(dotOSU), new File(customMusicPath + fileName + ".mp3"));
        writeOneBeatPerLineInSeconds(customMusicPath + fileName + ".mp3.txt", beats);
        System.out.println("Converted: " + outputName);
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
                if (timingData.length > 6 && timingData[6].equals("1")) {
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
        }catch (Exception e){
            return 3600000;
        }
    }

    public String sanitizeFileName(String name){
        return name.replaceAll("[^\\w| ]", "");
    }

    public double getMaxBPM(File dotOSU) throws IOException {
        double maxBPM = 0;
        for(double timingPoints : getTimingPoints(dotOSU).values()){
            if(timingPoints > maxBPM){
                maxBPM = timingPoints;
            }
        }
        return 1/maxBPM*60000;
    }
}
