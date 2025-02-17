﻿// Copyright (c) Microsoft. All rights reserved.

using System;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.SemanticKernel;
using Microsoft.SemanticKernel.AI.ChatCompletion;
using Microsoft.SemanticKernel.Connectors.AI.OpenAI.ChatCompletion;
using RepoUtils;

/**
 * The following example shows how to use Semantic Kernel with Text Completion as streaming
 */
// ReSharper disable once InconsistentNaming
public static class Example33_StreamingChat
{
    public static async Task RunAsync()
    {
        await AzureOpenAIChatStreamSampleAsync();
        await OpenAIChatStreamSampleAsync();
    }

    private static async Task OpenAIChatStreamSampleAsync()
    {
        Console.WriteLine("======== Open AI - ChatGPT Streaming ========");

        IKernel kernel = new KernelBuilder().WithLogger(ConsoleLogger.Log).Build();

        // Add your chat completion service
        kernel.Config.AddOpenAIChatCompletionService("gpt-3.5-turbo", Env.Var("OPENAI_API_KEY"));

        IChatCompletion chatGPT = kernel.GetService<IChatCompletion>();

        await StartStreamingChatAsync(chatGPT);
    }

    private static async Task AzureOpenAIChatStreamSampleAsync()
    {
        Console.WriteLine("======== Azure Open AI - ChatGPT Streaming ========");

        IKernel kernel = new KernelBuilder().WithLogger(ConsoleLogger.Log).Build();

        // Add your chat completion service
        kernel.Config.AddAzureChatCompletionService(
            Env.Var("AZURE_OPENAI_CHAT_DEPLOYMENT_NAME"),
            Env.Var("AZURE_OPENAI_ENDPOINT"),
            Env.Var("AZURE_OPENAI_KEY"));

        IChatCompletion chatGPT = kernel.GetService<IChatCompletion>();

        await StartStreamingChatAsync(chatGPT);
    }

    private static async Task StartStreamingChatAsync(IChatCompletion chatGPT)
    {
        Console.WriteLine("Chat content:");
        Console.WriteLine("------------------------");

        var chatHistory = (OpenAIChatHistory)chatGPT.CreateNewChat("You are a librarian, expert about books");
        await MessageOutputAsync(chatHistory);

        // First user message
        chatHistory.AddUserMessage("Hi, I'm looking for book suggestions");
        await MessageOutputAsync(chatHistory);

        // First bot assistant message
        await StreamMessageOutputAsync(chatGPT, chatHistory);

        // Second user message
        chatHistory.AddUserMessage("I love history and philosophy, I'd like to learn something new about Greece, any suggestion?");
        await MessageOutputAsync(chatHistory);

        // Second bot assistant message
        await StreamMessageOutputAsync(chatGPT, chatHistory);
    }

    private static async Task StreamMessageOutputAsync(IChatCompletion chatGPT, ChatHistory chatHistory,
        ChatHistory.AuthorRoles authorRole = ChatHistory.AuthorRoles.Assistant)
    {
        Console.Write($"{authorRole}: ");
        string fullMessage = string.Empty;

        await foreach (string message in chatGPT.GenerateMessageStreamAsync(chatHistory))
        {
            fullMessage += message;
            Console.Write(message);
        }

        Console.WriteLine("\n------------------------");
        chatHistory.AddMessage(authorRole, fullMessage);
    }

    /// <summary>
    /// Outputs the last message of the chat history
    /// </summary>
    private static Task MessageOutputAsync(ChatHistory chatHistory)
    {
        var message = chatHistory.Messages.Last();

        Console.WriteLine($"{message.AuthorRole}: {message.Content}");
        Console.WriteLine("------------------------");

        return Task.CompletedTask;
    }
}
