package com.salmankhan.i221285.adapters

import android.app.AlertDialog
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.salmankhan.i221285.R
import com.salmankhan.i221285.StoryService
import com.salmankhan.i221285.models.Message
import java.text.SimpleDateFormat
import java.util.*

class MessagesAdapter(
    private val messages: List<Message>,
    private val currentUserId: String,
    private val onEditMessage: (Message, String) -> Unit,
    private val onDeleteMessage: (Message) -> Unit
) : RecyclerView.Adapter<MessagesAdapter.MessageViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageContainer: FrameLayout = itemView.findViewById(R.id.message_container)
        val messageBubble: LinearLayout = itemView.findViewById(R.id.message_bubble)
        val messageText: TextView = itemView.findViewById(R.id.message_text)
        val messageImage: ImageView = itemView.findViewById(R.id.message_image)
        val messageTime: TextView = itemView.findViewById(R.id.message_time)
        val editedLabel: TextView = itemView.findViewById(R.id.edited_label)

        fun bind(message: Message, context: Context) {
            // Handle deleted messages
            if (message.isDeleted) {
                messageText.text = "🚫 This message was deleted"
                messageText.setTextColor(context.getColor(R.color.gray))
                messageImage.visibility = View.GONE
                editedLabel.visibility = View.GONE
                messageBubble.setOnLongClickListener(null)
                return
            }

            // Display message based on type
            when (message.type) {
                "text" -> {
                    messageText.text = message.content
                    messageText.visibility = View.VISIBLE
                    messageImage.visibility = View.GONE
                }
                "image" -> {
                    val bitmap = StoryService.base64ToBitmap(message.content)
                    if (bitmap != null) {
                        messageImage.setImageBitmap(bitmap)
                        messageImage.visibility = View.VISIBLE
                        messageText.visibility = View.GONE
                    }
                }
                "post" -> {
                    messageText.text = "📄 Shared a post"
                    messageText.visibility = View.VISIBLE
                    messageImage.visibility = View.GONE
                }
            }

            // Set time
            messageTime.text = formatTime(message.createdAt)

            // Show "edited" label
            editedLabel.visibility = if (message.isEdited) View.VISIBLE else View.GONE

            // Check if sent by current user for edit/delete functionality
            val isSentByMe = message.senderId == currentUserId

            // Long click for edit/delete (only for sent messages and within 5 min)
            if (isSentByMe && message.canEditOrDelete() && message.type == "text") {
                messageBubble.setOnLongClickListener {
                    showEditDeleteDialog(message, context)
                    true
                }
            } else {
                messageBubble.setOnLongClickListener(null)
            }
        }

        private fun showEditDeleteDialog(message: Message, context: Context) {
            val options = arrayOf("Edit", "Delete")
            AlertDialog.Builder(context)
                .setTitle("Message Options")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> showEditDialog(message, context)
                        1 -> showDeleteConfirmDialog(message, context)
                    }
                }
                .show()
        }

        private fun showEditDialog(message: Message, context: Context) {
            val editText = EditText(context)
            editText.setText(message.content)
            editText.hint = "Edit message"

            AlertDialog.Builder(context)
                .setTitle("Edit Message")
                .setView(editText)
                .setPositiveButton("Save") { _, _ ->
                    val newText = editText.text.toString().trim()
                    if (newText.isNotEmpty()) {
                        onEditMessage(message, newText)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        private fun showDeleteConfirmDialog(message: Message, context: Context) {
            AlertDialog.Builder(context)
                .setTitle("Delete Message")
                .setMessage("Are you sure you want to delete this message?")
                .setPositiveButton("Delete") { _, _ ->
                    onDeleteMessage(message)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        private fun formatTime(timestamp: Long): String {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutId = if (viewType == VIEW_TYPE_SENT) {
            R.layout.item_message_sent
        } else {
            R.layout.item_message_received
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position], holder.itemView.context)
    }

    override fun getItemCount(): Int = messages.size
}

