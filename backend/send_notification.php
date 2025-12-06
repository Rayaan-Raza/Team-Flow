<?php
require_once 'api_config.php';

/**
 * Send Push Notification API (FCM V1)
 * Sends FCM push notification to specific users using V1 API
 * Expected POST parameters:
 * - user_uids: Array of user UIDs or single UID
 * - title: Notification title
 * - body: Notification body
 * - data: (optional) Additional data payload (JSON object)
 */

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    sendResponse(false, null, 'Method not allowed', 405);
}

// Get JSON input
$input = getJsonInput();
if (!$input) {
    validateParams(['user_uids', 'title', 'body']);
    $user_uids = getParam('user_uids');
    $title = getParam('title');
    $body = getParam('body');
    $data = getParam('data', '{}');
} else {
    $user_uids = $input['user_uids'] ?? null;
    $title = $input['title'] ?? null;
    $body = $input['body'] ?? null;
    $data = $input['data'] ?? [];
}

if (empty($user_uids) || empty($title) || empty($body)) {
    sendResponse(false, null, 'user_uids, title, and body are required', 400);
}

// Convert single UID to array
if (!is_array($user_uids)) {
    $user_uids = [$user_uids];
}

$conn = getDbConnection();

try {
    // Get FCM tokens for users
    $placeholders = str_repeat('?,', count($user_uids) - 1) . '?';
    $stmt = $conn->prepare("SELECT DISTINCT token FROM fcm_tokens WHERE user_uid IN ($placeholders)");
    
    $types = str_repeat('s', count($user_uids));
    $stmt->bind_param($types, ...$user_uids);
    $stmt->execute();
    $result = $stmt->get_result();
    
    $tokens = [];
    while ($row = $result->fetch_assoc()) {
        $tokens[] = $row['token'];
    }
    $stmt->close();
    $conn->close();
    
    if (empty($tokens)) {
        sendResponse(false, null, 'No FCM tokens found for specified users', 404);
    }
    
    // Get Firebase project ID from service account
    $serviceAccount = json_decode(file_get_contents(FCM_SERVICE_ACCOUNT_PATH), true);
    $projectId = $serviceAccount['project_id'];
    
    // Get access token
    $accessToken = getFcmAccessToken();
    
    // Prepare FCM data
    $fcm_data = is_string($data) ? json_decode($data, true) : $data;
    
    // Send to FCM V1 API
    $success_count = 0;
    $failure_count = 0;
    
    foreach ($tokens as $token) {
        $message = [
            'message' => [
                'token' => $token,
                'notification' => [
                    'title' => $title,
                    'body' => $body
                ],
                'data' => $fcm_data,
                'android' => [
                    'priority' => 'high'
                ]
            ]
        ];
        
        $ch = curl_init("https://fcm.googleapis.com/v1/projects/{$projectId}/messages:send");
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, [
            'Authorization: Bearer ' . $accessToken,
            'Content-Type: application/json'
        ]);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($message));
        
        $response = curl_exec($ch);
        $http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        
        if ($http_code === 200) {
            $success_count++;
        } else {
            $failure_count++;
        }
    }
    
    sendResponse(true, [
        'total_tokens' => count($tokens),
        'success_count' => $success_count,
        'failure_count' => $failure_count,
        'title' => $title,
        'body' => $body
    ]);
    
} catch (Exception $e) {
    sendResponse(false, null, $e->getMessage(), 500);
}
?>
