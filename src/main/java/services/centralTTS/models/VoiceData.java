package main.java.services.centralTTS.models;

public class VoiceData {

    public VoiceServiceEnum voiceService;
    public String text;
    public String enhancedText;
    public String voiceId;
    public String name;
    public String [] effects = null;
    public long usageCount = 0;

    public enum VoiceServiceEnum {
        amazonPolly
    }

    public VoiceData() {

    }

    public VoiceData(VoiceServiceEnum voiceService, String voiceId, String name, String text, String enhancedText, String [] effects) {
        this.voiceService = voiceService;
        this.voiceId = voiceId;
        this.name = name;
        this.text = text;
        this.enhancedText = enhancedText;
        this.effects = effects;
    }
}
