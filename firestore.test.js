const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").get());
});

test("Authenticated user: can create their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Wonderland",
      username: "alicew",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: cannot create profile for another user", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob Fake",
      username: "bobfake",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Conversation security: non-member cannot read conversation or messages", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("conversations").doc("conv_1").set({
      conversationId: "conv_1",
      type: "direct",
      memberIds: [ALICE_UID, BOB_UID],
      adminIds: [ALICE_UID],
      createdAt: new Date(),
      updatedAt: new Date(),
    });
    await db.collection("conversations").doc("conv_1").collection("messages").doc("msg_1").set({
      messageId: "msg_1",
      conversationId: "conv_1",
      senderId: ALICE_UID,
      senderName: "Alice",
      messageType: "text",
      text: "Secret direct message",
      memberIds: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  await assertFails(charlieDb.collection("conversations").doc("conv_1").get());
  await assertFails(charlieDb.collection("conversations").doc("conv_1").collection("messages").doc("msg_1").get());

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("conversations").doc("conv_1").get());
  await assertSucceeds(aliceDb.collection("conversations").doc("conv_1").collection("messages").doc("msg_1").get());
});

test("Messages security: member can send message with matching memberIds", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("conversations").doc("conv_2").set({
      conversationId: "conv_2",
      type: "direct",
      memberIds: [ALICE_UID, BOB_UID],
      adminIds: [ALICE_UID],
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("conversations").doc("conv_2").collection("messages").doc("msg_bob").set({
      messageId: "msg_bob",
      conversationId: "conv_2",
      senderId: BOB_UID,
      senderName: "Bob",
      messageType: "text",
      text: "Hello Alice!",
      memberIds: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});
