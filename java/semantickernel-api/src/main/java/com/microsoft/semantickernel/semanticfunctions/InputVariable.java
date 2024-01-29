package com.microsoft.semantickernel.semanticfunctions;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.semantickernel.exceptions.SKException;

public class InputVariable {

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private String type;

    @JsonProperty("description")
    private String description;

    @JsonProperty("default")
    private String defaultValue;

    @JsonProperty("is_required")
    private boolean isRequired;

    public InputVariable(String name) {
        this.name = name;
    }

    public InputVariable(
        String name,
        String type,
        String description,
        String defaultValue,
        boolean isRequired) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.defaultValue = defaultValue;
        this.isRequired = isRequired;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public void setRequired(boolean required) {
        isRequired = required;
    }

    public Class<?> getTypeClass() {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(type);
        } catch (ClassNotFoundException e) {
            throw new SKException(
                "Could not load class for type: " + type + " when for input variable " + name +
                    ", note this needs to be a fully qualified class name, i.e 'java.lang.String'.");
        }
    }
}
