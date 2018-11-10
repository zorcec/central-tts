package main.java.services.centralTTS;

import main.java.helpers.configurations.*;
import main.java.helpers.logging.*;
import main.java.helpers.spark.*;
import main.java.helpers.data.fs.*;

public class Main {

    public static void main(String[] args) throws Exception {
        try {
            Logger.init();
            PropertiesReader.init();
            WebServer.init();
            PersistentFileSystem.init();

            // activate services
            Logger.info("Service is starting!");
            new Service();

        } catch (Exception exception) {
            Logger.exception(exception);
        }
    }

}