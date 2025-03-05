const admin = require('firebase-admin');
const serviceAccount = require('C:\Users\ethan\OneDrive\Documents\FirebaseKey\fyp-2025-firebase-adminsdk-oizmp-2c6207714a.json');

admin.initializeApp({
 credential: admin.credential.cert(serviceAccount),
});

const messaging = admin.messaging();

module.exports = messaging;