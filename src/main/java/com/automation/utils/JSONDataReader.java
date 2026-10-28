package com.automation.utils;

import com.automation.models.UserLoginPOJO;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public final class JSONDataReader {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** Prevents creating instances of this static JSON data utility. */
    private JSONDataReader() {
    }

    /**
     * Loads login scenarios from the testdata JSON resource.
     *
     * @return the login scenarios as POJOs
     * @throws IllegalStateException if the resource is missing, invalid, or empty
     */
    public static List<UserLoginPOJO> readLoginData() {
        try (InputStream input = JSONDataReader.class.getClassLoader()
                .getResourceAsStream("testdata/login-data.json")) {
            if (input == null) {
                throw new IllegalStateException("testdata/login-data.json was not found on the classpath");
            }
            UserLoginPOJO[] records = OBJECT_MAPPER.readValue(input, UserLoginPOJO[].class);
            List<UserLoginPOJO> loginData = Arrays.asList(records);
            if (loginData == null || loginData.isEmpty()) {
                throw new IllegalStateException("testdata/login-data.json must contain at least one record");
            }
            return loginData;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read testdata/login-data.json", exception);
        }
    }

}
