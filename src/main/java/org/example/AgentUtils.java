package org.example;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.HttpURLConnection;
import java.util.Scanner;

public class AgentUtils {
    public static String parseResponse(String response) {
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        JsonArray choices = jsonResponse.getAsJsonArray("choices");
        if (choices.size() > 0) {
            return choices.get(0).getAsJsonObject().get("message").getAsJsonObject().get("content").getAsString();
        }
        return "No response received.";
    }

    public static String readErrorResponse(HttpURLConnection connection) {
        try (Scanner scanner = new Scanner(connection.getErrorStream())) {
            return scanner.useDelimiter("\\A").next();
        } catch (Exception e) {
            return "Could not read error response.";
        }
    }
}