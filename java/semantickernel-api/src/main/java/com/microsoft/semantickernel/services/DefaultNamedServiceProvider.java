// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.services;

import com.microsoft.semantickernel.Verify;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

public class DefaultNamedServiceProvider<T> implements NamedServiceProvider<T> {

    // A dictionary that maps a service type to a nested dictionary of names and service instances
    // or factories
    // private readonly Dictionary<Type, Dictionary<string, object>> _services = new();
    private final Map<Class<? extends T>, Map<String, Supplier<? extends T>>> services;

    // A dictionary that maps a service type to the name of the default service
    private final Map<Class<? extends T>, String> defaultIds;

    public DefaultNamedServiceProvider(
            Map<Class<? extends T>, Map<String, Supplier<? extends T>>> services,
            Map<Class<? extends T>, String> defaultIds) {
        this.services = AIServiceCollection.cloneServices(services);
        this.defaultIds = new HashMap<>(defaultIds);
    }

    @Nullable
    @Override
    public <U extends T> U getService(@Nullable String name, Class<U> clazz) {
        // Return the service, casting or invoking the factory if needed
        Supplier<U> factory = this.getServiceFactory(name, clazz);
        if (factory != null) {
            return factory.get();
        }

        return null;
    }

    /// <inheritdoc/>
    @Nullable
    private <U extends T> String getDefaultServiceName(Class<U> clazz) {
        // Returns the name of the default service for the given type, or null if none
        String name = this.defaultIds.get(clazz);
        if (!Verify.isNullOrEmpty(name)) {
            return name;
        }

        return null;
    }

    @Nullable
    private <U extends T> Supplier<U> getServiceFactory(@Nullable String name, Class<U> clazz) {
        // Get the nested dictionary for the service type
        Map<String, Supplier<? extends T>> namedServices = services.get(clazz);

        if (namedServices != null) {
            Supplier<U> serviceFactory = null;

            // If the name is not specified, try to load the default factory
            name = name == null ? this.getDefaultServiceName(clazz) : name;
            if (name != null) {
                // Check if there is a service registered with the given name
                serviceFactory = (Supplier<U>) namedServices.get(name);
            }

            return serviceFactory;
        }

        return null;
    }
}
