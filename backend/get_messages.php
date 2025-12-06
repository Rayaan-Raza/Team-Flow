&lt;?php
require_once 'api_config.php';

/**
 * Get Messages API
 * Retrieves messages for a conversation from MySQL
 * Expected GET/POST parameters:
 * - conversation_id: Conversation ID
 * - limit: (optional) Number of messages to retrieve (default: 50)
 * - offset: (optional) Offset for pagination (default: 0)
 */

$conversation_id = isset($_GET['conversation_id']) ? $_GET['conversation_id'] : getParam('conversation_id');
$limit = isset($_GET['limit']) ? intval($_GET['limit']) : 50;
$offset = isset($_GET['offset']) ? intval($_GET['offset']) : 0;

if (empty($conversation_id)) {
    sendResponse(false, null, 'conversation_id is required', 400);
}

$conn = getDbConnection();

try {
    // Get messages
    $stmt = $conn->prepare("
        SELECT 
            m.message_id,
            m.conversation_id,
            m.sender_uid,
            m.message_text,
            m.timestamp,
            m.is_read,
            u.name as sender_name,
            u.email as sender_email
        FROM messages m
        LEFT JOIN users u ON m.sender_uid = u.firebase_uid
        WHERE m.conversation_id = ?
        ORDER BY m.timestamp ASC
        LIMIT ? OFFSET ?
    ");
    
    $stmt->bind_param("sii", $conversation_id, $limit, $offset);
    $stmt->execute();
    $result = $stmt->get_result();
    
    $messages = [];
    while ($row = $result->fetch_assoc()) {
        $messages[] = [
            'message_id' => $row['message_id'],
            'conversation_id' => $row['conversation_id'],
            'sender_uid' => $row['sender_uid'],
            'sender_name' => $row['sender_name'],
            'sender_email' => $row['sender_email'],
            'message_text' => $row['message_text'],
            'timestamp' => intval($row['timestamp']),
            'is_read' => boolval($row['is_read'])
        ];
    }
    
    $stmt->close();
    
    // Get total count
    $stmt = $conn->prepare("SELECT COUNT(*) as total FROM messages WHERE conversation_id = ?");
    $stmt->bind_param("s", $conversation_id);
    $stmt->execute();
    $result = $stmt->get_result();
    $total = $result->fetch_assoc()['total'];
    $stmt->close();
    
    sendResponse(true, [
        'messages' => $messages,
        'total' => intval($total),
        'limit' => $limit,
        'offset' => $offset
    ]);
    
} catch (Exception $e) {
    sendResponse(false, null, $e->getMessage(), 500);
} finally {
    $conn->close();
}
?&gt;
