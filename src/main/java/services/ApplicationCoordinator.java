package services;

import config.Page;
import config.ThemeManager;
import models.ContextEntry;
import viewmodels.ContextFormViewModel;
import viewmodels.DashboardViewModel;
import viewmodels.MainViewModel;
import viewmodels.SettingsViewModel;

public class ApplicationCoordinator {
    private final MainViewModel mainViewModel;
    private final DashboardViewModel dashboardViewModel;
    private final ContextFormViewModel contextFormViewModel;
    private final SettingsViewModel settingsViewModel;
    private final ContextService contextService;
    private final SettingsService settingsService;
    private final RestoreService restoreService;
    private final ClipboardService clipboardService;
    private final ThemeManager themeManager;

    public ApplicationCoordinator(MainViewModel mainViewModel,
                                  DashboardViewModel dashboardViewModel,
                                  ContextFormViewModel contextFormViewModel,
                                  SettingsViewModel settingsViewModel,
                                  ContextService contextService,
                                  SettingsService settingsService,
                                  RestoreService restoreService,
                                  ClipboardService clipboardService,
                                  ThemeManager themeManager) {
        this.mainViewModel = mainViewModel;
        this.dashboardViewModel = dashboardViewModel;
        this.contextFormViewModel = contextFormViewModel;
        this.settingsViewModel = settingsViewModel;
        this.contextService = contextService;
        this.settingsService = settingsService;
        this.restoreService = restoreService;
        this.clipboardService = clipboardService;
        this.themeManager = themeManager;
    }

    public void initialize() {
        dashboardViewModel.loadContexts();
        settingsViewModel.load();
        mainViewModel.navigate(Page.DASHBOARD);
    }

    public void refreshContexts() {
        dashboardViewModel.loadContexts();
    }

    public void editContext(ContextEntry contextEntry) {
        contextFormViewModel.editContext(contextEntry);
        mainViewModel.navigate(Page.CREATE_CONTEXT);
    }

    public void createContext() {
        contextFormViewModel.prepareForCreate();
        mainViewModel.navigate(Page.CREATE_CONTEXT);
    }

    public MainViewModel getMainViewModel() {
        return mainViewModel;
    }

    public DashboardViewModel getDashboardViewModel() {
        return dashboardViewModel;
    }

    public ContextFormViewModel getContextFormViewModel() {
        return contextFormViewModel;
    }

    public SettingsViewModel getSettingsViewModel() {
        return settingsViewModel;
    }

    public ContextService getContextService() {
        return contextService;
    }

    public SettingsService getSettingsService() {
        return settingsService;
    }

    public RestoreService getRestoreService() {
        return restoreService;
    }

    public ClipboardService getClipboardService() {
        return clipboardService;
    }

    public ThemeManager getThemeManager() {
        return themeManager;
    }
}
