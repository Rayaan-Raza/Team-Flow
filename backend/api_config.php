<?php
/**
 * Team-Flow API Configuration
 * IMPORTANT: Change only the values below to configure your server
 */

// Database Configuration
define('DB_HOST', 'localhost');           // Change to your MySQL server IP
define('DB_USER', 'root');                // Change to your MySQL username
define('DB_PASS', '');                    // Change to your MySQL password
define('DB_NAME', 'teamflow_db');         // Database name

// Firebase Cloud Messaging V1 Configuration
// Path to your Firebase service account JSON file
define('FCM_SERVICE_ACCOUNT_PATH', __DIR__ . '/firebase-service-account.json');

// CORS Headers (allow requests from Android app)
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');
header('Content-Type: application/json; charset=UTF-8');

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

/**
 * Get database connection
 */
function getDbConnection() {
    try {
        $conn = new mysqli(DB_HOST, DB_USER, DB_PASS, DB_NAME);
        
        if ($conn->connect_error) {
            throw new Exception("Connection failed: " . $conn->connect_error);
        }
        
        $conn->set_charset("utf8mb4");
        return $conn;
        
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode([
            'success' => false,
            'error' => 'Database connection failed',
            'message' => $e->getMessage()
        ]);
        exit();
    }
}

/**
 * Get FCM access token using service account
 */
function getFcmAccessToken() {
    if (!file_exists(FCM_SERVICE_ACCOUNT_PATH)) {
        throw new Exception('Firebase service account file not found');
    }
    
    $serviceAccount = json_decode(file_get_contents(FCM_SERVICE_ACCOUNT_PATH), true);
    
    // Create JWT
    $now = time();
    $header = json_encode(['alg' => 'RS256', 'typ' => 'JWT']);
    $payload = json_encode([
        'iss' => $serviceAccount['client_email'],
        'scope' => 'https://www.googleapis.com/auth/firebase.messaging',
        'aud' => 'https://oauth2.googleapis.com/token',
        'iat' => $now,
        'exp' => $now + 3600
    ]);
    
    $base64UrlHeader = str_replace(['+', '/', '='], ['-', '_', ''], base64_encode($header));
    $base64UrlPayload = str_replace(['+', '/', '='], ['-', '_', ''], base64_encode($payload));
    
    $signature = '';
    openssl_sign(
        $base64UrlHeader . "." . $base64UrlPayload,
        $signature,
        $serviceAccount['private_key'],
        'SHA256'
    );
    
    $base64UrlSignature = str_replace(['+', '/', '='], ['-', '_', ''], base64_encode($signature));
    $jwt = $base64UrlHeader . "." . $base64UrlPayload . "." . $base64UrlSignature;
    
    // Exchange JWT for access token
    $ch = curl_init('https://oauth2.googleapis.com/token');
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, http_build_query([
        'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
        'assertion' => $jwt
    ]));
    
    $response = curl_exec($ch);
    curl_close($ch);
    
    $tokenData = json_decode($response, true);
    
    if (!isset($tokenData['access_token'])) {
        throw new Exception('Failed to get FCM access token');
    }
    
    return $tokenData['access_token'];
}

/**
 * Send JSON response
 */
function sendResponse($success, $data = null, $error = null, $code = 200) {
    http_response_code($code);
    echo json_encode([
        'success' => $success,
        'data' => $data,
        'error' => $error,
        'timestamp' => time()
    ]);
    exit();
}

/**
 * Validate required POST parameters
 */
function validateParams($required) {
    $missing = [];
    foreach ($required as $param) {
        if (!isset($_POST[$param]) || empty($_POST[$param])) {
            $missing[] = $param;
        }
    }
    
    if (!empty($missing)) {
        sendResponse(false, null, 'Missing required parameters: ' . implode(', ', $missing), 400);
    }
}

/**
 * Get POST parameter safely
 */
function getParam($key, $default = null) {
    return isset($_POST[$key]) ? trim($_POST[$key]) : $default;
}

/**
 * Get JSON input
 */
function getJsonInput() {
    $json = file_get_contents('php://input');
    return json_decode($json, true);
}
?>
