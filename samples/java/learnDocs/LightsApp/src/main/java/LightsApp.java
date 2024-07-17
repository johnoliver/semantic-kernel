// Copyright (c) Microsoft. All rights reserved.

import com.azure.core.credential.AzureKeyCredential;
import com.google.gson.Gson;
// <Imports>
import com.azure.ai.openai.*;
import com.microsoft.semantickernel.*;
import com.microsoft.semantickernel.aiservices.openai.chatcompletion.*;
import com.microsoft.semantickernel.contextvariables.*;
import com.microsoft.semantickernel.orchestration.*;
import com.microsoft.semantickernel.plugin.*;
import com.microsoft.semantickernel.services.chatcompletion.*;
// <Imports>
import java.util.List;
import java.util.Scanner;

public class LightsApp {

    private static final String AZURE_CLIENT_KEY = System.getenv("AZURE_CLIENT_KEY");
    private static final String CLIENT_ENDPOINT = System.getenv("CLIENT_ENDPOINT");
    private static final String MODEL_ID = System.getenv().getOrDefault("MODEL_ID", "gpt-4o");

    public static void main(String[] args) throws Exception {

        // <LightAppExample>
        OpenAIAsyncClient client = new OpenAIClientBuilder()
            .credential(new AzureKeyCredential(AZURE_CLIENT_KEY))
            .endpoint(CLIENT_ENDPOINT)
            .buildAsyncClient();

        // Create your AI service client
        ChatCompletionService chatCompletionService = OpenAIChatCompletion.builder()
            .withModelId(MODEL_ID)
            .withOpenAIAsyncClient(client)
            .build();

        // Import the LightsPlugin
        KernelPlugin lightPlugin = KernelPluginFactory.createFromObject(new LightsPlugin(),
            "LightsPlugin");

        // Create a kernel with Azure OpenAI chat completion and plugin
        Kernel kernel = Kernel.builder()
            .withAIService(ChatCompletionService.class, chatCompletionService)
            .withPlugin(lightPlugin)
            .build();

        // Add a converter to the kernel to show it how to serialise LightModel objects into a prompt
        ContextVariableTypes
            .addGlobalConverter(
                ContextVariableTypeConverter.builder(LightModel.class)
                    .toPromptString(new Gson()::toJson)
                    .build());

        // Enable planning
        InvocationContext invocationContext = new InvocationContext.Builder()
            .withReturnMode(InvocationReturnMode.LAST_MESSAGE_ONLY)
            .withToolCallBehavior(ToolCallBehavior.allowAllKernelFunctions(true))
            .build();

        // Create a history to store the conversation
        ChatHistory history = new ChatHistory();

        // Initiate a back-and-forth chat
        Scanner scanner = new Scanner(System.in);
        String userInput;
        do {
            // Collect user input
            System.out.print("User > ");
            userInput = scanner.nextLine();
            // Add user input
            history.addUserMessage(userInput);

            // Prompt AI for response to users input
            List<ChatMessageContent<?>> results = chatCompletionService
                .getChatMessageContentsAsync(history, kernel, invocationContext)
                .block();

            for (ChatMessageContent<?> result : results) {
                // Print the results
                if (result.getAuthorRole() == AuthorRole.ASSISTANT && result.getContent() != null) {
                    System.out.println("Assistant > " + result);
                }
                // Add the message from the agent to the chat history
                history.addMessage(result);
            }
        } while (userInput != null && !userInput.isEmpty());

        // </LightAppExample>
    }
}
