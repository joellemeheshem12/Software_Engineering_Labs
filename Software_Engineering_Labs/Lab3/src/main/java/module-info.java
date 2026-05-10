/**
 * Module descriptor for the Lab 2 JavaFX application.
 *
 * - Requires javafx.controls for UI components (Button, TextField, Label, etc.)
 * - Requires javafx.fxml for loading .fxml layout files at runtime
 * - Opens the package to javafx.fxml so FXML can reflectively access controllers
 * - Exports the package so JavaFX runtime can instantiate the Application subclass
 */
module com.swelabs.gcmlogin {
    requires javafx.controls;
    requires javafx.fxml;

    // Allow FXML loader to access controller classes via reflection
    opens com.swelabs.lab2 to javafx.fxml;

    // Export the main package so the JavaFX launcher can find Main
    exports com.swelabs.lab2;
}
