package com.example.friday;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    TextView welcomeTextView;
    EditText messageEditText;
    ImageButton sendButton;
    List<Message> messageList;
    MessageAdapter messageAdapter;

    //  Declare global variables for trivia
    String triviaQuestion = null;
    String correctAnswer = null;

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        messageList = new ArrayList<>();
        recyclerView = findViewById(R.id.recycler_view);
        welcomeTextView = findViewById(R.id.welcome_text);
        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_btn);

        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        recyclerView.setLayoutManager(llm);

        sendButton.setOnClickListener(view -> {
            String message = messageEditText.getText().toString().trim();
            addToChat(message, Message.SENT_BY_ME);
            messageEditText.setText("");
            welcomeTextView.setVisibility(View.GONE);

            // Check if the user is answering a trivia
                  if (correctAnswer != null && message.equalsIgnoreCase(correctAnswer)) {
                addToChat("Correct! 🎉 Well done!", Message.SENT_BY_BOT);
                correctAnswer = null; // Reset after answering
            }
                  else if(message.toLowerCase().contains("i don't know")){
                      addToChat("The Correct Answer is "+"\n"+correctAnswer+"\n"+" 😊 Let Me Know if you need another question ", Message.SENT_BY_BOT);
                  }
            else if (correctAnswer != null&&  !message.equalsIgnoreCase(correctAnswer)) { //Handle incorrect answers
                addToChat("❌ Wrong answer!."+"\n"+"the correct answer is "+correctAnswer, Message.SENT_BY_BOT);
            }
            else if (message.toLowerCase().contains("trivia") || message.toLowerCase().contains("question")) {
                Log.d("BotDebug", "Calling Trivia API..."); //  Debugging
                callAPI();
            }
            else if (message.toLowerCase().contains("hi") || message.toLowerCase().contains("hello")||correctAnswer != null){
                      addToChat("Hi! Welcome to Friday. Please use \"trivia\" keyword to get your question?", Message.SENT_BY_BOT);
                  }
                  else {
                addToChat("You Are using ❌Wrong! keywords. please use trivia keyword to get your question", Message.SENT_BY_BOT);
            }

        });
    }

    void addToChat(String message, String sentBy) {
        runOnUiThread(() -> {
            messageList.add(new Message(message, sentBy));
            messageAdapter.notifyDataSetChanged();
            recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());
        });
    }

    void callAPI() {
        Request request = new Request.Builder()
                .url(" https://opentdb.com/api.php?amount=50&category=23&type=multiple")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                addToChat("Failed to fetch trivia question.\nThe issue may be:\n" +
                        "\n" +
                        "No internet connection\n" +
                        "\n" +
                        "API server issues\n" +
                        "\n" +
                        "Timeout due to slow response\n" , Message.SENT_BY_BOT);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String result = response.body().string();
                    Log.d("BotDebug", "Raw API Response: " + result); //  Debugging log

                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        JSONArray results = jsonObject.getJSONArray("results");
                        if (results.length() > 0) {
                            JSONObject questionObj = results.getJSONObject(0);

                            // Store both question & correct answer
                            triviaQuestion = questionObj.getString("question");
                            correctAnswer = questionObj.getString("correct_answer");

                            // Ensure proper text formatting
                            triviaQuestion = Html.fromHtml(triviaQuestion, Html.FROM_HTML_MODE_LEGACY).toString();
                            correctAnswer = Html.fromHtml(correctAnswer, Html.FROM_HTML_MODE_LEGACY).toString();

                            addToChat("Alright! Here's a trivia challenge for you: \n" + triviaQuestion, Message.SENT_BY_BOT);
                        } else {
                            addToChat("No trivia question found.", Message.SENT_BY_BOT);
                        }
                    } catch (JSONException e) {
                        addToChat("Error parsing trivia question.", Message.SENT_BY_BOT);
                    }
                } else {
                    addToChat("Error from server.", Message.SENT_BY_BOT);
                }
            }
        });
    }
}
