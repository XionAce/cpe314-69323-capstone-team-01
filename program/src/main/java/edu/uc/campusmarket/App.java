package edu.uc.campusmarket;

import edu.uc.campusmarket.persistence.Database;
import edu.uc.campusmarket.service.MarketplaceService;
import edu.uc.campusmarket.ui.MarketplaceView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Starts JavaFX and connects the screen, service and database. */
public class App extends Application {
    private MarketplaceView view;

    @Override
    public void start(Stage stage) {
        Database database = new Database(Database.defaultPath());
        MarketplaceService service = new MarketplaceService(database);
        view = new MarketplaceView(service);
        Scene scene = new Scene(view.getRoot(), 960, 660);
        scene.getStylesheets().add(App.class.getResource("/ui/marketplace.css").toExternalForm());
        stage.setTitle("UC-B Campus Marketplace - Milestone 1");
        stage.setMinWidth(900);
        stage.setMinHeight(620);
        stage.setScene(scene);
        stage.show();
        view.initialize();
    }

    @Override
    public void stop() {
        if (view != null) view.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
