package ru.hh.school.unittesting.example;

import java.util.HashMap;
import java.util.Map;

public class OrderProcessor {

  private final PaymentService paymentService;
  private Map<String, Integer> inventory = new HashMap<>();

  public OrderProcessor(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  public void setInventory(Map<String, Integer> inventory) {
    this.inventory = new HashMap<>(inventory);
  }

  public double processOrder(String itemId, int quantity, double pricePerUnit) {
    if (!inventory.containsKey(itemId)) {
      throw new IllegalArgumentException("Item %s not found in inventory.".formatted(itemId));
    }

    int availableStock = inventory.get(itemId);
    if (quantity > availableStock) {
      throw new IllegalArgumentException("Insufficient stock for item: " + itemId);
    }

    double totalPrice = quantity * pricePerUnit;
    if (totalPrice > 100) {
      totalPrice *= 0.9;
    }

    inventory.put(itemId, availableStock - quantity);

    return totalPrice;
  }

  public int getStock(String itemId) {
    return inventory.getOrDefault(itemId, 0);
  }

  public boolean processOrderWithPayment(String itemId, int quantity, double pricePerUnit) {
    double totalPrice = processOrder(itemId, quantity, pricePerUnit);
    return paymentService.processPayment(totalPrice);
  }
}
