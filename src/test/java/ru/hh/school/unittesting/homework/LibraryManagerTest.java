package ru.hh.school.unittesting.homework;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibraryManagerTest {

  @Mock
  private NotificationService notificationService;

  @Mock
  private UserService userService;

  @InjectMocks
  private LibraryManager libraryManager;

  @BeforeEach
  void setUp() {
    libraryManager.addBook("book1", 1);
  }

  @Test
  void addBookIncreasesAvailableCopies() {
    assertEquals(1, libraryManager.getAvailableCopies("book1"));
  }

  @Test
  void addBookMultipleTimesIncreasesAvailableCopies() {
    libraryManager.addBook("book1", 4);
    assertEquals(5, libraryManager.getAvailableCopies("book1"));
  }

  @Test
  void getAvailableCopiesReturnsZeroForUnknownBook() {
    assertEquals(0, libraryManager.getAvailableCopies("unknown"));
  }

  @Test
  void borrowBookSuccessAndNotifyUser() {
    when(userService.isUserActive("user1")).thenReturn(true);

    boolean result = libraryManager.borrowBook("book1", "user1");

    assertTrue(result);
    assertEquals(0, libraryManager.getAvailableCopies("book1"));
    verify(notificationService).notifyUser("user1", "You have borrowed the book: book1");
  }

  @Test
  void borrowBookFailsWhenBookNotAvailable() {
    when(userService.isUserActive("user1")).thenReturn(true);

    boolean result = libraryManager.borrowBook("book2", "user1");

    assertFalse(result);
    assertEquals(1, libraryManager.getAvailableCopies("book1"));
    verify(notificationService, never()).notifyUser(anyString(), anyString());
  }

  @Test
  void borrowBookFailsAndNotifyInactiveUser() {
    when(userService.isUserActive("user1")).thenReturn(false);
    boolean result = libraryManager.borrowBook("book1", "user1");

    assertFalse(result);
    verify(notificationService).notifyUser("user1", "Your account is not active.");
  }

  @Test
  void returnBookSuccessAndNotifyUser() {
    when(userService.isUserActive("user1")).thenReturn(true);
    libraryManager.borrowBook("book1", "user1");

    boolean result = libraryManager.returnBook("book1", "user1");

    assertTrue(result);
    assertEquals(1, libraryManager.getAvailableCopies("book1"));
    verify(notificationService).notifyUser("user1", "You have returned the book: book1");
  }

  @Test
  void returnBookFailsWhenBookWasNotBorrowed() {
    boolean result = libraryManager.returnBook("book2", "user1");
    assertFalse(result);
    assertEquals(0, libraryManager.getAvailableCopies("book2"));
  }

  @Test
  void returnBookFailsWhenReturnedByDifferentUser() {
    when(userService.isUserActive("user1")).thenReturn(true);
    libraryManager.borrowBook("book1", "user1");

    boolean result = libraryManager.returnBook("book1", "user2");

    assertFalse(result);
    assertEquals(0, libraryManager.getAvailableCopies("book1"));
  }

  @ParameterizedTest
  @CsvSource({
      "0, false, false, 0.0",
      "5, false, false, 2.5",
      "5, true,  false, 3.75",
      "5, false, true,  2.0",
      "5, true,  true,  3.0",
      "3, true,  true,  1.8"
  })
  void calculateDynamicLateFeeReturnsCorrectFee(int overdueDays, boolean isBestseller, boolean isPremiumMember, double expectedFee) {
    double fee = libraryManager.calculateDynamicLateFee(overdueDays, isBestseller, isPremiumMember);
    assertEquals(expectedFee, fee, 0.001);
  }

  @Test
  void calculateDynamicLateFeeThrowsOnNegativeDays() {
    IllegalArgumentException err = assertThrows(IllegalArgumentException.class,
        () -> libraryManager.calculateDynamicLateFee(-1, false, false));
    assertEquals("Overdue days cannot be negative.", err.getMessage());
  }

}
