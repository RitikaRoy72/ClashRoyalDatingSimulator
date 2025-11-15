package src;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import UserProfile;

public class CharacterConversationSimulator {
    
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private String apiKey;
    private String characterPersonality;
    private String userBio;
    private List<JSONObject> conversationHistory;
    private UserProfile userP = new UserProfile();
    
    public CharacterConversationSimulator(String apiKey, String characterPersonality, String userBio) {
        this.apiKey = apiKey;
        this.characterPersonality = characterPersonality;
        this.userBio = userBio;
        this.conversationHistory = new ArrayList<>();
        
        // Initialize with system message
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", 
            "You are roleplaying as a character with this personality: " + characterPersonality + "\n" +
            "You are on a date with someone who has this bio: " + userBio + "\n" +
            "Stay in character at all times. Be engaging and respond naturally to what they say."
        );
        conversationHistory.add(systemMessage);
    }
    
    /**
     * Represents a response from the character including their message and score
     */
    public static class CharacterResponse {
        public String message;
        public int score;
        
        public CharacterResponse(String message, int score) {
            this.message = message;
            this.score = score;
        }
        
        @Override
        public String toString() {
            return "Message: " + message + "\nScore: " + score + "/10";
        }
    }
    
    /**
     * Sends user statement to ChatGPT and gets character response with score
     * @param userStatement What the user said
     * @return CharacterResponse containing the character's reply and user's score
     * @throws Exception if the API call fails
     */
    public CharacterResponse sendMessage(String userStatement) throws Exception {
        // Add user message to history
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", userStatement);
        conversationHistory.add(userMessage);
        
        // Get character's response
        String characterReply = getCharacterReply();
        
        // Add character's response to history
        JSONObject assistantMessage = new JSONObject();
        assistantMessage.put("role", "assistant");
        assistantMessage.put("content", characterReply);
        conversationHistory.add(assistantMessage);
        
        // Get score for user's statement
        int score = scoreUserStatement(userStatement, characterReply);
        
        return new CharacterResponse(characterReply, score);
    }
    
    /**
     * Gets the character's response based on conversation history
     */
    private String getCharacterReply() throws Exception {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "gpt-4o-mini");
        
        // Convert conversation history to JSONArray
        JSONArray messages = new JSONArray();
        for (JSONObject msg : conversationHistory) {
            messages.put(msg);
        }
        
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 500);
        requestBody.put("temperature", 0.9); // Higher for more creative responses
        
        return makeAPICall(requestBody);
    }
    
    /**
     * Scores the user's statement based on how well it resonates with the character
     */
    private scoreUserStatement(String userStatement, String characterReply) throws Exception {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "gpt-4o-mini");
        
        JSONArray messages = new JSONArray();
        
        // Create scoring prompt
        JSONObject scoringMessage = new JSONObject();
        scoringMessage.put("role", "user");
        scoringMessage.put("content", 
            "You are evaluating a dating conversation. Rate the user's statement on a scale of 0-10 based on:\n" +
            "- How appropriate and engaging it is\n" +
            "- How well it matches the character's personality and interests\n" +
            "- Social skills and charm\n" +
            "- Romantic potential\n\n" +
            "Character Personality: " + characterPersonality + "\n" +
            "User Bio: " + userBio + "\n" +
            "User Said: \"" + userStatement + "\"\n" +
            "Character Responded: \"" + characterReply + "\"\n\n" +
            "Respond with ONLY a single number from 0 to 10."
        );
        messages.put(scoringMessage);
        
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 10);
        requestBody.put("temperature", 0.3); // Lower for consistent scoring
        
        String response = makeAPICall(requestBody);
        
        try {
        	this.userP.updateScore(Integer.parseInt(response.trim()));
            return this.userP.getScore();
        } catch (NumberFormatException e) {
            // Default to 5 if parsing fails
            System.err.println("Warning: Could not parse score, defaulting to 5. Response was: " + response);
            return 0;
        }
    }
    
    /**
     * Makes the actual API call to OpenAI
     */
    private String makeAPICall(JSONObject requestBody) throws Exception {
        URL url = new URL(API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setDoOutput(true);
        
        // Send request
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        
        // Read response
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), "utf-8"));
            StringBuilder response = new StringBuilder();
            String line;
            
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            
            // Parse JSON response
            JSONObject jsonResponse = new JSONObject(response.toString());
            String reply = jsonResponse
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");
            
            return reply;
        } else {
            BufferedReader errorReader = new BufferedReader(
                new InputStreamReader(connection.getErrorStream(), "utf-8"));
            StringBuilder errorResponse = new StringBuilder();
            String line;
            
            while ((line = errorReader.readLine()) != null) {
                errorResponse.append(line);
            }
            errorReader.close();
            
            throw new Exception("API Error (Code " + responseCode + "): " + errorResponse.toString());
        }
    }
    
    /**
     * Resets the conversation while keeping character and user info
     */
    public void resetConversation() {
        conversationHistory.clear();
        
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", 
            "You are roleplaying as a character with this personality: " + characterPersonality + "\n" +
            "You are on a date with someone who has this bio: " + userBio + "\n" +
            "Stay in character at all times. Be engaging and respond naturally to what they say."
        );
        conversationHistory.add(systemMessage);
    }
    
    // Example usage
    public static void main(String[] args) {
        try {
            String apiKey = "YOUR_OPENAI_API_KEY_HERE";
            
            // Define character personality
            String characterPersonality = "A fierce but kind knight from Clash Royale. " +
                "You are brave, honorable, and love talking about battles and strategy. " +
                "You have a soft side and appreciate genuine conversation. " +
                "You value courage and loyalty.";
            
            // Define user bio
            String userBio = "A strategy game enthusiast who enjoys deep conversations " +
                "and has a good sense of humor.";
            
            // Create simulator
            CharacterConversationSimulator simulator = 
                new CharacterConversationSimulator(apiKey, characterPersonality, userBio);
            
            // Simulate conversation
            System.out.println("=== CLASH ROYALE DATING SIMULATOR ===\n");
            
            // Message 1
            String userMsg1 = "Hi! I've heard you're quite the warrior. What's your favorite battle strategy?";
            System.out.println("You: " + userMsg1);
            CharacterResponse response1 = simulator.sendMessage(userMsg1);
            System.out.println("Knight: " + response1.message);
            System.out.println("Your Score: " + response1.score + "/10\n");
            
            // Message 2
            String userMsg2 = "That sounds amazing! I really admire your bravery and tactical thinking.";
            System.out.println("You: " + userMsg2);
            CharacterResponse response2 = simulator.sendMessage(userMsg2);
            System.out.println("Knight: " + response2.message);
            System.out.println("Your Score: " + response2.score + "/10\n");
            
            // Message 3
            String userMsg3 = "Want to grab some elixir together sometime?";
            System.out.println("You: " + userMsg3);
            CharacterResponse response3 = simulator.sendMessage(userMsg3);
            System.out.println("Knight: " + response3.message);
            System.out.println("Your Score: " + response3.score + "/10\n");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
