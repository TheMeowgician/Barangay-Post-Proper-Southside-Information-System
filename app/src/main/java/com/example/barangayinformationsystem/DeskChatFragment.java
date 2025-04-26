package com.example.barangayinformationsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeskChatFragment extends Fragment {
    private static final String TAG = "DeskChatFragment"; // Tag for logging

    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private ImageButton backButton;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private int userId;
    private long lastMessageTimestamp = 0;
    private Handler messageCheckHandler;
    private static final int MESSAGE_CHECK_INTERVAL = 3000; // 3 seconds

    // ** FIX: Set to track displayed message IDs **
    private Set<Integer> displayedMessageIds;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_desk_chat, container, false);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        userId = prefs.getInt("user_id", -1);
        if (userId == -1) {
            // Handle user not logged in scenario
            Toast.makeText(getContext(), "User not logged in.", Toast.LENGTH_LONG).show();
            // Redirect to login or handle appropriately
            return view; // Or redirect
        }

        initializeViews(view);
        setupRecyclerView();
        setupClickListeners();
        loadMessages(); // Load initial messages
        startMessageChecking(); // Start polling

        return view;
    }

    private void initializeViews(View view) {
        chatRecyclerView = view.findViewById(R.id.chatRecyclerView);
        messageInput = view.findViewById(R.id.messageInput);
        sendButton = view.findViewById(R.id.sendButton);
        backButton = view.findViewById(R.id.backButton);
        chatMessages = new ArrayList<>();
        // ** FIX: Initialize the Set **
        displayedMessageIds = new HashSet<>();
        messageCheckHandler = new Handler(Looper.getMainLooper());
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> {
            // Navigate back or finish activity
            if (getActivity() != null) {
                getActivity().onBackPressed(); // More standard way to go back
            }
        });

        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
            } else {
                Toast.makeText(getContext(), "Message cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void loadMessages() {
        Log.d(TAG, "Loading initial messages for user: " + userId);
        ApiService apiService = RetrofitClient.getApiService();
        Call<List<ChatMessage>> call = apiService.getMessages(userId);

        call.enqueue(new Callback<List<ChatMessage>>() {
            @Override
            public void onResponse(Call<List<ChatMessage>> call, Response<List<ChatMessage>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Successfully loaded " + response.body().size() + " initial messages.");
                    chatMessages.clear();
                    // ** FIX: Clear and populate displayed IDs **
                    displayedMessageIds.clear();
                    List<ChatMessage> initialMessages = response.body();
                    chatMessages.addAll(initialMessages);
                    for (ChatMessage msg : initialMessages) {
                        displayedMessageIds.add(msg.getId());
                    }

                    chatAdapter.notifyDataSetChanged();
                    if (!chatMessages.isEmpty()) {
                        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                        // Update last message timestamp based on the latest initial message
                        lastMessageTimestamp = chatMessages.get(chatMessages.size() - 1).getTimestamp();
                        Log.d(TAG, "Initial load - Last timestamp updated to: " + lastMessageTimestamp);
                    } else {
                        lastMessageTimestamp = 0; // Reset if no messages
                        Log.d(TAG, "Initial load - No messages found, timestamp reset.");
                    }
                } else {
                    Log.e(TAG, "Failed to load messages. Code: " + response.code() + ", Message: " + response.message());
                    Toast.makeText(getContext(), "Failed to load messages (Code: " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<ChatMessage>> call, Throwable t) {
                Log.e(TAG, "Failed to load messages (Network/API Error): " + t.getMessage(), t);
                Toast.makeText(getContext(), "Failed to load messages: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage(String messageText) {
        Log.d(TAG, "Sending message: " + messageText + " from user: " + userId);
        ApiService apiService = RetrofitClient.getApiService();
        // Pass message and sender_id (backend should ideally ignore sender_id and use authenticated user)
        Call<MessageResponse> call = apiService.sendMessage(messageText, userId);

        call.enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if ("success".equals(response.body().getStatus())) {
                        Log.d(TAG, "Message sent successfully.");
                        messageInput.setText("");
                        // Don't call loadMessages() here, let the polling handle updates
                        // loadMessages(); // Reload messages to show the new one - REMOVED
                        // Instead, trigger an immediate poll check
                        checkForNewMessages();
                    } else {
                        String errorMsg = response.body().getMessage() != null ? response.body().getMessage() : "Unknown error";
                        Log.e(TAG, "Failed to send message (API Error): " + errorMsg);
                        Toast.makeText(getContext(), "Send failed: " + errorMsg, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "Failed to send message (HTTP Error). Code: " + response.code() + ", Message: " + response.message());
                    Toast.makeText(getContext(), "Send failed (Code: " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {
                Log.e(TAG, "Failed to send message (Network/API Error): " + t.getMessage(), t);
                Toast.makeText(getContext(), "Failed to send message: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void startMessageChecking() {
        Log.d(TAG, "Starting message polling every " + MESSAGE_CHECK_INTERVAL + "ms");
        messageCheckHandler.removeCallbacksAndMessages(null); // Remove existing callbacks first
        messageCheckHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkForNewMessages();
                // Schedule the next check
                messageCheckHandler.postDelayed(this, MESSAGE_CHECK_INTERVAL);
            }
        }, MESSAGE_CHECK_INTERVAL); // Start after initial interval
    }

    private void checkForNewMessages() {
        // Ensure userId is valid before making the call
        if (userId <= 0) {
            Log.w(TAG, "User ID invalid, skipping message check.");
            return;
        }
        Log.d(TAG, "Checking for new messages for user " + userId + " since timestamp " + lastMessageTimestamp);
        ApiService apiService = RetrofitClient.getApiService();
        Call<MessageCheckResponse> call = apiService.checkNewMessages(userId, lastMessageTimestamp);

        call.enqueue(new Callback<MessageCheckResponse>() {
            @Override
            public void onResponse(Call<MessageCheckResponse> call, Response<MessageCheckResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    MessageCheckResponse checkResponse = response.body();
                    if (checkResponse.hasNewMessages()) {
                        List<ChatMessage> receivedMessages = checkResponse.getNewMessages();
                        if (receivedMessages != null && !receivedMessages.isEmpty()) {
                            Log.d(TAG, "Received " + receivedMessages.size() + " potential new messages.");

                            // ** FIX: Filter out messages already displayed **
                            List<ChatMessage> messagesToAdd = new ArrayList<>();
                            long latestReceivedTimestamp = lastMessageTimestamp; // Keep track of latest timestamp from THIS batch

                            for (ChatMessage msg : receivedMessages) {
                                if (!displayedMessageIds.contains(msg.getId())) {
                                    messagesToAdd.add(msg);
                                    displayedMessageIds.add(msg.getId()); // Add ID to set
                                    Log.d(TAG, "Adding new message ID: " + msg.getId());
                                } else {
                                    Log.d(TAG, "Skipping already displayed message ID: " + msg.getId());
                                }
                                // Update latest timestamp from received messages regardless of display status
                                if (msg.getTimestamp() > latestReceivedTimestamp) {
                                    latestReceivedTimestamp = msg.getTimestamp();
                                }
                            }

                            // Update the global last timestamp based on the latest message received in this poll
                            if (latestReceivedTimestamp > lastMessageTimestamp) {
                                Log.d(TAG, "Updating lastMessageTimestamp from " + lastMessageTimestamp + " to " + latestReceivedTimestamp);
                                lastMessageTimestamp = latestReceivedTimestamp;
                            }

                            // Add only the truly new messages to the list and notify adapter
                            if (!messagesToAdd.isEmpty()) {
                                Log.d(TAG, "Appending " + messagesToAdd.size() + " new unique messages to UI.");
                                int startPosition = chatMessages.size();
                                chatMessages.addAll(messagesToAdd);
                                // Use notifyItemRangeInserted for better performance/animation
                                chatAdapter.notifyItemRangeInserted(startPosition, messagesToAdd.size());
                                chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                            } else {
                                Log.d(TAG, "Polling received messages, but they were already displayed.");
                            }

                        } else {
                            Log.d(TAG, "API reported new messages, but the list was empty or null.");
                            // Update timestamp even if list is empty/null but hasNewMessages was true?
                            // This case shouldn't ideally happen based on backend logic.
                        }
                    } else {
                        Log.d(TAG, "No new messages found.");
                    }
                } else {
                    // Log non-successful responses during polling, but maybe don't show Toast
                    Log.w(TAG, "Check messages API call failed or returned empty body. Code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<MessageCheckResponse> call, Throwable t) {
                // Silent fail for background checks, but log it
                Log.e(TAG, "Check messages network call failed: " + t.getMessage());
            }
        });
    }

    @Override
    public void onDestroyView() { // Changed from onDestroy to onDestroyView for Fragments
        super.onDestroyView();
        Log.d(TAG, "Fragment view destroyed. Stopping message polling.");
        messageCheckHandler.removeCallbacksAndMessages(null); // Stop handler when view is destroyed
    }

    // --- ChatAdapter Class with Fixed Timezone ---
    private static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
        private static final int VIEW_TYPE_USER = 1;
        private static final int VIEW_TYPE_ADMIN = 2;
        private final List<ChatMessage> messages;
        private final int userId;

        ChatAdapter(List<ChatMessage> messages, int userId) {
            this.messages = messages;
            this.userId = userId;
        }

        @NonNull @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == VIEW_TYPE_USER) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message_user, parent, false);
            } else {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message_admin, parent, false);
            }
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            ChatMessage message = messages.get(position);
            holder.messageText.setText(message.getMessage());
            if (holder.senderNameText != null) {
                if (message.isAdmin()) {
                    holder.senderNameText.setVisibility(View.VISIBLE);
                    holder.senderNameText.setText(message.getSenderName() != null ? message.getSenderName() : "Admin");
                } else {
                    holder.senderNameText.setVisibility(View.GONE);
                }
            }

            // Get the raw timestamp (in milliseconds since epoch)
            long timestampMillis = message.getTimestamp();

            // Create a Calendar instance for Philippines timezone
            Calendar philCalendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila"));
            philCalendar.setTimeInMillis(timestampMillis);

            // Get current time in Philippines timezone
            Calendar currentPhilCalendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila"));

            // Create formatters with Philippines timezone - use 12-hour format with AM/PM
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            timeFormat.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));

            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));

            // Format the time
            String formattedTime = timeFormat.format(philCalendar.getTime());

            // Check if the message is from today (same year, month, and day)
            boolean isSameDay = (philCalendar.get(Calendar.YEAR) == currentPhilCalendar.get(Calendar.YEAR) &&
                    philCalendar.get(Calendar.MONTH) == currentPhilCalendar.get(Calendar.MONTH) &&
                    philCalendar.get(Calendar.DAY_OF_MONTH) == currentPhilCalendar.get(Calendar.DAY_OF_MONTH));

            if (isSameDay) {
                holder.timestampText.setText(formattedTime);
            } else {
                String formattedDate = dateFormat.format(philCalendar.getTime());
                holder.timestampText.setText(formattedDate + " " + formattedTime);
            }
        }

        @Override public int getItemCount() {
            return messages.size();
        }

        @Override public int getItemViewType(int position) {
            ChatMessage message = messages.get(position);
            return message.isAdmin() ? VIEW_TYPE_ADMIN : VIEW_TYPE_USER;
        }

        static class ChatViewHolder extends RecyclerView.ViewHolder {
            TextView messageText;
            TextView timestampText;
            TextView senderNameText;

            ChatViewHolder(@NonNull View itemView) {
                super(itemView);
                messageText = itemView.findViewById(R.id.messageText);
                timestampText = itemView.findViewById(R.id.timestampText);
                senderNameText = itemView.findViewById(R.id.senderNameText); /* Might be null in user layout */
            }
        }
    }
    // --- End ChatAdapter ---

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(chatMessages, userId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        // layoutManager.setStackFromEnd(true); // Keep messages stacked from bottom
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);
        Log.d(TAG, "RecyclerView setup complete.");
    }
}