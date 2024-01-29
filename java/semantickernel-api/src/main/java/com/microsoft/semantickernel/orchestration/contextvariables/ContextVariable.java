// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.orchestration.contextvariables;

import javax.annotation.Nullable;

public class ContextVariable<T> {

    private final ContextVariableType<T> type;

    @Nullable
    private final T value;

    public ContextVariable(ContextVariableType<T> type,
        @Nullable T value) {
        this.type = type;
        this.value = value;
    }

    public static <T> ContextVariable<T> of(Class<T> type, @Nullable T t) {
        if (t == null) {
            return new ContextVariable<>(
                ContextVariableTypes.getDefaultVariableTypeForClass(type),
                null);
        }

        return of(t);
    }

    @Nullable
    public T getValue() {
        return value;
    }

    @Nullable
    public <U> U getValue(Class<U> clazz) {
        if (value == null || clazz.isAssignableFrom(value.getClass())) {
            return clazz.cast(value);
        } else {
            throw new RuntimeException("Cannot cast " + value.getClass() + " to " + clazz);
        }
    }


    public ContextVariableType<T> getType() {
        return type;
    }

    public String toPromptString(ContextVariableTypeConverter<T> converter) {
        return converter.toPromptString(value);
    }

    public String toPromptString() {
        return toPromptString(type.getConverter());
    }

    public boolean isEmpty() {
        return value == null || value.toString().isEmpty();
    }

    public ContextVariable<T> cloneVariable() {
        return new ContextVariable<>(type, value);
    }

    @SuppressWarnings("unchecked")
    public static <T> ContextVariable<T> of(T value) {
        ContextVariableType<T> type = ContextVariableTypes.getDefaultVariableTypeForClass(
            (Class<T>) value.getClass());
        return new ContextVariable<>(type, value);
    }

    public static <T> ContextVariable<T> of(T value, ContextVariableTypeConverter<T> converter) {
        ContextVariableType<T> type = new ContextVariableType<>(converter, converter.getType());
        return new ContextVariable<>(type, value);
    }
}
