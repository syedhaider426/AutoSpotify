package com.spring.autospotify;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


// Get the secret values stored in properties files
public class GetPropertyValues {
    Properties prop;
    InputStream inputStream;
    public Properties getPropValues() throws IOException {
        try {
            Properties prop = new Properties();
            String propFileName = "secrets.properties";
            inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }
            return prop;
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        } finally {
            inputStream.close();
        }
        return prop;
    }
}
