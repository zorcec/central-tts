package main.java.services.centralTTS.clients;

import java.io.InputStream;
import java.io.IOException;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.polly.AmazonPollyClient;
import com.amazonaws.services.polly.model.DescribeVoicesRequest;
import com.amazonaws.services.polly.model.DescribeVoicesResult;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest;
import com.amazonaws.services.polly.model.SynthesizeSpeechResult;
import com.amazonaws.services.polly.model.Voice;
import com.amazonaws.services.polly.model.TextType;

import main.java.helpers.configurations.*;
import main.java.helpers.logging.*;

public class AmazonPolly {
    
    private AmazonPollyClient polly;
    private Voice voice;

    public AmazonPolly() {
        
    }

    public void createService() {

        AWSCredentials credentials = new BasicAWSCredentials(
            PropertiesReader.getProperty("secrets", "AWS_ACCESS_KEY_ID", ""),
            PropertiesReader.getProperty("secrets", "AWS_SECRET_ACCESS_KEY", ""));

        polly = new AmazonPollyClient(credentials);

        // Create describe voices request.
        DescribeVoicesRequest describeVoicesRequest = new DescribeVoicesRequest();

        // Synchronously ask Amazon Polly to describe available TTS voices & select the one we need
        DescribeVoicesResult describeVoicesResult = polly.describeVoices(describeVoicesRequest);
        for(Voice voice : describeVoicesResult.getVoices()) {
            if(voice.getName().equals(PropertiesReader.getProperty("service", "AWS_POLLY_VOICE_ID", "Salli"))) {
                this.voice = voice;
            }
        }
        Logger.info(String.format("Amazon Polly is ready, loaded voice: %s", this.voice.getName()));
    }

    public InputStream synthesize(String text) throws IOException {
        Logger.info(String.format("Synthesizing: %s", text));
        SynthesizeSpeechRequest synthReq = new SynthesizeSpeechRequest()
                .withTextType(TextType.Ssml)
                .withText(text)
                .withVoiceId(this.voice.getId())
                .withOutputFormat(OutputFormat.Mp3);
        SynthesizeSpeechResult synthRes = polly.synthesizeSpeech(synthReq);
        return synthRes.getAudioStream();
    }

    public Voice getVoice() {
        return this.voice;
    }

}
  