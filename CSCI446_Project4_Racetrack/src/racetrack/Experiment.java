/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package racetrack;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.*;


/**
 *
 * @author James
 */
public class Experiment 
{
    public Racetrack L;
    public Racetrack O;
    public Racetrack R;
    
    public Experiment() throws IOException
    {
        L = ConvertToRacetrack("tracks/L-track.txt", L);
        L.printTrack();
        
        O = ConvertToRacetrack("tracks/O-track.txt", O);
        O.printTrack();
        
        R = ConvertToRacetrack("tracks/R-track.txt", R);
        R.printTrack();
    }
    
    
    private Racetrack ConvertToRacetrack(String fileName, Racetrack T) throws IOException
    {
        // create file reader for dataset file and wrap it in a buffer
        FileReader fileReader = null;
        try {
                fileReader = new FileReader(fileName);
        } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
        }
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        
        String line = null;
        try {
            line = bufferedReader.readLine();
        } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
            }
            String[] data = line.split(",");
                        
        int rows = Integer.parseInt(data[0]);
        int cols = Integer.parseInt(data[1]);
        System.out.println("Track " + fileName + " (r, c)= (" + rows + ", " + cols + ")"); 
        T = new Racetrack(rows, cols);
        for (int i = 0; i < rows; i++)
        {
            try {
                line = bufferedReader.readLine();
            } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                }
            for(int j = 0; j < line.length(); j++)
            {
                T.setTrack(i, j, line.charAt(j));
            }
        }
        
        return T;
    }
    
    private void DisplayResults()
    {
        
    }
}
