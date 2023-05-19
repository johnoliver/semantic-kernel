// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Settings {
    private static final Logger LOGGER = LoggerFactory.getLogger(Settings.class);

    public static class OpenAISettings {
        public String key;
        public String organizationId;
    }

    public static class AzureOpenAISettings {
        public String key;
        public String endpoint;
        public String deploymentName;
    }

    private enum Property {
        OPEN_AI_KEY("openai.key"),
        OPEN_AI_ORGANIZATION_ID("openai.organizationid"),
        AZURE_OPEN_AI_KEY("azureopenai.key"),
        AZURE_OPEN_AI_ENDPOINT("azureopenai.endpoint"),
        AZURE_OPEN_AI_DEPLOYMENT_NAME("azureopenai.deploymentname");

        public final String label;

        Property(String label) {
            this.label = label;
        }
    }

    /**
     * Returns an instance of OpenAISettings with key and organizationId from the properties file
     *
     * @param path Path to the properties file
     * @return OpenAISettings
     */
    public static OpenAISettings getOpenAISettingsFromFile(String path) {
        OpenAISettings settings = new OpenAISettings();
        settings.key = Settings.getSettingsValue(path, Property.OPEN_AI_KEY.label);
        settings.organizationId =
                Settings.getSettingsValue(path, Property.OPEN_AI_ORGANIZATION_ID.label, "");
        return settings;
    }

    /**
     * Returns an instance of AzureOpenAISettings with key, endpoint and deploymentName from the
     * properties file
     *
     * @param path Path to the properties file
     * @return OpenAISettings
     */
    public static AzureOpenAISettings getAzureOpenAISettingsFromFile(String path) {
        AzureOpenAISettings settings = new AzureOpenAISettings();
        settings.key = Settings.getSettingsValue(path, Property.AZURE_OPEN_AI_KEY.label);
        settings.endpoint = Settings.getSettingsValue(path, Property.AZURE_OPEN_AI_ENDPOINT.label);
        settings.deploymentName =
                Settings.getSettingsValue(path, Property.AZURE_OPEN_AI_DEPLOYMENT_NAME.label, "");
        return settings;
    }

    private static String getSettingsValue(String SettingsFile, String propertyName) {
        return getSettingsValue(SettingsFile, propertyName, null);
    }

    private static String getSettingsValue(
            String SettingsFile, String propertyName, String defaultValue) {
        File Settings = new File(SettingsFile);
        try (FileInputStream fis = new FileInputStream(Settings.getAbsolutePath())) {
            Properties props = new Properties();
            props.load(fis);
            if (defaultValue == null) {
                return props.getProperty(propertyName);
            }
            return props.getProperty(propertyName, defaultValue);
        } catch (IOException e) {
            LOGGER.error(
                    "Unable to load config value " + propertyName + " from file " + SettingsFile,
                    e);
            throw new RuntimeException(SettingsFile + " not configured properly");
        }
    }
}
