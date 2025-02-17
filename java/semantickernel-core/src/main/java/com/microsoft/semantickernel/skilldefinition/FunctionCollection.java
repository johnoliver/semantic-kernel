// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.skilldefinition;

import com.microsoft.semantickernel.orchestration.SKFunction;

import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

/** A collection of functions. */
public class FunctionCollection implements ReadOnlyFunctionCollection {
    private final CaseInsensitiveMap<SKFunction<?, ?>> functionCollection;

    private final String skillName;

    private FunctionCollection(String skillName, Map<String, SKFunction<?, ?>> functionCollection) {
        this.functionCollection = new CaseInsensitiveMap<>(functionCollection);
        this.skillName = skillName;
    }

    public FunctionCollection(
            String skillName, List<? extends SKFunction<?, ?>> functionCollection) {
        this(
                skillName,
                functionCollection.stream()
                        .collect(Collectors.toMap(it -> it.getName().toLowerCase(), it -> it)));
    }

    public FunctionCollection(String skillName) {
        this.skillName = skillName;
        this.functionCollection = new CaseInsensitiveMap<>();
    }

    public FunctionCollection(ReadOnlyFunctionCollection value) {
        this.skillName = value.getSkillName();
        this.functionCollection =
                new CaseInsensitiveMap<SKFunction<?, ?>>(
                        Collections.unmodifiableMap(
                                value.getAll().stream()
                                        .collect(Collectors.toMap(SKFunction::getName, it -> it))));
    }

    @Override
    public String getSkillName() {
        return skillName;
    }

    @Override
    public SKFunction<?, ?> getFunction(String functionName) {
        SKFunction<?, ?> func = functionCollection.get(functionName.toLowerCase());
        if (func == null) {
            throw new FunctionNotFound(functionName);
        }
        return func;
    }

    /**
     * Get function with the given SKFunction type argument.
     *
     * @param functionName the name of the function to retrieve
     * @param clazz The class of the SKFunction parameter type
     * @param <T> SKFunction parameter type
     * @return The given function
     * @throws RuntimeException if the given entry is not of the expected type
     */
    @Override
    public <T extends SKFunction> T getFunction(String functionName, @Nullable Class<T> clazz) {
        SKFunction<?, ?> func = getFunction(functionName);
        if (clazz == null) {
            return (T) func;
        }
        if (clazz.isInstance(func)) {
            return (T) func;
        } else {
            throw new RuntimeException("Incorrect type requested");
        }
    }

    /**
     * @return A clone of this collection
     */
    @Override
    @CheckReturnValue
    public FunctionCollection copy() {
        return new FunctionCollection(skillName, functionCollection);
    }

    /**
     * @return An unmodifiable list of all functions
     */
    @Override
    public List<SKFunction<?, ?>> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(functionCollection.values()));
    }

    /**
     * Add function to the collection overwriting existing function
     *
     * @param functionName
     * @param functionInstance
     * @return Collection for fluent callse
     */
    @CheckReturnValue
    public FunctionCollection put(String functionName, SKFunction<?, ?> functionInstance) {
        HashMap<String, SKFunction<?, ?>> functionCollection =
                new HashMap<>(this.functionCollection);
        functionCollection.put(functionName.toLowerCase(), functionInstance);

        return new FunctionCollection(skillName, functionCollection);
    }

    /**
     * Merge in the given collection to this one, duplicate function names will overwrite
     *
     * @param value
     * @return Collection for fluent calls
     */
    public FunctionCollection merge(FunctionCollection value) {
        HashMap<String, SKFunction<?, ?>> mutable = new HashMap<>(functionCollection);

        mutable.putAll(value.functionCollection);

        return new FunctionCollection(skillName, mutable);
    }
}
