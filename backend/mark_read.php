&lt;?php
require_once 'api_config.php';

/**
 * Mark Messages as Read API
 * Marks messages in a conversation as read for a user
 * Expected POST parameters:
 * - conversation_id: Conversation ID
 * - user_uid: User's Firebase UID (to exclude their own messages)
 */

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    sendResponse(false, null, 'Method not allowed', 405);
}

validateParams(['conversation_id', 'user_uid']);

$conversation_id = getParam('conversation_id');
$user_uid = getParam('user_uid');

$conn = getDbConnection();

try {
    // Mark all messages in conversation as read (except user's own messages)
    $stmt = $conn->prepare("
        UPDATE messages 
        SET is_read = TRUE 
        WHERE conversation_id = ? 
        AND sender_uid != ? 
        AND is_read = FALSE
    ");
    
    $stmt->bind_param("ss", $conversation_id, $user_uid);
    $stmt->execute();
    
    $affected_rows = $stmt->affected_rows;
    $stmt->close();
    
    sendResponse(true, [
        'conversation_id' => $conversation_id,
        'marked_read' => $affected_rows
    ]);
    
} catch (Exception $e) {
    sendResponse(false, null, $e->getMessage(), 500);
} finally {
    $conn->close();
}
?&gt;
