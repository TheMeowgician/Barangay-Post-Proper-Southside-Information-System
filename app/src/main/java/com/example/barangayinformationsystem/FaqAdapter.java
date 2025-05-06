package com.example.barangayinformationsystem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter for displaying FAQ questions in a RecyclerView
 */
public class FaqAdapter extends RecyclerView.Adapter<FaqAdapter.FaqViewHolder> {
    
    private final List<FAQQuestion> questions;
    private final OnFaqQuestionClickListener listener;
    
    public interface OnFaqQuestionClickListener {
        void onQuestionClick(FAQQuestion question);
    }
    
    public FaqAdapter(List<FAQQuestion> questions, OnFaqQuestionClickListener listener) {
        this.questions = questions;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public FaqViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_faq_question, parent, false);
        return new FaqViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull FaqViewHolder holder, int position) {
        final FAQQuestion question = questions.get(position);
        holder.questionText.setText(question.getQuestion());
        
        // Set click listener for the entire question item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onQuestionClick(question);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return questions.size();
    }
    
    static class FaqViewHolder extends RecyclerView.ViewHolder {
        TextView questionText;
        
        public FaqViewHolder(@NonNull View itemView) {
            super(itemView);
            questionText = itemView.findViewById(R.id.tvFaqQuestion);
        }
    }
}