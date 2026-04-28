package com.project.sls.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class FirebaseLogAppender extends AppenderBase<ILoggingEvent> {

    private String collection = "sls_logs";
    private String environment = "dev";
    private Firestore firestore;

    // logback XML property setterek
    public void setCollection(String collection) { this.collection = collection; }
    public void setEnvironment(String environment) { this.environment = environment; }

    @Override
    public void start() {
        try {
            this.firestore = FirestoreClient.getFirestore(); // FirebaseApp init már megvan máshol
            super.start();
        } catch (Exception e) {
            addError("FirebaseLogAppender start failed", e);
        }
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (firestore == null) return;

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("ts", Instant.now().toString());
        doc.put("level", event.getLevel().toString());
        doc.put("logger", event.getLoggerName());
        doc.put("thread", event.getThreadName());
        doc.put("message", event.getFormattedMessage());
        doc.put("environment", environment);

        if (event.getThrowableProxy() != null) {
            doc.put("exception", event.getThrowableProxy().getClassName());
            doc.put("stacktrace", event.getThrowableProxy().getMessage());
        }

        firestore.collection(collection).add(doc);
    }
}