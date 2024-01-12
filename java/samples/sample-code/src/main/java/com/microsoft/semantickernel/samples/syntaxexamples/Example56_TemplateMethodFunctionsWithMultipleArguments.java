package com.microsoft.semantickernel.samples.syntaxexamples;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.credential.KeyCredential;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.chatcompletion.ChatCompletionService;
import com.microsoft.semantickernel.orchestration.PromptExecutionSettings;
import com.microsoft.semantickernel.orchestration.contextvariables.KernelArguments;
import com.microsoft.semantickernel.plugin.KernelPluginFactory;
import com.microsoft.semantickernel.samples.plugins.text.TextPlugin;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionFromPrompt;
import com.microsoft.semantickernel.semanticfunctions.KernelPromptTemplateFactory;
import com.microsoft.semantickernel.semanticfunctions.PromptTemplateConfig;

public class Example56_TemplateMethodFunctionsWithMultipleArguments {

    private static final boolean USE_AZURE_CLIENT = Boolean.parseBoolean(
        System.getenv("USE_AZURE_CLIENT"));
    private static final String CLIENT_KEY = System.getenv("CLIENT_KEY");

    // Only required if USE_AZURE_CLIENT is true
    private static final String CLIENT_ENDPOINT = System.getenv("CLIENT_ENDPOINT");

    public static void main(String[] args) {

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

        ChatCompletionService openAIChatCompletion = ChatCompletionService.builder()
            .withOpenAIAsyncClient(client)
            .withModelId("gpt-35-turbo-2")
            .build();

        Kernel kernel = Kernel.builder()
            .withDefaultAIService(ChatCompletionService.class, openAIChatCompletion)
            .build();

        System.out.println("======== TemplateMethodFunctionsWithMultipleArguments ========");

        var arguments = KernelArguments.builder()
            .withVariable("word2", " Potter")
            .build();

        // Load native plugin into the kernel function collection, sharing its functions with prompt templates
        // Functions loaded here are available as "text.*"
        kernel.getPlugins().add(KernelPluginFactory.createFromObject(new TextPlugin(), "text"));

        // Prompt Function invoking text.Concat method function with named arguments input and input2 where input is a string and input2 is set to a variable from context called word2.
        String functionDefinition = "Write a haiku about the following: {{text.Concat input='Harry' input2=$word2}}";

        // This allows to see the prompt before it's sent to OpenAI
        System.out.println("--- Rendered Prompt");
        var promptTemplateFactory = new KernelPromptTemplateFactory();
        var promptTemplate = promptTemplateFactory.tryCreate(
            new PromptTemplateConfig(functionDefinition));
        var renderedPrompt = promptTemplate.renderAsync(kernel, arguments).block();
        System.out.println(renderedPrompt);

        // Run the prompt / prompt function

        var haiku = KernelFunctionFromPrompt
            .builder()
            .withTemplate(functionDefinition)
            .withDefaultExecutionSettings(
                PromptExecutionSettings.builder()
                    .withMaxTokens(100)
                    .build())
            .build();

        // Show the result
        System.out.println("--- Prompt Function result");
        var result = kernel.invokeAsync(haiku, arguments, String.class).block();
        System.out.println(result.getValue());
    }

}
