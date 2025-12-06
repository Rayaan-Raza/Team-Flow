&lt;?php
require_once 'api_config.php';

/**
 * Get Conversations API
 * Retrieves all conversations for a user
 * Expected GET/POST parameters:
 * - user_uid: User's Firebase UID
 */

$user_uid = isset($_GET['user_uid']) ? $_GET['user_uid'] : getParam('user_uid');

if (empty($user_uid)) {
    sendResponse(false, null, 'user_uid is required', 400);
}

$conn = getDbConnection();

try {
    // Get conversations where user is a participant
    $stmt = $conn->prepare("
        SELECT 
            c.conversation_id,
            c.project_id,
            c.task_id,
            c.created_at,
            c.updated_at,
            (SELECT COUNT(*) FROM messages WHERE conversation_id = c.conversation_id) as message_count,
            (SELECT COUNT(*) FROM messages WHERE conversation_id = c.conversation_id AND is_read = FALSE AND sender_uid != ?) as unread_count,
            (SELECT message_text FROM messages WHERE conversation_id = c.conversation_id ORDER BY timestamp DESC LIMIT 1) as last_message,
            (SELECT timestamp FROM messages WHERE conversation_id = c.conversation_id ORDER BY timestamp DESC LIMIT 1) as last_message_time,
            (SELECT sender_uid FROM messages WHERE conversation_id = c.conversation_id ORDER BY timestamp DESC LIMIT 1) as last_sender_uid
        FROM conversations c
        INNER JOIN conversation_participants cp ON c.conversation_id = cp.conversation_id
        WHERE cp.user_uid = ?
        ORDER BY last_message_time DESC
    ");
    
    $stmt->bind_param("ss", $user_uid, $user_uid);
    $stmt->execute();
    $result = $stmt->get_result();
    
    $conversations = [];
    while ($row = $result->fetch_assoc()) {
        $conversations[] = [
            'conversation_id' => $row['conversation_id'],
            'project_id' => $row['project_id'],
            'task_id' => $row['task_id'],
            'message_count' => intval($row['message_count']),
            'unread_count' => intval($row['unread_count']),
            'last_message' => $row['last_message'],
            'last_message_time' => $row['last_message_time'] ? intval($row['last_message_time']) : null,
            'last_sender_uid' => $row['last_sender_uid'],
            'created_at' => $row['created_at'],
            'updated_at' => $row['updated_at']
        ];
    }
    
    $stmt->close();
    
    sendResponse(true, [
        'conversations' => $conversations,
        'total' => count($conversations)
    ]);
    
} catch (Exception $e) {
    sendResponse(false, null, $e->getMessage(), 500);
} finally {
    $conn->close();
}
?&gt;
