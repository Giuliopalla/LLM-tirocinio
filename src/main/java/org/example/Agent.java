package org.example;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;

public class Agent implements Runnable{

    private final String LLMinput;
    private final String context;
    private final int counter;
    private final Map<Integer, String> responseMap;


    public Agent(String LLMinput,String context,int counter,Map<Integer,String> responseMap){
        this.LLMinput=LLMinput;
        this.context=context;
        this.counter=counter;
        this.responseMap=responseMap;
    }

    @Override
    public void run() {
        try {
            URI uri=URI.create(Utils.OLLAMA_URL);
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Constructing the JSON request body
            JsonObject requestBody = new JsonObject();
            JsonArray messages = new JsonArray();

            // System message for context
            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content", context); // Adding context here
            messages.add(systemMessage);

            // User message
            JsonObject userMessage = new JsonObject();
            userMessage.addProperty("role", "user");
            userMessage.addProperty("content", LLMinput);
            messages.add(userMessage);

            requestBody.add("messages", messages);
            requestBody.addProperty("model", "codellama:13b-instruct");
            requestBody.addProperty("max_tokens", 4096); // Optional: Adjust if necessary
            requestBody.addProperty("temperature", 0.3); // Optional: Adjust if necessary

            String jsonInputString = requestBody.toString();
            System.out.println(Utils.ANSI_PURPLE+"Request Body: " + jsonInputString+Utils.ANSI_RESET); // Print the request body for debugging

            // Write the JSON request body to the output stream
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (Scanner scanner = new Scanner(connection.getInputStream())) {
                    String responseBody = scanner.useDelimiter("\\A").next();
                    toMap(AgentUtils.parseResponse(responseBody));
                }
            } else {
                // Read the error response body for additional information
                String errorResponse = AgentUtils.readErrorResponse(connection);
                System.out.println(Utils.ANSI_RED+"Error: Received response code " + responseCode + ". Message: " + errorResponse+Utils.ANSI_RESET);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(Utils.ANSI_RED+"Error: " + e.getMessage()+Utils.ANSI_RESET);
        }
    }


    private void toMap(String response){
        responseMap.put(counter, response);
        System.out.println(Utils.ANSI_CYAN+"Thread " + counter + " has finished."+Utils.ANSI_RESET);
    }
}