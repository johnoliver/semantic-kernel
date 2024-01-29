package com.microsoft.semantickernel.orchestration;

import com.microsoft.semantickernel.plugin.KernelParameterMetadata;
import com.microsoft.semantickernel.plugin.KernelReturnParameterMetadata;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionFromMethod.ImplementationFunc;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MethodDetails {

    private final String name;
    private final String description;
    private final ImplementationFunc function;
    private final List<KernelParameterMetadata<?>> parameters;
    private final KernelReturnParameterMetadata<?> returnParameter;

    public MethodDetails(
        String name,
        String description,
        ImplementationFunc function,
        List<KernelParameterMetadata<?>> parameters,
        KernelReturnParameterMetadata<?> returnParameter) {
        this.name = name;
        this.description = description;
        this.function = function;
        this.parameters = new ArrayList<>(parameters);
        this.returnParameter = returnParameter;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ImplementationFunc getFunction() {
        return function;
    }

    public List<KernelParameterMetadata<?>> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    public KernelReturnParameterMetadata<?> getReturnParameter() {
        return returnParameter;
    }
}
