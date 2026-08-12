# Context Vault

Context Vault is a Java 21 + JavaFX desktop application for saving and restoring developer work context.

## Features

- Save project contexts with name, folder, branch, commands, and notes
- Browse and search saved contexts instantly
- Edit or delete existing contexts
- Restore a context by opening the folder, launching the configured editor, opening a terminal, and showing saved commands and notes
- Persist user settings for editor, terminal, and theme
- SQLite-backed local storage with sample data on first launch

## Tech Stack

- Java 21
- JavaFX
- Maven
- SQLite with JDBC
- Jackson
- Ikonli
- SLF4J + Logback

## Run

### Prerequisites

- Java 21
- Maven 3.9+

### Launch

```bash
mvn javafx:run
```

### Package / Compile

```bash
mvn clean compile
```

## Project Structure

```text
src/main/java
├── app
├── config
├── controllers
├── database
├── models
├── repository
├── services
├── utils
└── viewmodels

src/main/resources
├── css
├── fxml
├── icons
└── images
```

## Restore Behavior

When you click **Open**, Context Vault automatically:

- Detects and validates the saved project folder
- Detects the current Git branch and switches to the saved branch when possible
- Opens the project folder in Explorer
- Opens the project in VS Code
- Opens your configured terminal in the project folder
- Starts saved commands in that terminal

If VS Code is unavailable, the app falls back to your configured editor from Settings.

## Notes

- The SQLite database is created automatically in the user application data directory on first run.
- Sample contexts are inserted automatically when the database is first initialized and empty.
