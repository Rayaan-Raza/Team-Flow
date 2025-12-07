<?php
require_once 'api_config.php';

/**
 * Send Message API
 * Stores message in MySQL database
 * Expected POST parameters:
 * - message_id: Unique message ID
 * - conversation_id: Conversation ID (format: conv_uid1_uid2)
 * - sender_uid: Sender's Firebase UID
 * - receiver_uid: Receiver's Firebase UID (optional, extracted from conversation_id if not provided)
 * - message_text: Message content (optional if image is sent)
 * - image_base64: Base64 encoded image (optional)
 * - timestamp: Message timestamp (milliseconds)
 */

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    sendResponse(false, null, 'Method not allowed', 405);
}

// Get parameters
$message_id = getParam('message_id');
$conversation_id = getParam('conversation_id');
$sender_uid = getParam('sender_uid');
$receiver_uid = getParam('receiver_uid', null);
$message_text = getParam('message_text', '');
$image_base64 = getParam('image_base64', null);
$timestamp = getParam('timestamp');
$project_id = getParam('project_id', null);
$task_id = getParam('task_id', null);

// Validate required fields
if (empty($message_id) || empty($conversation_id) || empty($sender_uid) || empty($timestamp)) {
    sendResponse(false, null, 'Missing required parameters: message_id, conversation_id, sender_uid, timestamp', 400);
}

// At least one of message_text or image_base64 must be provided
if (empty($message_text) && empty($image_base64)) {
    sendResponse(false, null, 'Either message_text or image_base64 must be provided', 400);
}

// Extract receiver_uid from conversation_id if not provided
// Format: conv_uid1_uid2
if (empty($receiver_uid) && !empty($conversation_id)) {
    $parts = explode('_', str_replace('conv_', '', $conversation_id));
    if (count($parts) >= 2) {
        // Find the UID that's not the sender
        foreach ($parts as $part) {
            if (!empty($part) && $part !== $sender_uid) {
                $receiver_uid = $part;
                break;
            }
        }
    }
}

// Validate receiver_uid
if (empty($receiver_uid)) {
    sendResponse(false, null, 'Could not determine receiver_uid', 400);
}

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

    // Insert message with receiver_uid
    $stmt = $conn->prepare("INSERT INTO messages (message_id, conversation_id, sender_uid, receiver_uid, message_text, image_base64, timestamp, is_read) VALUES (?, ?, ?, ?, ?, ?, ?, FALSE)");
    $stmt->bind_param("ssssssi", $message_id, $conversation_id, $sender_uid, $receiver_uid, $message_text, $image_base64, $timestamp);

    if (!$stmt->execute()) {
        throw new Exception("Failed to insert message: " . $stmt->error);
    }

    $inserted_id = $conn->insert_id;
    $stmt->close();

    // Add sender to conversation participants
    $stmt = $conn->prepare("INSERT IGNORE INTO conversation_participants (conversation_id, user_uid) VALUES (?, ?)");
    $stmt->bind_param("ss", $conversation_id, $sender_uid);
    $stmt->execute();
    $stmt->close();

    // Add receiver to conversation participants
    $stmt = $conn->prepare("INSERT IGNORE INTO conversation_participants (conversation_id, user_uid) VALUES (?, ?)");
    $stmt->bind_param("ss", $conversation_id, $receiver_uid);
    $stmt->execute();
    $stmt->close();

    // Commit transaction
    $conn->commit();

    sendResponse(true, [
        'id' => $inserted_id,
        'message_id' => $message_id,
        'conversation_id' => $conversation_id,
        'sender_uid' => $sender_uid,
        'receiver_uid' => $receiver_uid,
        'timestamp' => $timestamp,
        'has_image' => !empty($image_base64)
    ]);

} catch (Exception $e) {
    $conn->rollback();
    sendResponse(false, null, $e->getMessage(), 500);
} finally {
    $conn->close();
}
?>