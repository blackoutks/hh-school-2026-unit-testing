package ru.hh.school.unittesting.example;

import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderProcessorTest {

  @Mock
  private PaymentService paymentService;
  @InjectMocks
  private OrderProcessor orderProcessor;

  @BeforeEach
  void setUp() {
    orderProcessor.setInventory(Map.of("item1", 10));
  }

  @ParameterizedTest
  @CsvSource({
      "item1, 2, 50, 100, 8",
      "item1, 3, 40, 108, 7"
  })
  void testProcessOrder(
      String itemId,
      int quantity,
      double pricePerUnit,
      double expectedTotalPrice,
      int expectedStock
  ) {
    double totalPrice = orderProcessor.processOrder(itemId, quantity, pricePerUnit);
    int stock = orderProcessor.getStock(itemId);

    assertEquals(expectedTotalPrice, totalPrice);
    assertEquals(expectedStock, stock);
  }

  @Test
  void processOrderShouldThrowExceptionIfItemDoesNotExist() {
    var exception = assertThrows(
        IllegalArgumentException.class,
        () -> orderProcessor.processOrder("item4", 1, 10)
    );
    assertEquals("Item item4 not found in inventory.", exception.getMessage());
  }

  @Test
  void testProcessOrderWithPayment() {
    when(paymentService.processPayment(100)).thenReturn(true);

    boolean paymentResult = orderProcessor.processOrderWithPayment("item1", 2, 50);

    assertTrue(paymentResult);
    assertEquals(8, orderProcessor.getStock("item1"));
    verify(paymentService, times(1)).processPayment(100);
    verifyNoMoreInteractions(paymentService);
  }
}
