package com.mulesoft.ot.tracevault;

import com.mulesoft.ot.Constants;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Creates the connection for the OpenTelemetry connector
 *
 * <p>
 * The configuration of the library is based:
 * github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure
 *
 * <p>
 * The guide to configure the library for Manual Instrumentation
 * opentelemetry.io/docs/instrumentation/java/manual/
 */
public class OtelConnection implements ContextPropagation {

    private final Logger log = LoggerFactory.getLogger(OtelConnection.class);
    private final TraceVault traceVault;
    private static OtelConnection otelConnection;
    private final OpenTelemetry openTelemetry;
    private final Tracer tracer;

    /*
     * Set the configuration for the Open Telemetry library
     */
    private OtelConnection(String serviceName, String additionalTags, String collectorEndpoint) {
        final Map<String, String> configuration = new HashMap<>();

        configuration.put(Constants.OTEL_METRICS_EXPORTER, Constants.NONE);
        configuration.put(Constants.OTEL_TRACES_EXPORTER, Constants.OTLP);
        if (serviceName != null && !serviceName.trim().isEmpty()) {
            configuration.put(Constants.OTEL_SERVICE_NAME, serviceName);
        }
        if (additionalTags != null && !additionalTags.trim().isEmpty()) {
            configuration.put(Constants.OTEL_RESOURCE_ATTRIBUTES, additionalTags);
        }
        if (collectorEndpoint != null && !collectorEndpoint.trim().isEmpty()) {
            configuration.put(Constants.OTEL_EXPORTER_OTLP_ENDPOINT, collectorEndpoint);
        }
        AutoConfiguredOpenTelemetrySdkBuilder builder = AutoConfiguredOpenTelemetrySdk.builder()
                .addPropertiesSupplier(() -> Collections.unmodifiableMap(configuration));
        log.debug("Open Telemetry connector configuration: {}", configuration);

        builder.setServiceClassLoader(AutoConfiguredOpenTelemetrySdkBuilder.class.getClassLoader());
        openTelemetry = builder.build().getOpenTelemetrySdk();
        tracer = openTelemetry.getTracer(Constants.LIBRARY_NAME, Constants.LIBRARY_VERSION);
        traceVault = TraceVault.getInstance();
    }

    public static Optional<OtelConnection> get() {
        return Optional.ofNullable(otelConnection);
    }

    public static synchronized OtelConnection getInstance(String serviceName, String additionalTags,
            String collectorEndpoint) {
        if (otelConnection == null) {
            otelConnection = new OtelConnection(serviceName, additionalTags, collectorEndpoint);
        }
        return otelConnection;
    }

    public SpanBuilder spanBuilder(String spanName) {
        return tracer.spanBuilder(spanName);
    }

    public <T> Context get(T carrier, TextMapGetter<T> textMapGetter) {
        return openTelemetry.getPropagators().getTextMapPropagator().extract(Context.current(), carrier, textMapGetter);
    }

    /** Creates a transaction context for transactionId */
    public Map<String, String> getTraceContext(String transactionId) {
        Context transactionContext = getTraceVault().getContext(transactionId);
        Map<String, String> traceContext = new HashMap<>();
        traceContext.put(Constants.TRACE_CORRELATION_ID, transactionId);
        traceContext.put(Constants.TRACE_ID, getTraceVault().getTraceIdForTransaction(transactionId));
        try (Scope ignored = transactionContext.makeCurrent()) {
            set(traceContext, HashMapTextMapSetter.INSTANCE);
        }
        log.debug("Create a transaction context: {}", traceContext);
        return Collections.unmodifiableMap(traceContext);
    }

    public <T> void set(T carrier, TextMapSetter<T> textMapSetter) {
        openTelemetry.getPropagators().getTextMapPropagator().inject(Context.current(), carrier, textMapSetter);
    }

    public TraceVault getTraceVault() {
        return traceVault;
    }

    public enum HashMapTextMapSetter implements TextMapSetter<Map<String, String>> {
        INSTANCE;

        @Override
        public void set(@Nullable Map<String, String> carrier, String key, String value) {
            if (carrier != null)
                carrier.put(key, value);
        }
    }
}
