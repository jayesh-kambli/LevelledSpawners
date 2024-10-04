package org.first.jayesh;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MessageLoader {
    private final Properties properties;

    public MessageLoader(String languageCode) {
        properties = new Properties();
        loadProperties(languageCode);
    }

    private void loadProperties(String languageCode) {
        String fileName = "lang/messages_" + languageCode + ".properties";
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (input == null) {
                System.out.println("Sorry, unable to find " + fileName);
                return;
            }
            properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public String getMessage(String key) {
        return properties.getProperty(key);
    }
}
