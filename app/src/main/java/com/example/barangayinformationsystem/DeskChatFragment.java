package com.example.barangayinformationsystem;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * DeskChatFragment - Handles the chat interface for user inquiries
 * with automatic FAQ responses and option for live admin support
 */
public class DeskChatFragment extends Fragment implements FaqAdapter.OnFaqQuestionClickListener {
    private static final String TAG = "DeskChatFragment";
    private static final int MESSAGE_CHECK_INTERVAL = 3000; // 3 seconds

    private RecyclerView chatRecyclerView;
    private RecyclerView faqRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private ImageButton backButton;
    private View faqPanel;
    private TextView tvTalkToAdmin;
    private ChatAdapter chatAdapter;
    private FaqAdapter faqAdapter;
    private List<ChatMessage> chatMessages;
    private List<FAQQuestion> faqQuestions;
    private int userId;
    private long lastMessageTimestamp = 0;
    private Handler messageCheckHandler;
    
    // Counter to generate unique negative IDs for auto-responses
    private AtomicInteger autoResponseIdCounter = new AtomicInteger(-1);
    
    // Flag to track if user has switched to live chat
    private boolean liveChatMode = false;

    // Set to track displayed message IDs
    private Set<Integer> displayedMessageIds;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_desk_chat, container, false);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        userId = prefs.getInt("user_id", -1);
        if (userId == -1) {
            Toast.makeText(getContext(), "User not logged in.", Toast.LENGTH_LONG).show();
            return view;
        }

        initializeViews(view);
        setupRecyclerView();
        setupFaqRecyclerView();
        setupClickListeners();
        loadMessages();
        startMessageChecking();
        
        // Hide FAQ panel initially - user needs to press More FAQs to see it
        faqPanel.setVisibility(View.GONE);
        
        // Show welcome message with instructions immediately
        showWelcomeMessage();

        return view;
    }

    private void initializeViews(View view) {
        chatRecyclerView = view.findViewById(R.id.chatRecyclerView);
        messageInput = view.findViewById(R.id.messageInput);
        sendButton = view.findViewById(R.id.sendButton);
        backButton = view.findViewById(R.id.backButton);
        faqPanel = view.findViewById(R.id.faqPanel);
        faqRecyclerView = faqPanel.findViewById(R.id.rvFaqQuestions);
        tvTalkToAdmin = faqPanel.findViewById(R.id.tvTalkToAdmin);
        
        // Disable input field by default - user needs to explicitly enable it
        messageInput.setEnabled(false);
        messageInput.setHint("Press \"Type your question\" button to enable typing");
        sendButton.setEnabled(false);
        sendButton.setAlpha(0.5f);  // Visual indication that it's disabled
        
        chatMessages = new ArrayList<>();
        faqQuestions = FAQQuestion.getCommonQuestions();
        displayedMessageIds = new HashSet<>();
        messageCheckHandler = new Handler(Looper.getMainLooper());
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                
                // Hide FAQ panel when user sends a message
                if (!liveChatMode) {
                    liveChatMode = true;
                    faqPanel.setVisibility(View.GONE);
                }
            } else {
                Toast.makeText(getContext(), "Message cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        
        tvTalkToAdmin.setOnClickListener(v -> {
            liveChatMode = true;
            showLiveChatTransitionMessage();
            faqPanel.setVisibility(View.GONE);
        });
    }
    
    private void showWelcomeMessage() {
        String welcomeMessage = "Welcome to the Barangay Post Proper Southside Information System! " +
                "Press the \"More FAQs\" button below to see common questions, or type your own message to chat with a barangay representative.";
        
        long currentTime = System.currentTimeMillis();
        int newMessageId = autoResponseIdCounter.getAndDecrement();
        
        ChatMessage autoMessage = new ChatMessage(welcomeMessage, currentTime, true);
        autoMessage.setId(newMessageId);
        
        chatMessages.add(autoMessage);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
    }
    
    private void showLiveChatTransitionMessage() {
        String transitionMessage = "You are now chatting with a barangay representative. " +
                "Please note that responses may take a few minutes. Thank you for your patience.";
        
        long currentTime = System.currentTimeMillis();
        int newMessageId = autoResponseIdCounter.getAndDecrement();
        
        ChatMessage autoMessage = new ChatMessage(transitionMessage, currentTime, true);
        autoMessage.setId(newMessageId);
        
        chatMessages.add(autoMessage);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
    }

    private void loadMessages() {
        Log.d(TAG, "Loading initial messages for user: " + userId);
        ApiService apiService = RetrofitClient.getApiService();
        Call<List<ChatMessage>> call = apiService.getMessages(userId);

        call.enqueue(new Callback<List<ChatMessage>>() {
            @Override
            public void onResponse(Call<List<ChatMessage>> call, Response<List<ChatMessage>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Clear existing messages and displayedMessageIds
                    chatMessages.clear();
                    displayedMessageIds.clear();
                    
                    // Add messages from the server first
                    List<ChatMessage> initialMessages = response.body();
                    chatMessages.addAll(initialMessages);
                    for (ChatMessage msg : initialMessages) {
                        displayedMessageIds.add(msg.getId());
                    }
                    
                    // Now add welcome message at the end of the existing messages
                    if (!initialMessages.isEmpty()) {
                        // Update timestamp for latest message from server
                        long latestTimestamp = 0;
                        for (ChatMessage msg : initialMessages) {
                            if (msg.getTimestamp() > latestTimestamp) {
                                latestTimestamp = msg.getTimestamp();
                            }
                        }
                        lastMessageTimestamp = latestTimestamp;
                        
                        // After adding all server messages, add the welcome message
                        showWelcomeMessage();
                    } else {
                        // No messages from server, reset timestamp
                        lastMessageTimestamp = 0;
                        // Add welcome message for empty conversation
                        showWelcomeMessage();
                    }
                    
                    chatAdapter.notifyDataSetChanged();
                    chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                } else {
                    Log.e(TAG, "Failed to load messages. Code: " + response.code());
                    Toast.makeText(getContext(), "Failed to load messages", Toast.LENGTH_SHORT).show();
                    // Show welcome message even if server request fails
                    showWelcomeMessage();
                }
            }

            @Override
            public void onFailure(Call<List<ChatMessage>> call, Throwable t) {
                Log.e(TAG, "Failed to load messages: " + t.getMessage(), t);
                Toast.makeText(getContext(), "Failed to load messages", Toast.LENGTH_SHORT).show();
                // Show welcome message even if server request fails
                showWelcomeMessage();
            }
        });
    }

    private void sendMessage(String messageText) {
        ApiService apiService = RetrofitClient.getApiService();
        Call<MessageResponse> call = apiService.sendMessage(messageText, userId);

        call.enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if ("success".equals(response.body().getStatus())) {
                        messageInput.setText("");
                        checkForNewMessages();
                    } else {
                        String errorMsg = response.body().getMessage() != null ? 
                                response.body().getMessage() : "Unknown error";
                        Toast.makeText(getContext(), "Failed to send message", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Failed to send message", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Failed to send message", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startMessageChecking() {
        messageCheckHandler.removeCallbacksAndMessages(null);
        messageCheckHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkForNewMessages();
                messageCheckHandler.postDelayed(this, MESSAGE_CHECK_INTERVAL);
            }
        }, MESSAGE_CHECK_INTERVAL);
    }

    private void checkForNewMessages() {
        if (userId <= 0) {
            return;
        }
        
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
                            // Filter out messages already displayed
                            List<ChatMessage> messagesToAdd = new ArrayList<>();
                            long latestReceivedTimestamp = lastMessageTimestamp;

                            for (ChatMessage msg : receivedMessages) {
                                if (!displayedMessageIds.contains(msg.getId())) {
                                    messagesToAdd.add(msg);
                                    displayedMessageIds.add(msg.getId());
                                }
                                if (msg.getTimestamp() > latestReceivedTimestamp) {
                                    latestReceivedTimestamp = msg.getTimestamp();
                                }
                            }

                            // Update the timestamp if newer messages were received
                            if (latestReceivedTimestamp > lastMessageTimestamp) {
                                lastMessageTimestamp = latestReceivedTimestamp;
                            }

                            // Add only the new messages to the list
                            if (!messagesToAdd.isEmpty()) {
                                int startPosition = chatMessages.size();
                                chatMessages.addAll(messagesToAdd);
                                chatAdapter.notifyItemRangeInserted(startPosition, messagesToAdd.size());
                                chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                                
                                // If we received a message from an admin, switch to live chat mode
                                for (ChatMessage msg : messagesToAdd) {
                                    if (msg.isAdmin() && !msg.isAutoResponse()) {
                                        liveChatMode = true;
                                        faqPanel.setVisibility(View.GONE);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<MessageCheckResponse> call, Throwable t) {
                // Silent fail for background checks
                Log.e(TAG, "Check messages failed: " + t.getMessage());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        messageCheckHandler.removeCallbacksAndMessages(null);
    }
    
    @Override
    public void onQuestionClick(FAQQuestion question) {
        // Hide FAQ panel immediately to reveal the chat content
        faqPanel.setVisibility(View.GONE);
        
        // Get current time for both messages to ensure proper ordering
        long currentTime = System.currentTimeMillis();
        
        // Add the question as a user message immediately
        int questionMessageId = autoResponseIdCounter.getAndDecrement();
        ChatMessage userQuestion = new ChatMessage(question.getQuestion(), currentTime, false);
        userQuestion.setId(questionMessageId);
        chatMessages.add(userQuestion);  // Add user's question
        
        // Create and add auto-response immediately after the question
        int answerMessageId = autoResponseIdCounter.getAndDecrement();
        // Use a slightly later timestamp for the answer to ensure proper ordering
        ChatMessage autoAnswer = new ChatMessage(question.getAnswer(), currentTime + 1, true);
        autoAnswer.setId(answerMessageId);
        chatMessages.add(autoAnswer);
        
        // Notify adapter of both insertions and scroll to the bottom
        chatAdapter.notifyDataSetChanged();
        chatRecyclerView.post(() -> {
            chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
        });
        
        // If user selected "I need to speak with a barangay official", switch to live chat mode
        if (question.getId() == 10) {
            liveChatMode = true;
        }
    }
    
    private void showFaqPanel() {
        if (!liveChatMode) {
            faqPanel.setVisibility(View.VISIBLE);
            faqPanel.bringToFront();
        }
    }
    
    private void setupFaqRecyclerView() {
        faqAdapter = new FaqAdapter(faqQuestions, this);
        faqRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        faqRecyclerView.setAdapter(faqAdapter);
    }

    // --- ChatAdapter Class with Additional View Type for Auto-responses ---
    private class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int VIEW_TYPE_USER = 1;
        private static final int VIEW_TYPE_ADMIN = 2;
        private static final int VIEW_TYPE_AUTO = 3;  // Auto-response messages
        
        private final List<ChatMessage> messages;
        private final int userId;

        ChatAdapter(List<ChatMessage> messages, int userId) {
            this.messages = messages;
            this.userId = userId;
        }

        @NonNull @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == VIEW_TYPE_USER) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message_user, parent, false);
                return new UserMessageViewHolder(view);
            } else if (viewType == VIEW_TYPE_ADMIN) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message_admin, parent, false);
                return new AdminMessageViewHolder(view);
            } else {
                // Auto-response view type
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message_auto, parent, false);
                return new AutoResponseViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ChatMessage message = messages.get(position);
            
            if (holder instanceof UserMessageViewHolder) {
                bindUserMessage((UserMessageViewHolder) holder, message);
            } else if (holder instanceof AdminMessageViewHolder) {
                bindAdminMessage((AdminMessageViewHolder) holder, message);
            } else if (holder instanceof AutoResponseViewHolder) {
                bindAutoResponseMessage((AutoResponseViewHolder) holder, message);
            }
        }
        
        private void bindUserMessage(UserMessageViewHolder holder, ChatMessage message) {
            holder.messageText.setText(message.getMessage());
            formatTimestamp(holder.timestampText, message.getTimestamp());
        }
        
        private void bindAdminMessage(AdminMessageViewHolder holder, ChatMessage message) {
            holder.messageText.setText(message.getMessage());
            if (holder.senderNameText != null) {
                holder.senderNameText.setVisibility(View.VISIBLE);
                holder.senderNameText.setText(message.getSenderName() != null ? message.getSenderName() : "Admin");
            }
            formatTimestamp(holder.timestampText, message.getTimestamp());
        }
        
        private void bindAutoResponseMessage(AutoResponseViewHolder holder, ChatMessage message) {
            holder.messageText.setText(message.getMessage());
            formatTimestamp(holder.timestampText, message.getTimestamp());
            
            // Setup the "More FAQs" button to show the FAQ panel again
            holder.btnMoreQuestions.setOnClickListener(v -> {
                // Reset live chat mode to allow showing FAQ panel again
                liveChatMode = false;
                
                // Show the FAQ panel
                showFaqPanel();
            });
            
            // Setup the "Type your question instead" button to enable the input field
            holder.btnTypeQuestion.setOnClickListener(v -> {
                // Enable the message input field and send button
                messageInput.setEnabled(true);
                messageInput.setHint("Type your question here");
                messageInput.requestFocus();
                sendButton.setEnabled(true);
                sendButton.setAlpha(1.0f);
                
                // Switch to live chat mode and hide FAQ panel
                liveChatMode = true;
                faqPanel.setVisibility(View.GONE);
                
                // Show a transition message
                String typeMessage = "You can now type your question in the text box below.";
                long currentTime = System.currentTimeMillis();
                int newMessageId = autoResponseIdCounter.getAndDecrement();
                
                ChatMessage typeNotification = new ChatMessage(typeMessage, currentTime, true);
                typeNotification.setId(newMessageId);
                
                chatMessages.add(typeNotification);
                chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            });
        }
        
        private void formatTimestamp(TextView timestampView, long timestamp) {
            // Get the raw timestamp (in milliseconds since epoch)
            long timestampMillis = timestamp;

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
                timestampView.setText(formattedTime);
            } else {
                String formattedDate = dateFormat.format(philCalendar.getTime());
                timestampView.setText(formattedDate + " " + formattedTime);
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        @Override
        public int getItemViewType(int position) {
            ChatMessage message = messages.get(position);
            if (message.isAdmin()) {
                if (message.isAutoResponse()) {
                    return VIEW_TYPE_AUTO;  // Auto-response messages
                }
                return VIEW_TYPE_ADMIN;     // Admin messages
            }
            return VIEW_TYPE_USER;          // User messages
        }

        class UserMessageViewHolder extends RecyclerView.ViewHolder {
            TextView messageText;
            TextView timestampText;

            UserMessageViewHolder(@NonNull View itemView) {
                super(itemView);
                messageText = itemView.findViewById(R.id.messageText);
                timestampText = itemView.findViewById(R.id.timestampText);
            }
        }

        class AdminMessageViewHolder extends RecyclerView.ViewHolder {
            TextView messageText;
            TextView timestampText;
            TextView senderNameText;

            AdminMessageViewHolder(@NonNull View itemView) {
                super(itemView);
                messageText = itemView.findViewById(R.id.messageText);
                timestampText = itemView.findViewById(R.id.timestampText);
                senderNameText = itemView.findViewById(R.id.senderNameText);
            }
        }
        
        class AutoResponseViewHolder extends RecyclerView.ViewHolder {
            TextView messageText;
            TextView timestampText;
            TextView senderNameText;
            Button btnMoreQuestions;
            Button btnTypeQuestion;

            AutoResponseViewHolder(@NonNull View itemView) {
                super(itemView);
                messageText = itemView.findViewById(R.id.messageText);
                timestampText = itemView.findViewById(R.id.timestampText);
                senderNameText = itemView.findViewById(R.id.senderNameText);
                btnMoreQuestions = itemView.findViewById(R.id.btnMoreQuestions);
                btnTypeQuestion = itemView.findViewById(R.id.btnTypeQuestion);
            }
        }
    }
    // --- End ChatAdapter ---

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(chatMessages, userId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);
    }
}