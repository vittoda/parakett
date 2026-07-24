package com.parakett;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/*
 * Keys for Parakett server will have a prefix 'parakett'
 */
public class Keys {

    private static HashMap<String, String> _mCreds = new HashMap<>();

    private Keys() {

    }

    public static String getCred(String key) {
        return _mCreds.get(key);
    }

    public static void loadKeys() throws IOException {
        String homeDir = System.getProperty("user.home");
        File file = new File(homeDir + File.separatorChar + ".fskeys");
        _mCreds.clear();
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    int index = line.indexOf("=");
                    String lineKey = line.substring(0, index);
                    if(lineKey.startsWith("parakett.")) {
                        _mCreds.put(lineKey, line.substring(index+1));
                    }
                }
            }

        }
    }

}
