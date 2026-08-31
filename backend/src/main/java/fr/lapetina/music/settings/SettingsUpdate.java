package fr.lapetina.music.settings;

/** A partial change to the settings: null means "leave this one alone". */
public record SettingsUpdate(
        Boolean llmEnabled,
        Boolean toolsEnabled,
        Boolean knowledgeEnabled,
        String runtimeMode,
        String model,
        String baseUrl,
        Double temperature,
        Integer numCtx,
        Boolean think,
        Integer timeoutSeconds,
        Integer cooldownSeconds,
        Integer memoryMessages,
        String learnerName) {

    /**
     * A change that alters nothing, to be narrowed with one of the helpers below.
     *
     * <p>This record has thirteen nullable fields, so writing one positionally means
     * counting nulls, and adding a field silently shifts every existing call along by one.
     * It has already done so twice. Prefer these.
     */
    public static SettingsUpdate none() {
        return new SettingsUpdate(null, null, null, null, null, null, null, null, null, null,
                null, null, null);
    }

    public SettingsUpdate withRuntimeMode(String mode) {
        return new SettingsUpdate(llmEnabled, toolsEnabled, knowledgeEnabled, mode, model, baseUrl,
                temperature, numCtx, think, timeoutSeconds, cooldownSeconds, memoryMessages, learnerName);
    }

    public SettingsUpdate withKnowledgeEnabled(boolean enabled) {
        return new SettingsUpdate(llmEnabled, toolsEnabled, enabled, runtimeMode, model, baseUrl,
                temperature, numCtx, think, timeoutSeconds, cooldownSeconds, memoryMessages, learnerName);
    }

    public SettingsUpdate withModel(String model) {
        return new SettingsUpdate(llmEnabled, toolsEnabled, knowledgeEnabled, runtimeMode, model, baseUrl,
                temperature, numCtx, think, timeoutSeconds, cooldownSeconds, memoryMessages, learnerName);
    }
}
