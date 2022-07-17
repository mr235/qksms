package com.moez.QKSMS.receiver

import android.content.*
import android.widget.Toast

const val ACTION_COPY_VERIFY_CODE = "ACTION_COPY_VERIFY_CODE"
const val INTENT_VERIFY_CODE = "INTENT_VERIFY_CODE"

class MessageCodeCopyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action?.startsWith(ACTION_COPY_VERIFY_CODE) == true) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val code = intent.getStringExtra(INTENT_VERIFY_CODE)
            clipboard.setPrimaryClip(ClipData.newPlainText(code, code))
            Toast.makeText(context, "复制:$code", Toast.LENGTH_SHORT).show()
        }
    }
}