package com.spring.autospotify.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Get the secret values stored in properties files
 */
public class GetPropertyValues {
    Properties prop;
    InputStream inputStream;

    /**
     * Gets the properties created in 'secrets.properties' file
     * @return prop: Properties object in which properties from secrets.properties can be used
     * @throws IOException IOException is thrown if error with opening/reading/closing file occurs
     */
    public Properties getPropValues() throws IOException {
        try {
            Properties prop = new Properties();

            // file name
            String propFileName = "secrets.properties";

            // stream to get the file
            inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
            if (inputStream != null) {
                prop.load(inputStream); // load  properties from file into prop object
            } else {
                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }
            return prop;
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        } finally {
            assert inputStream != null;
            inputStream.close();
        }
        return prop;
    }
}