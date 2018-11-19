package com.example.overl.hipe.background;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by zhang on 2018/11/2.
 */

public class DataReader {
    private InputStreamReader in;
    private BufferedReader reader;

    public DataReader(File file) throws IOException{
        in = new InputStreamReader(new FileInputStream(file), "UTF-8");
        reader = new BufferedReader(in);
    }

    public DataReader(String path) throws IOException{
        in = new InputStreamReader(new FileInputStream(path), "UTF-8");
        reader = new BufferedReader(in);
    }

    public DataReader(File file, String charsetName) throws IOException{
        in = new InputStreamReader(new FileInputStream(file), charsetName);
        reader = new BufferedReader(in);
    }

    public DataReader(String path, String charsetName) throws IOException{
        in = new InputStreamReader(new FileInputStream(path), charsetName);
        reader = new BufferedReader(in);
    }

    public String readLine() throws IOException{
        return reader.readLine();
    }

    public void close() throws IOException{
        reader.close();
        in.close();
    }
}
