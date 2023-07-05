// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.NonAzureOpenAIKeyCredential;
import com.azure.core.credential.AzureKeyCredential;
import com.microsoft.semantickernel.util.AIProviderSettings;
import com.microsoft.semantickernel.util.AzureOpenAISettings;
import com.microsoft.semantickernel.util.ClientSettings;
import com.microsoft.semantickernel.util.OpenAISettings;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Supplier;

public class Config {

    public static final String CONF_PROPERTIES =
            getEnvOrProperty("CONF_PROPERTIES", "java/samples/conf.properties");

    public static final String OPENAI_CLIENT_TYPE =
            getEnvOrProperty("OPENAI_CLIENT_TYPE", "OPEN_AI");

    private static String getEnvOrProperty(String propertyName, String defaultValue) {
        String env = System.getenv(propertyName);
        if (env != null && !env.isEmpty()) {
            return env;
        }

        String property = System.getProperty(propertyName);
        if (property != null && !property.isEmpty()) {
            return property;
        }

        return defaultValue;
    }

    /**
     * Looks for settings in the order of:
     * <p>
     * 1. Properties file
     * 2. System properties
     * 3. Environment variables
     *
     * @return A client instance
     * @throws IOException If the settings are not found
     */
    public static OpenAIAsyncClient getClient() throws IOException {
        return ClientType.valueOf(OPENAI_CLIENT_TYPE).getClient();
    }

    public enum ClientType {
        OPEN_AI {
            @Override
            public OpenAIAsyncClient getClient() throws IOException {
                return buildClient(
                        () -> {
                            try {
                                return AIProviderSettings.getOpenAISettingsFromFile(CONF_PROPERTIES);
                            } catch (IOException e) {
                                return null;
                            }
                        },
                        AIProviderSettings::getOpenAISettingsFromSystemProperties,
                        AIProviderSettings::getOpenAISettingsFromEnv);
            }

            @Override
            public OpenAIAsyncClient getClient(String file) throws IOException {
                OpenAISettings settings = AIProviderSettings.getOpenAISettingsFromFile(file);
                return getClient(settings);
            }

            @Override
            protected OpenAIAsyncClient getClient(ClientSettings settingsIn) {
                if (!(settingsIn instanceof OpenAISettings settings)) {
                    throw new IllegalArgumentException("Must provide a settings of type OpenAISettings");
                }

                return new OpenAIClientBuilder()
                        .credential(new NonAzureOpenAIKeyCredential(settings.getKey()))
                        .buildAsyncClient();
            }

            @Override
            protected String getErrorMessage() {
                return "Unable to find OpenAI settings. Please provide an endpoint and organisation Id.";
            }
        },
        AZURE_OPEN_AI {
            @Override
            public OpenAIAsyncClient getClient() throws IOException {
                return buildClient(
                        () -> {
                            try {
                                return AIProviderSettings.getAzureOpenAISettingsFromFile(CONF_PROPERTIES);
                            } catch (IOException e) {
                                return null;
                            }
                        },
                        AIProviderSettings::getAzureOpenAISettingsFromSystemProperties,
                        AIProviderSettings::getAzureOpenAISettingsFromEnv);
            }

            @Override
            public OpenAIAsyncClient getClient(String file) throws IOException {
                AzureOpenAISettings settings = AIProviderSettings.getAzureOpenAISettingsFromFile(file);
                return getClient(settings);
            }

            @Override
            public OpenAIAsyncClient getClient(ClientSettings settingsIn) {
                if (!(settingsIn instanceof AzureOpenAISettings settings)) {
                    throw new IllegalArgumentException("Must provide a settings of type AzureOpenAISettings");
                }

                return new OpenAIClientBuilder()
                        .endpoint(settings.getEndpoint())
                        .credential(new AzureKeyCredential(settings.getKey()))
                        .buildAsyncClient();
            }

            @Override
            protected String getErrorMessage() {
                return "Unable to find Azure OpenAI settings. Please provide an endpoint and key.";
            }
        };

        /**
         * Builds the client based on the first valid settings provided.
         *
         * @param settings Suppliers that provide settings
         * @return The client
         * @throws IOException If no valid settings are found
         */
        protected OpenAIAsyncClient buildClient(Supplier<ClientSettings<?>>... settings) throws IOException {
            ClientSettings<?> firstSetting = Arrays.stream(settings)
                    .map(Supplier::get)
                    .filter(clientSettings -> clientSettings != null && clientSettings.isValid())
                    .findFirst()
                    .orElse(null);

            if (firstSetting != null) {
                return getClient(firstSetting);
            } else {
                throw new IOException(getErrorMessage());
            }
        }


        /**
         * Looks for settings in the order of:
         * <p>
         * 1. Properties file
         * 2. System properties
         * 3. Environment variables
         *
         * @return A client instance
         * @throws IOException If the settings are not found
         */
        public abstract OpenAIAsyncClient getClient() throws IOException;

        /**
         * Returns the client that will handle AzureOpenAI or OpenAI requests.
         *
         * @return client to be used by the kernel.
         */
        public abstract OpenAIAsyncClient getClient(String file) throws IOException;

        /**
         * Builds the client based on the settings provided.
         *
         * @param settings The settings to use
         * @return The client
         */
        protected abstract OpenAIAsyncClient getClient(ClientSettings settings);

        protected abstract String getErrorMessage();
    }
}
