// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.e2e;

import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.microsoft.openai.AzureOpenAIClient;
import com.microsoft.openai.OpenAIAsyncClient;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.KernelConfig;
import com.microsoft.semantickernel.builders.SKBuilders;
import com.microsoft.semantickernel.connectors.ai.openai.textcompletion.OpenAITextCompletion;
import com.microsoft.semantickernel.textcompletion.TextCompletion;

import org.junit.jupiter.api.condition.EnabledIf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

@EnabledIf("isAzureTestEnabled")
public class AbstractKernelTest {

    public static final String CONF_OPENAI_PROPERTIES = "conf.openai.properties";
    public static final String AZURE_CONF_PROPERTIES = "conf.properties";

    public static Kernel buildTextCompletionKernel() throws IOException {
        String model = "text-davinci-003";
        TextCompletion textCompletion = new OpenAITextCompletion(getAzureOpenAIClient(), model);

        KernelConfig kernelConfig =
                SKBuilders.kernelConfig()
                        .addTextCompletionService(model, kernel -> textCompletion)
                        .build();

        return SKBuilders.kernel().setKernelConfig(kernelConfig).build();
    }

    public static OpenAIAsyncClient getOpenAIClient() throws IOException {
        String apiKey = getToken(CONF_OPENAI_PROPERTIES);
        return new com.microsoft.openai.OpenAIClientBuilder().setApiKey(apiKey).build();
    }

    public static OpenAIAsyncClient getAzureOpenAIClient() throws IOException {
        String apiKey = getToken(AZURE_CONF_PROPERTIES);

        com.azure.ai.openai.OpenAIAsyncClient client =
                new OpenAIClientBuilder()
                        .endpoint(getEndpoint(AZURE_CONF_PROPERTIES))
                        .credential(new AzureKeyCredential(apiKey))
                        .buildAsyncClient();

        return new AzureOpenAIClient(client);
    }

    public static String getAzureModel() throws IOException {
        return getConfigValue(AZURE_CONF_PROPERTIES, "model");
    }

    public static String getOpenAIModel() throws IOException {
        return getConfigValue(CONF_OPENAI_PROPERTIES, "model");
    }

    public static String getModel(String configName) throws IOException {
        return getConfigValue(configName, "model");
    }

    public static String getToken(String configName) throws IOException {
        return getConfigValue(configName, "token");
    }

    public static String getEndpoint(String configName) throws IOException {
        return getConfigValue(configName, "endpoint");
    }

    private static String getConfigValue(String configName, String propertyName)
            throws IOException {
        String home = new File(System.getProperty("user.home")).getAbsolutePath();

        try (FileInputStream fis = new FileInputStream(home + "/.oai/" + configName)) {
            Properties props = new Properties();
            props.load(fis);
            String apiKey = props.getProperty(propertyName);
            if (apiKey == null) {
                System.err.println("NO PROPERTY " + propertyName);
                return "";
            }
            return apiKey;
        }
    }

    public static boolean isOpenAIComTestEnabled() {
        return checkConfig(AbstractKernelTest.CONF_OPENAI_PROPERTIES);
    }

    public static boolean isAzureTestEnabled() {
        return checkConfig(AbstractKernelTest.AZURE_CONF_PROPERTIES);
    }

    private static boolean checkConfig(String confOpenaiProperties) {
        if (!Boolean.getBoolean("enable_external_tests")
                && !System.getProperties().containsKey("intellij.debug.agent")) {
            return false;
        }

        try {
            if (AbstractKernelTest.getEndpoint(confOpenaiProperties) == null) {
                System.out.println("Test disabled due to lack of configured azure endpoint");
                return false;
            }

            if (AbstractKernelTest.getToken(confOpenaiProperties) == null) {
                System.out.println("Test disabled due to lack of configured azure token");
                return false;
            }
        } catch (IOException e) {
            return false;
        }

        return true;
    }
}
