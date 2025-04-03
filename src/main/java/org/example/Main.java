package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    private static int counter = 1; //counter for the number of threads
    //Agent.class responses are stored here from the map, and is input for SummAgent.class
    private static final Map<Integer, String> responseMap = new ConcurrentHashMap<>(); //the responses are stored here, needed for sorting them

    public static void main(String[] args) {
        int flag=0;
        List<String> AgentContexts=new ArrayList<>();
        //Retrieve data from GitHub
        try (Scanner scanner = new Scanner(System.in)) {
           String apiURL = Utils.getAPIUrl(scanner);
            while(flag==0){
                System.out.println(Utils.ANSI_CYAN+"Insert number from 0 to 7:\n" +
                        "1)Suitability of an alternative solution\n" +
                        "2)Correct understanding\n" +
                        "3)Code Description\n" +
                        "4)Code Context\n" +
                        "5)Necessity\n" +
                        "6)Atomicity\n" +
                        "7)All of the above\n" +
                        "0)Exit"+Utils.ANSI_RESET);
                if (scanner.hasNextInt()) {
                    int number = scanner.nextInt();
                    scanner.nextLine(); // Clear the leftover newline character
                   switch(number){
                       case 1:
                           AgentContexts = List.of(
                                   "You are an expert Java programmer that gives short, concise and helpful answers. Find alternatives and potential improvements in this code"

                           );
                           counter=1;
                           flag=1;
                           break;
                       case 2:
                           AgentContexts = List.of(
                                   "Do you think this code is readable, how you would improve it, give short, concise and helpful answers"
                           );
                           flag=1;
                           counter=2;
                           break;
                       case 3:
                           AgentContexts = List.of(
                                   "Do you think this code matches the pull request description, if not explain why, give a short and concise answer"
                           );
                           flag=1;
                           counter=3;
                           break;
                       case 4:
                           AgentContexts = List.of(
                                   "Do you think this code is relevant in his repository, if not explain why, give a short and concise answer"

                           );
                           counter=4;
                           flag=1;
                           break;
                       case 5:
                           AgentContexts = List.of(
                                  "Do you think this code has parts that could be omitted or simplified, give short and concise answers"
                                   );
                           counter=5;
                           flag=1;
                           break;
                       case 6:
                           AgentContexts = List.of(
                                   "Do you think this code is atomic or could be divided in multiple parts, give a short and concise answer"
                           );
                           flag=1;
                           counter=6;
                           break;
                       case 7:
                           AgentContexts = List.of(
                                   "You are an expert Java programmer that gives short, concise and helpful answers. Find alternatives and potential improvements in this code",
                                   "Do you think this code is readable, how you would improve it, give short, concise and helpful answers",
                                   "Do you think this code matches the pull request description, if not explain why, give a short and concise answer",
                                   "Do you think this code is relevant in his repository, if not explain why, give a short and concise answer",
                                   "Do you think this code has parts that could be omitted or simplified, give short and concise answers",
                                   "Do you think this code is atomic or could be divided in multiple parts, give a short and concise answer"
                           );
                           flag=1;
                           break;
                       case 0:
                           System.exit(0);
                           break;
                       default:
                           System.out.println(Utils.ANSI_RED+"Error: Insert number from 0 to 7..."+Utils.ANSI_RESET);
                           break;
                   }
                } else {
                System.out.println(Utils.ANSI_RED+"Input not valid! Insert number from 0 to 7..."+Utils.ANSI_RESET);
                scanner.nextLine(); // Consuma l'input errato per evitare loop infinito
            }
            }
            String pullRequestJson = Utils.sendGetRequest(apiURL);
            String prDescription = Utils.extractFieldFromJson(pullRequestJson, "title");
            String repoDescription = Utils.extractPullRequestDetails(pullRequestJson, "description");
            String filesURL = apiURL + "/files";
            String filesJson = Utils.sendGetRequest(filesURL);
            String prCode = Utils.extractFileDiffFromJson(filesJson);

            //Save details in the file to check input for LLM
            Utils.writeToFile(repoDescription, prDescription, prCode, Utils.codeDir);
            System.out.println(Utils.ANSI_YELLOW + "Information saved in file..." + Utils.ANSI_RESET);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String LLMinput = Utils.readFile(Utils.codeDir);

        //creation of all context for each LLM interrogation, 1 thread=1 context
        //environment variable OLLAMA_RUN_PARALLEL and value=#number of parallel requests


        List<Thread> threads = new ArrayList<>(); //List of threads

        //thread initialization
        for (String context : AgentContexts) {
            Thread informationNeedsAgentThread = new Thread(new Agent(LLMinput, context, counter, responseMap));
            System.out.println(Utils.ANSI_YELLOW + "Starting thread n:" + counter + Utils.ANSI_RESET);
            counter++;
            informationNeedsAgentThread.start();
            threads.add(informationNeedsAgentThread);
        }
        for (Thread thread : threads) {
            try {
                thread.join(); // Wait for the end of each thread
            } catch (InterruptedException e) {
                System.out.println(Utils.ANSI_RED + "Error during thread's creation: " + e.getMessage() + Utils.ANSI_RESET);
            }
        }

        // After all thread termination
        System.out.println(Utils.ANSI_GREEN + "Each thread has terminated..." + Utils.ANSI_RESET);
        Utils.writeResponsesToFile(Utils.INAResponseDir, responseMap); //writes on the file the responses contained in the map
    }
}