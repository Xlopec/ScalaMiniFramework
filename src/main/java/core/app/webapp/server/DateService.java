package core.app.webapp.server;

import core.di.annotation.Service;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

@Service
public final class DateService implements IDateService {

    @NotNull
    @Override
    public Date getCurrentDate() {
        return new Date();
    }
}
