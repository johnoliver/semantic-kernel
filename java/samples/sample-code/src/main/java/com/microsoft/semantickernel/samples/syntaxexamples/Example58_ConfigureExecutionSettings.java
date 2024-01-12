package com.microsoft.semantickernel.samples.syntaxexamples;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.credential.KeyCredential;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.chatcompletion.ChatCompletionService;
import com.microsoft.semantickernel.orchestration.PromptExecutionSettings;
import com.microsoft.semantickernel.plugin.KernelFunctionFactory;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionFromPrompt;
import com.microsoft.semantickernel.semanticfunctions.PromptTemplateConfig;

public class Example58_ConfigureExecutionSettings {


    private static final boolean USE_AZURE_CLIENT = Boolean.parseBoolean(
        System.getenv("USE_AZURE_CLIENT"));
    private static final String CLIENT_KEY = System.getenv("CLIENT_KEY");

    // Only required if USE_AZURE_CLIENT is true
    private static final String CLIENT_ENDPOINT = System.getenv("CLIENT_ENDPOINT");

    public static void main(String[] args) {
        System.out.println("======== Example58_ConfigureExecutionSettings ========");

        OpenAIAsyncClient client;

        if (USE_AZURE_CLIENT) {
            client = new OpenAIClientBuilder()
                .credential(new AzureKeyCredential(CLIENT_KEY))
                .endpoint(CLIENT_ENDPOINT)
                .buildAsyncClient();
        } else {
            client = new OpenAIClientBuilder()
                .credential(new KeyCredential(CLIENT_KEY))
                .buildAsyncClient();
        }
        System.out.println("======== Using Chat GPT model for text generation ========");

        var openAIChatCompletion = ChatCompletionService.builder()
            .withOpenAIAsyncClient(client)
            .withModelId("gpt-35-turbo-2")
            .build();

        var kernel = Kernel.builder()
            .withDefaultAIService(ChatCompletionService.class, openAIChatCompletion)
            .build();

        var prompt = "Hello AI, what can you do for me?";

        // Option 1:
        // Invoke the prompt function and pass an OpenAI specific instance containing the execution settings
        var result = kernel.invokeAsync(
            KernelFunctionFromPrompt.builder()
                .withTemplate(prompt)
                .withDefaultExecutionSettings(
                    PromptExecutionSettings.builder()
                        .withMaxTokens(60)
                        .withTemperature(0.7)
                        .build()
                ).build(),
            null,
            String.class
        ).block();

        System.out.println(result.getValue());

        // Option 2:
        // Load prompt template configuration including the execution settings from a JSON payload
        // Create the prompt functions using the prompt template and the configuration (loaded in the previous step)
        // Invoke the prompt function using the implicitly set execution settings
        String configPayload = """
              {
              "schema": 1,
              "name": "HelloAI",
              "description": "Say hello to an AI",
              "type": "completion",
              "completion": {
                "max_tokens": 256,
                "temperature": 0.5,
                "top_p": 0.0,
                "presence_penalty": 0.0,
                "frequency_penalty": 0.0
              }
            }""".stripIndent();

        var promptConfig = PromptTemplateConfig.parseFromJson(configPayload);
        promptConfig.setTemplate(prompt);

        var func = KernelFunctionFactory.createFromPrompt(promptConfig, null);

        result = kernel.invokeAsync(func, null, String.class).block();
        System.out.println(result.getValue());
    }
}
