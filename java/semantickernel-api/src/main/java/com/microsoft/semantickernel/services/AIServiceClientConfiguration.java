package com.microsoft.semantickernel.services;

public interface AIServiceClientConfiguration<T> {

    /**
     * Builds an instance of the Ai client.
     *
     * @return an instance of the Ai client.
     */
    T buildAsyncClient();
}
