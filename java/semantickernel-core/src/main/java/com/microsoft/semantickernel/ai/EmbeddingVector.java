
package com.microsoft.semantickernel.ai;

import com.microsoft.semantickernel.ai.vectoroperations.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents a strongly typed vector of numeric data
 *
 * @param <TEmbedding>
 */
public class EmbeddingVector<TEmbedding extends Number> implements
        DotProduct<TEmbedding>,
        EuclideanLength,
        CosineSimilarity<TEmbedding>,
        Multiply<TEmbedding>,
        Divide<TEmbedding>,
        Normalize<TEmbedding> {
    private final List<TEmbedding> vector;

    public EmbeddingVector(List<TEmbedding> vector) {
        this.vector = Collections.unmodifiableList(vector);
    }

    public EmbeddingVector(TEmbedding[] vector) {
        this.vector = Collections.unmodifiableList(Arrays.asList(vector));
    }

    public EmbeddingVector() {
        this.vector = Collections.unmodifiableList(new ArrayList<>());
    }

    /**
     * Size of the vector
     * @return Vector's size
     */
    public int size() {
        return this.vector.size();
    }

    public TEmbedding[] toArray() {
        return (TEmbedding[]) this.vector.toArray();
    }

    /**
     * Calculates the dot product of this vector with another.
     *
     * @param other The other vector to compute the dot product with
     * @return
     */
    @Override
    public double dot(EmbeddingVector<TEmbedding> other) {
        if (this.size() != other.size()) {
            throw new IllegalArgumentException("Vectors lengths must be equal");
        }

        TEmbedding[] x = this.toArray(), y = other.toArray();
        double result = 0;
        for (int i = 0; i < this.size(); ++i) {
            result += x[i].doubleValue() * y[i].doubleValue();
        }

        return result;
    }


    /**
     * Calculates the Euclidean length of this vector.
     *
     * @return Euclidean length
     */
    @Override
    public double euclideanLength() {
        return Math.sqrt(this.dot(this));
    }

    /**
     * Calculates the cosine similarity of this vector with another.
     *
     * @param other The other vector to compute cosine similarity with.
     * @return Cosine similarity between vectors
     */
    @Override
    public double cosineSimilarity(EmbeddingVector<TEmbedding> other) {
        if (this.size() != other.size()) {
            throw new IllegalArgumentException("Vectors lengths must be equal");
        }

        double dotProduct = this.dot(other);
        double normX = this.dot(this);
        double normY = other.dot(other);

        if (normX == 0 || normY == 0) {
            throw new IllegalArgumentException("Vectors cannot have zero norm");
        }

        return dotProduct / (Math.sqrt(normX) * Math.sqrt(normY));
    }

    @Override
    public EmbeddingVector<TEmbedding> multiply(double multiplier) {
        Double[] x = (Double[]) this.toArray();

        for (int i = 0; i < this.size(); ++i) {
            x[i] *= multiplier;
        }

        return new EmbeddingVector<>((TEmbedding[]) x);
    }

    @Override
    public EmbeddingVector<TEmbedding> divide(double divisor) {
        if (divisor == 0) {
            throw new IllegalArgumentException("Divisor cannot be zero");
        }

        Double[] x = (Double[]) this.toArray();

        for (int i = 0; i < this.size(); ++i) {
            x[i] /= divisor;
        }

        return new EmbeddingVector<>((TEmbedding[]) x);
    }

    /**
     * Normalizes the underlying vector, such that the Euclidean length is 1.
     *
     * @return Normalized embedding
     */
    @Override
    public EmbeddingVector<TEmbedding> normalize() {
        return this.divide(this.euclideanLength());
    }
}
