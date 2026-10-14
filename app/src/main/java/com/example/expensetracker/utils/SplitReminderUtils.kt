package com.example.expensetracker.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder

/**
 * Robust WhatsApp and SMS notification reminder utility for Split Bill participants.
 * Handles country code normalization (+91 for 10-digit Indian numbers), app package targeting,
 * browser fallbacks, and SMS intent dispatching across all Android versions.
 */
object SplitReminderUtils {

    /**
     * Normalizes a phone number for WhatsApp's API url (e.g. 917862819996).
     * WhatsApp requires digits only, with the international country code, and NO leading '+' or zeros.
     */
    fun normalizePhoneForWhatsApp(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        return when {
            digits.length == 10 -> "91$digits" // Default to India (+91) for standard 10-digit mobile
            digits.length == 11 && digits.startsWith("0") -> "91${digits.drop(1)}"
            digits.length == 12 && digits.startsWith("91") -> digits
            digits.length > 10 -> digits
            else -> digits
        }
    }

    /**
     * Formats a 10-digit phone number for clean, readable display (e.g. "+91 78628 19996").
     */
    fun formatPhoneForDisplay(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        return when {
            digits.length == 10 -> "+91 ${digits.substring(0, 5)} ${digits.substring(5)}"
            digits.length == 12 && digits.startsWith("91") -> {
                val sub = digits.substring(2)
                "+91 ${sub.substring(0, 5)} ${sub.substring(5)}"
            }
            phone.isNotBlank() -> phone.trim()
            else -> ""
        }
    }

    /**
     * Smartly extracts an exact 10-digit mobile number from pasted or typed input.
     * Strips international prefix (+91 / 91) or leading trunk zero (0) when pasted from contacts,
     * and strictly caps input to the FIRST 10 digits so that typing an 11th digit is ignored
     * and will NEVER drop digits from the front.
     */
    fun extractTenDigitNumber(input: String): String {
        val trimmed = input.trim()
        val digits = trimmed.filter { it.isDigit() }
        return when {
            // Explicit "+91..." international prefix (e.g. pasted or from contacts: "+91 98765 43210")
            (trimmed.startsWith("+91") || trimmed.startsWith("+ 91")) && digits.length >= 12 -> {
                digits.drop(2).take(10)
            }
            // Standard 12-digit format with leading "91" (e.g. from Google Contacts "919876543210")
            digits.length == 12 && digits.startsWith("91") -> {
                digits.substring(2).take(10)
            }
            // 11-digit format with leading trunk zero "0" (e.g. "09876543210")
            digits.length == 11 && digits.startsWith("0") -> {
                digits.substring(1).take(10)
            }
            // Normal typing or standard numbers: strictly take the FIRST 10 digits.
            // When already at 10 digits, typing an 11th digit will NOT be added from the end,
            // and will NOT push or remove front digits.
            else -> digits.take(10)
        }
    }

    /**
     * Builds a structured, beautifully formatted WhatsApp reminder card message.
     * Uses WhatsApp markdown (*bold*, _italic_) and clean unicode dividers to create
     * an aesthetic in-chat receipt structure with professional icons.
     */
    fun buildWhatsAppMessage(
        name: String,
        formattedAmount: String,
        categoryName: String,
        totalAmount: String = "",
        dateStr: String = "",
        note: String = ""
    ): String {
        val cleanName = name.trim().ifBlank { "Friend" }
        val categoryLabel = categoryName.trim().ifBlank { "Shared Expense" }

        return buildString {
            append("*Hello $cleanName,*\n\n")
            append("Please find the settlement details for our shared expense below:\n\n")
            append("━━━━━━━━━━━━━━━━━━━━━━\n")
            append("🧾 *EXPENSE SETTLEMENT*\n")
            append("━━━━━━━━━━━━━━━━━━━━━━\n")
            append("🏷️ *Category:* $categoryLabel\n")
            append("💳 *Amount Due:* *$formattedAmount*\n")

            if (totalAmount.isNotBlank() && totalAmount != formattedAmount) {
                append("📊 *Total Bill:* $totalAmount\n")
            }
            if (dateStr.isNotBlank()) {
                append("🗓️ *Date:* $dateStr\n")
            }
            if (note.isNotBlank()) {
                append("📌 *Note:* ${note.trim()}\n")
            }

            append("━━━━━━━━━━━━━━━━━━━━━━\n\n")
            append("Please arrange for settlement when convenient. Thank you.\n\n")
            append("_Sent via ExpenseTracker_")
        }
    }

    /**
     * Sends a reminder via WhatsApp.
     * Tries:
     * 1. Official WhatsApp app (com.whatsapp)
     * 2. WhatsApp Business app (com.whatsapp.w4b)
     * 3. Browser redirect (api.whatsapp.com)
     * 4. System share chooser fallback
     */
    fun sendWhatsAppReminder(
        context: Context,
        name: String,
        phone: String,
        formattedAmount: String,
        categoryName: String,
        totalAmount: String = "",
        dateStr: String = "",
        note: String = ""
    ) {
        val cleanPhone = normalizePhoneForWhatsApp(phone)
        val msg = buildWhatsAppMessage(
            name = name,
            formattedAmount = formattedAmount,
            categoryName = categoryName,
            totalAmount = totalAmount,
            dateStr = dateStr,
            note = note
        )
        val encodedMsg = try {
            URLEncoder.encode(msg, "UTF-8").replace("+", "%20")
        } catch (_: Exception) {
            msg
        }

        val whatsappUri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg")

        // 1. Try standard WhatsApp app
        try {
            val appIntent = Intent(Intent.ACTION_VIEW, whatsappUri).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(appIntent)
            return
        } catch (_: Exception) {}

        // 2. Try WhatsApp Business app
        try {
            val w4bIntent = Intent(Intent.ACTION_VIEW, whatsappUri).apply {
                setPackage("com.whatsapp.w4b")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(w4bIntent)
            return
        } catch (_: Exception) {}

        // 3. Try web browser / intent resolver
        try {
            val webIntent = Intent(Intent.ACTION_VIEW, whatsappUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            return
        } catch (_: Exception) {}

        // 4. Fallback to generic share chooser
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, msg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Remind $name").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open WhatsApp: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Sends a reminder via SMS.
     * Tries:
     * 1. ACTION_SENDTO with smsto: scheme
     * 2. ACTION_VIEW with sms: URI
     * 3. System text share chooser
     */
    fun sendSmsReminder(
        context: Context,
        name: String,
        phone: String,
        formattedAmount: String,
        categoryName: String
    ) {
        val cleanPhone = phone.replace(Regex("[^0-9+]"), "")
        val msg = "Hi $name, friendly reminder to settle your share of $formattedAmount for $categoryName in ExpenseTracker."

        // 1. Primary SMS SENDTO intent
        try {
            val smsUri = Uri.parse("smsto:$cleanPhone")
            val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                putExtra("sms_body", msg)
                putExtra(Intent.EXTRA_TEXT, msg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(smsIntent)
            return
        } catch (_: Exception) {}

        // 2. Alternative sms: view intent
        try {
            val encodedMsg = URLEncoder.encode(msg, "UTF-8")
            val altUri = Uri.parse("sms:$cleanPhone?body=$encodedMsg")
            val altIntent = Intent(Intent.ACTION_VIEW, altUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(altIntent)
            return
        } catch (_: Exception) {}

        // 3. Fallback to generic share chooser
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, msg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Send SMS reminder to $name").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open SMS: ${e.localizedMessage ?: "No messaging app found"}", Toast.LENGTH_SHORT).show()
        }
    }
}
