package com.microsoft.semantickernel;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.microsoft.semantickernel.ai.embeddings.EmbeddingGeneration;
import com.microsoft.semantickernel.builders.SKBuilders;
import com.microsoft.semantickernel.connectors.ai.openai.util.AIProviderSettings;
import com.microsoft.semantickernel.connectors.ai.openai.util.AzureOpenAISettings;
import com.microsoft.semantickernel.memory.MemoryStore;
import com.microsoft.semantickernel.memory.NullMemory;
import com.microsoft.semantickernel.memory.SemanticTextMemory;
import com.microsoft.semantickernel.orchestration.SKContext;
import com.microsoft.semantickernel.orchestration.SKFunction;
import com.microsoft.semantickernel.skilldefinition.annotations.DefineSKFunction;
import com.microsoft.semantickernel.skilldefinition.annotations.SKFunctionInputAttribute;
import com.microsoft.semantickernel.skilldefinition.annotations.SKFunctionParameters;
import com.microsoft.semantickernel.textcompletion.TextCompletion;

import java.io.IOException;
import java.nio.file.Paths;

public class Example_IsolatedRequest {
    private final Kernel kernel;
    private final OpenAIAsyncClient client;

    public static void main(String[] args) throws IOException {
        Example_IsolatedRequest example = new Example_IsolatedRequest();

        // Simulate HTTP request
        System.out.println(example.toUpper("convert this"));
    }

    // Mock HTTP endpoint
    // @Post("/toUpper/{text}")
    public String toUpper(
            //@PathParam("text")
            String text
    ) {
        // Build new context with isolated memory
        SKContext context = buildContext();

        SKFunction<?> skill = context.getSkills().getFunctions("StringFunctions").getFunction("toUpper");

        return skill.invokeAsync(text, context, null).block().getResult();
    }

    private SKContext buildContext() {
        SemanticTextMemory textMemory = createIsolatedMemory();

        // Build context with isolated memory and the current skills on the kernel
        return SKBuilders
                .context()
                .with(textMemory)
                .with(kernel.getSkills())
                .build();
    }

    private SemanticTextMemory createIsolatedMemory() {
        // Create new memory stores
        MemoryStore isolatedMemory = SKBuilders.memoryStore().build();
        EmbeddingGeneration<String, Float> isolatedEmbeddedGeneration = SKBuilders.textEmbeddingGenerationService().build(client, "text-embedding-ada-002");
        SemanticTextMemory textMemory = SKBuilders.semanticTextMemory()
                .setStorage(isolatedMemory)
                .setEmbeddingGenerator(isolatedEmbeddedGeneration)
                .build();
        return textMemory;
    }


    Example_IsolatedRequest() throws IOException {
        this.client = buildClient();
        this.kernel = buildKernel(client);
    }

    private static OpenAIAsyncClient buildClient() throws IOException {

        AzureOpenAISettings settings = AIProviderSettings.getAzureOpenAISettingsFromFile(
                Paths.get(System.getProperty("user.home"), ".sk", "conf.properties").toAbsolutePath().toString()
        );

        OpenAIAsyncClient client =
                new OpenAIClientBuilder()
                        .endpoint(settings.getEndpoint())
                        .credential(new AzureKeyCredential(settings.getKey()))
                        .buildAsyncClient();

        return client;
    }

    private static Kernel buildKernel(OpenAIAsyncClient client) {
        TextCompletion textCompletionService = SKBuilders.textCompletionService().build(client, "text-davinci-003");

        KernelConfig config = SKBuilders.kernelConfig()
                .addTextCompletionService("davinci",
                        kernel -> textCompletionService)
                .build();

        // Explicitly set memory to null to ensure that the kernel does not use the default memory
        Kernel kernel = SKBuilders.kernel()
                .withKernelConfig(config)
                .withMemory( new NullMemory())
                .build();

        kernel.importSkill(new StringFunctions(), "StringFunctions");

        return kernel;
    }

    public static class StringFunctions {
        @DefineSKFunction(
                name = "toUpper",
                description = "Upper cases a string")
        public String toUpper(
                @SKFunctionInputAttribute
                @SKFunctionParameters(name = "input", description = "The string to upper case")
                String input
        ) {
            return input.toUpperCase();
        }
    }

}

