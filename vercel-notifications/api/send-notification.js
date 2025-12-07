const admin = require('firebase-admin');

// Initialize Firebase Admin SDK
let app;
try {
    const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
    app = admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
    });
} catch (e) {
    // Already initialized
    app = admin.app();
}

/**
 * Send Push Notification API
 * POST /api/send-notification
 * 
 * Body:
 * - token: FCM device token
 * - title: Notification title
 * - body: Notification body
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
        const { token, title, body, data } = req.body;

        if (!token || !title || !body) {
            return res.status(400).json({
                success: false,
                error: 'Missing required fields: token, title, body'
            });
        }

        const message = {
            token: token,
            notification: {
                title: title,
                body: body
            },
            data: data || {},
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
            messageId: response
        });

    } catch (error) {
        console.error('FCM Error:', error);
        return res.status(500).json({
            success: false,
            error: error.message
        });
    }
};
