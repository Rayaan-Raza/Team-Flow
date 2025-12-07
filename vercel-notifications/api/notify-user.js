const admin = require('firebase-admin');

// Initialize Firebase Admin SDK
let app;
try {
    const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
    app = admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
        databaseURL: process.env.FIREBASE_DATABASE_URL || 'https://team-flow-1f54f-default-rtdb.firebaseio.com'
    });
} catch (e) {
    // Already initialized
    app = admin.app();
}

/**
 * Send Notification to User by UID
 * POST /api/notify-user
 * 
 * Looks up user's FCM token from Firebase RTDB and sends notification
 * 
 * Body:
 * - uid: Target user's Firebase UID
 * - title: Notification title
 * - body: Notification body
 * - type: Notification type (message, task_done, etc.)
 * - data: Additional data (optional)
 */
module.exports = async (req, res) => {
    // CORS headers
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

    if (req.method === 'OPTIONS') {
        return res.status(200).end();
    }

    if (req.method !== 'POST') {
        return res.status(405).json({ success: false, error: 'Method not allowed' });
    }

    try {
        const { uid, title, body, type, data } = req.body;

        if (!uid || !title || !body) {
            return res.status(400).json({
                success: false,
                error: 'Missing required fields: uid, title, body'
            });
        }

        // Get user's FCM token from Firebase RTDB
        // Token is stored at users/{uid}/fcmToken by the Android app
        const db = admin.database();
        const snapshot = await db.ref(`users/${uid}/fcmToken`).once('value');
        const fcmToken = snapshot.val();

        if (!fcmToken) {
            return res.status(404).json({
                success: false,
                error: 'FCM token not found for user'
            });
        }

        const message = {
            token: fcmToken,
            notification: {
                title: title,
                body: body
            },
            data: {
                type: type || 'general',
                ...(data || {})
            },
            android: {
                priority: 'high',
                notification: {
                    sound: 'default',
                    channelId: 'teamflow_notifications'
                }
            }
        };

        const response = await admin.messaging().send(message);

        return res.status(200).json({
            success: true,
            messageId: response,
            sentTo: uid
        });

    } catch (error) {
        console.error('FCM Error:', error);
        return res.status(500).json({
            success: false,
            error: error.message
        });
    }
};
