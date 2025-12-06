&lt;?php
require_once 'api_config.php';

/**
 * Register FCM Token API
 * Registers or updates a user's FCM token for push notifications
 * Expected POST parameters:
 * - user_uid: User's Firebase UID
 * - token: FCM token
 * - device_id: (optional) Device identifier
 */

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    sendResponse(false, null, 'Method not allowed', 405);
}

validateParams(['user_uid', 'token']);

$user_uid = getParam('user_uid');
$token = getParam('token');
$device_id = getParam('device_id', uniqid('device_'));

$conn = getDbConnection();

try {
    // Insert or update FCM token
    $stmt = $conn->prepare("
        INSERT INTO fcm_tokens (user_uid, token, device_id, updated_at) 
        VALUES (?, ?, ?, NOW())
        ON DUPLICATE KEY UPDATE 
        token = VALUES(token), 
        updated_at = NOW()
    ");
    
    $stmt->bind_param("sss", $user_uid, $token, $device_id);
    
    if (!$stmt->execute()) {
        throw new Exception("Failed to register token: " . $stmt->error);
    }
    
    $stmt->close();
    
    sendResponse(true, [
        'user_uid' => $user_uid,
        'device_id' => $device_id,
        'message' => 'FCM token registered successfully'
    ]);
    
} catch (Exception $e) {
    sendResponse(false, null, $e->getMessage(), 500);
} finally {
    $conn->close();
}
?&gt;
