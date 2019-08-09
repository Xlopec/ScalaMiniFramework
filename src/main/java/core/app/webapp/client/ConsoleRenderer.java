package core.app.webapp.client;

import core.di.annotation.Component;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

@Component
public final class ConsoleRenderer implements IRenderer {

    @Override
    public void render(@NotNull Reader reader) throws IOException {
        final BufferedReader input = new BufferedReader(reader);
        final String answer = input.readLine();

        System.out.println(answer);
    }
}
