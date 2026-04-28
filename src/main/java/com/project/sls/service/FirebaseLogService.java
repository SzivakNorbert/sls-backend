package com.project.sls.service;

import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class FirebaseLogService {
    private final Firestore firestore;

    public FirebaseLogService(Firestore firestore) {
        this.firestore = firestore;
    }

    public void write(String collection, Map<String, Object> payload) {
        payload.putIfAbsent("ts", Instant.now().toString());
        firestore.collection(collection).add(payload); // async write
    }
}