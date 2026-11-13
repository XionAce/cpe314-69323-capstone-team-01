package edu.uc.campusmarket.ui;

import edu.uc.campusmarket.controller.AnalyticsController;
import edu.uc.campusmarket.controller.TaskRunner;
import edu.uc.campusmarket.model.AnalyticsSnapshot;
import edu.uc.campusmarket.model.CategorySummary;
import edu.uc.campusmarket.model.WeeklySales;
import java.util.List;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Renders service results; aggregation is never performed by chart code. */
public class ChartsView extends ScrollPane {
    private final BarChart<String, Number> prices = chart("Average listing price by category",
            "Category", "Price (PHP)");
    private final BarChart<String, Number> ratings = chart("Average review rating by category",
            "Category", "Rating (1-5)");
    private final BarChart<String, Number> weeks = chart("Historical sales per week",
            "Week starting Monday (UTC)", "Sales");
    private final Label activity = UiTools.label("", "muted");
    private final Label ratingSummary = UiTools.label("", "muted");

    /** Creates this component from the supplied values or dependencies. */
    public ChartsView(AnalyticsController analytics, TaskRunner tasks) {
        ComboBox<String> currency = new ComboBox<>();
        currency.setId("analytics-currency");
        currency.getItems().setAll("PHP", "USD", "EUR");
        currency.setEditable(true);
        currency.setValue("PHP");
        currency.getEditor().textProperty().bindBidirectional(analytics.currencyProperty());
        Button refresh = UiTools.button("Refresh analytics", "analytics-button", tasks,
                analytics::refresh);
        prices.setId("price-chart");
        ratings.setId("rating-chart");
        weeks.setId("weekly-sales-chart");
        NumberAxis ratingAxis = (NumberAxis) ratings.getYAxis();
        ratingAxis.setAutoRanging(false);
        ratingAxis.setLowerBound(0);
        ratingAxis.setUpperBound(5);
        ratingAxis.setTickUnit(1);
        ((NumberAxis) weeks.getYAxis()).setTickUnit(1);
        analytics.snapshotProperty().addListener(
                (o, a, snapshot) -> show(snapshot, analytics.currencyProperty().get()));
        VBox body = new VBox(12, new HBox(10, new Label("Price currency:"), currency, refresh),
                weeks, activity, prices, ratings, ratingSummary);
        body.getStyleClass().add("page-content");
        setContent(body);
        setFitToWidth(true);
        getStyleClass().add("page-scroll");
    }

    private void show(AnalyticsSnapshot snapshot, String currency) {
        prices.getData().clear();
        ratings.getData().clear();
        weeks.getData().clear();
        if (snapshot == null) {
            activity.setText("");
            ratingSummary.setText("");
            return;
        }
        prices.setTitle("Average listing price by category"
                + (snapshot.prices().isEmpty() ? " - no data" : ""));
        ratings.setTitle("Average review rating by category"
                + (snapshot.ratings().isEmpty() ? " - no reviews" : ""));
        weeks.setTitle(
                "Historical sales per week" + (snapshot.weeks().isEmpty() ? " - no history" : ""));
        prices.getYAxis()
                .setLabel("Price (" + currency.strip().toUpperCase(java.util.Locale.ROOT) + ")");
        fill(prices, snapshot.prices());
        fill(ratings, snapshot.ratings());
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (WeeklySales point : snapshot.weeks()) {
            series.getData()
                    .add(new XYChart.Data<>(point.weekStarting().toString(), point.count()));
        }
        weeks.getData().add(series);
        colorBars(series);
        NumberAxis counts = (NumberAxis) weeks.getYAxis();
        int largest = snapshot.weeks().stream().mapToInt(WeeklySales::count).max().orElse(1);
        counts.setAutoRanging(false);
        counts.setLowerBound(0);
        counts.setUpperBound(Math.max(1, Math.ceil(largest * 1.2)));
        counts.setTickUnit(Math.max(1, Math.ceil(largest / 6.0)));
        counts.setMinorTickVisible(false);
        activity.setText(snapshot.activitySummary());
        if (snapshot.ratings().isEmpty()) {
            ratingSummary.setText("No campus reviews yet. Categories without reviews are unrated.");
        } else {
            double best = snapshot.ratings().getFirst().average();
            StringBuilder text = new StringBuilder("Best-rated: ");
            for (CategorySummary row : snapshot.ratings()) {
                if (Double.compare(best, row.average()) == 0) {
                    text.append(row.category()).append(" ")
                            .append(String.format(java.util.Locale.ROOT, "%.2f", best))
                            .append("/5 (").append(row.count()).append(" reviews)  ");
                }
            }
            ratingSummary.setText(text.toString());
        }
    }

    private void fill(BarChart<String, Number> chart, List<CategorySummary> rows) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (CategorySummary row : rows) {
            series.getData().add(new XYChart.Data<>(row.category(), row.average()));
        }
        chart.getData().add(series);
        colorBars(series);
    }

    private void colorBars(XYChart.Series<String, Number> series) {
        String[] palette = { "#8575e8", "#3457a5", "#ba729d", "#299583", "#d99549" };
        for (int i = 0; i < series.getData().size(); i++) {
            var point = series.getData().get(i);
            String color = palette[i % palette.length];
            java.util.function.Consumer<javafx.scene.Node> paint = node -> {
                if (node != null) {
                    node.setStyle("-fx-bar-fill: " + color + "; -fx-background-radius: 5 5 0 0;");
                    javafx.scene.control.Tooltip.install(node, new javafx.scene.control.Tooltip(
                            point.getXValue() + ": " + point.getYValue()));
                }
            };
            point.nodeProperty().addListener((o, before, node) -> paint.accept(node));
            paint.accept(point.getNode());
        }
    }

    private static BarChart<String, Number> chart(String title, String xLabel, String yLabel) {
        CategoryAxis x = new CategoryAxis();
        x.setLabel(xLabel);
        NumberAxis y = new NumberAxis();
        y.setLabel(yLabel);
        y.setForceZeroInRange(true);
        BarChart<String, Number> chart = new BarChart<>(x, y);
        chart.setTitle(title);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(360);
        chart.setMinHeight(360);
        chart.setCategoryGap(40);
        x.categorySpacingProperty().addListener((o, before, spacing) ->
                chart.setCategoryGap(Math.max(18, spacing.doubleValue() * .65)));
        chart.setBarGap(12);
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(false);
        return chart;
    }
}
