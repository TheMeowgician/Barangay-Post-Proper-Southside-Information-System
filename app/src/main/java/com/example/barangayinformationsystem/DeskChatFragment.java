package com.example.barangayinformationsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeskChatFragment extends Fragment {
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_desk_chat, container, false);

        // Get userId from SharedPreferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        userId = prefs.getInt("user_id", -1);

        initializeViews(view);
        setupRecyclerView();
        setupClickListeners();
        loadMessages();
        startMessageChecking();

        return view;
    }

    private void initializeViews(View view) {
        chatRecyclerView = view.findViewById(R.id.chatRecyclerView);
        messageInput = view.findViewById(R.id.messageInput);
        sendButton = view.findViewById(R.id.sendButton);
        backButton = view.findViewById(R.id.backButton);
        chatMessages = new ArrayList<>();
        messageCheckHandler = new Handler(Looper.getMainLooper());
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), HomeActivity.class);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        });

        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
            }
        });
    }

    private void loadMessages() {
        ApiService apiService = RetrofitClient.getApiService();
        Call<List<ChatMessage>> call = apiService.getMessages(userId);

        call.enqueue(new Callback<List<ChatMessage>>() {
            @Override
            public void onResponse(Call<List<ChatMessage>> call, Response<List<ChatMessage>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    chatMessages.clear();
                    chatMessages.addAll(response.body());
                    chatAdapter.notifyDataSetChanged();
                    chatRecyclerView.scrollToPosition(chatMessages.size() - 1);

                    // Update last message timestamp
                    if (!chatMessages.isEmpty()) {
                        lastMessageTimestamp = chatMessages.get(chatMessages.size() - 1).getTimestamp();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<ChatMessage>> call, Throwable t) {
                Toast.makeText(getContext(), "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage(String messageText) {
        ApiService apiService = RetrofitClient.getApiService();
        Call<MessageResponse> call = apiService.sendMessage(
                messageText,  // message
                userId        // sender_id
        );

        call.enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if ("success".equals(response.body().getStatus())) {
                        messageInput.setText("");
                        loadMessages(); // Reload messages to show the new one
                    } else {
                        Toast.makeText(getContext(),
                                response.body().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Failed to send message: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startMessageChecking() {
        messageCheckHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkForNewMessages();
                messageCheckHandler.postDelayed(this, MESSAGE_CHECK_INTERVAL);
            }
        }, MESSAGE_CHECK_INTERVAL);
    }

    private void checkForNewMessages() {
        ApiService apiService = RetrofitClient.getApiService();
        Call<MessageCheckResponse> call = apiService.checkNewMessages(userId, lastMessageTimestamp);

        call.enqueue(new Callback<MessageCheckResponse>() {
            @Override
            public void onResponse(Call<MessageCheckResponse> call, Response<MessageCheckResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().hasNewMessages()) {
                        // Add new messages and update the UI
                        chatMessages.addAll(response.body().getNewMessages());
                        chatAdapter.notifyDataSetChanged();
                        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);

                        // Update last message timestamp
                        if (!chatMessages.isEmpty()) {
                            lastMessageTimestamp = chatMessages.get(chatMessages.size() - 1).getTimestamp();
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<MessageCheckResponse> call, Throwable t) {
                // Silent fail for background checks
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        messageCheckHandler.removeCallbacksAndMessages(null);
    }

    private static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
        private static final int VIEW_TYPE_USER = 1;
        private static final int VIEW_TYPE_ADMIN = 2;

        private final List<ChatMessage> messages;
        private final int userId;

        ChatAdapter(List<ChatMessage> messages, int userId) {
            this.messages = messages;
            this.userId = userId;
        }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == VIEW_TYPE_USER) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_chat_message_user, parent, false);
            } else {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_chat_message_admin, parent, false);
            }
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            ChatMessage message = messages.get(position);
            holder.messageText.setText(message.getMessage());
            holder.senderNameText.setText(message.getSenderName());

            // Format and set timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            String formattedTime = sdf.format(new Date(message.getTimestamp()));
            holder.timestampText.setText(formattedTime);
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        @Override
        public int getItemViewType(int position) {
            ChatMessage message = messages.get(position);
            // Messages from the current user should be on the right
            // Messages from admin (is_admin = true) should be on the left
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
                senderNameText = itemView.findViewById(R.id.senderNameText);
            }
        }
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(chatMessages, userId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);
    }

}