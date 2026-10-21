package dev.local.haze.sim;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Owner-only native replay recordings are separate from this source repository. */
final class RetailTestFixtures {
    static String read(Path path) throws IOException {
        assumeTrue(Files.isRegularFile(path),()->"Missing owner-only native replay recording: "+path);
        return Files.readString(path);
    }
}
