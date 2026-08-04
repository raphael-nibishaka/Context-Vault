package viewmodels;

import config.AppMetadata;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

public class AboutViewModel {
    private final ReadOnlyStringWrapper appName = new ReadOnlyStringWrapper(AppMetadata.APP_NAME);
    private final ReadOnlyStringWrapper version = new ReadOnlyStringWrapper(AppMetadata.APP_VERSION);
    private final ReadOnlyStringWrapper developer = new ReadOnlyStringWrapper(AppMetadata.APP_DEVELOPER);
    private final ReadOnlyStringWrapper tagline = new ReadOnlyStringWrapper(AppMetadata.APP_TAGLINE);

    public ReadOnlyStringProperty appNameProperty() {
        return appName.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty versionProperty() {
        return version.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty developerProperty() {
        return developer.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty taglineProperty() {
        return tagline.getReadOnlyProperty();
    }
}
