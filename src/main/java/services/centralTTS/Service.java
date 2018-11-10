package main.java.services.centralTTS;

import java.util.Arrays;
import java.io.File;
import java.io.InputStream;
import javax.servlet.ServletOutputStream;
import org.apache.commons.io.FileUtils;
import com.amazonaws.services.polly.model.Voice;

import main.java.helpers.configurations.*;
import main.java.helpers.logging.*;
import main.java.helpers.spark.*;
import main.java.helpers.data.fs.*;

import main.java.services.centralTTS.clients.AmazonPolly;
import main.java.services.centralTTS.models.*;

public class Service {

    private VoicesData voicesData;
    private String dataPath;
    private AmazonPolly voiceService;

    public Service() {
        try {
            this.dataPath = PropertiesReader.getProperty("service", "AUDIO_DATA", "");
            this.voicesData = (VoicesData)PersistentFileSystem.define(VoicesData.class);

            voiceService = new AmazonPolly();
            voiceService.createService();

            WebServer.registerGet("transform", (req, res) -> {
                String text = req.queryParams("text");
                String [] effects = null;
                Logger.info("New request: " + text);

                if (req.queryParams("effects") != null) {
                    effects = req.queryParams("effects").split(",");
                }

                // send audio
                res.type("audio/mp3");
                ServletOutputStream out = res.raw().getOutputStream();
                InputStream audio = this.getAudio(text, effects);
                int data = audio.read();
                while (data >= 0) {
                    out.write((char) data);
                    data = audio.read();
                }
                out.close();
                PersistentFileSystem.save(this.voicesData);
                Logger.info("Request done: " + text);

                return 200;
            });

        } catch (Exception exception) {
            Logger.exception(exception);
        }
    }

    private InputStream getAudio(String text, String [] effects) {
        InputStream audio = null;
        String enhancedText = enhanceText(text, effects);

        try {

            // try to find existing
            for(VoiceData voiceData : this.voicesData.items) {
                if(audioIsMatching(voiceData, text, effects)) {
                    Logger.info("Found a match!");
                    File audioFile = new File(getFileNamePath(voiceData.name));
                    voiceData.usageCount++;
                    return FileUtils.openInputStream(audioFile);
                }
            }

            // NOT FOUND
            // create new mp3 file
            Logger.info(String.format("No match found for: %s", enhancedText));
            audio = voiceService.synthesize(enhancedText);

            String audioName = String.format("%s_%s_%s",
                    VoiceData.VoiceServiceEnum.amazonPolly,
                    this.voiceService.getVoice().getName(),
                    System.currentTimeMillis());

            File file = new File(getFileNamePath(audioName));
            file.getParentFile().mkdirs();
            file.createNewFile();

            // write audio to it
            FileUtils.copyInputStreamToFile(audio, file);

            // save info about the audio
            VoiceData voiceData = new VoiceData(
                VoiceData.VoiceServiceEnum.amazonPolly,
                this.voiceService.getVoice().getName(),
                audioName,
                text,
                enhancedText,
                effects);

            this.voicesData.items.add(voiceData);
            PersistentFileSystem.save(this.voicesData);
        } catch (Exception exception) {
            Logger.info(String.format("Synthesizing failed for: %s", enhancedText));
            Logger.exception(exception);
        }

        return audio;
    }

    private boolean audioIsMatching(VoiceData voiceData, String text, String [] effects) {
        boolean effectsMismatch = false;
        Voice currentVoice = this.voiceService.getVoice();
        if (effects != null && voiceData.effects != null) {
            if (effects.length == voiceData.effects.length) {
                for (String effect : effects) {
                    boolean effectFound = false;
                    for (String appliedEffect : voiceData.effects) {
                        if (appliedEffect.equals(effect)) {
                            effectFound = true;
                            break;
                        }
                    }
                    effectsMismatch = !effectFound;
                }
            } else {
                effectsMismatch = true;
            }
        } else if (effects != voiceData.effects) {
            effectsMismatch = true;
        }
        return !effectsMismatch && voiceData.voiceId.equals(currentVoice.getName()) && voiceData.text.equals(text);
    }

    private String enhanceText(String text, String [] effects) {
        if (Arrays.asList(effects).contains("whispered")) {
            text = applyWhispered(text);
        }
        if (Arrays.asList(effects).contains("auto-breaths")) {
            text = applyAutoBreaths(text);
        }
        return String.format("<speak>%s</speak>", text);
    }

    private String applyAutoBreaths(String text) {
        return String.format("<amazon:auto-breaths frequency=\"medium\" volume=\"medium\" duration=\"x-short\">%s</amazon:auto-breaths>", text);
    }

    private String applyWhispered(String text) {
        return String.format("<amazon:effect name=\"whispered\">%s</amazon:effect>", text);
    }

    private String getFileNamePath(String name) {
        return dataPath + "/" + name + ".mp3";
    }

}