package net.behavr.api;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Loads a {@code .env} file from the working directory into {@linkplain System#setProperty system
 * properties} so Spring {@code ${...}} placeholders resolve. Missing file is ignored.
 * <p>
 * Values already present in the OS environment are not overridden (env wins over {@code .env}).
 */
public final class DotenvBootstrap {

    private DotenvBootstrap() {
    }

    public static void apply() {
        Dotenv dotenv =
                Dotenv.configure()
                        .ignoreIfMalformed()
                        .ignoreIfMissing()
                        .load();
        dotenv
                .entries()
                .forEach(
                        e -> {
                            String key = e.getKey();
                            if (System.getenv(key) != null) {
                                return;
                            }
                            if (System.getProperty(key) != null) {
                                return;
                            }
                            System.setProperty(key, e.getValue());
                        });
    }
}
