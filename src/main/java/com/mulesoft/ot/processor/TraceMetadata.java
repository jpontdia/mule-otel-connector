package com.mulesoft.ot.processor;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import java.util.Map;

public class TraceMetadata {

    String correlationId;
    String name;
    String spanName;
    String location;
    Context context;
    SpanKind spanKind;
    String errorMessage;
    Map<String, String> tags;
    StatusCode statusCode;

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSpanName(String spanName) {
        this.spanName = spanName;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setSpanKind(SpanKind spanKind) {
        this.spanKind = spanKind;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public void setStatusCode(StatusCode statusCode) {
        this.statusCode = statusCode;
    }

    public SpanKind getSpanKind() {
        return spanKind;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public String getName() {
        return name;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getSpanName() {
        return spanName;
    }

    public Context getContext() {
        return context;
    }

    public String getLocation() {
        return location;
    }

    public StatusCode getStatusCode() {
        return statusCode;
    }
}
