package com.warehouse.ui;

import com.warehouse.model.Order;
import com.warehouse.model.OrderStatus;
import com.warehouse.model.Product;
import com.warehouse.service.OrderService;
import com.warehouse.util.Position;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * JavaFX sidebar view providing interactive order creation, priority inspection,
 * and order lifecycle monitoring.
 */
public class OrderPanel extends VBox {

    private final OrderService orderService;
    private final Consumer<Order> onOrderCreated;
    private final Runnable onSampleOrdersRequested;

    private final TextField orderIdField;
    private final ComboBox<String> priorityComboBox;
    private final VBox orderCardsContainer;

    private final Label statPending;
    private final Label statActive;
    private final Label statCompleted;

    private static final AtomicInteger ORDER_SEQ = new AtomicInteger(100);

    public OrderPanel(OrderService orderService, Consumer<Order> onOrderCreated, Runnable onSampleOrdersRequested) {
        this.orderService = orderService;
        this.onOrderCreated = onOrderCreated;
        this.onSampleOrdersRequested = onSampleOrdersRequested;

        setSpacing(12);
        setPadding(new Insets(14));
        setPrefWidth(340);
        setMinWidth(300);
        setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8px; -fx-border-color: #334155; -fx-border-radius: 8px;");

        // 1. Header
        Label title = new Label("Order Management");
        title.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        // 2. Statistics KPI Bar
        HBox kpiBox = new HBox(8);
        kpiBox.setAlignment(Pos.CENTER);

        statPending = createKpiBadge("Pending", "0", "#f59e0b");
        statActive = createKpiBadge("Active", "0", "#0ea5e9");
        statCompleted = createKpiBadge("Done", "0", "#10b981");

        kpiBox.getChildren().addAll(statPending, statActive, statCompleted);

        // 3. Order Creation Section
        VBox formBox = new VBox(8);
        formBox.setPadding(new Insets(10));
        formBox.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 6px; -fx-border-color: #334155; -fx-border-radius: 6px;");

        Label formTitle = new Label("Create New Order");
        formTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");

        orderIdField = new TextField("ORD-" + ORDER_SEQ.incrementAndGet());
        orderIdField.setPromptText("Order ID");
        orderIdField.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-border-color: #475569; -fx-border-radius: 4px;");

        HBox formRow = new HBox(6);
        formRow.setAlignment(Pos.CENTER_LEFT);

        priorityComboBox = new ComboBox<>();
        priorityComboBox.getItems().addAll("Priority 1 (Low)", "Priority 2 (Normal)", "Priority 3 (High)", "Priority 4 (Urgent)");
        priorityComboBox.setValue("Priority 2 (Normal)");
        priorityComboBox.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-font-size: 11px;");
        HBox.setHgrow(priorityComboBox, Priority.ALWAYS);

        Button createBtn = new Button("Add Order");
        createBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4px; -fx-cursor: hand;");
        createBtn.setOnAction(e -> handleCreateOrder());

        formRow.getChildren().addAll(priorityComboBox, createBtn);

        Button sampleBtn = new Button("Quick Sample Orders");
        sampleBtn.setMaxWidth(Double.MAX_VALUE);
        sampleBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 4px; -fx-cursor: hand;");
        sampleBtn.setOnAction(e -> {
            if (this.onSampleOrdersRequested != null) {
                this.onSampleOrdersRequested.run();
                refresh();
            }
        });

        formBox.getChildren().addAll(formTitle, orderIdField, formRow, sampleBtn);

        // 4. Order List Container
        orderCardsContainer = new VBox(8);
        orderCardsContainer.setStyle("-fx-background-color: transparent;");

        ScrollPane scrollPane = new ScrollPane(orderCardsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(title, kpiBox, formBox, scrollPane);
        refresh();
    }

    private Label createKpiBadge(String label, String value, String colorHex) {
        Label badge = new Label(label + ": " + value);
        badge.setStyle("-fx-background-color: " + colorHex + "22; -fx-text-fill: " + colorHex + "; -fx-border-color: " + colorHex + "; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 3 8; -fx-font-size: 10px; -fx-font-weight: bold;");
        return badge;
    }

    private void handleCreateOrder() {
        String id = orderIdField.getText().trim();
        if (id.isEmpty()) {
            id = "ORD-" + ORDER_SEQ.incrementAndGet();
        }

        int priority = 2;
        String pVal = priorityComboBox.getValue();
        if (pVal != null && pVal.contains("4")) priority = 4;
        else if (pVal != null && pVal.contains("3")) priority = 3;
        else if (pVal != null && pVal.contains("1")) priority = 1;

        List<Product> items = List.of(new Product("P-" + id, "General Item", 1, new Position(3, 2)));
        Order newOrder = new Order(id, items, priority, new Position(1, 10));

        if (onOrderCreated != null) {
            onOrderCreated.accept(newOrder);
        } else if (orderService != null) {
            try {
                orderService.addOrder(newOrder);
            } catch (Exception ex) {
                // ignore duplicate
            }
        }

        orderIdField.setText("ORD-" + ORDER_SEQ.incrementAndGet());
        refresh();
    }

    /**
     * Refreshes orders display from OrderService.
     */
    public void refresh() {
        if (orderService == null) return;

        statPending.setText("Pending: " + orderService.getPendingOrderCount());
        statActive.setText("Active: " + orderService.getInProgressOrderCount());
        statCompleted.setText("Done: " + orderService.getCompletedOrderCount());

        orderCardsContainer.getChildren().clear();
        List<Order> allOrders = orderService.getAllOrders();

        if (allOrders.isEmpty()) {
            Label empty = new Label("No orders placed yet.");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-font-style: italic;");
            orderCardsContainer.getChildren().add(empty);
            return;
        }

        // Show highest priority and recent orders first
        List<Order> sorted = new ArrayList<>(allOrders);
        sorted.sort(OrderService.ORDER_PRIORITY_COMPARATOR);

        for (Order order : sorted) {
            VBox card = createOrderCard(order);
            orderCardsContainer.getChildren().add(card);
        }
    }

    private VBox createOrderCard(Order order) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(8));
        card.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 6px; -fx-border-color: #334155; -fx-border-radius: 6px;");

        HBox top = new HBox(6);
        top.setAlignment(Pos.CENTER_LEFT);

        Label idLbl = new Label(order.getId());
        idLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #f1f5f9;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label prioBadge = new Label("P" + order.getPriority());
        prioBadge.setStyle(getPriorityBadgeStyle(order.getPriority()));

        Label statusBadge = new Label(order.getStatus().name());
        statusBadge.setStyle(getOrderStatusBadgeStyle(order.getStatus()));

        top.getChildren().addAll(idLbl, prioBadge, spacer, statusBadge);

        String botText = (order.getAssignedRobotId() != null) ? "Robot: " + order.getAssignedRobotId() : "Awaiting robot";
        Label botLbl = new Label(botText);
        botLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        card.getChildren().addAll(top, botLbl);
        return card;
    }

    private String getPriorityBadgeStyle(int priority) {
        String color = switch (priority) {
            case 4 -> "#ef4444"; // Urgent Red
            case 3 -> "#f97316"; // High Orange
            case 2 -> "#3b82f6"; // Normal Blue
            default -> "#6b7280"; // Low Gray
        };
        return "-fx-background-color: " + color + "33; -fx-text-fill: " + color + "; -fx-border-color: " + color + "; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 1 5; -fx-font-size: 10px; -fx-font-weight: bold;";
    }

    private String getOrderStatusBadgeStyle(OrderStatus status) {
        String color = switch (status) {
            case PENDING -> "#f59e0b";
            case ASSIGNED -> "#0ea5e9";
            case IN_PROGRESS -> "#8b5cf6";
            case COMPLETED -> "#10b981";
            case CANCELLED -> "#ef4444";
        };
        return "-fx-background-color: " + color + "; -fx-text-fill: white; -fx-background-radius: 4px; -fx-padding: 2 6; -fx-font-size: 10px; -fx-font-weight: bold;";
    }
}
