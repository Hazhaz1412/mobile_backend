package com.react.mobile.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
@Slf4j
public class GoogleIdTokenVerifierService {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    public GoogleIdToken verify(String idTokenString, String googleClientId)
            throws GeneralSecurityException, IOException {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                JSON_FACTORY)
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        try {
            return verifier.verify(idTokenString);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid Google ID token format");
            return null;
        }
    }
}
