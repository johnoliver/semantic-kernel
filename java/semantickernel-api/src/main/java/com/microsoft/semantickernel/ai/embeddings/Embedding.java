// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.ai.embeddings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a strongly typed vector of numeric data.
 *
 * @param <EmbeddingType>
 */
public class Embedding<EmbeddingType extends Number> {

    public List<EmbeddingType> getVector() {
        return Collections.unmodifiableList(vector);
    }

    private final List<EmbeddingType> vector;

    //    /// <summary>
    //    /// An empty <see cref="Embedding{TEmbedding}"/> instance.
    //    /// </summary>
    //    [SuppressMessage("Design", "CA1000:Do not declare static members on generic types",
    // Justification = "Static empty struct instance.")]
    //    public static Embedding<TEmbedding> Empty { get; } = new
    // Embedding<TEmbedding>(Array.Empty<TEmbedding>());

    private static final Embedding<Number> EMPTY =
            new Embedding(Collections.unmodifiableList(new ArrayList<>()));

    @SuppressWarnings("unchecked")
    public static <EmbeddingType extends Number> Embedding<EmbeddingType> empty() {
        return (Embedding<EmbeddingType>) EMPTY;
    }

    //
    //    /// <summary>
    //    /// Initializes a new instance of the <see cref="Embedding{TEmbedding}"/> class that
    // contains numeric elements copied from the specified collection.
    //    /// </summary>
    //    /// <exception cref="ArgumentException">Type <typeparamref name="TEmbedding"/> is
    // unsupported.</exception>
    //    /// <exception cref="ArgumentNullException">A <c>null</c> vector is passed in.</exception>
    public Embedding() {
        this.vector = new ArrayList<>();
    }

    /**
     * Initializes a new instance of the <see cref="Embedding{TEmbedding}"/> class that contains
     * numeric elements copied from the specified collection
     *
     * @param vector
     */
    public Embedding(List<EmbeddingType> vector) {
        //        Verify.NotNull(vector, nameof(vector));
        this.vector = Collections.unmodifiableList(vector);
    }
}
