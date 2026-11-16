package edu.uc.campusmarket.ui;

import edu.uc.campusmarket.controller.AnalyticsController;
import edu.uc.campusmarket.controller.TaskRunner;
import edu.uc.campusmarket.model.AnalyticsSnapshot;
import edu.uc.campusmarket.model.CategorySummary;
import edu.uc.campusmarket.model.WeeklySales;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Displays saved analytics, with one chart and its exact breakdown at a time.
 */
public class ChartsView extends ScrollPane {
    private final BarChart<String, Number> prices = chart("Average listing price by category",
            "Category", "Price (PHP)");
    private final BarChart<String, Number> ratings = chart("Average review rating by category",
            "Category", "Rating (out of 5)");
    private final BarChart<String, Number> weeks = chart("Historical sales per week",
            "Week starting Monday (UTC)", "Sales");
    private final Label activity = UiTools.label("", "muted");
    private final Label ratingSummary = UiTools.label("", "muted");
    private final Label salesCount = UiTools.label("0", "stat-value");
    private final Label listingCount = UiTools.label("0", "stat-value");
    private final Label reviewCount = UiTools.label("0", "stat-value");
    private final Label breakdownTitle = UiTools.label("Weekly breakdown", "section-title");
    private final VBox rows = new VBox(16);
    private final VBox breakdown = new VBox(18, breakdownTitle, rows);
    private AnalyticsSnapshot current;
    private String metric = "weeks";
    private String selectedCurrency = "PHP";

    /**
     * Keeps all three real datasets available while simplifying the visible chart.
     */
    public ChartsView(AnalyticsController analytics, TaskRunner tasks) {
        ComboBox<String> currency = new ComboBox<>();
        currency.setId("analytics-currency");
        currency.getItems().setAll("PHP", "USD", "EUR");
        currency.setEditable(true);
        currency.setValue("PHP");
        currency.setPrefWidth(120);
        currency.getEditor().textProperty().bindBidirectional(analytics.currencyProperty());
        Button refresh = UiTools.button("Refresh analytics", "analytics-button", tasks,
                analytics::refresh);
        HBox controls = new HBox(10, UiTools.fieldLabel("Price currency", currency), currency,
                refresh);
        controls.setAlignment(Pos.CENTER_LEFT);
        prices.setId("price-chart");
        ratings.setId("rating-chart");
        weeks.setId("weekly-sales-chart");
        NumberAxis ratingAxis = (NumberAxis) ratings.getYAxis();
        ratingAxis.setAutoRanging(false);
        ratingAxis.setLowerBound(0);
        ratingAxis.setUpperBound(5);
        ratingAxis.setTickUnit(1);
        FlowPane choices = choices();
        VBox chartPanel = new VBox(14, choices, new StackPane(weeks, prices, ratings), activity,
                ratingSummary);
        chartPanel.getStyleClass().add("analytics-main");
        breakdown.setId("analytics-breakdown");
        breakdown.getStyleClass().addAll("card", "breakdown-card");
        FlowPane layout = new FlowPane(20, 20, chartPanel, breakdown);
        viewportBoundsProperty()
                .addListener((o, before, bounds) -> arrange(bounds.getWidth(), chartPanel));
        FlowPane stats = new FlowPane(16, 16, stat("Imported sales", salesCount),
                stat("Active listings in selected currency", listingCount),
                stat("Reviews on active listings", reviewCount), controls);
        stats.setAlignment(Pos.CENTER_LEFT);
        VBox body = new VBox(18,
                UiTools.label("Understand what is listed, reviewed and sold. "
                        + "Every figure below comes from saved marketplace data.", "muted"),
                stats, layout);
        body.getStyleClass().add("page-content");
        setContent(body);
        setFitToWidth(true);
        getStyleClass().add("page-scroll");
        analytics.snapshotProperty().addListener(
                (o, before, snapshot) -> show(snapshot, analytics.currencyProperty().get()));
        select("weeks");
    }

    private FlowPane choices() {
        ToggleGroup group = new ToggleGroup();
        FlowPane buttons = new FlowPane(8, 8);
        String[][] modes = { { "weeks", "Weekly sales" }, { "prices", "Listing prices" },
                { "ratings", "Review ratings" } };
        for (String[] mode : modes) {
            ToggleButton button = new ToggleButton(mode[1]);
            button.setId("analytics-mode-" + mode[0]);
            button.setToggleGroup(group);
            button.setSelected(mode[0].equals(metric));
            button.setOnAction(event -> {
                button.setSelected(true);
                select(mode[0]);
            });
            buttons.getChildren().add(button);
        }
        return buttons;
    }

    private void arrange(double width, VBox chartPanel) {
        double available = Math.max(260, width - 58);
        boolean wide = available >= 850;
        chartPanel.setPrefWidth(wide ? available - 320 : available);
        breakdown.setPrefWidth(wide ? 300 : available);
    }

    private VBox stat(String title, Label value) {
        VBox box = new VBox(8, UiTools.label(title, "muted"), value);
        box.setPrefWidth(200);
        box.getStyleClass().add("stat-card");
        return box;
    }

    private void select(String name) {
        metric = name;
        for (var chart : List.of(weeks, prices, ratings)) {
            boolean visible = chart == (name.equals("weeks") ? weeks
                    : name.equals("prices") ? prices : ratings);
            chart.setVisible(visible);
            chart.setManaged(visible);
        }
        activity.setVisible(name.equals("weeks"));
        activity.managedProperty().bind(activity.visibleProperty());
        ratingSummary.setVisible(name.equals("ratings"));
        ratingSummary.managedProperty().bind(ratingSummary.visibleProperty());
        breakdown();
    }

    private void show(AnalyticsSnapshot snapshot, String currency) {
        current = snapshot;
        selectedCurrency = currency.strip().toUpperCase(Locale.ROOT);
        prices.getData().clear();
        ratings.getData().clear();
        weeks.getData().clear();
        salesCount.setText("0");
        listingCount.setText("0");
        reviewCount.setText("0");
        if (snapshot == null) {
            activity.setText("");
            ratingSummary.setText("");
            breakdown();
            return;
        }
        prices.setTitle("Average listing price by category"
                + (snapshot.prices().isEmpty() ? " — no listings" : ""));
        ratings.setTitle("Average review rating by category"
                + (snapshot.ratings().isEmpty() ? " — no reviews" : ""));
        weeks.setTitle(
                "Historical sales per week" + (snapshot.weeks().isEmpty() ? " — no history" : ""));
        prices.getYAxis().setLabel("Price (" + selectedCurrency + ")");
        fill(prices, snapshot.prices());
        fill(ratings, snapshot.ratings());
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (WeeklySales point : snapshot.weeks()) {
            series.getData()
                    .add(new XYChart.Data<>(point.weekStarting().toString(), point.count()));
        }
        weeks.getData().add(series);
        colorBars(series);
        counts(snapshot);
        salesCount.setText(
                String.valueOf(snapshot.weeks().stream().mapToLong(WeeklySales::count).sum()));
        listingCount.setText(
                String.valueOf(snapshot.prices().stream().mapToLong(CategorySummary::count).sum()));
        reviewCount.setText(String
                .valueOf(snapshot.ratings().stream().mapToLong(CategorySummary::count).sum()));
        activity.setText(snapshot.activitySummary());
        ratingSummary.setText(
                "Each review is weighted equally. Unreviewed categories are unrated, not zero stars.");
        breakdown();
    }

    private void counts(AnalyticsSnapshot snapshot) {
        NumberAxis counts = (NumberAxis) weeks.getYAxis();
        int largest = snapshot.weeks().stream().mapToInt(WeeklySales::count).max().orElse(1);
        counts.setAutoRanging(false);
        counts.setLowerBound(0);
        counts.setUpperBound(Math.max(1, Math.ceil(largest * 1.2)));
        counts.setTickUnit(Math.max(1, Math.ceil(largest / 6.0)));
        counts.setMinorTickVisible(false);
    }

    private void breakdown() {
        rows.getChildren().clear();
        breakdownTitle.setText(metric.equals("weeks") ? "Sales by week"
                : metric.equals("prices") ? "Category price breakdown"
                        : "Category rating breakdown");
        if (current == null) {
            rows.getChildren().add(UiTools.label("Load analytics to see saved data.", "muted"));
            return;
        }
        if (metric.equals("weeks")) {
            long total = current.weeks().stream().mapToLong(WeeklySales::count).sum();
            DateTimeFormatter date = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
            for (WeeklySales point : current.weeks()) {
                double share = total == 0 ? 0 : point.count() / (double) total;
                rows.getChildren()
                        .add(row(point.weekStarting().format(date), point.count() + " sales",
                                String.format(Locale.ROOT, "%.0f%% of imported sales", share * 100),
                                share));
            }
        } else {
            categoryRows(metric.equals("prices") ? current.prices() : current.ratings());
        }
        if (rows.getChildren().isEmpty()) {
            rows.getChildren()
                    .add(UiTools.label(
                            metric.equals("weeks") ? "Import sales history to see weekly activity."
                                    : "No matching saved data yet.",
                            "muted"));
        }
    }

    private void categoryRows(List<CategorySummary> categories) {
        boolean price = metric.equals("prices");
        double maximum = price
                ? categories.stream().mapToDouble(CategorySummary::average).max().orElse(1)
                : 5;
        for (CategorySummary category : categories) {
            String value = price
                    ? selectedCurrency + " "
                            + String.format(Locale.ROOT, "%,.2f", category.average())
                    : String.format(Locale.ROOT, "%.2f / 5", category.average());
            rows.getChildren()
                    .add(row(category.category(), value,
                            category.count() + (price ? " active listings · average asking price"
                                    : " reviews · average rating"),
                            maximum == 0 ? 0 : category.average() / maximum));
        }
        if (price && !categories.isEmpty()) {
            rows.getChildren()
                    .add(UiTools.label("Bars compare category averages with the highest average. "
                            + "Currencies are kept separate.", "muted"));
        }
    }

    private VBox row(String name, String value, String note, double proportion) {
        Label title = UiTools.label(name, "breakdown-name");
        HBox.setHgrow(title, Priority.ALWAYS);
        title.setMaxWidth(Double.MAX_VALUE);
        Label number = UiTools.label(value, "breakdown-value");
        HBox heading = new HBox(8, title, number);
        ProgressBar bar = new ProgressBar(proportion);
        bar.setMaxWidth(Double.MAX_VALUE);
        return new VBox(7, heading, bar, UiTools.label(note, "muted"));
    }

    private void fill(BarChart<String, Number> chart, List<CategorySummary> values) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (CategorySummary row : values) {
            series.getData().add(new XYChart.Data<>(row.category(), row.average()));
        }
        chart.getData().add(series);
        colorBars(series);
    }

    private void colorBars(XYChart.Series<String, Number> series) {
        String[] palette = { "#087f86", "#123c64", "#c49116", "#3d9a9b", "#547b9c" };
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
        y.setMinorTickVisible(false);
        BarChart<String, Number> chart = new BarChart<>(x, y);
        chart.setTitle(title);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(300);
        chart.setMinHeight(280);
        chart.setMinWidth(0);
        chart.setCategoryGap(40);
        x.categorySpacingProperty().addListener((o, before, spacing) -> chart.setCategoryGap(
                Math.max(18, spacing.doubleValue() - Math.min(80, spacing.doubleValue() * .45))));
        chart.setBarGap(12);
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(false);
        return chart;
    }
}
