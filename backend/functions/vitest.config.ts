import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    // Emulator tests share one Firestore; running files in parallel would let them clear each other's data.
    fileParallelism: false,
  },
});
