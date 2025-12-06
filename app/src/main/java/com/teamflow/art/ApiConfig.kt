package com.teamflow.art

/**
 * API Configuration
 * IMPORTANT: Change only the BASE_URL to match your server IP address
 */
object ApiConfig {
    // TODO: Replace with your server IP address
    private const val SERVER_IP = "192.168.0.106"  // Change this to your server IP
    private const val PORT = "80"  // Change if using different port
    
    const val BASE_URL = "http://$SERVER_IP:$PORT/Team-Flow/backend/"
    
    // API Endpoints
    const val SEND_MESSAGE = "${BASE_URL}send_message.php"
    const val GET_MESSAGES = "${BASE_URL}get_messages.php"
    const val GET_CONVERSATIONS = "${BASE_URL}get_conversations.php"
    const val MARK_READ = "${BASE_URL}mark_read.php"
    const val REGISTER_FCM_TOKEN = "${BASE_URL}register_fcm_token.php"
    const val SEND_NOTIFICATION = "${BASE_URL}send_notification.php"
    
    // Request timeout (milliseconds)
    const val TIMEOUT_MS = 30000
}
