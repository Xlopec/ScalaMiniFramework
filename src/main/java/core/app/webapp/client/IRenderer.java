package core.app.webapp.client;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;

public interface IRenderer {

    void render(@NotNull Reader reader) throws IOException;

}
