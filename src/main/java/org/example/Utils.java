package org.example;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;

public class Utils {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    //Information Needs Agent Response directory
    public static final String INAResponseDir = "src/main/resources/InformationNeedsAgentResponse.txt";
    //We store there the data retrieved from gitHub
    public static final String codeDir = "src/main/resources/code.txt";
    //Ollama local url
    public static final String OLLAMA_URL = "http://localhost:11434/v1/chat/completions";
    //gitHub token
    private static final String token = ""; //insert GitHub token here

    //reads content from a file and store it in a string
    public static String readFile(String filePath) {
        StringBuilder content = new StringBuilder();
        try {
            File file = new File(filePath);
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                content.append(scanner.nextLine()).append(System.lineSeparator());
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println(ANSI_RED + "Error: File not found " + e.getMessage() + ANSI_RESET);
        }
        return content.toString();
    }


    //reads responses(strings) from the map and save them in a file, the responses are sorted
    public static void writeResponsesToFile(String dir, Map<Integer, String> map) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dir, StandardCharsets.UTF_8))) {
            if (map.containsKey(1)) {
                writer.write("Suitability of an alternative solution:\n" + map.get(1));
                writer.newLine();
            }
            if (map.containsKey(2)) {
                writer.write("Correct understanding:\n" + map.get(2));
                writer.newLine();
            }
            if (map.containsKey(3)) {
                writer.write("Code Description:\n" + map.get(3));
                writer.newLine();
            }
            if (map.containsKey(4)) {
                writer.write("Code Context:\n" + map.get(4));
                writer.newLine();
            }
            if (map.containsKey(5)) {
                writer.write("Necessity:\n" + map.get(5));
                writer.newLine();
            }
            if (map.containsKey(6)) {
                writer.write("Atomicity:\n" + map.get(6));
                writer.newLine();
            }
            System.out.println(ANSI_GREEN + "Responses have been written to the file." + ANSI_RESET);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //get important data from the url
    public static String getAPIUrl(Scanner scanner) {
        System.out.println(ANSI_BLUE + "Insert Pull Request URL:" + ANSI_RESET);
        String pullRequestUrl = scanner.nextLine();
        String[] urlParts = pullRequestUrl.split("/");
        String repoOwner = urlParts[3]; // repository owner
        String repoName = urlParts[4];  // repository name
        String pullRequestNumber = urlParts[6]; // pull request number
        return "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/pulls/" + pullRequestNumber;
    }

    //Generic method for a GitHub API request
    public static String sendGetRequest(String url) throws Exception {
        //connection creation
        URI obj = new URI(url);
        HttpURLConnection con = (HttpURLConnection) obj.toURL().openConnection();

        //GET request settings
        con.setRequestMethod("GET");
        con.setRequestProperty("Authorization", "token " + token);
        con.setRequestProperty("Accept", "application/vnd.github.v3+json");
        int responseCode = con.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + responseCode);
        }

        //Reading response
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return response.toString();
    }

    //Extract a specific field from JSON using Gson
    public static String extractFieldFromJson(String json, String fieldName) {
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        return jsonObject.has(fieldName) ? jsonObject.get(fieldName).getAsString() : "Field not found";
    }

    //Extract repository description
    public static String extractPullRequestDetails(String pullRequestJson, String fieldName) {
        JsonObject pullRequest = JsonParser.parseString(pullRequestJson).getAsJsonObject();
        JsonObject repository = pullRequest.getAsJsonObject("base").getAsJsonObject("repo");
        return repository.has(fieldName) ? repository.get(fieldName).getAsString() : "Field not found";
    }

    //Extract code from the pull request using Gson
    public static String extractFileDiffFromJson(String json) {
        StringBuilder diffs = new StringBuilder();
        JsonArray jsonArray = JsonParser.parseString(json).getAsJsonArray();
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject fileObject = jsonArray.get(i).getAsJsonObject();
            String fileName = fileObject.has("filename") ? fileObject.get("filename").getAsString() : "Name not found";
            String patch = fileObject.has("patch") ? fileObject.get("patch").getAsString() : "Patch not found";

            diffs.append("File: ").append(fileName).append("\n");
            diffs.append("Changes:\n").append(patch).append("\n\n");
        }
        return diffs.toString();
    }

    //write string into a file
    public static void writeToFile(String repoDescription, String prDescription, String prCode, String dir) {
        try (FileWriter writer = new FileWriter(dir, false)) {
            writer.write("Repository Description:\n");
            writer.write(repoDescription);
            writer.write("\n\nPull Request Description:\n");
            writer.write(prDescription);
            writer.write("\n\nCode:\n");
            writer.write(prCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
