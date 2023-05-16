// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.orchestration;

import com.microsoft.semantickernel.builders.SKBuilders;
import com.microsoft.semantickernel.memory.NullMemory;
import com.microsoft.semantickernel.skilldefinition.ParameterView;
import com.microsoft.semantickernel.skilldefinition.ReadOnlySkillCollection;

import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public abstract class AbstractSkFunction<
                RequestConfiguration, ContextType extends SKContext<ContextType>>
        implements SKFunction<RequestConfiguration, ContextType>, RegistrableSkFunction {

    private final DelegateTypes delegateType;
    private final List<ParameterView> parameters;
    private final String skillName;
    private final String functionName;
    private final String description;
    @Nullable protected Supplier<ReadOnlySkillCollection> skillCollection;

    public AbstractSkFunction(
            AbstractSkFunction.DelegateTypes delegateType,
            List<ParameterView> parameters,
            String skillName,
            String functionName,
            String description,
            @Nullable Supplier<ReadOnlySkillCollection> skillCollection) {

        this.delegateType = delegateType;
        this.parameters = new ArrayList<>(parameters);
        this.skillName = skillName;
        this.functionName = functionName;
        this.description = description;
        this.skillCollection = skillCollection;
    }

    @Override
    public Mono<ContextType> invokeAsync(
            String input, @Nullable ContextType context, @Nullable RequestConfiguration settings) {
        if (context == null) {
            context =
                    buildContext(
                            SKBuilders.variables().build(),
                            NullMemory.getInstance(),
                            this.skillCollection);
        }

        context = context.update(input);

        return this.invokeAsync(context, settings);
    }

    @Override
    public Mono<ContextType> invokeAsync(String input) {
        return invokeAsync(input, null, null);
    }

    @Override
    public Mono<ContextType> invokeAsync(
            ContextType context, @Nullable RequestConfiguration settings) {
        // return new FutureTask<SKContext>(() -> function.run(null, settings, context));

        if (context == null) {
            context = buildContext(SKBuilders.variables().build(), NullMemory.getInstance(), null);
        }

        return this.invokeAsyncInternal(context, settings);
    }

    protected abstract Mono<ContextType> invokeAsyncInternal(
            ContextType context, @Nullable RequestConfiguration settings);

    @Override
    public String getSkillName() {
        return skillName;
    }

    @Override
    public String getName() {
        return functionName;
    }

    public DelegateTypes getDelegateType() {
        return delegateType;
    }

    public List<ParameterView> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    @Nullable
    public Supplier<ReadOnlySkillCollection> getSkillCollection() {
        return skillCollection;
    }

    public enum DelegateTypes {
        Unknown(0),
        Void(1),
        OutString(2),
        OutTaskString(3),
        InSKContext(4),
        InSKContextOutString(5),
        InSKContextOutTaskString(6),
        ContextSwitchInSKContextOutTaskSKContext(7),
        InString(8),
        InStringOutString(9),
        InStringOutTaskString(10),
        InStringAndContext(11),
        InStringAndContextOutString(12),
        InStringAndContextOutTaskString(13),
        ContextSwitchInStringAndContextOutTaskContext(14),
        InStringOutTask(15),
        InContextOutTask(16),
        InStringAndContextOutTask(17),
        OutTask(18);

        final int num;

        DelegateTypes(int num) {
            this.num = num;
        }
    }

    /**
     * The function to create a fully qualified name for
     *
     * @return A fully qualified name for a function
     */
    @Override
    public String toFullyQualifiedName() {
        return skillName + "." + functionName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String toEmbeddingString() {
        String inputs =
                parameters.stream()
                        .map(p -> "    - " + p.getName() + ": " + p.getDescription())
                        .collect(Collectors.joining("\n"));

        return getName() + ":\n  description: " + getDescription() + "\n  inputs:\n" + inputs;
    }

    @Override
    public String toManualString() {
        String inputs =
                parameters.stream()
                        .map(
                                parameter -> {
                                    String defaultValueString;
                                    if (parameter.getDefaultValue() == null
                                            || parameter.getDefaultValue().isEmpty()) {
                                        defaultValueString = "";
                                    } else {
                                        defaultValueString =
                                                " (default value: "
                                                        + parameter.getDefaultValue()
                                                        + ")";
                                    }

                                    return "  - "
                                            + parameter.getName()
                                            + ": "
                                            + parameter.getDescription()
                                            + defaultValueString;
                                })
                        .collect(Collectors.joining("\n"));

        return toFullyQualifiedName()
                + ":\n"
                + "  description: "
                + getDescription()
                + "\n"
                + "  inputs:\n"
                + inputs;
    }

    @Override
    public ContextType buildContext() {
        return buildContext(SKBuilders.variables().build(), null, skillCollection);
    }
}
