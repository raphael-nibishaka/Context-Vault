package services;

import com.fasterxml.jackson.databind.ObjectMapper;
import config.ThemeManager;
import database.ConnectionFactory;
import database.DatabaseInitializer;
import database.DebugSampleDataSeeder;
import database.SampleDataSeeder;
import repository.ContextRepository;
import repository.DebugRepository;
import repository.JdbcContextRepository;
import repository.JdbcDebugRepository;
import repository.JdbcSettingsRepository;
import repository.SettingsRepository;
import viewmodels.ContextFormViewModel;
import viewmodels.DashboardViewModel;
import viewmodels.DebugFormViewModel;
import viewmodels.DebugMemoryViewModel;
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
        DebugRepository debugRepository = new JdbcDebugRepository(connectionFactory);
        SettingsRepository settingsRepository = new JdbcSettingsRepository(connectionFactory, objectMapper);

        SampleDataSeeder sampleDataSeeder = new SampleDataSeeder(contextRepository);
        sampleDataSeeder.seedIfEmpty();

        DebugSampleDataSeeder debugSampleDataSeeder = new DebugSampleDataSeeder(debugRepository);
        debugSampleDataSeeder.seedIfEmpty();

        ContextService contextService = new ContextService(contextRepository);
        DebugMemoryService debugMemoryService = new DebugMemoryService(debugRepository);
        SettingsService settingsService = new SettingsService(settingsRepository, themeManager);
        GitService gitService = new GitService();
        ExtensionBridgeService extensionBridgeService = new ExtensionBridgeService();
        ExternalLaunchService externalLaunchService = new ExternalLaunchService();
        RestoreService restoreService = new RestoreService(externalLaunchService, settingsService, gitService);
        ClipboardService clipboardService = new ClipboardService();

        MainViewModel mainViewModel = new MainViewModel();
        DashboardViewModel dashboardViewModel = new DashboardViewModel(contextService);
        ContextFormViewModel contextFormViewModel = new ContextFormViewModel(
                contextService,
                gitService,
                extensionBridgeService
        );
        SettingsViewModel settingsViewModel = new SettingsViewModel(settingsService);
        DebugMemoryViewModel debugMemoryViewModel = new DebugMemoryViewModel(debugMemoryService);
        DebugFormViewModel debugFormViewModel = new DebugFormViewModel(debugMemoryService);

        applicationCoordinator = new ApplicationCoordinator(
                mainViewModel,
                dashboardViewModel,
                contextFormViewModel,
                settingsViewModel,
                debugMemoryViewModel,
                debugFormViewModel,
                contextService,
                settingsService,
                restoreService,
                gitService,
                clipboardService,
                themeManager
        );
    }

    public ApplicationCoordinator getApplicationCoordinator() {
        return applicationCoordinator;
    }
}
