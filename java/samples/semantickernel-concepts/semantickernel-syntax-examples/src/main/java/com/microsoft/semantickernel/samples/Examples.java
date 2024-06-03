package com.microsoft.semantickernel.samples;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.credential.KeyCredential;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.aiservices.openai.chatcompletion.OpenAIChatCompletion;
import com.microsoft.semantickernel.aiservices.openai.chatcompletion.OpenAIChatMessageContent;
import com.microsoft.semantickernel.aiservices.openai.chatcompletion.OpenAIFunctionToolCall;
import com.microsoft.semantickernel.hooks.KernelHooks;
import com.microsoft.semantickernel.implementation.EmbeddedResourceLoader;
import com.microsoft.semantickernel.orchestration.InvocationContext;
import com.microsoft.semantickernel.orchestration.PromptExecutionSettings;
import com.microsoft.semantickernel.orchestration.ToolCallBehavior;
import com.microsoft.semantickernel.plugin.KernelPluginFactory;
import com.microsoft.semantickernel.samples.openapi.SemanticKernelOpenAPIImporter;
import com.microsoft.semantickernel.samples.syntaxexamples.functions.Example03_Arguments.StaticTextPlugin;
import com.microsoft.semantickernel.semanticfunctions.KernelFunction;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionArguments;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionFromPrompt;
import com.microsoft.semantickernel.services.audio.TextToAudioService;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;
import com.microsoft.semantickernel.services.chatcompletion.ChatHistory;
import com.microsoft.semantickernel.services.textcompletion.TextGenerationService;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.List;

public class Examples {

    private static final String CLIENT_KEY = System.getenv("CLIENT_KEY");
    private static final String AZURE_CLIENT_KEY = System.getenv("AZURE_CLIENT_KEY");

    // Only required if AZURE_CLIENT_KEY is set
    private static final String CLIENT_ENDPOINT = System.getenv("CLIENT_ENDPOINT");
    private static final String MODEL_ID = System.getenv()
        .getOrDefault("MODEL_ID", "gpt-35-turbo");


    public static void main(String[] args) throws FileNotFoundException {
        two();
        three();
        four();
        six();
        seven();
        eight();
        nine();
        ten();
    }

    private static Kernel getKernel() {

        OpenAIAsyncClient client;

        if (AZURE_CLIENT_KEY != null) {
            client = new OpenAIClientBuilder()
                .credential(new AzureKeyCredential(AZURE_CLIENT_KEY))
                .endpoint(CLIENT_ENDPOINT)
                .buildAsyncClient();

        } else {
            client = new OpenAIClientBuilder()
                .credential(new KeyCredential(CLIENT_KEY))
                .buildAsyncClient();
        }

        ChatCompletionService openAIChatCompletion = OpenAIChatCompletion.builder()
            .withOpenAIAsyncClient(client)
            .withModelId(MODEL_ID)
            .build();

        return Kernel.builder()
            .withAIService(ChatCompletionService.class, openAIChatCompletion)
            .build();
    }

    public static void two() {

        ///////////////////////////////////////////////////////////////

        // Create client
        var client = new OpenAIClientBuilder()
            .credential(new AzureKeyCredential(AZURE_CLIENT_KEY))
            .endpoint(CLIENT_ENDPOINT)
            .buildAsyncClient();

        // Chat completion example
        var openAIChatCompletion = OpenAIChatCompletion.builder()
            .withOpenAIAsyncClient(client)
            .withModelId(MODEL_ID)
            .build();

        // Text generation example
        var textGenerationService = TextGenerationService.builder()
            .withOpenAIAsyncClient(client)
            .withModelId(MODEL_ID)
            .build();

        // Audio example
        var textToAudioService = TextToAudioService.builder()
            .withModelId("tts-1")
            .withOpenAIAsyncClient(client)
            .build();

        // Setup and prompt execution settings needed for configuring an invocation
        var settings = PromptExecutionSettings.builder()
            .withMaxTokens(4096)
            .withTemperature(0.1)
            .withTopP(0.5)
            .build();

        ///////////////////////////////////////////////////////////////
    }

    public static void three() {
        Kernel kernel = getKernel();
        // Initialize function
        String functionPrompt = "Write a paragraph about Handlers.";

        var writerFunction = KernelFunctionFromPrompt.builder()
            .withTemplate(functionPrompt)
            .withName("Writer")
            .withDefaultExecutionSettings(PromptExecutionSettings
                .builder()
                .withMaxTokens(100)
                .withTemperature(0.4)
                .withTopP(1)
                .build())
            .build();

        ///////////////////////////////////////////////////////////////
        KernelHooks kernelHooks = new KernelHooks();
        kernelHooks.addPreChatCompletionHook(event -> {
            // PROCESS EVENT
            return event;
        });

        // Invoke prompt to trigger execution hooks.
        var result = kernel.invokeAsync(writerFunction)
            .withArguments(KernelFunctionArguments.builder().build())
            .addKernelHooks(kernelHooks)
            .block();
        ///////////////////////////////////////////////////////////////
    }

    public static void four() {
        var client = new OpenAIClientBuilder()
            .credential(new AzureKeyCredential(AZURE_CLIENT_KEY))
            .endpoint(CLIENT_ENDPOINT)
            .buildAsyncClient();

        var openAIChatCompletion = OpenAIChatCompletion.builder()
            .withOpenAIAsyncClient(client)
            .withModelId(MODEL_ID)
            .build();

        var textGenerationService = TextGenerationService.builder()
            .withOpenAIAsyncClient(client)
            .withModelId(MODEL_ID)
            .build();

        ////////////////////////////////////////////////////////////////////
        Kernel.builder()
            .withAIService(ChatCompletionService.class, openAIChatCompletion)
            .withAIService(TextGenerationService.class, textGenerationService)
            .build();
        ////////////////////////////////////////////////////////////////////
    }


    public static void six() throws FileNotFoundException {

        Kernel kernel = getKernel();

        String yaml = EmbeddedResourceLoader.readFile("petstore.yaml", Examples.class);

        var args = KernelFunctionArguments.builder()
            .withVariable("input", "foo")
            .build();

        ////////////////////////////////////////////////
        var openApiPlugin = SemanticKernelOpenAPIImporter.builder()
            .withPluginName("petstore")
            .withSchema(yaml)
            .withServer("http://localhost:8090/api/v3")
            .build();

        var textPlugins = KernelPluginFactory
            .createFromObject(new StaticTextPlugin(), "text");

        var summarize = KernelPluginFactory
            .importPluginFromDirectory(
                Path.of("Plugins"),
                "SummarizePlugin",
                null);

        var result = kernel.invokeAsync(textPlugins.get("Uppercase"))
            .withArguments(args)
            .block();
        ////////////////////////////////////////////////
    }

    public static void seven() {

        var kernel = getKernel();
        var invocationContext = InvocationContext.builder().build();
        var userInput = "user input";

        var client = new OpenAIClientBuilder()
            .credential(new AzureKeyCredential(AZURE_CLIENT_KEY))
            .endpoint(CLIENT_ENDPOINT)
            .buildAsyncClient();

        var openAIChatCompletion = OpenAIChatCompletion.builder()
            .withOpenAIAsyncClient(client)
            .withModelId(MODEL_ID)
            .build();

        ////////////////////////////////////////////////

        var arguments = KernelFunctionArguments.builder()
            .withInput("Today is: ")
            .withVariable("day", "Monday")
            .build();

        ChatHistory chatHistory = new ChatHistory();

        var result = openAIChatCompletion
            .getChatMessageContentsAsync(
                chatHistory,
                kernel,
                invocationContext)
            .block();

        chatHistory.addUserMessage(userInput);

        ////////////////////////////////////////////////
    }


    public static void eight() {

        var kernel = getKernel();

        ////////////////////////////////////////////////

        // Basic Prompt
        var function = KernelFunction.createFromPrompt("My name is '{{$input}}'.").build();

        // Handle Bars Prompt Templates
        var handlebarsFunction = KernelFunctionFromPrompt.<String>builder()
            .withTemplate("My name is {{input}}.")
            .withTemplateFormat("handlebars")
            .build();

        // Templated Prompt - pull in dynamic content
        kernel.invokeAsync(function)
            .withArguments(KernelFunctionArguments.builder()
                .withVariable("input", "Steven")
                .build())
            .block();

        ////////////////////////////////////////////////
    }


    public static void nine() {

        var kernel = getKernel();
        var client = new OpenAIClientBuilder()
            .credential(new AzureKeyCredential(AZURE_CLIENT_KEY))
            .endpoint(CLIENT_ENDPOINT)
            .buildAsyncClient();

        var chat = OpenAIChatCompletion.builder()
            .withOpenAIAsyncClient(client)
            .withModelId(MODEL_ID)
            .build();

        ////////////////////////////////////////////////
        var plan = chat.getChatMessageContentsAsync(
                "What time is it in Paris?",
                kernel,
                InvocationContext.builder()
                    .withToolCallBehavior(ToolCallBehavior.allowAllKernelFunctions(false))
                    .build())
            .block();

        List<OpenAIFunctionToolCall> toolCalls = plan
            .stream()
            .filter(it -> it instanceof OpenAIChatMessageContent)
            .flatMap(it -> ((OpenAIChatMessageContent) it).getToolCall().stream())
            .toList();
        ////////////////////////////////////////////////
    }

    public static void ten() {
        var kernel = getKernel();
        String excuseFunction = "";

        ////////////////////////////////////////////////
        var result = kernel.invokePromptAsync(excuseFunction)
            .withToolCallBehavior(ToolCallBehavior.allowAllKernelFunctions(true))
            .block();
        ////////////////////////////////////////////////
    }
}