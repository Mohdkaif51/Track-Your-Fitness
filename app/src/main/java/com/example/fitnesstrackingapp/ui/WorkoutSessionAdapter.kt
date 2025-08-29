package com.example.fitnesstrackingapp.ui

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.util.TimeUtils.formatDuration
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesstrackingapp.databinding.ItemWorkoutSessionBinding
import com.example.fitnesstrackingapp.model.WorkoutSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WorkoutSessionAdapter(
    private val onItemClick: (WorkoutSession) -> Unit,
    private val onDeleteClick: (WorkoutSession) -> Unit
) : ListAdapter<WorkoutSession, WorkoutSessionAdapter.WorkoutSessionViewHolder>(DiffCallback()) {

    class WorkoutSessionViewHolder(
        private val binding: ItemWorkoutSessionBinding,
        private val onItemClick: (WorkoutSession) -> Unit,
        private val onDeleteClick: (WorkoutSession) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(session: WorkoutSession) {
            binding.apply {
                tvWorkoutType.text = session.workoutType
                tvWorkoutDate.text = formatDate(session.startTime)
                tvWorkoutDuration.text = formatDuration(session.startTime, session.endTime)

                // ✅ Always show distance & calories
                tvWorkoutDistance.text = formatDistance(session.distance)
                tvWorkoutCalories.text = formatCalories(session.caloriesBurned)

                Log.d("AdapterBind", "Distance: ${session.distance}, Calories: ${session.caloriesBurned}")

                root.setOnClickListener { onItemClick(session) }
                btnDelete.setOnClickListener { onDeleteClick(session) }
            }
        }

        private fun formatDate(timestamp: Long): String {
            val date = Date(timestamp)
            val format = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
            return format.format(date)
        }

        private fun formatDistance(distance: Double): String {
            if(distance < 1000){
            return "%.2f km".format(distance/1000)
        }else{
            return "%.2f km".format(distance)}
        }

        private fun formatCalories(calories: Double): String {
            return "%.1f cal".format(calories)
        }

        @SuppressLint("DefaultLocale")
        private fun formatDuration(startTime: Long, endTime: Long?): String {
            val end = endTime ?: System.currentTimeMillis()
            val duration = end - startTime
            val seconds = (duration / 1000) % 60
            val minutes = (duration / (1000 * 60)) % 60
            val hours = (duration / (1000 * 60 * 60))

            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<WorkoutSession>() {
        override fun areItemsTheSame(oldItem: WorkoutSession, newItem: WorkoutSession): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: WorkoutSession, newItem: WorkoutSession): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutSessionViewHolder {
        val binding = ItemWorkoutSessionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WorkoutSessionViewHolder(binding, onItemClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: WorkoutSessionViewHolder, position: Int) {
        val session = getItem(position)
        holder.bind(session)
    }

    fun deleteItem(position: Int) {
        val currentList = currentList.toMutableList()
        if (position in 0 until currentList.size) {
            val removedItem = currentList.removeAt(position)
            submitList(currentList)
            onDeleteClick(removedItem)

            Log.d("delete", "deleteItem:${onDeleteClick(removedItem)} ")

        }

    }
}
