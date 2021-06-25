package com.tophatdemon;

import java.io.InputStream;
import java.util.Scanner;

public class Assets {
    public static String getFileContents(String path) throws Exception {
        String output = "";
        InputStream stream = ClassLoader.getSystemResourceAsStream(path); 
        if (stream != null) {
            Scanner scanner = new Scanner(stream);
            output = scanner.useDelimiter("\\A").next();
            scanner.close();
            stream.close();
        } else {
            throw new Exception("Failed to located resource at " + path);
        }
        return output;
    }
}
