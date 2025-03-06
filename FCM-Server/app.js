const messaging = admin.messaging();
const express = require("express");
const bodyParser = require("body-parser");
const admin = require("firebase-admin");
const serviceAccount = require('C:\Users\ethan\OneDrive\Documents\FirebaseKey\fyp-2025-firebase-adminsdk-oizmp-2c6207714a.json');
const cors = require('cors');
require('dotenv').config();

admin.initializeApp({
	credential: admin.credential.cert(serviceAccount),
	databaseURL: 'https://fyp-2025-default-rtdb.europe-west1.firebasedatabase.app/'
});

module.exports = messaging;

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(bodyParser.json());

app.post('/api/send-admin-notifications', async (req, res) => {
  try {
    const { sessionId, title, message } = req.body;
    
    if (!sessionId || !title || !message) {
      return res.status(400).json({ error: 'Missing required parameters' });
    }
    
    // Get admin tokens from Firebase
    const adminTokens = await getAdminTokensForSession(sessionId);
    
    if (adminTokens.length === 0) {
      return res.status(404).json({ message: 'No admin users with tokens found in this session' });
    }
    
    // Send notifications to all admin tokens
    const results = await sendNotificationsToTokens(adminTokens, title, message);
    
    res.status(200).json({ 
      success: true, 
      message: `Notifications sent to ${adminTokens.length} admins`, 
      results 
    });
  } catch (error) {
    console.error('Error sending notifications:', error);
    res.status(500).json({ error: 'Failed to send notifications', details: error.message });
  }
});

async function getAdminTokensForSession(sessionId) {
  try {
    const sessionsRef = admin.database().ref(`sessions/${sessionId}/users`);
    const snapshot = await sessionsRef.once('value');
    const sessionData = snapshot.val();
    
    const adminTokens = [];
    
    if (!sessionData) {
      console.log('No session data found for session ID:', sessionId);
      return adminTokens;
    }
    
    // Extract tokens for users with "Admin" role
    Object.values(sessionData).forEach(user => {
      if (user.role === 'Admin' && user.not_token && user.not_token.trim() !== '') {
        adminTokens.push(user.not_token);
      }
    });
    
    return adminTokens;
  } catch (error) {
    console.error('Error getting admin tokens:', error);
    throw error;
  }
}

async function sendNotificationsToTokens(tokens, title, message) {
  try {
    const notification = {
      notification: {
        title: title,
        body: message
      }
    };
    
    // For each token, send a notification
    const sendPromises = tokens.map(token => {
      return admin.messaging().sendToDevice(token, notification)
        .then(response => {
          console.log('Successfully sent message to token:', token);
          return { token, success: true, response: response };
        })
        .catch(error => {
          console.error('Error sending message to token:', token, error);
          return { token, success: false, error: error.message };
        });
    });
    
    return Promise.all(sendPromises);
  } catch (error) {
    console.error('Error in sendNotificationsToTokens:', error);
    throw error;
  }
}

// Start server
app.listen(PORT, () => {
  console.log(`FCM Notification server running on port ${PORT}`);
});