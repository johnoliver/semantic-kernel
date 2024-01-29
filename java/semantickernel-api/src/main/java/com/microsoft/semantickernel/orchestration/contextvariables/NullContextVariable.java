package com.microsoft.semantickernel.orchestration.contextvariables;

public class NullContextVariable<T> extends ContextVariable<T> {

    public NullContextVariable(ContextVariableType<T> type) {
        super(type, null);
    }
}
