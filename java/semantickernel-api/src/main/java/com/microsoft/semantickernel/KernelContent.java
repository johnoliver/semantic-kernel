package com.microsoft.semantickernel;

import com.microsoft.semantickernel.orchestration.contextvariables.ContextVariable;
import java.util.Map;
import javax.annotation.Nullable;

public abstract class KernelContent<T> {

    public KernelContent(
        T innerContent,
        @Nullable
        String modelId,
        @Nullable
        Map<String, ContextVariable<?>> metadata
    ) {
    }


    public abstract T getContent();

}
