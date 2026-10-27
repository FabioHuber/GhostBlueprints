package dev.ghostblueprints;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Locates .schem files in the plugin folder and, optionally, in WorldEdit/FAWE schematic folders. */
public final class BlueprintLibrary {
    private static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z0-9_\\-. ]+");
    private static final String EXTENSION = ".schem";

    private final Path ownFolder;
    private final Path pluginsFolder;
    private final Supplier<Settings> settings;

    public BlueprintLibrary(Path ownFolder, Path pluginsFolder, Supplier<Settings> settings) {
        this.ownFolder = ownFolder;
        this.pluginsFolder = pluginsFolder;
        this.settings = settings;
    }

    public Optional<Path> find(String name) {
        if (!SAFE_NAME.matcher(name).matches() || name.contains("..")) {
            return Optional.empty();
        }
        return folders().stream()
                .map(folder -> folder.resolve(name + EXTENSION))
                .filter(Files::isRegularFile)
                .findFirst();
    }

    public List<String> names() throws IOException {
        List<String> names = new ArrayList<>();
        for (Path folder : folders()) {
            if (!Files.isDirectory(folder)) {
                continue;
            }
            try (Stream<Path> files = Files.list(folder)) {
                files.map(path -> path.getFileName().toString())
                        .filter(file -> file.endsWith(EXTENSION))
                        .map(file -> file.substring(0, file.length() - EXTENSION.length()))
                        .filter(name -> !names.contains(name))
                        .forEach(names::add);
            }
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    private List<Path> folders() {
        List<Path> folders = new ArrayList<>();
        folders.add(ownFolder);
        if (settings.get().importWorldEditFolders()) {
            folders.add(pluginsFolder.resolve("WorldEdit").resolve("schematics"));
            folders.add(pluginsFolder.resolve("FastAsyncWorldEdit").resolve("schematics"));
        }
        return folders;
    }
}
