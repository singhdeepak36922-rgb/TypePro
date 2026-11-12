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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function serverTimestamp() {
  return new Date(Date.now() - 1000);
}

function validProfile(uid) {
  return {
    userId: uid,
    displayName: "Alice Typist",
    theme: "dark",
    language: "en",
    soundEnabled: true,
    cursorStyle: "line",
    keyboardLayout: "qwerty",
    defaultDuration: 60,
    defaultCategory: "beginner",
    dailyGoalMinutes: 15,
    showKeyboard: true,
    strictMode: false,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validResult(uid) {
  return {
    userId: uid,
    mode: "test",
    refId: "en-beginner-home-row",
    refTitle: "Home Row Warm Up",
    language: "en",
    durationSeconds: 60,
    elapsedSeconds: 60,
    grossWpm: 45.5,
    netWpm: 42.0,
    standardWpm: 43.0,
    accuracy: 96.4,
    errors: 3,
    totalTyped: 220,
    correctChars: 212,
    keystrokeErrors: 5,
    backspaceCount: 2,
    score: 40,
    createdAt: serverTimestamp(),
  };
}

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

test("Unauthenticated user cannot read or write user profile or results", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("results").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("keyStats").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("lessonProgress").get());
});

test("User cannot list root users collection (no PII scraping)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").get());
});

test("Authenticated owner can create, read, and update their profile settings", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(ref.set(validProfile(ALICE_UID)));
  await assertSucceeds(ref.get());
  await assertSucceeds(
    ref.update({
      dailyGoalMinutes: 25,
      strictMode: true,
      updatedAt: serverTimestamp(),
    }),
  );
});

test("Cross-user access to profile, results, keyStats, and lessonProgress is denied", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(BOB_UID).set(validProfile(BOB_UID));
    await db.collection("users").doc(BOB_UID).collection("results").doc("r1").set(validResult(BOB_UID));
    await db.collection("users").doc(BOB_UID).collection("keyStats").doc("k_61").set({
      userId: BOB_UID,
      keyChar: "a",
      correct: 10,
      incorrect: 1,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
    await db.collection("users").doc(BOB_UID).collection("lessonProgress").doc("home-row-keys").set({
      userId: BOB_UID,
      lessonSlug: "home-row-keys",
      completed: true,
      bestWpm: 35,
      bestAccuracy: 98,
      attempts: 1,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("results").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("results").doc("r2").set(validResult(ALICE_UID)));
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("keyStats").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("lessonProgress").get());
});

test("Shadow update with ghost field and invalid profile bounds are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection("users").doc(ALICE_UID);

  await assertFails(ref.set({ ...validProfile(ALICE_UID), isAdmin: true }));
  await assertSucceeds(ref.set(validProfile(ALICE_UID)));
  await assertFails(ref.update({ isVerified: true, updatedAt: serverTimestamp() }));
  await assertFails(ref.update({ dailyGoalMinutes: 500, updatedAt: serverTimestamp() }));
  await assertFails(ref.update({ userId: BOB_UID, updatedAt: serverTimestamp() }));
});

test("Owner can create, list, and delete typing results, but cannot update them", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const resRef = aliceDb.collection("users").doc(ALICE_UID).collection("results").doc("res_1");

  await assertSucceeds(resRef.set(validResult(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("results").get());
  await assertFails(resRef.update({ netWpm: 999 }));
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("results").doc("res_bad").set({
      ...validResult(ALICE_UID),
      accuracy: 150,
    }),
  );
  await assertSucceeds(resRef.delete());
});

test("Owner can create, update, and list keyStats and lessonProgress", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const keyRef = aliceDb.collection("users").doc(ALICE_UID).collection("keyStats").doc("k_61");
  await assertSucceeds(
    keyRef.set({
      userId: ALICE_UID,
      keyChar: "a",
      correct: 20,
      incorrect: 2,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    }),
  );
  await assertSucceeds(keyRef.update({ correct: 25, incorrect: 3, updatedAt: serverTimestamp() }));
  await assertFails(keyRef.update({ keyChar: "b", updatedAt: serverTimestamp() }));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("keyStats").get());

  const lessonRef = aliceDb.collection("users").doc(ALICE_UID).collection("lessonProgress").doc("home-row-keys");
  await assertSucceeds(
    lessonRef.set({
      userId: ALICE_UID,
      lessonSlug: "home-row-keys",
      completed: true,
      bestWpm: 32.5,
      bestAccuracy: 97.2,
      attempts: 1,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    }),
  );
  await assertSucceeds(lessonRef.update({ bestWpm: 40.0, attempts: 2, updatedAt: serverTimestamp() }));
  await assertFails(lessonRef.update({ lessonSlug: "other-lesson", updatedAt: serverTimestamp() }));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("lessonProgress").get());
});
