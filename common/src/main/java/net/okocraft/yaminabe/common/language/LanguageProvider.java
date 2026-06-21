package net.okocraft.yaminabe.common.language;

import dev.siroshun.mcmsgdef.DefaultMessageDefiner;
import dev.siroshun.mcmsgdef.directory.DirectorySource;
import dev.siroshun.mcmsgdef.directory.MessageProcessors;
import dev.siroshun.mcmsgdef.file.PropertiesFile;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.Translator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class LanguageProvider {

    private static final Key LANGUAGE_KEY = Key.key("yaminabe", "language");

    public static void load(Path directory, List<DefaultMessageDefiner> defaultMessages) throws IOException {
        DirectorySource.propertiesFiles(directory)
            .defaultLocale(Locale.ENGLISH, Locale.JAPANESE)
            .primaryLocale(Locale.ENGLISH)
            .messageProcessor(MessageProcessors.appendMissingMessagesToPropertiesFile(locale -> {
                if (locale.equals(Locale.ENGLISH)) {
                    Map<String, String> messageMap = new LinkedHashMap<>();
                    for (DefaultMessageDefiner definer : defaultMessages) {
                        messageMap.putAll(definer.getCollectedMessages());
                    }
                    return messageMap;
                }

                try (InputStream input = LanguageProvider.class.getClassLoader().getResourceAsStream("languages/" + locale + ".properties")) {
                    return input != null ? PropertiesFile.load(input) : null;
                }
            }))
            .loadAndRegister(LANGUAGE_KEY);
    }

    public static void unload() {
        for (Translator source : GlobalTranslator.translator().sources()) {
            if (source.name().equals(LANGUAGE_KEY)) {
                GlobalTranslator.translator().removeSource(source);
            }
        }
    }
}
