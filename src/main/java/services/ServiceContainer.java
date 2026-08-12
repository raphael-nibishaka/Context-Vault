package services;

import com.fasterxml.jackson.databind.ObjectMapper;
import config.ThemeManager;
import database.ConnectionFactory;
import database.DatabaseInitializer;
import database.SampleDataSeeder;
import repository.ContextRepository;
import repository.JdbcContextRepository;
import repository.JdbcSettingsRepository;
import repository.SettingsRepository;
import viewmodels.ContextFormViewModel;
import viewmodels.DashboardViewModel;
import viewmodels.MainViewModel;
import viewmodels.SettingsViewModel;

public class ServiceContainer {
    private final ApplicationCoordinator applicationCoordinator;

    public ServiceContainer() {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        DatabaseInitializer databaseInitializer = new DatabaseInitializer(connectionFactory);
        databaseInitializer.initialize();

        ObjectMapper objectMapper = new ObjectMapper();
        ThemeManager themeManager = new ThemeManager();

        ContextRepository contextRepository = new JdbcContextRepository(connectionFactory);
        SettingsRepository settingsRepository = new JdbcSettingsRepository(connectionFactory, objectMapper);

        SampleDataSeeder sampleDataSeeder = new SampleDataSeeder(contextRepository);
        sampleDataSeeder.seedIfEmpty();

        ContextService contextService = new ContextService(contextRepository);
        SettingsService settingsService = new SettingsService(settingsRepository, themeManager);
        GitService gitService = new GitService();
        ExternalLaunchService externalLaunchService = new ExternalLaunchService();
        RestoreService restoreService = new RestoreService(externalLaunchService, settingsService, gitService);
        ClipboardService clipboardService = new ClipboardService();

        MainViewModel mainViewModel = new MainViewModel();
        DashboardViewModel dashboardViewModel = new DashboardViewModel(contextService);
        ContextFormViewModel contextFormViewModel = new ContextFormViewModel(contextService);
        SettingsViewModel settingsViewModel = new SettingsViewModel(settingsService);

        applicationCoordinator = new ApplicationCoordinator(
                mainViewModel,
                dashboardViewModel,
                contextFormViewModel,
                settingsViewModel,
                contextService,
                settingsService,
                restoreService,
                clipboardService,
                themeManager
        );
    }

    public ApplicationCoordinator getApplicationCoordinator() {
        return applicationCoordinator;
    }
}
