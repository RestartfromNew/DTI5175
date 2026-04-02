const stripe = require('stripe')('sk_test_51TFmAtPVVT1phEo73BS5bBSQQ2WxTvYZRlZRUnQrRJ8raQk4HBQHLchsQ5O2oHPjxoWr6pnFQX8xv7byrFfvqBox00IUbRcObs');
const { execSync } = require('child_process');

async function main() {
  console.log("Creating webhook in Stripe...");
  const webhookUrl = 'https://us-central1-chat-chat-chat-fc723.cloudfunctions.net/stripeWebhook';
  
  try {
    const webhookEndpoint = await stripe.webhookEndpoints.create({
      url: webhookUrl,
      enabled_events: [
        'payment_intent.succeeded',
      ],
    });
    console.log("Webhook created:", webhookEndpoint.id);
    console.log("Webhook Secret is:", webhookEndpoint.secret);
    
    // Save it to firebase
    console.log("Setting STRIPE_WEBHOOK_SECRET in Firebase...");
    execSync(`echo "${webhookEndpoint.secret}" | firebase functions:secrets:set STRIPE_WEBHOOK_SECRET --project chat-chat-chat-fc723`);
    console.log("Secret set successfully.");
  } catch (err) {
    console.log("Error creating webhook or setting secret:", err.message);
  }
}

main().catch(console.error);
