const functions = require('firebase-functions');
const admin = require('firebase-admin');
const {onCall, HttpsError} = require("firebase-functions/v2/https");
const {logger} = require("firebase-functions/v2");
admin.initializeApp();

/**
 * Cloud function to send notifications to all admin users in a session
 * 
 * Expected request body:
 * {
 *   "sessionId": "548742",
 *   "title": "Alert Title",
 *   "message": "Alert Message Content"
 * }
 */
exports.sendAdminNotifications = functions.https.onCall(async (request, context) => {
  try {
  
    const sessionId = request.data.sessionId;
    const title = request.data.title;
    const message = request.data.message;
    
    
    // Get all users in the specified session
    const sessionRef = admin.database().ref(`sessions/${sessionId}/users`);
    const snapshot = await sessionRef.once('value');
    const users = snapshot.val();
    
    // Find all admin users with notification tokens
    const adminTokens = [];
    Object.values(users).forEach(user => {
      if (user.role === 'Admin' && user.not_token && user.not_token.trim() !== '') {
        adminTokens.push(user.not_token);
      }
    });
    
    if (adminTokens.length === 0) {
      return { success: false, message: 'No admin users with FCM tokens found in this session' };
    }
    
    // Prepare the notification message
    const notification = {
      data: {
        sessionId: sessionId,
        title: title,        
        message: message,    

      }
    };
    
    // Send notifications to all admin tokens at once
    const response = await admin.messaging().sendEachForMulticast({
      tokens: adminTokens,
      data: notification.data
    });
    
    // Return the overall result
    return {
      success: true,
      message: `Notifications sent to ${adminTokens.length} admin users`,
      successCount: response.successCount,
      failureCount: response.failureCount
    };
  } catch (error) {
    console.error('Error in sendAdminNotifications:', error);
    throw new functions.https.HttpsError('internal', error.message);
  }
});