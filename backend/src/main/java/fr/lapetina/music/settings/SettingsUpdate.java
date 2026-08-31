package fr.lapetina.music.settings;

/** A partial change to the settings: null means "leave this one alone". */
public record SettingsUpdate(
        Boolean llmEnabled,
        Boolean toolsEnabled,
        String model,
        String baseUrl,
        Double temperature,
        Integer numCtx,
        Boolean think,
        Integer timeoutSeconds,
        Integer cooldownSeconds,
        Integer memoryMessages,
        String learnerName) {
}
