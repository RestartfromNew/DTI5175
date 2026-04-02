import * as functions from "firebase-functions/v2"; // force redeploy to pick up new secret v2
import * as admin from "firebase-admin";
import Stripe from "stripe";

admin.initializeApp();

const getStripe = () =>
  new Stripe(process.env.STRIPE_SECRET_KEY!, { apiVersion: "2025-02-24.acacia" });

/**
 * Called by Android app when user selects a slot package.
 *
 * Request body: { priceId: string }
 * Response:     { clientSecret: string }
 */
export const createPaymentIntent = functions.https.onCall(
  { secrets: ["STRIPE_SECRET_KEY"], invoker: "public" },
  async (request) => {
    // Must be logged in
    if (!request.auth) {
      throw new functions.https.HttpsError("unauthenticated", "Login required");
    }

    const uid = request.auth.uid;
    const { priceId } = request.data as { priceId: string };

    if (!priceId || typeof priceId !== "string") {
      throw new functions.https.HttpsError("invalid-argument", "priceId is required");
    }

    const stripe = getStripe();

    // Fetch price from Stripe to get the amount
    const price = await stripe.prices.retrieve(priceId);
    if (!price.unit_amount) {
      throw new functions.https.HttpsError("internal", "Invalid price — no unit_amount");
    }

    // Read slot_increment from product metadata
    const product = await stripe.products.retrieve(price.product as string);
    const slotIncrement = parseInt(product.metadata.slot_increment ?? "1", 10);

    if (isNaN(slotIncrement) || slotIncrement < 1) {
      throw new functions.https.HttpsError("internal", "Invalid slot_increment in product metadata");
    }

    // Enforce max slot cap server-side
    const db = admin.firestore();
    const userDoc = await db.collection("users").doc(uid).get();
    const currentLimit = userDoc.data()?.slotLimit ?? 1;
    const MAX_SLOTS = 20;
    if (currentLimit + slotIncrement > MAX_SLOTS) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        `Slot limit would exceed maximum (${MAX_SLOTS})`
      );
    }

    // Create PaymentIntent — embed uid + slotIncrement so webhook knows what to do
    const paymentIntent = await stripe.paymentIntents.create({
      amount: price.unit_amount,
      currency: price.currency,
      automatic_payment_methods: { enabled: true },
      metadata: {
        uid,
        slotIncrement: slotIncrement.toString(),
        priceId,
      },
    });

    return { clientSecret: paymentIntent.client_secret };
  }
);

/**
 * Stripe webhook handler.
 * Verifies signature, then increments slotLimit in Firestore.
 */
export const stripeWebhook = functions.https.onRequest(
  { secrets: ["STRIPE_SECRET_KEY", "STRIPE_WEBHOOK_SECRET"], invoker: "public" },
  async (req, res) => {
    const stripe = getStripe();
    const sig = req.headers["stripe-signature"] as string;
    const webhookSecret = process.env.STRIPE_WEBHOOK_SECRET!;

    let event: Stripe.Event;

    // Verify the request genuinely came from Stripe
    try {
      event = stripe.webhooks.constructEvent(req.rawBody, sig, webhookSecret);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Unknown error";
      console.error("Webhook signature verification failed:", message);
      res.status(400).send(`Webhook Error: ${message}`);
      return;
    }

    if (event.type === "payment_intent.succeeded") {
      const paymentIntent = event.data.object as Stripe.PaymentIntent;
      const uid = paymentIntent.metadata.uid;
      const slotIncrement = parseInt(paymentIntent.metadata.slotIncrement ?? "1", 10);

      if (!uid) {
        console.error("No uid in PaymentIntent metadata:", paymentIntent.id);
        res.status(400).send("Missing uid");
        return;
      }

      if (isNaN(slotIncrement) || slotIncrement < 1) {
        console.error("Invalid slotIncrement in metadata:", paymentIntent.id);
        res.status(400).send("Invalid slotIncrement");
        return;
      }

      const db = admin.firestore();
      const processedRef = db.collection("processed_payments").doc(paymentIntent.id);

      // Idempotency: skip if already processed (Stripe may retry webhooks)
      const alreadyProcessed = await processedRef.get();
      if (alreadyProcessed.exists) {
        console.log("Already processed, skipping:", paymentIntent.id);
        res.status(200).send("Already processed");
        return;
      }

      const userRef = db.collection("users").doc(uid);

      // Atomic transaction: increment slotLimit + mark as processed
      await db.runTransaction(async (tx) => {
        const userDoc = await tx.get(userRef);
        if (!userDoc.exists) {
          throw new Error(`User not found: ${uid}`);
        }

        const currentLimit = userDoc.data()!.slotLimit ?? 1;
        const MAX_SLOTS = 20;
        const newLimit = Math.min(currentLimit + slotIncrement, MAX_SLOTS);

        tx.update(userRef, {
          slotLimit: newLimit,
          updatedAt: Date.now(),
        });

        tx.set(processedRef, {
          uid,
          slotIncrement,
          priceId: paymentIntent.metadata.priceId,
          processedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
      });

      console.log(`✅ Slot limit +${slotIncrement} for uid: ${uid}`);
    }

    res.status(200).json({ received: true });
  }
);
