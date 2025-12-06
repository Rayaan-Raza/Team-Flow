&lt;?php
require_once 'api_config.php';

/**
 * Send Message API
 * Stores message in MySQL database
 * Expected POST parameters:
 * - message_id: Unique message ID (from Firebase)
 * - conversation_id: Conversation ID
 * - sender_uid: Sender's Firebase UID
 * - message_text: Message content
 * - timestamp: Message timestamp (milliseconds)
 */

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    sendResponse(false, null, 'Method not allowed', 405);
}

// Validate required parameters
validateParams(['message_id', 'conversation_id', 'sender_uid', 'message_text', 'timestamp']);

$message_id = getParam('message_id');
$conversation_id = getParam('conversation_id');
$sender_uid = getParam('sender_uid');
$message_text = getParam('message_text');
$timestamp = getParam('timestamp');
$project_id = getParam('project_id', null);
$task_id = getParam('task_id', null);

$conn = getDbConnection();

try {
    // Start transaction
    $conn->begin_transaction();
    
    // Check if conversation exists, if not create it
    $stmt = $conn->prepare("SELECT id FROM conversations WHERE conversation_id = ?");
    $stmt->bind_param("s", $conversation_id);
    $stmt->execute();
    $result = $stmt->get_result();
    
    if ($result->num_rows === 0) {
        // Create conversation
        $stmt = $conn->prepare("INSERT INTO conversations (conversation_id, project_id, task_id) VALUES (?, ?, ?)");
        $stmt->bind_param("sss", $conversation_id, $project_id, $task_id);
        $stmt->execute();
    }
    $stmt->close();
    
    // Insert message
    $stmt = $conn->prepare("INSERT INTO messages (message_id, conversation_id, sender_uid, message_text, timestamp, is_read) VALUES (?, ?, ?, ?, ?, FALSE)");
    $stmt->bind_param("ssssi", $message_id, $conversation_id, $sender_uid, $message_text, $timestamp);
    
    if (!$stmt->execute()) {
        throw new Exception("Failed to insert message: " . $stmt->error);
    }
    
    $inserted_id = $conn->insert_id;
    $stmt->close();
    
    // Add sender to conversation participants if not already added
    $stmt = $conn->prepare("INSERT IGNORE INTO conversation_participants (conversation_id, user_uid) VALUES (?, ?)");
    $stmt->bind_param("ss", $conversation_id, $sender_uid);
    $stmt->execute();
    $stmt->close();
    
    // Commit transaction
    $conn->commit();
    
    sendResponse(true, [
        'id' => $inserted_id,
        'message_id' => $message_id,
        'conversation_id' => $conversation_id,
        'timestamp' => $timestamp
    ]);
    
} catch (Exception $e) {
    $conn->rollback();
    sendResponse(false, null, $e->getMessage(), 500);
} finally {
    $conn->close();
}
?&gt;
